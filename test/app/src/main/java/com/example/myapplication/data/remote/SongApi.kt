package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.data.remote.search.SearchResponse
import com.example.myapplication.utils.Contains.BASE_SEARCH_URL
import com.example.myapplication.utils.Contains.BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import retrofit2.Call
import java.util.concurrent.TimeUnit


interface SongApi {
    @GET("xhr/chart-realtime")
    suspend fun getTopSong(): TopSongResponse

    @GET("xhr/recommend")
    fun getRecommend(
        @Query("type") type: String = "audio",
        @Query("id") id: String
    ): Call<RecommendResponses>

    // use Call
    @GET("complete")
    fun search(
        @Query("type") type: String,
        @Query("num") num: Int,
        @Query("query") query: String
    ): Call<SearchResponse>

    companion object {
        fun create(): SongApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SongApi::class.java)
        }
        fun createSearch():SongApi{
            return Retrofit.Builder()
                .baseUrl(BASE_SEARCH_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SongApi::class.java)
        }
    }
}