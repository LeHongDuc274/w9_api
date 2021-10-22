package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.relations.SongPlaylistCrossRef

@Database(
    entities = [SongFavourite::class, Playlist::class, SongPlaylistCrossRef::class],
    version = 1
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun getDao(): SongDao

    companion object {
        @Volatile
        private var instance: SongDatabase? = null
        fun getInstance(context: Context): SongDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context,
                    SongDatabase::class.java,
                    "songDatabase"
                ).build()
            }
            return instance!!
        }
    }
}