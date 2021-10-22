package com.example.myapplication.fragmment

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.search.SearchResponse
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.TYPE_RECOMMEND
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class RecommendFragment(
    val musicService: MusicService,
    context: Context,
    val query: String? = null
) : Fragment() {

    lateinit var rvrecommnend: RecyclerView
    lateinit var close: ImageView
    lateinit var tvState: TextView
    lateinit var progressBar: ProgressBar
    lateinit var tvName : TextView
    lateinit var btnPlay : Button
    private var adapter = SongAdapter(context)
    private var listSong = mutableListOf<Song>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_recommend, container, false)
        initRv(view)

        if (query == null) {
            setRecommendSong()
            musicService.cursong?.let {
                tvName.text = "Playlist Recommend của bài hát :" +it.title
            }

        }
        else {
            getSearchResult(query)
            tvName.text = "Playlist Kết quả tìm kiếm của :  " + query + " key"
        }
        return view
    }


    private fun initRv(view: View) {
        tvState = view.findViewById(R.id.tv_state)
        progressBar = view.findViewById(R.id.progress_circular)
        tvName = view.findViewById(R.id.tv_recommed)
        tvName.isSelected = true
        close = view.findViewById(R.id.close)
        close.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack(null,POP_BACK_STACK_INCLUSIVE)
        }
        btnPlay = view.findViewById(R.id.play_playlist)
        btnPlay.isClickable = false
        btnPlay.setOnClickListener {
            if(listSong.isNotEmpty()){
                musicService.setPlaylist(listSong)
                musicService.setNewSong(listSong[0].id)
                musicService.playSong()
                musicService.sendToActivity(ACTION_CHANGE_SONG)
            } else showSnack("List empty")
        }
        rvrecommnend = view.findViewById(R.id.rv_recommed)
        rvrecommnend.adapter = adapter
        rvrecommnend.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            musicService.setPlaylist(listSong)
            musicService.setNewSong(it.id)
            musicService.playSong()
            musicService.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(requireActivity(), MusicService::class.java)
            requireActivity().startService(intentService)
        }
        adapter.setDownloadClick {
            downloadSong(it)
        }
    }

    private fun getSearchResult(text: String) {
        val search = text.trim()
        val searchApi = SongApi.createSearch()
        searchApi.search("artist,song", 20, search).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(
                call: Call<SearchResponse>,
                response: Response<SearchResponse>
            ) {
                progressBar.visibility = View.GONE
                tvState.visibility = View.GONE
                if (response.isSuccessful) {
                    val body = response.body()
//                    "https://photo-resize-zmp3.zadn.vn/w94_r1x1_jpeg" +
//                            "/cover/4/9/d/a/49da6a1d6cf13a42e77bc3a945d9dd6b.jpg?fs=MTYzNDY4MzgyOTUwM3x3ZWJWNHw"
                    body?.let {
                        if (it.data.isNotEmpty()) {
                            val listSongSearch = it.data[0].song.toMutableList()
                            listSong = listSongSearch.map {
                                Song(
                                    artists_names = it.artist,
                                    title = it.name,
                                    duration = it.duration.toInt(),
                                    thumbnail = Contains.BASE_IMG_URL + it.thumb,
                                    id = it.id
                                )
                            }.toMutableList()
                            adapter.setData(listSong)
                            if (listSongSearch.isNotEmpty()) btnPlay.isClickable = true
                        } else showSnack("Not Result for this key ${search.uppercase()}")
                    }
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                showSnack("Error call api")
            }
        })
    }

    private fun downloadSong(song: Song) {
        if (isStoragePermissionGranted()) {

            val downloadManager =
                requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${song.id}/128")
            val fileName = song.title
            val appFile = File("/storage/emulated/0/Download/" + fileName + ".mp3")
            if (appFile.canRead()) {
                showSnack("File Already Exists...")
            } else {
                showSnack("Waiting Download...")
                val request = DownloadManager.Request(uri)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setTitle(fileName)
                request.setDescription("downloading..")
                request.setAllowedOverRoaming(true)
                request.setDestinationInExternalPublicDir(
                    (Environment.DIRECTORY_DOWNLOADS),
                    fileName + ".mp3"
                )
                val downloadId = downloadManager.enqueue(request)
            }
        }
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }

    private fun setRecommendSong() {
        val isOffline = musicService.cursong?.isOffline ?: true
        if (!isOffline ) {
            loadRecommendSong()
        } else {
            tvState.visibility = View.VISIBLE
            tvState.text = "OffLine Music - Not Has Recommend Song"
        }
    }

    private fun loadRecommendSong() {
        val songId = musicService.cursong?.id
        progressBar.visibility = View.VISIBLE
        tvState.visibility = View.VISIBLE
        tvState.text = "fetching"
        val recommendResponses = SongApi.create().getRecommend("audio", songId!!)
        recommendResponses.enqueue(object:Callback<RecommendResponses>{
            override fun onResponse(
                call: Call<RecommendResponses>,
                response: Response<RecommendResponses>
            ) {
                if (response.isSuccessful){
                    val body = response.body()
                    body?.let {
                        listSong = it.data.items.toMutableList()
                        listSong.let { adapter.setData(it.toMutableList()) }
                        progressBar.visibility = View.GONE
                        tvState.visibility = View.GONE
                    }
                }
            }
            override fun onFailure(call: Call<RecommendResponses>, t: Throwable) {
                showSnack("Error call API")
            }
        })
    }

    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }
}