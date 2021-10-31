package com.example.myapplication.viewmodels

import android.app.Application
import android.content.*
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.activities.MainActivity
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.checkNetWorkAvailable
import com.example.myapplication.utils.NetworkResponseCallback
import java.lang.IllegalArgumentException

class MainViewModel(private val app: Application) : ViewModel() {
    private val repo = MainActivityRepository.getInstance()
    private var _listTopSong = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val listTopSong: LiveData<List<Song>> = _listTopSong

    private var _listSearch = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val listSearch: LiveData<List<Song>> = _listSearch

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
    private var musicService: MusicService? = null
    private var isBound = false
    var internetState = MutableLiveData<Boolean>(true)

    val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MusicService.Mybind
            musicService = binder.getInstance()
            musicService?.internetConnected = Contains.checkNetWorkAvailable(app)
            musicService?.isPlaying()?.let {
                getPlaybackState()
            }
            musicService?.cursong?.let { it1 -> setNewSong(it1) }
            isBound = true
        }
        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", -1)
                    when (value) {
                        Contains.ACTION_CHANGE_SONG -> {
                            musicService?.cursong?.let { it1 -> setNewSong(it1) }
                            musicService?.isPlaying()?.let { it1 -> getPlaybackState() }
                        }
                        else -> musicService?.isPlaying()?.let { it1 -> getPlaybackState() }
                    }
                }
            }
        }
    }
        val broadcastInternet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    var state = checkNetWorkAvailable(app)
                    musicService?.internetConnected = state
                    internetState.value = state
                    _message.value =  if(state) "Internet Connected" else "No Internet"
                    if (state) fetchTopSong()
                }
            }
        }
    }
    init {
        val intent = Intent(app, MusicService::class.java)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        val filter = IntentFilter("fromNotifyToActivity")
        LocalBroadcastManager.getInstance(app).registerReceiver(
            broadcast,
            filter
        )
        val filter2 = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        app.registerReceiver(broadcastInternet, filter2)
    }

    override fun onCleared() {
        super.onCleared()
        LocalBroadcastManager.getInstance(app).unregisterReceiver(broadcast)
        app.unregisterReceiver(broadcastInternet)
        if (isBound) app.unbindService(connection)

    }

    fun fetchTopSong() {
        _progressBar.value = true
        repo.getTopSong(object : NetworkResponseCallback<Song> {
            override fun onNetworkSuccess(data: List<Song>?) {
                if (data.isNullOrEmpty()) {
                    _isEmptyList.value = true
                } else {
                    _listTopSong.value = data
                }
                _progressBar.value = false
            }
            override fun onNetworkFailure(error: String) {
                _progressBar.value = false
            }
        })
    }

    fun getSearchResult(text: String) {
        _progressBar.value = true
        repo.getSearchResult(text, object : NetworkResponseCallback<Song> {
            override fun onNetworkSuccess(data: List<Song>?) {
                _listSearch.value = data
                _progressBar.value = false
            }

            override fun onNetworkFailure(error: String) {
                _progressBar.value = false
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

    fun createNewPlaylist(str:String){
        repo.createNewPlaylist(app,str,object :NetworkResponseCallback<Playlist>{
            override fun onNetworkSuccess(data: List<Playlist>?) {
                _playlists.value = data
                _message.value = "Add playlist " + str.uppercase()
            }
            override fun onNetworkFailure(error: String) {
                _message.value = error
            }
        })
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

    fun setNewPlaylist(list: List<Song>, name: String) {
        musicService?.setPlaylist(list,name)
        setNewSong(list[0])
        musicService?.playSong()
    }
    fun playSong(){
        musicService?.playSong()
    }
    fun togglePlayPause(){
        musicService?.togglePlayPause()
    }
    fun setNewSong(song: Song) {
        _curSong.value = song
        musicService?.setNewSong(song)
    }
    fun getCursong(){
        _curSong.value = musicService?.cursong
    }
    fun getPlaybackState(){
        _isPlaying.value = musicService?.isPlaying()
    }

    class MainViewmodelFactory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
            throw IllegalArgumentException("Unable construct")
        }
    }
}