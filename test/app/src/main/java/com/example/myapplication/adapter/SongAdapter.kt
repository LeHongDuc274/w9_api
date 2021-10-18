package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.Contains.durationString

class SongAdapter : RecyclerView.Adapter<SongAdapter.ViewHolder>() {
    private var itemClick: ((String) ->Unit)? = null

    inner class ViewHolder(itemVIew: View) : RecyclerView.ViewHolder(itemVIew) {

    }

    private var listSongs = listOf<Song>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tvTitle = holder.itemView.findViewById<TextView>(R.id.tv_title)
        val tvSinger = holder.itemView.findViewById<TextView>(R.id.tv_singer)
        val tvDuration = holder.itemView.findViewById<TextView>(R.id.tv_duration)
        tvTitle.text = listSongs[position].title
        tvSinger.text = listSongs[position].artists_names
        tvDuration.text = durationString(listSongs[position].duration)
        holder.itemView.setOnClickListener {
            itemClick?.invoke(listSongs[position].id)
        }
    }

    override fun getItemCount(): Int {
        return listSongs.size
    }

    fun setData(list: List<Song>) {
        listSongs = list
        notifyDataSetChanged()
    }

    fun setItemClick(action: (String) -> Unit) {
            itemClick = action
    }
}