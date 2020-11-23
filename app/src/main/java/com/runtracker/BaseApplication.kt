package com.runtracker

import android.app.Application
import com.huawei.hms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class BaseApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        // Setting Huawei Map API Key
        MapsInitializer.setApiKey("CgB6e3x92FZHmP2/m7mdrL53fEkRNCSCfbFg9QhGJx0anhD/t4bE+UltlNcMGnvJOT+Cyejo1+U4wtfxRzPPke+Y")

        // Enables logging with the timber library
        Timber.plant(Timber.DebugTree())
    }
}