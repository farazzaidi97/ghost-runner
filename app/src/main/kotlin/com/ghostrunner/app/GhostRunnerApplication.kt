package com.ghostrunner.app

import android.app.Application
import com.ghostrunner.app.di.AppContainer

class GhostRunnerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
