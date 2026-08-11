package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {
    @Query("SELECT * FROM detection_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DetectionLog>>

    @Query("SELECT * FROM detection_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(): Flow<DetectionLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DetectionLog): Long

    @Query("DELETE FROM detection_logs")
    suspend fun clearAllLogs()

    // Emergency Contacts
    @Query("SELECT * FROM emergency_contacts ORDER BY isHotline DESC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContact>)

    // Shelters
    @Query("SELECT * FROM evacuation_shelters ORDER BY distanceKm ASC")
    fun getAllShelters(): Flow<List<EvacuationShelter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelters(shelters: List<EvacuationShelter>)
}
