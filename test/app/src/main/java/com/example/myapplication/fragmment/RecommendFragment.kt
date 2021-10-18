package com.example.myapplication.fragmment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.service.MusicService
import kotlinx.coroutines.*

class RecommendFragment(val musicService: MusicService,context:Context) : Fragment() {

    lateinit var rvrecommnend: RecyclerView
    lateinit var close : ImageView
    lateinit var tvState: TextView
    lateinit var progressBar : ProgressBar
    private var adapter = SongAdapter(context)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_recommend, container, false)
        initRv(view)
        loadRecommendSong()
        return view
    }

    private fun initRv(view: View) {
        tvState = view.findViewById(R.id.tv_state)
        progressBar = view.findViewById(R.id.progress_circular)

        close = view.findViewById(R.id.close)
        close.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        rvrecommnend = view.findViewById(R.id.rv_recommed)
        rvrecommnend.adapter = adapter
        rvrecommnend.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            musicService.playRecommend(it)
        }
    }

    private fun loadRecommendSong() {
        val songId = musicService.cursong?.id
        progressBar.visibility = View.VISIBLE
        tvState.visibility = View.VISIBLE
        tvState.text = "fetching"
        var recommendResponses: RecommendResponses? = null
        CoroutineScope(Dispatchers.IO).launch {
            try{
                withTimeout(3000){
                    if (songId != null) {
                        recommendResponses = SongApi.create().getRecommend("audio", songId)
                    }
                    withContext(Dispatchers.Main) {
                        val listSong = recommendResponses?.data?.items
                        listSong?.let { adapter.setData(it) }
                        progressBar.visibility = View.GONE
                        tvState.visibility = View.GONE
                    }
                }
            } catch (e:TimeoutCancellationException){
                withContext(Dispatchers.Main){
                    tvState.visibility = View.GONE
                    tvState.text = "Retry"
                    tvState.setOnClickListener {
                        loadRecommendSong()
                    }
                }

            }
        }
    }
}