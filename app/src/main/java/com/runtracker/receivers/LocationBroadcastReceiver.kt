package com.runtracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.huawei.hms.location.ActivityIdentificationResponse
import com.runtracker.services.TrackingService
import com.runtracker.utils.FormatUtility
import timber.log.Timber


// Activity identification broadcast receiver.
class LocationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_PROCESS_LOCATION == action) {
                // Obtains ActivityIdentificationResponse from extras of the intent sent by the activity identification service.
                val activityIdentificationResponse =
                    ActivityIdentificationResponse.getDataFromIntent(intent)
                val activitiesIdentified = activityIdentificationResponse.activityIdentificationDatas

                for (activityIdentified in activitiesIdentified) {
                    var name = ""
                    var time = activityIdentificationResponse.time
                    when (activityIdentified.identificationActivity) {
                        100 -> {
                            name = "VEHICLE"
                        }
                        101 ->  {
                            name = "BIKE"
                        }
                        102 ->  {
                            name = "FOOT"
                        }
                        103 -> {
                            name = "STILL"
                        }
                        104 -> {
                            name = "OTHERS"
                        }
                        107 -> {
                            name = "WALKING"
                        }
                        108 -> {
                            name = "RUNNING"
                        }
                        else -> {
                            name = "UNDETECTED"
                        }
                    }
                    TrackingService.activities.value?.apply {
                        add(FormatUtility().formattedActivityListItem(time, name))
                        TrackingService.activities.postValue(this)
                    }
                    Timber.e("ACTIVITY_DETECTED: $name")
                }
            }
        }
    }

    companion object {
        // Activity identification service broadcast action.
        const val ACTION_PROCESS_LOCATION = "com.huawei.hms.location.ACTION_PROCESS_LOCATION"
    }
}