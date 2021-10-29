package com.example.myapplication.fragmment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.FragmentRecommendBinding
import com.example.myapplication.databinding.FragmentSearchBinding
import com.example.myapplication.utils.FragmentAction
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    lateinit var adapter: SongAdapter
    private lateinit var vm: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adapter = SongAdapter(requireActivity())
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        vm = ViewModelProvider(
            requireActivity(),
            MainViewModel.MainViewmodelFactory(requireActivity().application)
        )[MainViewModel::class.java]
        val view = binding.root
        initRv()
        obverse()
        return view
    }

    private fun obverse() {
        vm.listSearch.observe(viewLifecycleOwner, {
             adapter.setData(it.toMutableList())
        })
        vm.progressBar.observe(viewLifecycleOwner, {
            if (it) {
                binding.progressCircular.visibility = View.VISIBLE
                binding.tvState.visibility = View.VISIBLE
            } else {
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE
            }
        })
    }

    private fun initRv() {
        binding.tvRecommed.isSelected = true
        binding.close.setOnClickListener {
            super.requireActivity().onBackPressed()
        }
        binding.playPlaylist.setOnClickListener {
            if (vm.listSearch.value!!.isNotEmpty()) {
                vm.setNewPlaylist(vm.listSearch.value!!,"SEARCH")
            } else showSnack("List empty")
        }
        adapter.setItemClick {
            if(vm.namePlaylist.value != "SEARCH"){
                vm.setNewPlaylist(vm.listSearch.value!!,"SEARCH")
            }
            vm.setNewSong(it)
        }
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setDownloadClick {
            vm.downloadSong(it)
        }
    }


    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }

}