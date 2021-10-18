package com.example.myapplication.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.activities.MainActivity
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.Contains.durationString

class SongAdapter(val context:Context) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {
    private var itemClick: ((Song) ->Unit)? = null

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
        val iv = holder.itemView.findViewById<ImageView>(R.id.img_song)

        val imgUrl = listSongs[position].thumbnail
        Glide.with(context).load(imgUrl).centerInside().into(iv)
        holder.itemView.setOnClickListener {
            itemClick?.invoke(listSongs[position])
        }
    }

    override fun getItemCount(): Int {
        return listSongs.size
    }

    fun setData(list: List<Song>) {
        listSongs = list
        notifyDataSetChanged()
    }

    fun setItemClick(action: (Song) -> Unit) {
            itemClick = action
    }
}