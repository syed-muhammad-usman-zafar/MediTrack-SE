package com.usmanzafar.meditrack

import java.io.Serializable

data class FolderData(
    val name: String,
    val images: List<String>, // List of image URIs or file paths
    val schedule: List<MedicationSchedule> = listOf() // Added schedule information
) : Serializable

data class MedicationSchedule(
    val time: String,
    val daysOfWeek: List<Int>,
    val dosage: String,
    val notes: String = "",
    val startDate: Long = System.currentTimeMillis(), // Default to today
    val endDate: Long? = null // Null means no end date
) : Serializable