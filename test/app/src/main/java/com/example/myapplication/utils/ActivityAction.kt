package com.example.myapplication.utils

interface ActivityAction {
    fun loading(state: Int = 1,mess: String)
    fun success(state: Int = 2 ,mess: String)
}