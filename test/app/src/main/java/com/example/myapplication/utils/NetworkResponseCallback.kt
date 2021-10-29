package com.example.myapplication.utils

import com.example.myapplication.data.remote.responses.Song

interface NetworkResponseCallback<T> {
    fun onNetworkSuccess(data: List<T>?)
    fun onNetworkFailure(error: String)
}