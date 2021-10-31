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
import com.example.myapplication.utils.FragmentAction
import com.example.myapplication.viewmodels.PlayingViewModel
import com.example.myapplication.viewmodels.RecommendViewModel
import com.google.android.material.snackbar.Snackbar

class RecommendFragment(
) : Fragment() {
    private var _binding: FragmentRecommendBinding? = null
    private val binding get() = _binding!!
    lateinit var adapter: SongAdapter
    lateinit var recommendVm: RecommendViewModel
    lateinit var playingViewModel: PlayingViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        playingViewModel = ViewModelProvider(
            requireActivity(),
            PlayingViewModel.PlayingViewmodelFactory(requireActivity().application)
        )[PlayingViewModel::class.java]
        recommendVm = ViewModelProvider(
            this,
            RecommendViewModel.RecommnedFactory(requireActivity().application)
        )[RecommendViewModel::class.java]
        adapter = SongAdapter(requireActivity())
        _binding = FragmentRecommendBinding.inflate(inflater, container, false)
        val view = binding.root
        initRv()
        obverser()
        return view
    }

    private fun obverser() {
        recommendVm.listRecommend.observe(viewLifecycleOwner, {
            adapter.setData(it.toMutableList())
        })
        recommendVm.progressBar.observe(viewLifecycleOwner, {
            if (it) {
                binding.progressCircular.visibility = View.VISIBLE
                binding.tvState.visibility = View.VISIBLE
            } else {
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE
            }
        })
        recommendVm.message.observe(viewLifecycleOwner, {
            showSnack(it)
        })
    }

    private fun initRv() {
        binding.tvRecommed.isSelected = true
        binding.close.setOnClickListener {
            super.requireActivity().onBackPressed()
        }
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recommendVm.getRecommendSong(playingViewModel.curSong.value!!)
        binding.playPlaylist.setOnClickListener {
            if (!recommendVm.listRecommend.value.isNullOrEmpty()) {
                playingViewModel.setNewPlaylist(
                    recommendVm.listRecommend.value!!,
                    "RECOMMEND ${playingViewModel.curSong.value!!}"
                )
            } else showSnack("Local song blank")
        }
        adapter.setItemClick {
            if (playingViewModel.namePlaylist.value != "RECOMMEND ${playingViewModel.curSong.value!!}") {
                playingViewModel.setNewPlaylist(
                    recommendVm.listRecommend.value!!,
                    "RECOMMEND ${playingViewModel.curSong.value!!}"
                )
            }
            playingViewModel.setNewSong(it)
            playingViewModel.playSong()
        }
        adapter.setDownloadClick {
            playingViewModel.downloadSong(it)
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }
}