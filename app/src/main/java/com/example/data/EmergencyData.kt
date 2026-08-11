package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val role: String,
    val phoneNumber: String,
    val isHotline: Boolean = false
)

@Entity(tableName = "evacuation_shelters")
data class EvacuationShelter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val distanceKm: Double,
    val capacity: Int,
    val currentOccupancy: Int = 0,
    val status: String = "OPEN" // "OPEN", "LIMITED", "FULL"
)
