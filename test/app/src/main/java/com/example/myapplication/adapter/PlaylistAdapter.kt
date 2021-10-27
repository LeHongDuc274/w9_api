package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.models.Playlist

class PlaylistAdapter() : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {
    private var action: ((Playlist) -> Unit)? = null
    class ViewHolder(itemview:View): RecyclerView.ViewHolder(itemview) {
    }
    private var listPlaylist = mutableListOf<Playlist>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_playlist,parent,false)
        )
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.tv_playlist_name).text = listPlaylist[position].playlistName
        holder.itemView.setOnClickListener {
            action?.invoke(listPlaylist[position])
        }
    }
    override fun getItemCount(): Int= listPlaylist.size

    fun setOnClickItem(click:(Playlist)->Unit){
        action = click
    }
    fun setData(list: MutableList<Playlist>){
        listPlaylist = list
        notifyDataSetChanged()
    }
}