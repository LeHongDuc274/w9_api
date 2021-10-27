package com.example.myapplication.fragmment

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.BaseAdapter
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.FragmentBaseBinding
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.FragmentAction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BaseFragment() :
    Fragment() {
//
//    lateinit var rvrecommnend: RecyclerView
//    lateinit var close: ImageView
//    lateinit var tvState: TextView
//    lateinit var tvName : TextView
//    lateinit var progressBar: ProgressBar
//    lateinit var btnPlay : Button
    private  var _binding: FragmentBaseBinding? = null
    private val binding get() = _binding!!

    lateinit var adapter : BaseAdapter
    private var listSongLocal = mutableListOf<Song>()
    var itemClick: FragmentAction? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentAction) {
            itemClick = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        itemClick = null
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBaseBinding.inflate(inflater,container,false)
        val view = binding.root
        adapter = BaseAdapter(requireActivity())
        initRv(view)
        setData()
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
    private fun initRv(view: View) {
        binding.playPlaylist.isClickable = false
        binding.playPlaylist.setOnClickListener {
         if(listSongLocal.isNotEmpty()){
             itemClick?.setNewPlaylistOnFragment(listSongLocal,"OFFLINE")
            } else showSnack("Local song blank")
        }
        binding.tvRecommed .text = "Local Playlist"
        binding.close.setOnClickListener {
            val navHostFragment =
                requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            val controller = navHostFragment.navController
            controller.popBackStack()
        }
        binding.rvRecommed.adapter = adapter
        binding.rvRecommed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            clickItem(it)
        }
    }
    val intent = IntentFilter()
    private fun clickItem(it: Song) {
        itemClick?.setNewSongOnFragment(it,listSongLocal,"OFFLINE")
    }

    private fun setData() {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs =
            arrayOf(TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES).toString())
        val query =
            requireActivity().contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )
        val retriever = MediaMetadataRetriever()

        CoroutineScope(Dispatchers.IO).launch {
            query?.let { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val title = cursor.getString(1)
                    val singer = cursor.getString(2)
                    val duration = cursor.getInt(3)
                    val uri =
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    retriever.setDataSource(requireActivity().applicationContext, uri)
                    val bitmap = retriever.embeddedPicture ?: byteArrayOf()
                    val songLocal = Song(
                        artists_names = singer,
                        duration = duration / 1000,
                        id = id.toString(),
                        title = title,
                        image = bitmap,
                        isOffline = true
                    )
                    withContext(Dispatchers.Main) {
                        listSongLocal.add(songLocal)
                    }
                }
                cursor.close()
            }
            withContext(Dispatchers.Main) {
                binding.progressCircular.visibility = View.GONE
                binding.tvState.visibility = View.GONE
                adapter.setData(listSongLocal)
                if (listSongLocal.isNotEmpty()){
                    binding.playPlaylist.isClickable = true
                }
            }
        }
    }
    private fun showSnack(mess: String) {
        Snackbar.make(
            requireActivity().findViewById(R.id.root_layout),
            mess,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
