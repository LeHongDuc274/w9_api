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
import androidx.lifecycle.ViewModelProvider
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
import com.example.myapplication.viewmodels.FavouriteViewModel
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File


class FavouriteFragment : Fragment() {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding get() = _binding!!
    lateinit var adapter: SongAdapter

    private lateinit var vm: MainViewModel
    private lateinit var favoVm : FavouriteViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFavouriteBinding.inflate(inflater, container, false)
        val view = binding.root
        vm = ViewModelProvider(
            requireActivity(),
            MainViewModel.MainViewmodelFactory(requireActivity().application)
        )[MainViewModel::class.java]
        favoVm = ViewModelProvider(
            this,
            FavouriteViewModel.FavouriteViewmodelFactory(requireActivity().application)
        )[FavouriteViewModel::class.java]
        adapter = SongAdapter(requireActivity())
        initRv()
        obverseMessage()
        getFavouriteSong()
        return view
    }

    private fun obverseMessage() {
        favoVm.message.observe(viewLifecycleOwner,{
            showSnack(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun initRv() {
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
            if (!favoVm.listFavourite.value.isNullOrEmpty()) {
                vm.setNewPlaylist(favoVm.listFavourite.value!!,"FAVOURITE")
            } else showSnack("Favourite songs blank")
        }
        adapter.setItemClick {
            if(vm.namePlaylist.value != "FAVOURITE"){
                vm.setNewPlaylist(favoVm.listFavourite.value!!,"FAVOURITE")
            }
            vm.setNewSong(it)
            vm.playSong()
        }
        adapter.setDownloadClick {
            vm.downloadSong(it)
        }
        adapter.setFavouriteClick {
            favoVm.removeFavourite(it)
        }
    }

//    private fun removeSongInplaylist(it: Song) {
//        val db = SongDatabase.getInstance(requireActivity().applicationContext)
//        CoroutineScope(Dispatchers.IO).launch {
//            val id = it.id
//            val isExistCrossRef = db.getDao().isExistCrossRef(id, otherPlaylist!!)
//            if (isExistCrossRef) {
//                db.getDao().deleteCrossRef(id, otherPlaylist!!)
//            }
//            withContext(Dispatchers.Main) {
//                showSnack("Remove from ${otherPlaylist} playlist")
//            }
//        }
//    }

    private fun getFavouriteSong() {
        favoVm.fetchFavouriteSong()
        favoVm.listFavourite.observe(viewLifecycleOwner,{
            adapter.setData(it.toMutableList())
        })
        favoVm.progressBar.observe(viewLifecycleOwner,{
            if (it) {
                binding.progressCircular.visibility = View.VISIBLE
                binding.tvState.visibility = View.VISIBLE
            }
            else {
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE
            }
        })
    }


//    private fun getOtherPlaylist(otherPlaylist: String) {
//        val db = SongDatabase.getInstance(requireActivity().applicationContext)
//        CoroutineScope(Dispatchers.IO).launch {
//            val listSongCrossRef = db.getDao().getSongOfPlaylist(otherPlaylist) //otherPlaylist
//            if (listSongCrossRef != null) {
//                withContext(Dispatchers.Main) {
//                    newListFavourite = listSongCrossRef.songs.map {
//                        Song(
//                            artists_names = it.artists_names,
//                            duration = it.duration,
//                            title = it.title,
//                            id = it.id,
//                            thumbnail = it.thumbnail,
//                            favorit = true
//                        )
//                    }
//                    Log.e("tag", listSongCrossRef.toString())
//                    adapter.setData(newListFavourite.toMutableList())
//                    binding.progressCircular.visibility = View.GONE
//                    binding.tvState.visibility = View.GONE
//                    binding.playPlaylist.isClickable = true
//                }
//            }
//        }
//    }

    private fun showSnack(mess: String) {
        Snackbar.make(
            requireActivity().findViewById(R.id.root_layout),
            mess,
            Snackbar.LENGTH_LONG
        ).show()
    }
}