package com.ghostrunner.wear

import android.app.Application
import com.ghostrunner.wear.di.AppContainer

class GhostRunnerWearApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
