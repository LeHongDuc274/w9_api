package com.example.myapplication.fragmment

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.activities.PlayingActivity
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File

class HomeFragment(val musicService: MusicService) : Fragment() {

    private var response: TopSongResponse? = null
    private var listSong = mutableListOf<Song>()
    lateinit var adapter: SongAdapter
    lateinit var rv: RecyclerView
    lateinit var close: ImageView
    lateinit var tvState: TextView
    lateinit var btnPlay : Button

    lateinit var progressBar: ProgressBar
    lateinit var tvName: TextView

    val broadcastInternet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                     getTopSong()
                    musicService.internetConnected =
                        Contains.checkNetWorkAvailable(requireActivity())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter2 = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(broadcastInternet, filter2)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(broadcastInternet)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        adapter = SongAdapter(requireActivity())
        initView(view)
        initRv(view)
        getTopSong()
        return view
    }

    private fun initView(view: View) {
        tvState = view.findViewById(R.id.tv_state)
        progressBar = view.findViewById(R.id.progress_circular)
        tvName = view.findViewById(R.id.tv_recommed)
        tvName.isSelected = true
    }

    private fun initRv(view: View) {
        rv = view.findViewById<RecyclerView>(R.id.rv_recommed)
        rv.adapter = adapter
        rv.layoutManager =
            LinearLayoutManager(
                requireActivity().applicationContext,
                LinearLayoutManager.VERTICAL,
                false
            )
        btnPlay = view.findViewById(R.id.play_playlist)
        btnPlay.setOnClickListener {
            if (listSong.isNotEmpty()) {
                musicService.setPlaylist(listSong)
                musicService.setNewSong(listSong[0].id)
                musicService.playSong()
                musicService.sendToActivity(ACTION_CHANGE_SONG)
                val intentService = Intent(requireActivity(), MusicService::class.java)
                requireActivity().startService(intentService)
            } else showSnack("List empty")
        }
        adapter.setItemClick { song ->
            val id = song.id
            musicService.setPlaylist(listSong)
            musicService.setNewSong(id)
            musicService.playSong()
            val intentService = Intent(requireActivity(), MusicService::class.java)
            requireActivity().startService(intentService)
//            changeContent()
//            changeTogglePausePlayUi(Contains.ACTION_PLAY)
            val intent = Intent(requireActivity(), PlayingActivity::class.java)
            startActivity(intent)
            musicService.sendToActivity(ACTION_CHANGE_SONG)
        }
        adapter.setDownloadClick { song -> downloadSong(song) }
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

    private fun downloadSong(song: Song) {
        if (isStoragePermissionGranted()) {
            val downloadManager =
                requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${song.id}/128")
            val fileName = song.title
            val appFile = File("/storage/emulated/0/Download/" + fileName + ".mp3")
            if (appFile.canRead()) {
                showSnack("File Already Exists")
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

    private fun getTopSong() {
        val songApi = SongApi.create()

        if (Contains.checkNetWorkAvailable(requireActivity())) {
            tvState.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            tvState.text = "fetching"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeout(3000) {
                        try {
                            response = songApi.getTopSong()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        withContext(Dispatchers.Main) {
                            response?.let {
                                listSong = it.data.song.toMutableList()
                                adapter.setData(listSong)
                                tvState.visibility = View.GONE
                                progressBar.visibility = View.GONE
                                if (!listSong.isEmpty()) {
                                    musicService.let {
                                        if (it.isPlaylistEmpty()) {
                                            it.setPlaylist(listSong)
                                            if (it.cursong == null) it.setNewSong(listSong[0].id)
                                            musicService.sendToActivity(ACTION_CHANGE_SONG)
                                            musicService.sendToActivity(ACTION_PAUSE)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        tvState.text = "Retry"
                        tvState.setOnClickListener {
                            getTopSong()
                        }
                    }
                }
            }
        } else {
            tvState.visibility = View.GONE
            progressBar.visibility = View.GONE
            showSnack("No Internet")
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }
}