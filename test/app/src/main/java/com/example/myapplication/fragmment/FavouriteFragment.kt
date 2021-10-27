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
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.FragmentFavouriteBinding
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.FragmentAction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File


class FavouriteFragment(
) : Fragment() {


//    lateinit var rvrecommnend: RecyclerView
//    lateinit var close: ImageView
//    lateinit var tvState: TextView
//    lateinit var progressBar: ProgressBar
//    lateinit var tvName: TextView
//    lateinit var btnPlay: Button
    private var _binding : FragmentFavouriteBinding? = null
    private val binding get() = _binding!!
    lateinit var adapter: SongAdapter
    var newListFavourite: List<Song> = listOf()
    var otherPlaylist: String? = null
    var itemClick: FragmentAction? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentAction) {
            itemClick = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        itemClick = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFavouriteBinding.inflate(inflater,container,false)
        val view = binding.root

        otherPlaylist = arguments?.getString("playlist")
        adapter = SongAdapter(requireActivity())
        initRv(view)
        setFavouriteSong()
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
    private fun initRv(view: View) {
//        tvState = view.findViewById(R.id.tv_state)
//        progressBar = view.findViewById(R.id.progress_circular)
//        tvName = view.findViewById(R.id.tv_recommed)
//        btnPlay = view.findViewById(R.id.play_playlist)
//        close = view.findViewById(R.id.close)
        binding.tvRecommed.text = "Favourite Playlit"
        if (otherPlaylist != null) {
            binding.tvRecommed.text = "${otherPlaylist} Playlist"
        }
        binding.playPlaylist.isClickable = false
        binding.playPlaylist.setOnClickListener {
            if (newListFavourite.isNotEmpty()) {
                itemClick?.setNewPlaylistOnFragment(newListFavourite as MutableList<Song>)
            } else showSnack("list empty")
        }
        binding.close.setOnClickListener {
                var navHostFragment =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
                var controller = navHostFragment.navController
                controller.popBackStack()
        }
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            itemClick?.setNewSongOnFragment(it, newListFavourite.toMutableList())
        }
        adapter.setDownloadClick {
            // downloadSong(it)
            itemClick?.clickDownload(it)
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
            val isExistCrossRef = db.getDao().isExistCrossRef(id, otherPlaylist!!)
            if (isExistCrossRef) {
                db.getDao().deleteCrossRef(id, otherPlaylist!!)
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

    private fun setFavouriteSong() {
        loadFavouriteSong()
    }

    private fun loadFavouriteSong() {
        binding.progressCircular.visibility = View.VISIBLE
        binding.tvState.visibility = View.VISIBLE
        binding.tvState.text = "fetching"
        if (otherPlaylist == null)
            getFavouritePlaylist() // other playlist == null _> getFavourite Playlist
        else getOtherPlaylist(otherPlaylist!!) // get playlist by playlist name
    }

    private fun getFavouritePlaylist() {
        val db = SongDatabase.getInstance(requireActivity().applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val listSongFavourite = db.getDao().getAllSong()

            withContext(Dispatchers.Main) {
                if (listSongFavourite.isNotEmpty()) {
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
                } else showSnack("list empty")
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE
                binding.playPlaylist.isClickable = true
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
                    binding.progressCircular.visibility = View.GONE
                    binding.tvState.visibility = View.GONE
                    binding.playPlaylist.isClickable = true
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