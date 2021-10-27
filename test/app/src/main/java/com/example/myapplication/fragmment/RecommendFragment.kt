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
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.search.SearchResponse
import com.example.myapplication.databinding.FragmentRecommendBinding
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.TYPE_RECOMMEND
import com.example.myapplication.utils.FragmentAction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class RecommendFragment(
) : Fragment() {
    private var _binding : FragmentRecommendBinding ? = null
    private  val binding get() = _binding!!
    lateinit var adapter: SongAdapter
    private var listSong = mutableListOf<Song>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       adapter =  SongAdapter(requireActivity())
        _binding = FragmentRecommendBinding.inflate(inflater,container,false)
        val view = binding.root
        initRv()
        return view
    }

    private fun initRv() {
        binding.tvRecommed.isSelected = true
        binding.close.setOnClickListener {
            super.requireActivity().onBackPressed()
        }
        binding.playPlaylist.isClickable = false
        binding.playPlaylist.setOnClickListener {
            if (listSong.isNotEmpty()) {
                itemClick?.setNewPlaylistOnFragment(listSong)
            } else showSnack("List empty")
        }
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            itemClick?.setNewSongOnFragment(it, listSong)
        }
        adapter.setDownloadClick {
            itemClick?.clickDownload(it)
        }
    }

    var itemClick: FragmentAction? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentAction) {
            itemClick = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        itemClick= null
    }
    fun receiverStateLoad(state: Int, data: MutableList<Song>?, mess: String?) {
        when (state) {
            1 -> {// loading
                binding.tvState.visibility = View.VISIBLE
                binding.progressCircular.visibility = View.VISIBLE
                binding.tvState.text = mess
            }
            2 -> {  //succes
                binding.tvState.visibility = View.GONE
                binding.progressCircular.visibility = View.GONE
                data?.let {
                    listSong = it
                    adapter.setData(it)
                }
            }
            3 -> { // error
                binding.tvState.visibility = View.GONE
                binding.progressCircular.visibility = View.GONE
                showSnack(mess ?: "error")
            }
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(requireActivity().findViewById(R.id.root_layout), mess, Snackbar.LENGTH_SHORT)
            .show()
    }
}