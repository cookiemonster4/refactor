package com.elyonut.wow

import android.app.Application
import android.content.res.Resources

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        resources_ = resources
    }

    companion object {
        var instance: App? = null
            private set
        lateinit var resources_: Resources
            private set
    }

}