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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {
    lateinit var vm: MainViewModel
    lateinit var adapter: SongAdapter
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    val broadcastInternet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

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
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        adapter = SongAdapter(requireActivity())
        initView()
        initRv()
        vm = ViewModelProvider(requireActivity(),MainViewModel.MainViewmodelFactory(requireActivity().application))[MainViewModel::class.java]
        // getTopSong()
        vm.fetchTopSong()
        vm.listTopSong.observe(viewLifecycleOwner, {
            adapter.setData(it.toMutableList())
        })
        vm.progressBar.observe(viewLifecycleOwner, {
            if (it) {
                binding.progressCircular.visibility = View.VISIBLE
                binding.tvState.visibility = View.VISIBLE
            }
            else {
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE

            }
        })
        return view
    }

    private fun initView() {
        binding.tvRecommed.isSelected = true
    }

    private fun initRv() {
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager =
            LinearLayoutManager(
                requireActivity().applicationContext,
                LinearLayoutManager.VERTICAL,
                false
            )
        binding.playPlaylist.setOnClickListener {
            if (vm.listTopSong.value!!.isNotEmpty()) {
                vm.setNewPlaylist(vm.listTopSong.value!!,"ONLINE")
            } else showSnack("List empty")
        }
        adapter.setItemClick { song ->
            if(vm.namePlaylist.value != "ONLINE"){
                vm.setNewPlaylist(vm.listTopSong.value!!,"ONLINE")
            }
            vm.setNewSong(song)
            vm.playSong()
        }
       adapter.setDownloadClick { song -> vm.downloadSong(song) }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }
}