package com.example.myapplication.fragmment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PlaylistAdapter
import com.example.myapplication.data.local.SongDao
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.models.SongInPlaylist
import com.example.myapplication.data.local.relations.SongPlaylistCrossRef
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.FragmentMyPlaylistBinding
import com.example.myapplication.service.MusicService
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

class PlaylistFragment(
) : Fragment() {
    private var _binding: FragmentMyPlaylistBinding? = null
    private val binding get() = _binding!!

    var listPlaylist = mutableListOf<Playlist>()
    val adapter = PlaylistAdapter()
    lateinit var db: SongDao
    var song: Song? = null
    lateinit var vm: MainViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        song = arguments?.getSerializable("song") as Song?
        _binding = FragmentMyPlaylistBinding.inflate(inflater, container, false)
        val view = binding.root
        vm = ViewModelProvider(
            requireActivity(),
            MainViewModel.MainViewmodelFactory(requireActivity().application)
        )[MainViewModel::class.java]
        initViews()
        getlistPlaylist()
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun getlistPlaylist() {
        vm.getPlaylists()
        vm.playlists.observe(viewLifecycleOwner, {
            adapter.setData(it.toMutableList())
        })
    }

    private fun initViews() {
        adapter.setData(listPlaylist)
        adapter.setOnClickItem {
            itemClick(it)
        }
        binding.rvPlaylist.adapter = adapter
        binding.rvPlaylist.layoutManager = LinearLayoutManager(
            requireActivity().applicationContext,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.create.setOnClickListener {
            val name = binding.edtPlaylist.text.toString()
            vm.createNewPlaylist(name)
        }
        binding.close.setOnClickListener {
            super.requireActivity().onBackPressed()
        }
    }

    private fun itemClick(playlist: Playlist) {
        showPlaylist(playlist)
    }

    private fun showPlaylist(playlist: Playlist) {
        val navHostFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val controller = navHostFragment.navController
        controller.navigate(
            R.id.detailPlaylistFragment,
            bundleOf("playlistName" to playlist.playlistName)
        )
    }

    private fun showSnack(mess: String) {
        Snackbar.make(
            requireActivity().findViewById(R.id.root_layout),
            mess,
            Snackbar.LENGTH_LONG
        ).show()
    }
}