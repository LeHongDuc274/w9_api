package com.example.myapplication.fragmment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PlaylistAdapter
import com.example.myapplication.data.local.SongDao
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.models.SongInPlaylist
import com.example.myapplication.data.local.relations.SongPlaylistCrossRef
import com.example.myapplication.service.MusicService
import kotlinx.coroutines.*

class MyPlaylistFragment(
    val addSong: Boolean = false,
    val id: String? = null,
    val musicService: MusicService
) : Fragment() {

    lateinit var btn_create: Button
    lateinit var edt_name: EditText
    lateinit var rv: RecyclerView
    lateinit var btn_close: ImageButton
    var listPlaylist = mutableListOf<Playlist>()
    val adapter = PlaylistAdapter()
    lateinit var db: SongDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_playlist, container, false)
        db = SongDatabase.getInstance(requireContext().applicationContext).getDao()
        initViews(view)
        getlistPlaylist()
        return view
    }

    private fun getlistPlaylist() {
        CoroutineScope(Dispatchers.IO).launch {
            listPlaylist = db.getAllPlaylist()
            withContext(Dispatchers.Main) {
                adapter.setData(listPlaylist)
            }
        }
    }

    private fun initViews(view: View) {
        btn_close = view.findViewById(R.id.close)
        btn_create = view.findViewById(R.id.create)
        edt_name = view.findViewById(R.id.edt_playlist)
        rv = view.findViewById(R.id.rv_playlist)
        adapter.setData(listPlaylist)
        adapter.setOnClickItem {
            itemClick(it)
        }
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(
            requireActivity().applicationContext,
            LinearLayoutManager.VERTICAL,
            false
        )
        btn_create.setOnClickListener {
            createNewPlaylist()
        }
        btn_close.setOnClickListener {
            super.requireActivity().onBackPressed()
        }
    }

    private fun itemClick(playlist: Playlist) {
        if (addSong) {
            //add song to play list
            addSongtoPlaylist(playlist)
        } else {
            //show all song in playlist
            showPlaylist(playlist)
        }
    }

    private fun showPlaylist(playlist: Playlist) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.add(
            R.id.fragment_container,
            FavouriteFragment(musicService = musicService, requireActivity(), playlist.playlistName)
        )
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun addSongtoPlaylist(playlist: Playlist) {
        val song = musicService.cursong!!
        if (musicService.cursong != null) {
            val crossRef = SongPlaylistCrossRef(playlist.playlistName, id!!)
            val songInPlaylist = SongInPlaylist(
                artists_names = song.artists_names,
                duration = song.duration,
                id = song.id,
                thumbnail = song.thumbnail,
                title = song.title
            )
            CoroutineScope(Dispatchers.IO).launch {
                db.insertSongPlaylistCrossRef(crossRef)
                db.insertSongInPlaylist(songInPlaylist)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireActivity(),
                        "add to ${playlist.playlistName} successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createNewPlaylist() {
        edt_name.clearFocus()
        val name = edt_name.text.trim().toString()
        if (name.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val isPlaylistExist = db.isPlaylistExits(name)
                if (!isPlaylistExist) {
                    db.insertNewPlaylist(Playlist(name))
                    getlistPlaylist()
                    withContext(Dispatchers.Main) {
                        adapter.setData(listPlaylist)
                    }
                } else withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireActivity(),
                        "Playlist is Existed,change name",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(requireActivity(), "please enter the name", Toast.LENGTH_SHORT).show()
        }
    }
}