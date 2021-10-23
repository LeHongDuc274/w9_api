package com.example.myapplication.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.Contains.durationString

class SongAdapter(val context: Context) :
    RecyclerView.Adapter<SongAdapter.ViewHolder>() {
    private var itemClick: ((Song) -> Unit)? = null
    private var favouriteClick: ((Song) -> Unit)? = null
    private var downloadClick: ((Song) -> Unit)? = null
    private var listSongs = mutableListOf<Song>()
    inner class ViewHolder(itemVIew: View) : RecyclerView.ViewHolder(itemVIew) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tvTitle = holder.itemView.findViewById<TextView>(R.id.tv_title)
        val tvSinger = holder.itemView.findViewById<TextView>(R.id.tv_singer)
        val tvDuration = holder.itemView.findViewById<TextView>(R.id.tv_duration)
        val ivFavourite = holder.itemView.findViewById<ImageView>(R.id.iv_favourite)
        val ivDownLoad = holder.itemView.findViewById<ImageView>(R.id.download)
        val iv = holder.itemView.findViewById<ImageView>(R.id.img_song)

        if (listSongs[position].thumbnail != null) {
            var imgUrl: String? = null
            imgUrl = listSongs[position].thumbnail
            Glide.with(context).load(imgUrl).centerInside().into(iv)
        } else iv.setImageResource(R.drawable.ic_baseline_music_note_24)

        tvTitle.text = listSongs[position].title
        tvSinger.text = listSongs[position].artists_names
        tvDuration.text = durationString(listSongs[position].duration)

        // when favourite change
        if (listSongs[position].favorit) ivFavourite.setImageResource(R.drawable.ic_heart_checked)
        else ivFavourite.visibility = View.GONE

        holder.itemView.setOnClickListener { itemClick?.invoke(listSongs[position]) }
        ivFavourite.setOnClickListener {
            favouriteClick?.invoke(listSongs[position])
//            ivFavourite.visibility = View.GONE
            listSongs.removeAt(position)
            notifyDataSetChanged()
        }
        ivDownLoad.setOnClickListener { downloadClick?.invoke(listSongs[position]) }
    }


    override fun getItemCount(): Int = listSongs.size

    fun setData(list: MutableList<Song>) {
        listSongs = list
        notifyDataSetChanged()
    }

    fun setItemClick(action: (Song) -> Unit) {
        itemClick = action
    }

    fun setFavouriteClick(action: (Song) -> Unit) {
        favouriteClick = action
    }

    fun setDownloadClick(action: (Song) -> Unit) {
        downloadClick = action
    }
}