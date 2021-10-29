package com.example.myapplication.activities

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.widget.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.fragmment.*
import com.example.myapplication.utils.Contains.checkNetWorkAvailable
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File


class MainActivity : AppCompatActivity() {
    private var musicService: MusicService? = null
    private var isBound = false
    var searchApi: SongApi
    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: MainViewModel

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
                            musicService?.cursong?.let { it1 -> vm.setNewSong(it1) }
                            musicService?.isPlaying()?.let { it1 -> vm.setPlaybackState(it1) }
                        }
                        else -> musicService?.isPlaying()?.let { it1 -> vm.setPlaybackState(it1) }
                    }
                }
            }
        }
    }
    val broadcastInternet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    musicService?.internetConnected = checkNetWorkAvailable(this@MainActivity)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        vm = ViewModelProvider(
            this,
            MainViewModel.MainViewmodelFactory(this.application)
        )[MainViewModel::class.java]
        supportActionBar?.hide()
        setSupportActionBar(binding.toolbar)
        setupNav()
        initViews()
        observePlaylist()
        bindService()
        registerReceiver()
        vm.message.observe(this,{
            showSnack(it)
        })
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
    private fun setupNav() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val controller = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(controller)
    }
    private fun initViews() {
        binding.tvInfor.isSelected = true
        isStoragePermissionGranted()
        initControlBottomBar()
        initControlSearchView()
    }
    private fun observePlaylist() {
        vm.curList.observe(this, {
            if (!it.isNullOrEmpty()) {
                musicService?.setPlaylist(it, vm.namePlaylist.value!!)
            }
        })
    }

    private fun initControlSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                musicService?.let {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
                    val controller = navHostFragment.navController
                    controller.navigate(R.id.searchFragment)
                    binding.searchView.clearFocus()
                    if (p0 != null) {
                        vm.getSearchResult(p0)
                    }
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
            musicService?.isPlaying()?.let {
                vm.setPlaybackState(it)
            }
            musicService?.cursong?.let { it1 -> vm.setNewSong(it1) }
            observeCurSong()
            observePlaybackState()
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

    private fun observePlaybackState() {
        vm.isPlaying.observe(this, {
            if (it) {
                binding.btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
            } else binding.btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        })
    }

    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) binding.btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        else binding.btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        musicService?.let {
            if (it.internetConnected == false && it.namePlaylist != "OFFLINE") {
                showSnack("No Internet")
            }
        }
    }

    private fun observeCurSong() {
        vm.curSong.observe(this, {
            musicService?.setNewSong(it.id)
            if(vm.isPlaying.value == true) musicService?.playSong()
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
            binding.tvInfor.text = it.title
            if (vm.namePlaylist.value != "OFFLINE") {
                val imgUrl = it.thumbnail
                Glide.with(this).load(imgUrl).placeholder(R.drawable.ic_baseline_music_note_24)
                    .centerInside().into(binding.imgSong)
            } else {
                val byteArray = it.image
                byteArray.let { byteArr ->
                    if (byteArr.isEmpty()) {
                        binding.imgSong.setImageResource(R.drawable.ic_baseline_music_note_24)
                    } else {
                        binding.imgSong.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                byteArr,
                                0,
                                byteArr.size
                            )
                        )
                    }
                }
            }
        })
    }

    private fun initControlBottomBar() {
        binding.btnPause.setOnClickListener {
            if (vm.curSong.value != null) {
                val intent = Intent(this, MusicService::class.java)
                startService(intent)
                musicService?.togglePlayPause()
            } else {
                showSnack("Chưa chọn bài hát")
            }
        }
        binding.llLayout.setOnClickListener {
            if (isBound && vm.curSong.value != null) {
                val intent = Intent(this, PlayingActivity::class.java)
                startActivity(intent)
            } else {
                showSnack("Chưa chọn bài hát")
            }
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(findViewById(R.id.root_layout), mess, Snackbar.LENGTH_LONG).show()
    }
}
