package com.runtracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.huawei.hms.location.*
import com.huawei.hms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.huawei.hms.maps.model.LatLng
import com.runtracker.R
import com.runtracker.receivers.LocationBroadcastReceiver
import com.runtracker.utils.Constants.ACTION_PAUSE_SERVICE
import com.runtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.runtracker.utils.Constants.ACTION_STOP_SERVICE
import com.runtracker.utils.Constants.FASTEST_LOCATION_INTERVAL
import com.runtracker.utils.Constants.LOCATION_UPDATE_INTERVAL
import com.runtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.runtracker.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.runtracker.utils.Constants.NOTIFICATION_ID
import com.runtracker.utils.Constants.TIMER_UPDATE_INTERVAL
import com.runtracker.utils.FormatUtility
import com.runtracker.utils.TrackingUtility
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService: LifecycleService() {

    var isFirstRun = true
    var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val runTimeInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currNotificationBuilder: NotificationCompat.Builder

    private var pendingIntent: PendingIntent? = null
    private var activityIdentificationService: ActivityIdentificationService? = null

    companion object {
        val runTimeInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
        val totalDistance = MutableLiveData<Int>()
        val activity = MutableLiveData<String>()
        val activities = MutableLiveData<MutableList<String>>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        totalDistance.postValue(0)
        activity.postValue("")
        activities.postValue(mutableListOf())
        runTimeInSeconds.postValue(0L)
        runTimeInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        currNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        // Create an activityIdentificationService instance.
        activityIdentificationService = ActivityIdentification.getService(this);
        // Obtain a pendingIntent instance.
        pendingIntent = getPendingIntent();

        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    // Obtain PendingIntent associated with the custom static broadcast class LocationBroadcastReceiver.
    private fun getPendingIntent(): PendingIntent? {
        val intent = Intent(this, LocationBroadcastReceiver::class.java)
        intent.action = LocationBroadcastReceiver.ACTION_PROCESS_LOCATION
        return PendingIntent.getBroadcast(this, 0, intent, FLAG_UPDATE_CURRENT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // checking what the user intends to do with the service
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                    } else {
                        startTimer()
                        Timber.d("RESUMED SERVICE...")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                    Timber.d("SERVICE PAUSED")
                }
                ACTION_STOP_SERVICE -> {
                    killService()
                    Timber.d("SERVICE STOPPED")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun killService() {
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var runTime = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer() {
        // Adding the initial polyline
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true

        // Tracking the current time is a co-routine
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                // the difference between the time now and the time started
                lapTime = System.currentTimeMillis() - timeStarted
                // post a new
                runTimeInMillis.postValue(runTime + lapTime)
                if (runTimeInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    runTimeInSeconds.postValue(runTimeInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            runTime += lapTime
        }

    }


    private fun pauseService() {
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    // Updating location tracking
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Create an activity identification request.
                activityIdentificationService!!.createActivityIdentificationUpdates(
                    1000,
                    pendingIntent
                ) // Define callback for request success.
                    .addOnSuccessListener {
                        Timber.i("createActivityIdentificationUpdates onSuccess")
                    } // Define callback for request failure.
                    .addOnFailureListener { e ->
                        Timber.e("createActivityIdentificationUpdates onFailure:" + e.message)
                    }
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)

            // Stop requesting activity identification updates.
            activityIdentificationService!!.deleteActivityIdentificationUpdates(pendingIntent) // Define callback for success in stopping requesting activity identification updates.
                .addOnSuccessListener {
                    Timber.i("deleteActivityIdentificationUpdates onSuccess")
                } // Define callback for failure in stopping requesting activity identification updates.
                .addOnFailureListener { e ->
                    Timber.e("deleteActivityIdentificationUpdates onFailure:" + e.message)
                }
        }
    }

    // callback for getting actual location results
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                result?.locations?.let { locations ->
                    for (location in locations) {
                        addPathPoint(location)
                    }
                }
            }
        }
    }

    private fun addPathPoint(location: Location) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)

            }
        }
    }


    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)

        // Getting the system service for showing notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        // stating the service as foreground service
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        // Updating the time in out notification using an observer
        runTimeInSeconds.observe(this, Observer {
            if (!serviceKilled) {
                val notification = currNotificationBuilder
                    .setContentText(FormatUtility().getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if (isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //Removing all actions from a notification before updating it and adding new actions
        currNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        if (!serviceKilled) {
            // Adding a new notification action
            currNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)

            // Updating the notification
            notificationManager.notify(NOTIFICATION_ID, currNotificationBuilder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}