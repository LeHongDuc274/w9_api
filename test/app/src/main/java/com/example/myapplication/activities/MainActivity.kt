package com.example.myapplication.activities

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.Fragment
import android.content.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import kotlinx.coroutines.*
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.myapplication.data.remote.search.SearchResponse
import com.example.myapplication.fragmment.*
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.checkNetWorkAvailable
import com.example.myapplication.utils.FragmentAction
import com.google.android.material.snackbar.Snackbar
import okhttp3.Cache
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class MainActivity : AppCompatActivity(), FragmentAction {
    private var listSong = mutableListOf<Song>()
    private var musicService: MusicService? = null
    private var isBound = false
    lateinit var tvContent: TextView
    lateinit var btnPause: ImageView
    lateinit var ivContent: ImageView
    lateinit var searchView: androidx.appcompat.widget.SearchView
    lateinit var btnFavourite: ImageButton
    lateinit var btnOnline: ImageButton
    lateinit var btnOffline: ImageButton
    lateinit var btnMyPlaylist: ImageButton
    lateinit var tabName: TextView
    var searchApi: SongApi

    init {
        searchApi = SongApi.createSearch()
    }

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
    val broadcastInternet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    getTopSong()
                    musicService?.internetConnected = checkNetWorkAvailable(this@MainActivity)
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
        val filter2 = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(broadcastInternet, filter2)
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
        unregisterReceiver(broadcastInternet)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        initViews(searchApi)
        bindService()
        registerReceiver()
    }

    private fun initViews(searchApi: SongApi) {
        tvContent = findViewById(R.id.tv_infor)
        tvContent.isSelected = true
        btnPause = findViewById(R.id.btn_pause)
        ivContent = findViewById(R.id.img_song)
        searchView = findViewById(R.id.search_view)
        btnFavourite = findViewById(R.id.btn_favourite)
        btnOffline = findViewById(R.id.btn_offline)
        btnOnline = findViewById(R.id.btn_online)
        btnOnline.setImageResource(R.drawable.outline_cloud_checked)
        btnMyPlaylist = findViewById(R.id.btn_playlist)
        tabName = findViewById<TextView>(R.id.tv_tab_name)
        isStoragePermissionGranted()
        initControlBottomBar()
        initControlTabBar()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                musicService?.let {
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.add(
                        R.id.fragment_container,
                        RecommendFragment()
                    )
                    transaction.addToBackStack(null)
                    transaction.commit()
                    supportFragmentManager.executePendingTransactions()
                    searchView.clearFocus()
                    //add fragment ,load data activity-> send state and data to fragment
                    if (p0 != null) {
                        getSearchResult(p0)
                    }


                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }
        })
    }

    private fun getSearchResult(text: String) {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as? RecommendFragment
        Log.e("frag",fragment.toString())
        val search = text.trim()
        fragment?.receiverStateLoad(1, null, "loading")
        val searchApi = SongApi.createSearch()
        searchApi.search("artist,song", 20, search).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(
                call: Call<SearchResponse>,
                response: Response<SearchResponse>
            ) {

                if (response.isSuccessful) {
                    val body = response.body()
//                    "https://photo-resize-zmp3.zadn.vn/w94_r1x1_jpeg" +
//                            "/cover/4/9/d/a/49da6a1d6cf13a42e77bc3a945d9dd6b.jpg?fs=MTYzNDY4MzgyOTUwM3x3ZWJWNHw"
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
                            fragment?.receiverStateLoad(2, newlistSong, "suucess")
                        } else fragment?.receiverStateLoad(
                            2,
                            null,
                            "Not Result for this key ${search.uppercase()}"
                        )
                    }
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                showSnack("Error call api")
            }
        })
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
            musicService?.internetConnected = checkNetWorkAvailable(this@MainActivity)
            changeContent()
            changePausePlayBtn()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, HomeFragment(), "1")
            transaction.commitNow()
            supportFragmentManager.executePendingTransactions()

            getTopSong()
            isBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
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

    private fun changePausePlayBtn() {
        musicService?.let {
            if (it.isPlaying()) {
                btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
            } else btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }


    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        else btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        musicService?.let {
            if (it.internetConnected == false && it.namePlaylist != "OFFLINE") {
                showSnack("No Internet")
            }
        }
    }

    private fun changeContent() {
        tvContent.text = musicService?.cursong?.title
        musicService?.let {
            if (it.namePlaylist != "OFFLINE") {
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
            if (isBound) {
                val intent = Intent(this, PlayingActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun initControlTabBar() {
        btnMyPlaylist.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.fragment_container,
                MyPlaylistFragment()
            )
            transaction.addToBackStack(null)
            transaction.commit()
            supportFragmentManager.executePendingTransactions()

            btnOnline.setImageResource(R.drawable.outline_cloud_done_24)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_24)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_checked)
        }

        btnOnline.setOnClickListener {
            supportFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)
            btnOnline.setImageResource(R.drawable.outline_cloud_checked)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_24)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_24)
        }
        btnFavourite.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            musicService?.let {
                transaction.add(
                    R.id.fragment_container,
                    FavouriteFragment()
                )
            }
            transaction.addToBackStack(null)
            transaction.commit()
            supportFragmentManager.executePendingTransactions()


            btnOnline.setImageResource(R.drawable.outline_cloud_done_24)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_checked)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_24)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_24)

        }
        btnOffline.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            musicService?.let {
                transaction.add(
                    R.id.fragment_container,
                    BaseFragment()
                )
            }
            transaction.addToBackStack(null)
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
            btnOnline.setImageResource(R.drawable.outline_cloud_done_24)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_checked)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_24)
        }
    }

    override fun onBackPressed() {

        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)
            btnOnline.setImageResource(R.drawable.outline_cloud_checked)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_24)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_24)
        } else {
            val dialog = AlertDialog.Builder(this).setTitle("Exit App ?")
                .setMessage("Do you want exit app @@ ")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", { _, _ ->
                    val intent = Intent(this, MusicService::class.java)
                    stopService(intent)
                    unbindService(connection)
                    finish()
                })
                .show()
        }
    }

    //
