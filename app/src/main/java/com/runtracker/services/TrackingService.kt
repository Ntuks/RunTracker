package com.runtracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.runtracker.R
import com.runtracker.ui.MainActivity
import com.runtracker.utils.Constants.ACTION_PAUSE_SERVICE
import com.runtracker.utils.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.runtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.runtracker.utils.Constants.ACTION_STOP_SERVICE
import com.runtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.runtracker.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.runtracker.utils.Constants.NOTIFICATION_ID
import timber.log.Timber

class TrackingService: LifecycleService() {

    var isFirstRun = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // checking what the user intends to do with the service
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                    } else {
                        Timber.d("RESUMED SERVICE...")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("SERVICE PAUSED")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("SERVICE STOPPED")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(){
        // Getting the system service for showing notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        // creating the notification
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Run Tracker")
            .setContentText("00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        // stating the service as foreground service
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )
}