package com.example.myapplication.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.R
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.NetworkResponseCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException

class PlayingViewModel(private val app: Application) : ViewModel() {
    private val repo = MainActivityRepository.getInstance()

    private var _listRecommend = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val listRecommend: LiveData<List<Song>> = _listRecommend
    private var _playlists = MutableLiveData<List<Playlist>>().apply { value = emptyList() }
    val playlists: LiveData<List<Playlist>> = _playlists

    private var _isEmptyList = MutableLiveData<Boolean>(false)
    val isEmptyList: LiveData<Boolean> = _isEmptyList

    private var _curlist = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val curList: LiveData<List<Song>> = _curlist

    private var _curSong = MutableLiveData<Song>()
    val curSong: LiveData<Song> = _curSong

    private var _namePlaylist = MutableLiveData<String>()
    val namePlaylist: LiveData<String> = _namePlaylist

    private var _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private var _progressBar = MutableLiveData<Boolean>(false)
    val progressBar: LiveData<Boolean> = _progressBar

    private var _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying
    private var _infor = MutableLiveData<String>()
    val infor: LiveData<String> = _infor

    var isFavoriteSong = MutableLiveData<Boolean>()
    var isRepeat = MutableLiveData<Boolean>()
    var isShuffle = MutableLiveData<Boolean>()
    private var musicService: MusicService? = null
    private var isBound = false
    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "updatePosition") {
                    val value = it.getIntExtra("value", 0)
                    updateSeekBar(value)
                    Log.e("pos", value.toString())
                }
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", 0)
                    when (value) {
                        Contains.ACTION_CHANGE_SONG -> {
                            musicService?.isPlaying()?.let { it1 -> getPlaybackState() }
                            musicService?.let { service ->
                                setNewSong(service.cursong!!)

                            }
                        }
                        else -> musicService?.isPlaying()
                            ?.let { it1 -> getPlaybackState() }
                    }
                }
            }
        }
    }
    val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MusicService.Mybind
            musicService = binder.getInstance()
            isBound = true
            musicService?.let {
                getRepeat()
                getShuffle()
                getPlaybackState()
                getCurSong()
                it.updateDuration()
                updateSeekBar(it.getMediaCurrentPos())
//
//            it.isPlaying().let {
//                playingVm.setPlaybackState(it)
//            }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    init {
        val intent = Intent(app, MusicService::class.java)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        val filter = IntentFilter("fromNotifyToActivity")
        filter.addAction("updatePosition")
        LocalBroadcastManager.getInstance(app).registerReceiver(broadcast, filter)
    }

    override fun onCleared() {
        super.onCleared()
        app.unbindService(connection)
        LocalBroadcastManager.getInstance(app).unregisterReceiver(broadcast)
    }

    fun checkIsFavou(song: Song) {
        repo.isFavouriteSong(app, song, object : NetworkResponseCallback<Boolean> {
            override fun onNetworkSuccess(data: List<Boolean>?) {
                isFavoriteSong.value = data?.get(0)
            }

            override fun onNetworkFailure(error: String) {
            }
        })
    }

    var curPos = MutableLiveData<Int>()
    fun updateSeekBar(value: Int) {
        curPos.value = value
    }

    fun addFavorite(song: Song) {
        val songFavourite =
            song.thumbnail?.let {
                SongFavourite(
                    song.artists_names,
                    song.duration,
                    song.id,
                    it,
                    song.title
                )
            }
        val db = SongDatabase.getInstance(app)
        CoroutineScope(Dispatchers.IO).launch {
            if (songFavourite != null) {
                val id = songFavourite.id
                val isExists = db.getDao().isExist(id)
                if (isExists) {
                    db.getDao().deleteById(id)
                    withContext(Dispatchers.Main) {
                        _message.value = "Remove Favourite List"
                        isFavoriteSong.value = false
                        //binding.ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
                    }
                } else {
                    db.getDao().insert(songFavourite)
                    withContext(Dispatchers.Main) {
                        _message.value = "Add Favourite List"
                        isFavoriteSong.value = true
                        //binding.ivFavourite.setImageResource(R.drawable.ic_heart_checked)
                    }
                }
            }
        }
    }

    fun getInfor(song: Song) {
        if (!song.isOffline) {
            repo.getInfoSong(song, object : NetworkResponseCallback<String> {
                override fun onNetworkSuccess(data: List<String>?) {
                    _infor.value = data?.get(0)
                }

                override fun onNetworkFailure(error: String) {
                    _infor.value = error
                }
            })
        } else _infor.value = "Local Song"
    }

    fun setNewPlaylist(list: List<Song>, name: String) {
        musicService?.setPlaylist(list,name)
        setNewSong(list[0])
        musicService?.playSong()
    }

    fun downloadSong(song: Song) {
        repo.downloadSong(song, app, object : NetworkResponseCallback<Song> {
            override fun onNetworkSuccess(data: List<Song>?) {
                _message.value = "Watting Download"
            }
            override fun onNetworkFailure(error: String) {
                _message.value = error
            }
        })
    }
    fun getPlaylists() {
        repo.getlistPlaylist(app, object : NetworkResponseCallback<Playlist> {
            override fun onNetworkSuccess(data: List<Playlist>?) {
                _playlists.value = data
            }
            override fun onNetworkFailure(error: String) {
            }
        })
    }
    fun addSongtoPlaylist(playlist: Playlist,song:Song){
        repo.addSongtoPlaylist(playlist,song,app,object :NetworkResponseCallback<Song>{
            override fun onNetworkSuccess(data: List<Song>?) {
                _message.value = "Add ${song.title} to ${playlist.playlistName} playlist success"
            }
            override fun onNetworkFailure(error: String) {
                _message.value = error
            }
        })
    }
    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    fun nextSong() {
        musicService?.nextSong()
    }

    fun prevSong() {
        musicService?.prevSong()
    }

    fun setRepeat() {
        musicService?.setRepeat()
        isRepeat.value = musicService?.repeat
    }

    fun getRepeat() {
        isRepeat.value = musicService?.repeat
    }

    fun setShuffle() {
        musicService?.setShuffle()
        isShuffle.value = musicService?.shuffle
    }

    fun getShuffle() {
        isShuffle.value = musicService?.shuffle
    }

    fun setNewSong(song: Song) {
        _curSong.value = song
        musicService?.setNewSong(song)
    }

    fun getCurSong() {
        _curSong.value = musicService?.cursong
    }

    fun getPlaybackState() {
        _isPlaying.value = musicService?.isPlaying()
    }

    fun playSong() {
        musicService?.playSong()
    }

    fun getMediaCurrentPos(): Int {
        return musicService?.getMediaCurrentPos() ?: 0
    }

    fun seekTo(newPos: Int) {
        musicService?.seekTo(newPos)
    }

    class PlayingViewmodelFactory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PlayingViewModel(app) as T
            }
            throw IllegalArgumentException("Unable construct")
        }
    }

}