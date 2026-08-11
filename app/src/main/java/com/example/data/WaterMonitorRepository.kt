package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterMonitorRepository(private val dao: DetectionDao) {

    val allLogs: Flow<List<DetectionLog>> = dao.getAllLogs()
    val latestLog: Flow<DetectionLog?> = dao.getLatestLog()
    val emergencyContacts: Flow<List<EmergencyContact>> = dao.getAllContacts()
    val evacuationShelters: Flow<List<EvacuationShelter>> = dao.getAllShelters()

    suspend fun saveDetectionLog(log: DetectionLog): Long {
        return dao.insertLog(log)
    }

    suspend fun clearLogs() {
        dao.clearAllLogs()
    }

    suspend fun seedContacts(contacts: List<EmergencyContact>) {
        dao.insertContacts(contacts)
    }

    suspend fun seedShelters(shelters: List<EvacuationShelter>) {
        dao.insertShelters(shelters)
    }
}
