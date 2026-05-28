package com.example.homeserve

import android.app.Application
import android.content.Context

class HomeServeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private var instance: HomeServeApp? = null

        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }
}
