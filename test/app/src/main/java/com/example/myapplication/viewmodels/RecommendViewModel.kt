package com.example.myapplication.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.NetworkResponseCallback
import java.lang.IllegalArgumentException

class RecommendViewModel(private val app: Application): ViewModel() {
    private val repo = MainActivityRepository.getInstance()

    private var _listRecommed = MutableLiveData<List<Song>>().apply { value = emptyList() }
    val listRecommend: LiveData<List<Song>> = _listRecommed

    private var _isEmptyList = MutableLiveData<Boolean>(false)
    val isEmptyList: LiveData<Boolean> = _isEmptyList

    private var _progressBar = MutableLiveData<Boolean>(false)
    val progressBar: LiveData<Boolean> = _progressBar

    private var _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun getRecommendSong(song: Song){
        if(!song.isOffline){
            _progressBar.value = true
            repo.getRecommendSong(song, object : NetworkResponseCallback<Song> {
                override fun onNetworkSuccess(data: List<Song>?) {
                    _listRecommed.value = data
                    _progressBar.value = false
                }

                override fun onNetworkFailure(error: String) {
                    _message.value = error
                    _progressBar.value = false
                }
            })
        } else{
            _message.value = "Local Song....."
            _progressBar.value = false
        }
    }

    class RecommnedFactory(private val  app: Application) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass.isAssignableFrom(RecommendViewModel::class.java)){
                @Suppress("UNCHECKED_CAST")
                return RecommendViewModel(app) as T
            }
            throw IllegalArgumentException("Unable construct viewmodel")
        }

    }
}