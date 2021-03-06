package com.example.myapplication.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.example.myapplication.R
import com.example.myapplication.activities.MainActivity
import com.example.myapplication.activities.PlayingActivity
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.receiver.NotifyReceiver
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_PLAYLIST
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_NEXT
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import com.example.myapplication.utils.Contains.ACTION_PREV
import com.example.myapplication.utils.Contains.FROM_NOTIFY
import com.example.myapplication.utils.MyApp
import kotlinx.coroutines.*
import java.io.Serializable
import java.lang.IllegalStateException
import kotlin.random.Random

class MusicService : Service() {
    private var mediaPlayer = MediaPlayer()

    private var playlist = listOf<Song>()
    var cursong: Song? = null
    private var songPos = 0
    var shuffle = false
    var repeat = false
    var namePlaylist = "ONLINE"
    var internetConnected = false

    inner class Mybind() : Binder() {
        fun getInstance(): MusicService {
            return this@MusicService
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        return Mybind()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pushNotification(cursong!!)
        val actionFromNotify = intent?.getIntExtra("fromNotify", -1)
        actionFromNotify?.let { handlerActionFromNotify(actionFromNotify) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun isPlaying() = mediaPlayer.isPlaying
    fun setPlaylist(list: List<Song>, name: String = "ONLINE") {
        namePlaylist = name
        playlist = list
    }

    fun getPlaylist() = playlist
    fun isPlaylistEmpty() = if (playlist.isEmpty()) true else false
    fun playOneSong(song: Song) {
        // dang dinh dat lai cursong o day
        cursong = song
        mediaPlayer.stop()
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            cursong!!.id.toLong()
        )
        mediaPlayer = MediaPlayer.create(applicationContext, uri)

        mediaPlayer.setOnCompletionListener {
            nextSong()
        }
       // sendToActivity(ACTION_CHANGE_SONG)
    }

    fun setNewSong(song:Song) {
        if (song.id != cursong?.id) {
            if (namePlaylist != "OFFLINE") {
                cursong = playlist.find {
                    it.id == song.id
                } ?: song
                songPos = playlist.indexOf(cursong)
                mediaPlayer.stop()
                val uri =
                    Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${cursong!!.id}/128")
                if (internetConnected) mediaPlayer = MediaPlayer.create(applicationContext, uri)
                else mediaPlayer.reset()
                mediaPlayer.setOnCompletionListener {
                    nextSong()
                }
            } else {
                cursong = playlist.find {
                    it.id == song.id
                } ?: song
                songPos = playlist.indexOf(cursong)
                mediaPlayer.stop()
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursong!!.id.toLong()
                )
                mediaPlayer = MediaPlayer.create(applicationContext, uri)
                mediaPlayer.setOnCompletionListener {
                    nextSong()
                }
            }
        }
    }


    fun playSong() {
        try {
            mediaPlayer.start()
            sendToActivity(ACTION_PAUSE)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun setRepeat() {
        repeat = !repeat
        if (repeat) {
            // repeat == true
            mediaPlayer.isLooping = true
        } else if (!repeat) {
            //repeat = false
            mediaPlayer.isLooping = false
        }
    }

    fun setShuffle() {
        shuffle = !shuffle
    }

    fun nextSong() {
        if (!repeat && !shuffle) { // repeat disable _>next
            if (songPos < playlist.size - 1) {
                songPos++
                val newSong = playlist[songPos]
                setNewSong(newSong)
                playSong()
            }
        } else if (!repeat && shuffle) { // next random
            val random = Random.nextInt(songPos, playlist.size)
            setNewSong(playlist[random])
            playSong()
        } else { // repeat enable -> seek to start
            mediaPlayer.seekTo(0)
            mediaPlayer.isLooping = true
            playSong()
        }
        pushNotification(cursong!!)
        sendToActivity(ACTION_CHANGE_SONG)
    }

    fun prevSong() {
        if (mediaPlayer.currentPosition > 20000) {
            mediaPlayer.seekTo(0)
            return
        } else if (songPos > 0) {
            songPos--
            val newSong = playlist[songPos]
            setNewSong(newSong)
            playSong()
        }
        sendToActivity(ACTION_CHANGE_SONG)
        pushNotification(cursong!!)

    }

    fun getCurSong() = cursong

    fun getMediaCurrentPos() = mediaPlayer.currentPosition

    fun seekTo(newPos: Int) {
        mediaPlayer.seekTo(newPos * 1000)
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            sendToActivity(ACTION_PAUSE)
        } else {
            mediaPlayer.start()
            sendToActivity(ACTION_PLAY)
        }
    }

    fun sendToActivity(action: Int) {
        val intent = Intent()
        intent.action = "fromNotifyToActivity"
        intent.putExtra("fromNotifyToActivity", action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun updateDuration() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val curPos = this@MusicService.getMediaCurrentPos()
                withContext(Dispatchers.Main) {
                    sendUpdatePos(curPos / 1000)
                }
                delay(300)
            }
        }
    }

    private fun sendUpdatePos(curPos: Int) {
        val intent = Intent()
        intent.action = "updatePosition"
        intent.putExtra("value", curPos)
        LocalBroadcastManager.getInstance(this@MusicService).sendBroadcast(intent)
    }

    private fun handlerActionFromNotify(actionFromNotify: Int) {
        when (actionFromNotify) {
            ACTION_PAUSE -> {
                togglePlayPause()
                pushNotification(cursong!!)
            }
            ACTION_PREV -> {
                prevSong()
            }
            ACTION_NEXT -> {
                nextSong()
            }
            ACTION_CANCEL -> {
                stopForeground(true)
                mediaPlayer.stop()
                sendToActivity(ACTION_CANCEL)
            }
            else -> Unit
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun pushNotification(song: Song) {
        val remoteView = RemoteViews(packageName, R.layout.notify_layout)
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification =
            NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setCustomContentView(remoteView)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pending)
                .setSound(null)
                .build()

        initRemoteView(remoteView, song, notification)
        initControlRemoteView(remoteView, song)
        startForeground(1, notification)
    }

    private fun initControlRemoteView(remoteView: RemoteViews, song: Song) {
        remoteView.apply {
            setOnClickPendingIntent(R.id.btn_next, getPendingIntent(ACTION_NEXT))
            setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(ACTION_PREV))
            setOnClickPendingIntent(R.id.btn_pause, getPendingIntent(ACTION_PAUSE))
            setOnClickPendingIntent(R.id.btn_cancel, getPendingIntent(ACTION_CANCEL))
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntent(action: Int): PendingIntent {
        val intent = Intent(this, NotifyReceiver::class.java)
        intent.putExtra("fromNotify", action)
        intent.action = FROM_NOTIFY
        return PendingIntent.getBroadcast(
            applicationContext,
            action,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun initRemoteView(
        remoteView: RemoteViews,
        song: Song,
        notification: Notification
    ) {
        remoteView.setTextViewText(R.id.tv_title, song.title)
        remoteView.setTextViewText(R.id.tv_singer, song.artists_names)
        // setImageNotify(remoteView)
        if (song.thumbnail != null) {
            val imgUrl = song.thumbnail
            val target: NotificationTarget =
                NotificationTarget(this, R.id.iv_notify, remoteView, notification, 1)
            Glide.with(applicationContext).asBitmap().load(imgUrl).into(target)
        } else {
            setImageNotify(remoteView)
        }
        //set btnPause
        remoteView.setImageViewResource(
            R.id.btn_pause,
            if (mediaPlayer.isPlaying) R.drawable.outline_pause_circle_black_24 else R.drawable.outline_not_started_black_24
        )
    }

    private fun setImageNotify(remoteView: RemoteViews) {
        val byteArray = cursong?.image
        if (byteArray!!.isNotEmpty()) {
            remoteView.setImageViewBitmap(
                R.id.iv_notify,
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            )
        } else remoteView.setImageViewResource(R.id.iv_notify, R.drawable.ic_baseline_music_note_24)
    }
}