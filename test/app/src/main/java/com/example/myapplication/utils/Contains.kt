package com.example.myapplication.utils

object Contains {
    const val ACTION_PAUSE = 1
    const val ACTION_PLAY = 2
    const val ACTION_CHANGE_SONG = 3
    const val ACTION_PREV = 4
    const val ACTION_NEXT = 5
    const val ACTION_CANCEL = 6
    const val TYPE_ONLINE = 3
    const val TYPE_FOVOURITE = 4
    const val TYPE_OFLINE = 5
    const val TYPE_RECOMMEND = 6
    const val FROM_NOTIFY = "fromNotify"
    const val BASE_URL = "http://mp3.zing.vn"
    const val BASE_SEARCH_URL = "http://ac.mp3.zing.vn/"
    const val BASE_IMG_URL = "https://photo-resize-zmp3.zadn.vn/w94_r1x1_jpeg/"
    fun durationString(duration: Int): String {
        val minute = duration / 60
        val seconds = duration % 60
        return "${if (minute > 9) minute else "0$minute"}:${if (seconds > 9) seconds else "0$seconds"}"
    }

}