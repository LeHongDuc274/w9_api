package com.example.myapplication.activities

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.SongFavourite
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import com.example.myapplication.utils.Contains.TYPE_ONLINE
import kotlinx.coroutines.*
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

import android.os.Build
import android.provider.MediaStore
import com.example.myapplication.utils.Contains.TYPE_OFLINE
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private var response: TopSongResponse? = null
    private var listSong = listOf<Song>()
    private var adapter = SongAdapter(this, TYPE_ONLINE)
    private var musicService: MusicService? = null
    private var isBound = false
    lateinit var tvContent: TextView
    lateinit var btnPause: ImageView
    lateinit var ivContent: ImageView
    var listSongFavourite = listOf<SongFavourite>()
    var listSongLocal = mutableListOf<Song>()
    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", -1)
                    when (value) {
                        ACTION_CHANGE_SONG -> {
                            changeContent()
                            changeTogglePausePlayUi(ACTION_PLAY)
                        }
                        ACTION_PAUSE -> changeTogglePausePlayUi(value)
                        ACTION_PLAY -> changeTogglePausePlayUi(value)
                        ACTION_CANCEL -> changeTogglePausePlayUi(ACTION_PAUSE)
                    }
                }
            }
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter("fromNotifyToActivity")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcast,
            filter
        )
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        initViews()
        val songApi = SongApi.create()
        initRv()
        bindService()
        getTopSong(songApi)
    }

    private fun initViews() {
        tvContent = findViewById(R.id.tv_infor)
        btnPause = findViewById(R.id.btn_pause)
        ivContent = findViewById(R.id.img_song)
        initControlBottomBar()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(connection)
        unregisterReceiver()
    }

    private fun bindService() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MusicService.Mybind
            musicService = binder.getInstance()
            isBound = true
