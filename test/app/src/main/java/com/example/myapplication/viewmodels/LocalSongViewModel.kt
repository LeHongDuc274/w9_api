package com.example.myapplication.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.NetworkResponseCallback
import java.lang.IllegalArgumentException

class LocalSongViewModel(private val app: Application):ViewModel() {
    private val repo = MainActivityRepository.getInstance()
    private var _progressBar = MutableLiveData<Boolean>(false)
    val progressBar: LiveData<Boolean> = _progressBar

    private var _listLocalSong = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val lisLocalSong: LiveData<List<Song>> = _listLocalSong
    private var _isEmptyList = MutableLiveData<Boolean>(false)
    val isEmptyList: LiveData<Boolean> = _isEmptyList
    fun fetchLocalSong() {
        _progressBar.value = true
        repo.getLocalSong(app.applicationContext, object : NetworkResponseCallback<Song> {
            override fun onNetworkSuccess(data: List<Song>?) {
                if (data.isNullOrEmpty()) {
                    _isEmptyList.value = true
                } else {
                    _listLocalSong.value = data
                }
                _progressBar.value = false
            }

            override fun onNetworkFailure(error: String) {
                _progressBar.value = false
            }
        })
    }

    class LocalViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocalSongViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LocalSongViewModel(app) as T
            }
            throw IllegalArgumentException("Unable construct")
        }
    }

}