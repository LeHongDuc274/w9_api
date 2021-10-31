package com.example.myapplication.activities

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf

import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.ActivityPlayingBinding
import com.example.myapplication.fragmment.AddSongToPlaylistFragment
import com.example.myapplication.fragmment.RecommendFragment
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.viewmodels.PlayingViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File

class PlayingActivity : AppCompatActivity() {

    private var fromUser = false
    private lateinit var binding: ActivityPlayingBinding
    lateinit var playingVm: PlayingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playingVm = ViewModelProvider(
            this,
            PlayingViewModel.PlayingViewmodelFactory(this.application)
        )[PlayingViewModel::class.java]
        binding = ActivityPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        changeRepeatState()
        changeShuffleState()
        observeCurSong()
        updateUiWhenChangeSong()
        listenSeekBarChange()
        obverseInfor()
        updateSeekBar()
        obverseMessage()
    }

    private fun obverseMessage() {
        playingVm.message.observe(this, {
            showSnack(it)
        })
    }

    private fun initViews() {
        binding.tvTitle.isSelected = true
        binding.tvSinger.isSelected = true
        binding.tvProgressChange.isVisible = false
        binding.btnBack.setOnClickListener {
            super.onBackPressed()
        }
        binding.btnPause.setOnClickListener {
            playingVm.togglePlayPause()
            val intent = Intent(this, MusicService::class.java)
            startService(intent)

        }
        binding.btnNext.setOnClickListener {
            playingVm.nextSong()
        }
        binding.btnPrev.setOnClickListener {
            playingVm.prevSong()
        }
        binding.btnRepeat.setOnClickListener {
            playingVm.setRepeat()

        }
        binding.btnShuffle.setOnClickListener {
            playingVm.setShuffle()

        }
        binding.ivFavourite.setOnClickListener {
            playingVm.addFavorite(playingVm.curSong.value!!)
        }

        binding.addFragment.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, RecommendFragment::class.java, null)
            transaction.addToBackStack(null)
            transaction.commit()
        }
        binding.addToPlaylist.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.fragment_container,
                AddSongToPlaylistFragment::class.java,
                bundleOf("song" to playingVm.curSong.value)
            )
            transaction.addToBackStack(null)
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
        }
    }

    private fun obverseInfor() {
        playingVm.infor.observe(this, {
            binding.tvInfor.text = it
        })
    }

    private fun changeRepeatState() {
        playingVm.isRepeat.observe(this, {
            if (it) binding.btnRepeat.setImageResource(R.drawable.ic_repeat_on)
            else binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
        })
    }

    private fun changeShuffleState() {
        playingVm.isShuffle.observe(this) {
            if (it) binding.btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
            else binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
        }
    }

    private fun updateSeekBar() {
        playingVm.curPos.observe(this, {
            if (!fromUser) {
                binding.tvCurrentDuration.text = Contains.durationString(it)
                binding.progressHorizontal.progress = it
            }
        })
    }

    private fun listenSeekBarChange() {
        binding.progressHorizontal.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            var newPos = 0
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (fromUser) newPos = p1
                binding.tvProgressChange.text = Contains.durationString(newPos)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                fromUser = true
                binding.tvProgressChange.isVisible = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                fromUser = false
                playingVm.seekTo(newPos)
                playingVm.curPos.value = newPos
                binding.tvProgressChange.isVisible = false
            }
        })
    }

    fun observeCurSong() {
        playingVm.curSong.observe(this, { curSong ->
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
            binding.tvTitle.text = curSong.title
            binding.tvSinger.text = curSong.artists_names
            binding.tvDuration.text = Contains.durationString(curSong.duration)
            binding.tvCurrentDuration.text =
                Contains.durationString(playingVm.getMediaCurrentPos() / 1000)
            binding.progressHorizontal.max = (curSong.duration)

            //image change
            if (curSong.thumbnail != null) { //online
                val imgUrl = curSong.thumbnail
                Glide.with(applicationContext).load(imgUrl).circleCrop().into(binding.ivContent)
            } else if (curSong.image.isNotEmpty()) { // offline
                binding.ivContent.setImageBitmap(
                    BitmapFactory.decodeByteArray(curSong.image, 0, curSong.image.size)
                )
            } else binding.ivContent.setImageResource(R.drawable.ic_baseline_music_note_24)
            playingVm.getInfor(curSong)
            if (curSong.isOffline) binding.ivFavourite.visibility = View.GONE
            else binding.ivFavourite.visibility = View.VISIBLE
            //favourite change
            //check existed database
            playingVm.checkIsFavou(curSong)
        })
        playingVm.isFavoriteSong.observe(this, {
            if (it) binding.ivFavourite.setImageResource(R.drawable.ic_heart_checked)
            else binding.ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
        })
    }

    private fun updateUiWhenChangeSong() {
        playingVm.isPlaying.observe(this, {
            if (!it) binding.btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            else binding.btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
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

    private fun showSnack(mess: String) {
        Snackbar.make(findViewById(R.id.root_layout), mess, Snackbar.LENGTH_LONG).show()
    }
}
