package com.example.myapplication.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.MainActivityRepository
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.NetworkResponseCallback
import java.lang.Appendable
import java.lang.IllegalArgumentException

class PlaylistViewModel(private val app: Application) : ViewModel() {
    val repo = MainActivityRepository.getInstance()
    var listSong = MutableLiveData<List<Song>>(emptyList())
    var message = MutableLiveData<String>()
    fun getListSongOnplaylist(name :String){
        repo.getListSongOnplaylist(app,name,object :NetworkResponseCallback<Song>{
            override fun onNetworkSuccess(data: List<Song>?) {
                if (!data.isNullOrEmpty()) listSong.value = data!!
            }
            override fun onNetworkFailure(error: String) {
                message.value = "List Empty"
            }
        })
    }

    fun removeSongInplaylist(it: Song, playlistName: String) {
        repo.removeSongInplaylist(it,playlistName,app,object : NetworkResponseCallback<Song>{
            override fun onNetworkSuccess(data: List<Song>?) {
                message.value = "Remove Succes"
            }
            override fun onNetworkFailure(error: String) {
                message.value = error+""
            }
        })
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PlaylistViewModel(app) as T
            }
            throw IllegalArgumentException("unable contructor viewmodel")
        }

    }
}