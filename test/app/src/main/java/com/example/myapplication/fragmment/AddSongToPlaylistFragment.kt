package com.example.myapplication.fragmment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.adapter.PlaylistAdapter
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.FragmentAddSongToPlaylistBinding
import com.example.myapplication.viewmodels.PlayingViewModel
import com.google.android.material.snackbar.Snackbar

class AddSongToPlaylistFragment : Fragment() {
    private var _binding: FragmentAddSongToPlaylistBinding? = null
    private val binding get() = _binding!!
    val adapter = PlaylistAdapter()
    lateinit private var playingVm: PlayingViewModel
    private var song : Song? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddSongToPlaylistBinding.inflate(inflater, container, false)
        val view = binding.root
        playingVm = ViewModelProvider(
            this,
            PlayingViewModel.PlayingViewmodelFactory(requireActivity().application)
        )[PlayingViewModel::class.java]
        song = arguments?.getSerializable("song") as Song
        initViews()
        getlistPlaylist()
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
    private  fun getlistPlaylist(){
        playingVm.getPlaylists()
        playingVm.playlists.observe(viewLifecycleOwner,{
            adapter.setData(it.toMutableList())
        })
        playingVm.message.observe(viewLifecycleOwner, {
            showSnack(it)
        })
    }
    private fun initViews() {
        adapter.setOnClickItem {
            addSongTopPlaylist(it)
        }
        binding.rvPlaylist.adapter = adapter
        binding.rvPlaylist.layoutManager = LinearLayoutManager(
            requireActivity().applicationContext,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.close.setOnClickListener {
            super.requireActivity().onBackPressed()
        }
    }

    private fun addSongTopPlaylist(playlist: Playlist) {
        song?.let {
            playingVm.addSongtoPlaylist(playlist,it)
        }
    }
    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT).show()
    }
}