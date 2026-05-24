package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,
    val transcript: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val isNoiseCancelled: Boolean = true
)
