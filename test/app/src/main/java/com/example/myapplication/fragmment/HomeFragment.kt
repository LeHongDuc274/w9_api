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
import android.util.Log
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
import com.example.myapplication.utils.FragmentAction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import okhttp3.Cache
import java.io.File

class HomeFragment : Fragment() {

    private var listSong = mutableListOf<Song>()
    lateinit var adapter: SongAdapter
    lateinit var rv: RecyclerView
    lateinit var close: ImageView
    lateinit var tvState: TextView
    lateinit var btnPlay: Button
    lateinit var progressBar: ProgressBar
    lateinit var tvName: TextView

    val broadcastInternet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

        }
    }
    var itemCLick: FragmentAction? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentAction) {
            itemCLick = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        itemCLick = null
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
                itemCLick?.setNewPlaylistOnFragment(listSong)
            } else showSnack("List empty")
        }
        adapter.setItemClick { song ->
            itemCLick?.setNewSongOnFragment(song, listSong)
        }
        adapter.setDownloadClick { song -> itemCLick?.clickDownload(song) }
    }

    fun receiverStateLoad(state: Int, data: MutableList<Song>?, mess: String?) {
        when (state) {
            1 -> {// loading
                tvState.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                tvState.text = mess
            }
            2 -> {  //succes
                tvState.visibility = View.GONE
                progressBar.visibility = View.GONE
                data?.let {
                    listSong = it
                    Log.e("tag",it.toString())
                    adapter.setData(it)
                }
            }
            3 -> {
                showSnack(mess ?: "error")
            }
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }
}