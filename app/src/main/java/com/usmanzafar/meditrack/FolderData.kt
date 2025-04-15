package com.usmanzafar.meditrack

import java.io.Serializable

data class FolderData(
    val name: String,
    val images: List<String>, // List of image URIs or file paths
    val schedule: List<MedicationSchedule> = listOf() // Added schedule information
) : Serializable

data class MedicationSchedule(
    val time: String, // Time of day in HH:mm format
    val daysOfWeek: List<Int>, // Days of week (1-7, where 1 is Sunday)
    val dosage: String, // Dosage information
    val notes: String = "" // Optional notes
) : Serializable