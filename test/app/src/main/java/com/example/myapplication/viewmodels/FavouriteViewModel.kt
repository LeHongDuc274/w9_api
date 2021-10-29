package com.example.myapplication.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.NetworkResponseCallback
import java.lang.IllegalArgumentException

class FavouriteViewModel(private val app: Application) : ViewModel() {
    private val repo = MainActivityRepository.getInstance()
    private var _listFavourite = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val listFavourite: LiveData<List<Song>> = _listFavourite

    private var _isEmptyList = MutableLiveData<Boolean>(false)
    val isEmptyList: LiveData<Boolean> = _isEmptyList

    private var _progressBar = MutableLiveData<Boolean>(false)
    val progressBar: LiveData<Boolean> = _progressBar

    private var _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun fetchFavouriteSong() {
        _progressBar.value = true
        repo.getFavouritePlaylist(app.applicationContext, object : NetworkResponseCallback<Song> {
            override fun onNetworkSuccess(data: List<Song>?) {
                if (data.isNullOrEmpty()) {
                    _isEmptyList.value = true
                } else {
                    _listFavourite.value = data
                }
                _progressBar.value = false
            }

            override fun onNetworkFailure(error: String) {
                _progressBar.value = false
            }
        })
    }

    fun removeFavourite(song: Song) {
        repo.removeFavourite(app, song, object : NetworkResponseCallback<Song> {
            override fun onNetworkSuccess(data: List<Song>?) {
                _message.value = "Remove Succesfully"
            }
            override fun onNetworkFailure(error: String) {
            }
        })
    }

    class FavouriteViewmodelFactory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavouriteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FavouriteViewModel(app) as T
            }
            throw IllegalArgumentException("Unable construct")
        }
    }
}