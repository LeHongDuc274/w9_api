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
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.fragmment.*
import com.example.myapplication.utils.Contains.ACTION_CHANGE_PLAYLIST
import com.example.myapplication.utils.Contains.checkNetWorkAvailable
import com.example.myapplication.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File


class MainActivity : AppCompatActivity() {
    var searchApi: SongApi
    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: MainViewModel

    init {
        searchApi = SongApi.createSearch()
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
        vm.message.observe(this, {
            showSnack(it)
        })
    }
//    private fun registerReceiver() {

//    }

    override fun onRestart() {
        super.onRestart()
        if (vm.curSong.value != null) vm.getCursong()

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
        observePlaybackState()
        observeCurSong()
    }

    private fun initControlSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
                val controller = navHostFragment.navController
                controller.navigate(R.id.searchFragment)
                binding.searchView.clearFocus()
                if (p0 != null) {
                    vm.getSearchResult(p0)
                }

                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }
        })
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

//            if (it.internetConnected == false && it.namePlaylist != "OFFLINE") {
//                showSnack("No Internet")
//            }

    private fun observeCurSong() {
        vm.curSong.observe(this, { song ->
            if (song.thumbnail != null) { //online
                val imgUrl = song.thumbnail
                Glide.with(applicationContext).load(imgUrl).circleCrop().into(binding.imgSong)
            } else if (song.image.isNotEmpty()) { // offline
                binding.imgSong.setImageBitmap(
                    BitmapFactory.decodeByteArray(song.image, 0, song.image.size)
                )
            }
            binding.tvInfor.text = song.title
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
        })
    }


    private fun initControlBottomBar() {
        binding.btnPause.setOnClickListener {
            if (vm.curSong.value != null) {
                if(!vm.internetState.value!! && !vm.curSong.value!!.isOffline){
                    showSnack("Offline Mode")
                }
                val intent = Intent(this, MusicService::class.java)
                startService(intent)
                vm.togglePlayPause()
            } else {
                showSnack("Chưa chọn bài hát")
            }
        }
        binding.llLayout.setOnClickListener {
            if (vm.curSong.value != null) {
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
