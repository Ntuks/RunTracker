package com.runtracker.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs_table")
data class Run(
    var img: Bitmap? = null,
    var timestamp: Long = 0L,
    var timeInMillis: Long = 0L,
    var avgSpeedInKMH: Float = 0f,
    var caloriesBurned: Int = 0,
    var distanceInMeters: Int = 0
) {

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}