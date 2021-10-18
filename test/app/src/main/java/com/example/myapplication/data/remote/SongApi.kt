package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.responses.TopSongResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface SongApi {
    @GET("/xhr/chart-realtime")
    suspend fun getTopSong(): TopSongResponse
    companion object {
        private const val BASE_URL ="https://mp3.zing.vn"
        fun create(): SongApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SongApi::class.java)
        }
    }
}