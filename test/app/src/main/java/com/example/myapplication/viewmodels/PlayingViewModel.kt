package com.example.myapplication.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.NetworkResponseCallback
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

    fun checkIsFavou(song: Song) {
        repo.isFavouriteSong(app,song, object : NetworkResponseCallback<Boolean> {
            override fun onNetworkSuccess(data: List<Boolean>?) {
                isFavoriteSong.value = data?.get(0)
            }
            override fun onNetworkFailure(error: String) {
            }
        })
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

    fun setNewPlaylist(list : List<Song> ,name: String) {
        // offline có cách phát media khác . các kiểu còn lại đều là phát online
        _namePlaylist.value = name
        _curlist.value = list
        _curlist.value?.get(0)?.let { setNewSong(it) }
    }
    fun setNewSong(song: Song) {
        _curSong.value = song
    }
    fun setPlaybackState(b: Boolean){
        _isPlaying.value = b
    }

    fun downloadSong(song: Song){
        repo.downloadSong(song,app,object :NetworkResponseCallback<Song>{
            override fun onNetworkSuccess(data: List<Song>?) {
                _message.value = "Watting Download"
            }

            override fun onNetworkFailure(error: String) {
                _message.value = error
            }
        })
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