package com.example.myapplication.activities

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.myapplication.fragmment.*
import com.example.myapplication.utils.Contains.checkNetWorkAvailable
import com.google.android.material.snackbar.Snackbar
import java.io.File


class MainActivity : AppCompatActivity() {
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
    lateinit var btnMyPlaylist : ImageButton
    lateinit var tabName: TextView
    var songApi: SongApi
    var searchApi: SongApi

    init {
        songApi = SongApi.create()
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
                   // getTopSong(songApi)
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
                        RecommendFragment(it, this@MainActivity, p0)
                    )
                    transaction.addToBackStack(null)
                    transaction.commit()
                    searchView.clearFocus()
                }
                return true
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                return true
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
            transaction.replace(R.id.fragment_container,HomeFragment(musicService!!))
            transaction.commit()
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
            if(it.internetConnected== false && it.namePlaylist !="OFFLINE"){
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
            if (isBound ) {
                val intent = Intent(this, PlayingActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun initControlTabBar() {
        btnMyPlaylist.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container,MyPlaylistFragment(musicService = musicService!!))
            transaction.addToBackStack(null)
            transaction.commit()
            btnOnline.setImageResource(R.drawable.outline_cloud_done_24)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_24)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_checked)
        }

        btnOnline.setOnClickListener {
            supportFragmentManager.popBackStack(null,POP_BACK_STACK_INCLUSIVE)
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
                    FavouriteFragment(it, this)
                )
            }
            transaction.addToBackStack(null)
            transaction.commit()

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
                    BaseFragment(it, this)
                )
            }
            transaction.addToBackStack(null)
            transaction.commit()
            btnOnline.setImageResource(R.drawable.outline_cloud_done_24)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_checked)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_24)
        }
    }

    override fun onBackPressed() {

        if(supportFragmentManager.backStackEntryCount > 0){
            supportFragmentManager.popBackStack(null,POP_BACK_STACK_INCLUSIVE)
            btnOnline.setImageResource(R.drawable.outline_cloud_checked)
            btnFavourite.setImageResource(R.drawable.outline_folder_special_24)
            btnOffline.setImageResource(R.drawable.outline_sim_card_download_24)
            btnMyPlaylist.setImageResource(R.drawable.ic_baseline_playlist_add_24)
        } else{
            val dialog = AlertDialog.Builder(this).setTitle("Exit App ?")
                .setMessage("Do you want exit app @@ ")
                .setNegativeButton("No",null)
                .setPositiveButton("Yes",{_,_ ->
                    finish()
                })
                .show()
        }
           // super.onBackPressed()

    }
    private fun showSnack(mess: String) {
        Snackbar.make(findViewById(R.id.root_layout), mess, Snackbar.LENGTH_LONG).show()
    }
}
