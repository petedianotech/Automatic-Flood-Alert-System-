package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DetectionLog::class, EmergencyContact::class, EvacuationShelter::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun detectionDao(): DetectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workbee_hydrowatch.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.detectionDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(dao: DetectionDao) {
            // Initial Detection Log
            val initialLog = DetectionLog(
                timestamp = System.currentTimeMillis(),
                ledState = LedState.GREEN.name,
                waterLevelMeters = 1.15,
                confidence = 0.98f,
                hsvDetails = "H: 124° S: 92% V: 88%",
                triggerType = "INITIALIZATION",
                detectedColorHex = "#22C55E",
                statusSummary = "System initialized. Safe river flow detected."
            )
            dao.insertLog(initialLog)

            // Default Emergency Contacts
            val contacts = listOf(
                EmergencyContact(
                    name = "River Valley Emergency Rescue",
                    role = "Disaster Rescue Team",
                    phoneNumber = "911-RESCUE",
                    isHotline = true
                ),
                EmergencyContact(
                    name = "Flood Control Headquarters",
                    role = "Local Water Management",
                    phoneNumber = "+1-800-FLOOD-ALERT",
                    isHotline = true
                ),
                EmergencyContact(
                    name = "Village Chief Office",
                    role = "Local Administration",
                    phoneNumber = "+1-555-0192-384",
                    isHotline = false
                ),
                EmergencyContact(
                    name = "Community Health Clinic",
                    role = "Emergency Medical Services",
                    phoneNumber = "+1-555-911-0088",
                    isHotline = false
                )
            )
            dao.insertContacts(contacts)

            // Default Evacuation Shelters
            val shelters = listOf(
                EvacuationShelter(
                    name = "High School Gymnasium",
                    address = "104 Hilltop Drive, Upper Village",
                    distanceKm = 1.2,
                    capacity = 350,
                    currentOccupancy = 45,
                    status = "OPEN"
                ),
                EvacuationShelter(
                    name = "Community Center Auditorium",
                    address = "12 Main Street, Central Heights",
                    distanceKm = 2.4,
                    capacity = 200,
                    currentOccupancy = 10,
                    status = "OPEN"
                ),
                EvacuationShelter(
                    name = "St. Mary's Elevated Hall",
                    address = "85 Ridge Avenue",
                    distanceKm = 3.8,
                    capacity = 150,
                    currentOccupancy = 0,
                    status = "OPEN"
                )
            )
            dao.insertShelters(shelters)
        }
    }
}
