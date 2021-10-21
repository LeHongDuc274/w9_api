package com.example.myapplication.fragmment

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.BaseAdapter
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BaseFragment(
    val musicService: MusicService,
    context: Context
) :
    Fragment() {


    lateinit var rvrecommnend: RecyclerView
    lateinit var close: ImageView
    lateinit var tvState: TextView
    lateinit var tvName : TextView
    lateinit var progressBar: ProgressBar
    lateinit var btnPlay : Button
    private var adapter = BaseAdapter(context)
    private var listSongLocal = mutableListOf<Song>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_base, container, false)
        initRv(view)
        setData()
        return view
    }

    private fun initRv(view: View) {
        tvState = view.findViewById(R.id.tv_state)
        progressBar = view.findViewById(R.id.progress_circular)
        tvName = view.findViewById(R.id.tv_recommed)
        btnPlay = view.findViewById(R.id.play_playlist)
        btnPlay.isClickable = false
        btnPlay.setOnClickListener {
            musicService.setPlaylist(listSongLocal, "OFFLINE")
            musicService.setNewSong(listSongLocal[0].id)
            musicService.playSong()
            musicService.sendToActivity(ACTION_CHANGE_SONG)
        }
        tvName.text = "Local Playlist"
        close = view.findViewById(R.id.close)
        close.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        rvrecommnend = view.findViewById(R.id.rv_recommed)
        rvrecommnend.adapter = adapter
        rvrecommnend.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter.setItemClick {
            clickItem(it)
        }
    }

    private fun clickItem(it: Song) {
        musicService.setPlaylist(listSongLocal, "OFFLINE")
        musicService.setNewSong(it.id)
        musicService.playSong()
        musicService.sendToActivity(ACTION_CHANGE_SONG)
        val intentService = Intent(requireActivity(), MusicService::class.java)
        requireActivity().startService(intentService)
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
                progressBar.visibility = View.GONE
                tvState.visibility = View.GONE
                adapter.setData(listSongLocal)
                if (listSongLocal.isNotEmpty()){
                    btnPlay.isClickable = true
                }
            }
        }
    }
}
