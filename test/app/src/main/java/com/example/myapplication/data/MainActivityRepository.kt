package com.example.myapplication.data

import android.Manifest
import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongInPlaylist
import com.example.myapplication.data.local.relations.SongPlaylistCrossRef
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.info.Infor
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.data.remote.search.SearchResponse
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.NetworkResponseCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivityRepository {
    companion object {
        private var instance: MainActivityRepository? = null
        fun getInstance(): MainActivityRepository {
            if (instance == null) {
                synchronized(this) {
                    instance = MainActivityRepository()
                }
            }
            return instance!!
        }
    }

    fun getTopSong(callback: NetworkResponseCallback<Song>) {
        val res = SongApi.create().getTopSong()
        res.enqueue(object : Callback<TopSongResponse> {
            override fun onResponse(
                call: Call<TopSongResponse>,
                response: Response<TopSongResponse>
            ) {
                response.body()?.let {
                    if (response.isSuccessful) {
                        callback.onNetworkSuccess(it.data.song)
                    }
                }
            }

            override fun onFailure(call: Call<TopSongResponse>, t: Throwable) {
                t.message?.let { callback.onNetworkFailure(it) }
            }
        })
    }

    fun getLocalSong(context: Context, callback: NetworkResponseCallback<Song>) {
        val listSongLocal = mutableListOf<Song>()
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
            context.contentResolver.query(
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
                    retriever.setDataSource(context.applicationContext, uri)
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
                if (listSongLocal.isNullOrEmpty()) {
                    callback.onNetworkFailure("List empty")
                } else {
                    callback.onNetworkSuccess(listSongLocal)
                }
            }
        }
    }

    fun getFavouritePlaylist(context: Context, callback: NetworkResponseCallback<Song>) {
        val db = SongDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val listSongFavourite = db.getDao().getAllSong()
            withContext(Dispatchers.Main) {
                if (listSongFavourite.isNotEmpty()) {
                    var newListFavourite = listSongFavourite.map {
                        Song(
                            artists_names = it.artists_names,
                            duration = it.duration,
                            title = it.title,
                            id = it.id,
                            thumbnail = it.thumbnail,
                            favorit = true
                        )
                    }
                    callback.onNetworkSuccess(newListFavourite)
                } else {
                    callback.onNetworkFailure("list empty")
                }
            }
        }
    }

    fun getSearchResult(text: String, callback: NetworkResponseCallback<Song>) {
        val search = text.trim()
        val searchApi = SongApi.createSearch()
        searchApi.search("artist,song", 20, search).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(
                call: Call<SearchResponse>,
                response: Response<SearchResponse>
            ) {

                if (response.isSuccessful) {
                    val body = response.body()

                    body?.let {
                        if (it.data.isNotEmpty()) {
                            val listSongSearch = it.data[0].song.toMutableList()
                            var newlistSong = listSongSearch.map {
                                Song(
                                    artists_names = it.artist,
                                    title = it.name,
                                    duration = it.duration.toInt(),
                                    thumbnail = Contains.BASE_IMG_URL + it.thumb,
                                    id = it.id
                                )
                            }.toMutableList()
                            callback.onNetworkSuccess(newlistSong)
                        } else callback.onNetworkFailure("No result")
                    }
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                callback.onNetworkFailure(t.message!!)
            }
        })
    }

    fun getlistPlaylist(context: Context, callback: NetworkResponseCallback<Playlist>) {
        val db = SongDatabase.getInstance(context.applicationContext).getDao()
        CoroutineScope(Dispatchers.IO).launch {
            val listPlaylist = db.getAllPlaylist()
            withContext(Dispatchers.Main) {
                callback.onNetworkSuccess(listPlaylist)
            }
        }
    }

    private fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                false
            }
        } else {
            true // > api 23
        }
    }

    fun downloadSong(song: Song, context: Context, callback: NetworkResponseCallback<Song>) {
        if (isStoragePermissionGranted(context)) {
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${song.id}/128")
            val fileName = song.title
            val appFile = File("/storage/emulated/0/Download/" + fileName + ".mp3")
            if (appFile.canRead()) {
                callback.onNetworkFailure("File Already Exists...")
            } else {
                callback.onNetworkSuccess(listOf(song))
                val request = DownloadManager.Request(uri)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setTitle(fileName)
                request.setDescription("downloading..")
                request.setAllowedOverRoaming(true)
                request.setDestinationInExternalPublicDir(
                    (Environment.DIRECTORY_DOWNLOADS),
                    fileName + ".mp3"
                )
                val downloadId = downloadManager.enqueue(request)
            }
        } else {
            callback.onNetworkFailure("require permission")
        }
    }

    fun removeFavourite(context: Context, song: Song, callback: NetworkResponseCallback<Song>) {
        val db = SongDatabase.getInstance(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val id = song.id
            val isExists = db.getDao().isExist(id)
            if (isExists) {
                db.getDao().deleteById(id)
                withContext(Dispatchers.Main) {
                    callback.onNetworkSuccess(listOf(song))
                }
            }
        }
    }

    fun getInfoSong(song: Song, callback: NetworkResponseCallback<String>) {
        val inforApi = SongApi.create()
        val infor = inforApi.getInfo("audio", song.id).enqueue(
            object : Callback<Infor> {
                override fun onResponse(call: Call<Infor>, response: Response<Infor>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        body?.let {
                            val listGenre = body.data.genres
                            if (listGenre.isNotEmpty()) {
                                var text = ""
                                listGenre.forEach {
                                    text = text + " " + it.name
                                }
                                callback.onNetworkSuccess(listOf(text))
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<Infor>, t: Throwable) {
                    callback.onNetworkFailure("UNKNOWN")
                }
            }
        )
    }

    fun isFavouriteSong(context: Context, song: Song, callback: NetworkResponseCallback<Boolean>) {
        val db = SongDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val isExists = db.getDao().isExist(song.id)
            withContext(Dispatchers.Main) {
                callback.onNetworkSuccess(listOf(isExists))
            }
        }
    }

    fun getRecommendSong(song: Song, callback: NetworkResponseCallback<Song>) {
        val songId = song.id
        val recommendResponses = SongApi.create().getRecommend("audio", songId)
        recommendResponses.enqueue(object : Callback<RecommendResponses> {
            override fun onResponse(
                call: Call<RecommendResponses>,
                response: Response<RecommendResponses>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        val newlist = it.data.items.toMutableList()
                        callback.onNetworkSuccess(newlist)
                    }
                }
            }

            override fun onFailure(call: Call<RecommendResponses>, t: Throwable) {
                callback.onNetworkFailure("error call api")
            }
        })
    }

    fun createNewPlaylist(
        context: Context,
        str: String,
        callback: NetworkResponseCallback<Playlist>
    ) {
        val db = SongDatabase.getInstance(context)
        if (str.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val isPlaylistExist = db.getDao().isPlaylistExits(str)
                if (!isPlaylistExist) {
                    db.getDao().insertNewPlaylist(Playlist(str))
                    val list = db.getDao().getAllPlaylist()
                    withContext(Dispatchers.Main) {
                        callback.onNetworkSuccess(list)
                    }
                } else withContext(Dispatchers.Main) {
                    callback.onNetworkFailure("Playlist is Existed,change other name")
                }
            }
        } else {
            callback.onNetworkFailure("Please enter the name")
        }
    }

    fun getListSongOnplaylist(
        context: Context,
        otherPlaylist: String,
        callback: NetworkResponseCallback<Song>
    ) {
        val db = SongDatabase.getInstance(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val listSongCrossRef = db.getDao().getSongOfPlaylist(otherPlaylist) //otherPlaylist
            if (listSongCrossRef != null) {
                withContext(Dispatchers.Main) {
                    var newList = listSongCrossRef.songs.map {
                        Song(
                            artists_names = it.artists_names,
                            duration = it.duration,
                            title = it.title,
                            id = it.id,
                            thumbnail = it.thumbnail,
                            favorit = true
                        )
                    }
                    callback.onNetworkSuccess(newList)
                }
            }
        }
    }

    fun addSongtoPlaylist(
        playlist: Playlist,
        song: Song,
        context: Context,
        callback: NetworkResponseCallback<Song>
    ) {
        val db = SongDatabase.getInstance(context.applicationContext).getDao()
        if (!song.isOffline) {
            val crossRef = SongPlaylistCrossRef(playlist.playlistName, song.id)
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
                    callback.onNetworkSuccess(listOf(song))
                }
            }
        } else {
            callback.onNetworkFailure("Can't add offline music to this playlist")
        }
    }

    fun removeSongInplaylist(
        it: Song,
        playlistName: String,
        context: Context,
        callback: NetworkResponseCallback<Song>
    ) {
        val db = SongDatabase.getInstance(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val id = it.id
            val isExistCrossRef = db.getDao().isExistCrossRef(id, playlistName)
            if (isExistCrossRef) {
                db.getDao().deleteCrossRef(id, playlistName)
            }
            withContext(Dispatchers.Main) {
                callback.onNetworkSuccess(listOf(it))
            }
        }
    }
}