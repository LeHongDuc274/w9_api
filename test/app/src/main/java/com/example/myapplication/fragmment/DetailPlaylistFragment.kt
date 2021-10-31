package com.example.myapplication.fragmment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.databinding.FragmentFavouriteBinding
import com.example.myapplication.viewmodels.FavouriteViewModel
import com.example.myapplication.viewmodels.MainViewModel
import com.example.myapplication.viewmodels.PlaylistViewModel
import com.google.android.material.snackbar.Snackbar


class DetailPlaylistFragment : Fragment() {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding get() = _binding!!
    lateinit var adapter: SongAdapter
    private var playlistName: String? = null
    private lateinit var vm: MainViewModel
    private lateinit var playlistVm: PlaylistViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavouriteBinding.inflate(inflater, container, false)
        val view = binding.root
        vm = ViewModelProvider(
            requireActivity(),
            MainViewModel.MainViewmodelFactory(requireActivity().application)
        )[MainViewModel::class.java]
        playlistVm = ViewModelProvider(
            this,
            PlaylistViewModel.Factory(requireActivity().application)
        )[PlaylistViewModel::class.java]
        adapter = SongAdapter(requireActivity())
        initRv()
        obverseMessage()
        playlistName = arguments?.getString("playlistName")
        playlistName?.let {
            getplaylistSong(it)
        }
        return view
    }

    private fun obverseMessage() {
        playlistVm.message.observe(viewLifecycleOwner, {
            showSnack(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun initRv() {
        binding.progressCircular.visibility = View.GONE
        binding.tvState.visibility = View.GONE
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.tvRecommed.text = "Favourite Playlit"
        binding.close.setOnClickListener {
            var navHostFragment =
                requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            var controller = navHostFragment.navController
            controller.popBackStack()
        }
        binding.playPlaylist.setOnClickListener {
            if (!playlistVm.listSong.value.isNullOrEmpty()) {
                vm.setNewPlaylist(playlistVm.listSong.value!!, "PLAYLIST")
            } else showSnack("playlist songs blank")
        }
        adapter.setItemClick {
            if (vm.namePlaylist.value != "PLAYLIST") {
                vm.setNewPlaylist(playlistVm.listSong.value!!, "PLAYLIST")
            }
            vm.setNewSong(it)
            vm.playSong()
        }
        adapter.setDownloadClick {
            vm.downloadSong(it)
        }
        adapter.setFavouriteClick {
            playlistVm.removeSongInplaylist(it,playlistName!!)
        }
    }

    private fun getplaylistSong(name: String) {
        playlistVm.getListSongOnplaylist(name)
        playlistVm.listSong.observe(viewLifecycleOwner, {
            adapter.setData(it.toMutableList())
        })
    }

    private fun showSnack(mess: String) {
        Snackbar.make(
            requireActivity().findViewById(R.id.root_layout),
            mess,
            Snackbar.LENGTH_LONG
        ).show()
    }
}