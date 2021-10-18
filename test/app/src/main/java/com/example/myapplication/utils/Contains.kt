package com.example.myapplication.utils

object Contains {
    fun durationString(duration: Int) : String{
        val minute = duration / 60
        val seconds = duration % 60
        return "${if(minute>9) minute else "0$minute"}:${if(seconds>9) seconds else "0$seconds"}"
    }
}