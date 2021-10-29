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
import com.example.myapplication.adapter.BaseAdapter
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.FragmentBaseBinding
import com.example.myapplication.viewmodels.LocalSongViewModel
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar

class BaseFragment() :
    Fragment() {

    private var _binding: FragmentBaseBinding? = null
    private val binding get() = _binding!!
    lateinit var vm: MainViewModel
    lateinit var localVm: LocalSongViewModel
    lateinit var adapter: BaseAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBaseBinding.inflate(inflater, container, false)
        val view = binding.root
        adapter = BaseAdapter(requireActivity())
        vm = ViewModelProvider(
            requireActivity(),
            MainViewModel.MainViewmodelFactory(requireActivity().application)
        )[MainViewModel::class.java]
        localVm = ViewModelProvider(
            this,
            LocalSongViewModel.LocalViewModelFactory(requireActivity().application)
        )[LocalSongViewModel::class.java]
        initRv()
        getData()
        return view
    }

    private fun getData() {
        localVm.fetchLocalSong()
        localVm.lisLocalSong.observe(viewLifecycleOwner, {
            adapter.setData(it)
        })
        localVm.progressBar.observe(viewLifecycleOwner, {
            if (it) {
                binding.progressCircular.visibility = View.VISIBLE
                binding.tvState.visibility = View.VISIBLE
            } else {
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun initRv() {
        binding.tvRecommed.text = "Local Playlist"
        binding.close.setOnClickListener {
            val navHostFragment =
                requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            val controller = navHostFragment.navController
            controller.popBackStack()
        }
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        binding.playPlaylist.setOnClickListener {
            if (!localVm.lisLocalSong.value.isNullOrEmpty()) {
                vm.setNewPlaylist(localVm.lisLocalSong.value!!,"OFFLINE")
            } else showSnack("Local song blank")
        }
        adapter.setItemClick {
            clickItem(it)
        }
    }

    private fun clickItem(song: Song) {
        if(vm.namePlaylist.value != "OFFLINE"){
            vm.setNewPlaylist(localVm.lisLocalSong.value!!,"OFFLINE")
        }
        vm.setNewSong(song)
    }

    private fun showSnack(mess: String) {
        Snackbar.make(
            requireActivity().findViewById(R.id.root_layout),
            mess,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
