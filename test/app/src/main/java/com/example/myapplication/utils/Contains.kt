package com.example.myapplication.utils

object Contains {
    const val ACTION_PAUSE = 1
    const val ACTION_PLAY = 2
    const val ACTION_CHANGE_SONG = 3
    const val ACTION_PREV = 4
    const val ACTION_NEXT = 5
    const val ACTION_CANCEL = 6
    const val FROM_NOTIFY = "fromNotify"
    fun durationString(duration: Int): String {
        val minute = duration / 60
        val seconds = duration % 60
        return "${if (minute > 9) minute else "0$minute"}:${if (seconds > 9) seconds else "0$seconds"}"
    }
}