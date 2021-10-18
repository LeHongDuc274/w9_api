package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains.FROM_NOTIFY

class NotifyReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == FROM_NOTIFY) {
            val value = p1.getIntExtra("fromNotify", -1)
             Log.e("value",value.toString())
            replyToService(value, p0)
        }
    }
    private fun replyToService(value: Int, p0: Context?) {
        val intent = Intent(p0, MusicService::class.java)
        intent.putExtra("fromNotify", value)
        p0?.startService(intent)
    }
}