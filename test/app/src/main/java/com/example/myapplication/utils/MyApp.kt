package com.example.myapplication.utils

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.myapplication.data.local.SongDatabase

class MyApp : Application() {
    companion object {
        val CHANNEL_ID = "channel id"
        val CHANNEL_NAME = "channel name"
    }

   // var instance : MyApp? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
//     if(instance==null){
//         instance = this
//     }
    }
//    @JvmName("getInstance1")
//    fun getInstance() : MyApp{
//        return instance!!
//    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null, null)
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}