package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.TopSongResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface SongApi {
    @GET("/xhr/chart-realtime")
    suspend fun getTopSong(): TopSongResponse

    @GET("xhr/recommend")
    suspend fun getRecommend(
        @Query("type") type : String = "audio",
        @Query("id") id : String
    ) : RecommendResponses

    companion object {
        const val BASE_URL ="https://mp3.zing.vn"
        fun create(): SongApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SongApi::class.java)
        }
    }
}