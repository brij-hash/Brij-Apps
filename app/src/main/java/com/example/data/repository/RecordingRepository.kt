package com.example.data.repository

import com.example.data.database.RecordingDao
import com.example.data.database.RecordingEntity
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val recordingDao: RecordingDao) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun getRecordingById(id: Int): RecordingEntity? {
        return recordingDao.getRecordingById(id)
    }

    suspend fun insertRecording(recording: RecordingEntity): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun deleteRecording(recording: RecordingEntity) {
        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteRecordingById(id: Int) {
        recordingDao.deleteRecordingById(id)
    }
}
