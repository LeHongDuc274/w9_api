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
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File


class FavouriteFragment(
    val musicService: MusicService,
    context: Context,
    val otherPlaylist: String? = null,
) : Fragment() {


    lateinit var rvrecommnend: RecyclerView
    lateinit var close: ImageView
    lateinit var tvState: TextView
    lateinit var progressBar: ProgressBar
    lateinit var tvName: TextView
    lateinit var btnPlay: Button
    private var adapter = SongAdapter(context)
    var newListFavourite: List<Song> = listOf()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_favourite, container, false)
        initRv(view)
        setFavouriteSong()
        return view
    }

    private fun initRv(view: View) {
        tvState = view.findViewById(R.id.tv_state)
        progressBar = view.findViewById(R.id.progress_circular)
        tvName = view.findViewById(R.id.tv_recommed)
        btnPlay = view.findViewById(R.id.play_playlist)
        close = view.findViewById(R.id.close)
        tvName.text = "Favourite Playlit"
        btnPlay.isClickable = false
        btnPlay.setOnClickListener {
            if (newListFavourite.isNotEmpty()) {
                musicService.setPlaylist(newListFavourite)
                musicService.setNewSong(newListFavourite[0].id)
                musicService.playSong()
                musicService.sendToActivity(ACTION_CHANGE_SONG)
            } else showSnack("Favourite list empty")
        }
        close.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        rvrecommnend = view.findViewById(R.id.rv_recommed)
        rvrecommnend.adapter = adapter
        rvrecommnend.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            musicService.setPlaylist(newListFavourite)
            musicService.setNewSong(it.id)
            musicService.playSong()
            musicService.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(requireActivity(), MusicService::class.java)
            requireActivity().startService(intentService)
        }
        adapter.setDownloadClick {
            downloadSong(it)
        }
        adapter.setFavouriteClick {
            if (otherPlaylist == null) removeFavourite(it)
            else removeSongInplaylist(it)
        }
    }

    private fun removeSongInplaylist(it: Song) {
        val db = SongDatabase.getInstance(requireActivity().applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val id = it.id
            val isExistCrossRef = db.getDao().isExistCrossRef(id,otherPlaylist!!)
            if(isExistCrossRef){
                db.getDao().deleteCrossRef(id,otherPlaylist)
            }
            withContext(Dispatchers.Main) {
                showSnack("Remove from ${otherPlaylist} playlist")
            }
        }
    }

    private fun removeFavourite(song: Song) {
        val db = SongDatabase.getInstance(requireActivity().applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val id = song.id
            val isExists = db.getDao().isExist(id)
            if (isExists) {
                db.getDao().deleteById(id)
                withContext(Dispatchers.Main) {
                    showSnack("Remove Favourite List")
                }
            }
        }
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

    private fun setFavouriteSong() {
        loadFavouriteSong()
    }

    private fun loadFavouriteSong() {
        progressBar.visibility = View.VISIBLE
        tvState.visibility = View.VISIBLE
        tvState.text = "fetching"
        if (otherPlaylist == null)
            getFavouritePlaylist() // other playlist == null _> getFavourite Playlist
        else getOtherPlaylist(otherPlaylist) // get playlist by playlist name


    }

    private fun getFavouritePlaylist() {
        val db = SongDatabase.getInstance(requireActivity().applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val listSongFavourite = db.getDao().getAllSong()
            if (listSongFavourite.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    newListFavourite = listSongFavourite.map {
                        Song(
                            artists_names = it.artists_names,
                            duration = it.duration,
                            title = it.title,
                            id = it.id,
                            thumbnail = it.thumbnail,
                            favorit = true
                        )
                    }
                    adapter.setData(newListFavourite.toMutableList())
                    progressBar.visibility = View.GONE
                    tvState.visibility = View.GONE
                    btnPlay.isClickable = true
                }
            }
        }
    }

    private fun getOtherPlaylist(otherPlaylist: String) {
        val db = SongDatabase.getInstance(requireActivity().applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val listSongCrossRef = db.getDao().getSongOfPlaylist(otherPlaylist) //otherPlaylist
            if (listSongCrossRef != null) {
                withContext(Dispatchers.Main) {
                    newListFavourite = listSongCrossRef.songs.map {
                        Song(
                            artists_names = it.artists_names,
                            duration = it.duration,
                            title = it.title,
                            id = it.id,
                            thumbnail = it.thumbnail,
                            favorit = true
                        )
                    }
                    Log.e("tag", listSongCrossRef.toString())
                    adapter.setData(newListFavourite.toMutableList())
                    progressBar.visibility = View.GONE
                    tvState.visibility = View.GONE
                    btnPlay.isClickable = true
                }
            }
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(
            requireActivity().findViewById(R.id.root_layout),
            mess,
            Snackbar.LENGTH_LONG
        ).show()
    }
}