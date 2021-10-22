package com.example.myapplication.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.utils.Contains

class BaseAdapter(val context: Context) :
    RecyclerView.Adapter<BaseAdapter.ViewHolder>() {
    private var itemClick: ((Song) -> Unit)? = null
    private var listSongs = listOf<Song>()
    private var listFavourite = listOf<SongFavourite>()

    inner class ViewHolder(itemVIew: View) : RecyclerView.ViewHolder(itemVIew) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        )
    }


    override fun onBindViewHolder(holder: BaseAdapter.ViewHolder, position: Int) {
        val tvTitle = holder.itemView.findViewById<TextView>(R.id.tv_title)
        val tvSinger = holder.itemView.findViewById<TextView>(R.id.tv_singer)
        val tvDuration = holder.itemView.findViewById<TextView>(R.id.tv_duration)
        val ivFavourite = holder.itemView.findViewById<ImageView>(R.id.iv_favourite)
        val ivDownLoad = holder.itemView.findViewById<ImageView>(R.id.download)
        val iv = holder.itemView.findViewById<ImageView>(R.id.img_song)
        ivDownLoad.visibility = View.GONE
        ivFavourite.visibility = View.GONE
        //image off
        if (listSongs[position].image.isNotEmpty()) {
            iv.setImageBitmap(
                BitmapFactory.decodeByteArray(
                    listSongs[position].image,
                    0,
                    listSongs[position].image.size
                )
            )
        } else iv.setImageResource(R.drawable.ic_baseline_music_note_24)
        //text off
        tvTitle.text = listSongs[position].title
        tvSinger.text = listSongs[position].artists_names
        tvDuration.text = Contains.durationString(listSongs[position].duration)
        holder.itemView.setOnClickListener { itemClick?.invoke(listSongs[position]) }
    }

    override fun getItemCount(): Int = listSongs.size

    fun setData(list: List<Song>) {
        listSongs = list
        notifyDataSetChanged()
    }

    fun setItemClick(action: (Song) -> Unit) {
        itemClick = action
    }

}
