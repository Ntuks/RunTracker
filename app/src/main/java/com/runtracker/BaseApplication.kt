package com.runtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class BaseApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Enables logging with the timber library
        Timber.plant(Timber.DebugTree())
    }
}