//    private fun setRecommendSong() {
//        val isOffline = musicService.cursong?.isOffline ?: true
//        if (!isOffline) {
//            loadRecommendSong()
//        } else {
//            tvState.visibility = View.VISIBLE
//            progressBar.visibility = View.GONE
//            tvState.text = "OffLine Music - Not Has Recommend Song"
//        }
//    }
//
//    private fun loadRecommendSong() {
//        val songId = musicService.cursong?.id
//        progressBar.visibility = View.VISIBLE
//        tvState.visibility = View.VISIBLE
//        tvState.text = "fetching"
//        val recommendResponses = SongApi.create().getRecommend("audio", songId!!)
//        recommendResponses.enqueue(object : Callback<RecommendResponses> {
//            override fun onResponse(
//                call: Call<RecommendResponses>,
//                response: Response<RecommendResponses>
//            ) {
//                if (response.isSuccessful) {
//                    val body = response.body()
//                    body?.let {
//                        listSong = it.data.items.toMutableList()
//                        listSong.let { adapter.setData(it.toMutableList()) }
//                        progressBar.visibility = View.GONE
//                        tvState.visibility = View.GONE
//                    }
//                }
//            }
//
//            override fun onFailure(call: Call<RecommendResponses>, t: Throwable) {
//                showSnack("Error call API")
//            }
//        })
//    }

    fun getTopSong() {
        val songApi = SongApi.create()
        var response: TopSongResponse? = null
        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as? HomeFragment
        if (Contains.checkNetWorkAvailable(this)) {
            fragment?.receiverStateLoad(1, null, "fetching")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeout(3000) {
                        try {
                            response = songApi.getTopSong()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        withContext(Dispatchers.Main) {
                            response?.let {
                                val listSongTop = it.data.song.toMutableList()
                                if (!listSongTop.isEmpty()) {
                                    musicService.let {
                                        Log.e("tag", fragment.toString())
                                        fragment?.receiverStateLoad(2, listSongTop, null)
                                        if (it!!.isPlaylistEmpty()) {
                                            it.setPlaylist(listSongTop)
                                            if (it.cursong == null) it.setNewSong(listSongTop[0].id)
                                            musicService?.sendToActivity(ACTION_CHANGE_SONG)
                                            musicService?.sendToActivity(ACTION_PAUSE)
                                        }
                                    }
                                } else fragment?.receiverStateLoad(2, listSongTop, null)
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    fragment?.receiverStateLoad(3, null, "error")
                }
            }
        } else {
            fragment?.receiverStateLoad(3, null, "No internet")
        }
    }

    override fun setRecommendSong(song: Song) {

    }


    override fun clickDownload(song: Song) {
        downloadSong(song)
    }

    override fun setNewPlaylistOnFragment(newlistSong: MutableList<Song>,name:String) {
        musicService?.let {
            it.setPlaylist(newlistSong,name)
            it.setNewSong(newlistSong[0].id)
            it.playSong()
            it.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
        }
    }

    override fun setNewSongOnFragment(newSong: Song, newlistSong: MutableList<Song>,name: String) {
        musicService?.let {
            it.setPlaylist(newlistSong,name)
            it.setNewSong(newSong.id)
            it.playSong()
            it.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
        }
    }

    private fun downloadSong(song: Song) {
        if (isStoragePermissionGranted()) {

            val downloadManager =
                getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${song.id}/128")
            val fileName = song.title
            val appFile = File("/storage/emulated/0/Download/" + fileName + ".mp3")
            if (appFile.canRead()) {
                showSnack("File Already Exists...")
            } else {
                showSnack("Waiting Download...")
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
        }
    }


    private fun showSnack(mess: String) {
        Snackbar.make(findViewById(R.id.root_layout), mess, Snackbar.LENGTH_LONG).show()
    }
}