//            if (!listSong.isEmpty() && isBound) {
//                musicService?.setPlaylist(listSong)
//                changeContent()
//            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    private fun changePausePlayBtn() {
        musicService?.let {
            if (it.isPlaying()) {
                btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
            } else btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    private fun initRv() {
        val rv = findViewById<RecyclerView>(R.id.rv_top_song)
        rv.adapter = adapter
        rv.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        adapter.setItemClick { song ->
            val id = song.id
            // Toast.makeText(this,song.favorit.toString(),Toast.LENGTH_LONG).show()
            musicService?.setNewSong(id)
            musicService?.playSong()
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
            changeContent()
            changeTogglePausePlayUi(ACTION_PLAY)
            val intent = Intent(this, PlayingActivity::class.java)
            startActivity(intent)
        }

        adapter.setFavouriteClick { song ->
            addFavorite(song)
        }
        adapter.setDownloadClick { song ->
            downloadSong(song)
        }
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }

    private fun downloadSong(song: Song) {
        if (isStoragePermissionGranted()) {

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${song.id}/128")
            val fileName = song.title
            val request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(fileName)
            request.setDescription("downloading..")
            request.setAllowedOverRoaming(false)
            request.setDestinationInExternalPublicDir(
                (Environment.DIRECTORY_DOWNLOADS),
                fileName + ".mp3"
            )
//            request.setDestinationUri(
//                Uri.fromFile(
//                    File(
//                        applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
//                        fileName + ".mp3"
//                    )
//                )
//            )
            val reference = downloadManager.enqueue(request)
        }
    }

    private fun addFavorite(song: Song) {
        val db = SongDatabase.getInstance(applicationContext)
        val songFavourite =
            song.thumbnail?.let {
                SongFavourite(
                    song.artists_names, song.duration, song.id,
                    it, song.title
                )
            }
        CoroutineScope(Dispatchers.IO).launch {
            if (songFavourite != null) {
                db.getDao().insert(songFavourite)
            }
        }
    }

    private fun showProgress() {

    }

    private fun hideProgress() {
        val fetch = findViewById<TextView>(R.id.fetch)
        val progressBar = findViewById<ProgressBar>(R.id.progress_circular)

    }

    private fun retry(retry: () -> Unit) {

    }

    private fun getTopSong(songApi: SongApi) {
        val fetch = findViewById<TextView>(R.id.fetch)
        val progressBar = findViewById<ProgressBar>(R.id.progress_circular)
        fetch.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        fetch.text = "fetching"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(3000) {
                    response = songApi.getTopSong()
                    withContext(Dispatchers.Main) {
                        response?.let {
                            listSong = it.data.song
                            adapter.setData(listSong)
                            fetch.visibility = View.GONE
                            progressBar.visibility = View.GONE
                            if (!listSong.isEmpty()) {
                                musicService?.let {
                                    it.setPlaylist(listSong)
                                    if (it.cursong == null) {
                                        it.setNewSong(listSong[0].id)
                                    }
                                }
                                changeContent()
                                changePausePlayBtn()
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    fetch.text = "Retry"
                    fetch.setOnClickListener {
                        getTopSong(songApi)
                    }
                }
            }
        }
        getFavouriteSong()
    }

    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) {
            btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        }
    }

    private fun changeContent() {
        tvContent.text = musicService?.cursong?.title
        musicService?.let {
            if (it.isPlayOnline) {
                val imgUrl = it.cursong?.thumbnail
                Glide.with(this).load(imgUrl).placeholder(R.drawable.ic_baseline_music_note_24)
                    .centerInside().into(ivContent)
            } else {
                val byteArray = it.cursong?.image
                byteArray?.let {
                    if (it.isEmpty()) {
                        ivContent.setImageResource(R.drawable.ic_baseline_music_note_24)
                    } else {
                        ivContent.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                byteArray,
                                0,
                                it.size
                            )
                        )
                    }
                }
            }
        }

    }

    private fun initControlBottomBar() {
        btnPause = findViewById<ImageView>(R.id.btn_pause)
        val layout = findViewById<RelativeLayout>(R.id.ll_layout)

        btnPause.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
            musicService?.togglePlayPause()
        }

        layout.setOnClickListener {
            if (isBound && listSong.isNotEmpty()) {
                val intent = Intent(this, PlayingActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun getFavouriteSong() {
        val db = SongDatabase.getInstance(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            listSongFavourite = db.getDao().getAllSong()
            withContext(Dispatchers.Main) {
                adapter.setListFavourite(listSongFavourite)
            }
        }
    }

    private fun getLocalSong() {
        if (isStoragePermissionGranted()) {
            val collection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
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
            val selectionArgs = arrayOf(
                TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES).toString()
            )
            val query = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )
            val retriever = MediaMetadataRetriever()
            val fetch = findViewById<TextView>(R.id.fetch)
            val progressBar = findViewById<ProgressBar>(R.id.progress_circular)
            fetch.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            fetch.text = "fetching"
            listSongLocal.clear()
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
                        retriever.setDataSource(applicationContext, uri)
                        val bitmap = retriever.embeddedPicture ?: byteArrayOf()
                        val songLocal = Song(
                            artists_names = singer,
                            duration = duration / 1000,
                            id = id.toString(),
                            title = title,
                            image = bitmap
                        )
                        withContext(Dispatchers.Main) {
                            listSongLocal.add(songLocal)
                        }
                    }
                    cursor.close()
                }
                withContext(Dispatchers.Main) {
                    fetch.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    adapter.setData(listSongLocal)
                    musicService?.setPlaylistOffline(listSongLocal)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.online -> {
                adapter.setData(listSong)
                musicService?.isPlayOnline = true
            }
            R.id.favourite -> {
                val db = SongDatabase.getInstance(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    listSongFavourite = db.getDao().getAllSong()
                    withContext(Dispatchers.Main) {
                        adapter.setListFavourite(listSongFavourite)
                        val newList: List<Song> = listSongFavourite.map {
                            Song(
                                artists_names = it.artists_names,
                                duration = it.duration,
                                title = it.title,
                                id = it.id,
                                thumbnail = it.thumbnail,
                            )
                        }
                        adapter.setData(newList)
                        musicService?.isPlayOnline = true
                    }
                }
            }
            R.id.local_song -> {
                getLocalSong()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
