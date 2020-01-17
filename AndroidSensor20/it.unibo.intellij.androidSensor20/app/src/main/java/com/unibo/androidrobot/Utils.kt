package com.unibo.androidrobot

class Utils {
    fun fix(str : String) : String {
        return str.replace(" ", "").replace("-", "")
    }
}