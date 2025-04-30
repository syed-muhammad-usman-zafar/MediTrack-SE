package com.usmanzafar.meditrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.widget.Toolbar
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var medicationListLayout: LinearLayout
    private lateinit var noMedicationsText: TextView
    private lateinit var selectedDateHeader: TextView
    private var folderList = mutableListOf<FolderData>()
    private val folderKey = "folderList"
    private lateinit var currentUserId: String
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Ensure user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        currentUserId = currentUser.uid

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("MediTrackPrefs", MODE_PRIVATE)

        // Initialize views
        calendarView = findViewById(R.id.calendarView)
        medicationListLayout = findViewById(R.id.medicationListLayout)
        noMedicationsText = findViewById(R.id.noMedicationsText)
        selectedDateHeader = findViewById(R.id.selectedDateHeader)

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Medication Calendar"

        // Load medications data - make sure this is called after currentUserId is set
        loadUserFolders()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up calendar date change listener
        setupCalendarListener()

        // Display medications for the current date
        displayMedicationsForDate(System.currentTimeMillis())
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
    }

    override fun onResume() {
        super.onResume()
        // Always get the latest user and reload data
        FirebaseAuth.getInstance().currentUser?.let {
            currentUserId = it.uid
            loadUserFolders()
            displayMedicationsForDate(calendarView.date)
        } ?: run {
            // If somehow there's no user on resume, return to login
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupCalendarListener() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            displayMedicationsForDate(calendar.timeInMillis)
        }
    }
    private fun displayMedicationsForDate(dateInMillis: Long) {
        // Make sure we have the latest user data before displaying
        loadUserFolders()

        // Clear previous medications
        medicationListLayout.removeAllViews()

        // Update the date header
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(dateInMillis))
        selectedDateHeader.text = formattedDate

        // Get the day of week (1-7, where 1 is Sunday)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Find medications scheduled for this day AND within the valid date range
        val medicationsForDay = findMedicationsForDay(dayOfWeek, dateInMillis)

        if (medicationsForDay.isEmpty()) {
            noMedicationsText.visibility = View.VISIBLE
        } else {
            noMedicationsText.visibility = View.GONE

            for (medication in medicationsForDay) {
                val medicationView = createMedicationView(medication)
                medicationListLayout.addView(medicationView)
            }
        }
    }

    private data class ScheduledMedication(
        val name: String,
        val time: String,
        val dosage: String,
        val notes: String,
        val hasImage: Boolean
    )

    private fun findMedicationsForDay(dayOfWeek: Int, selectedDateMillis: Long): List<ScheduledMedication> {
        val medicationsForDay = mutableListOf<ScheduledMedication>()

        // Double check that we're using the correct user data
        if (folderList.isEmpty()) {
            loadUserFolders()
        }

        // Get selected date at the beginning of the day for accurate comparison
        val selectedDate = Calendar.getInstance()
        selectedDate.timeInMillis = selectedDateMillis
        selectedDate.set(Calendar.HOUR_OF_DAY, 0)
        selectedDate.set(Calendar.MINUTE, 0)
        selectedDate.set(Calendar.SECOND, 0)
        selectedDate.set(Calendar.MILLISECOND, 0)
        val selectedDateStart = selectedDate.timeInMillis

        Log.d("CalendarActivity", "Finding medications for date: ${Date(selectedDateStart)}")

        // Ensure that folderList is not empty
        for (folder in folderList) {
            // Check if folder.schedule exists and is not empty
            if (!folder.schedule.isNullOrEmpty()) {
                for (schedule in folder.schedule) {
                    // First check if this schedule applies to the selected day of week
                    if (schedule.daysOfWeek.contains(dayOfWeek)) {
                        // Now check if the selected date is within the schedule's date range
                        val isWithinRange = isDateInScheduleRange(selectedDateStart, schedule)

                        if (isWithinRange) {
                            Log.d("CalendarActivity", "Found matching medication: ${folder.name} for ${Date(selectedDateStart)}")
                            medicationsForDay.add(
                                ScheduledMedication(
                                    name = folder.name,
                                    time = schedule.time,
                                    dosage = schedule.dosage,
                                    notes = schedule.notes,
                                    hasImage = folder.images.isNotEmpty()
                                )
                            )
                        } else {
                            Log.d("CalendarActivity", "Medication ${folder.name} not in date range for ${Date(selectedDateStart)}")
                            Log.d("CalendarActivity", "  Start: ${Date(schedule.startDate)}, End: ${if (schedule.endDate != null) Date(schedule.endDate) else "Indefinite"}")
                        }
                    }
                }
            }
        }

        // Sort medications by time
        return medicationsForDay.sortedBy { it.time }
    }


    // Helper function to check if a date is within a schedule's valid range
    private fun isDateInScheduleRange(dateToCheckMillis: Long, schedule: MedicationSchedule): Boolean {
        // If schedule doesn't have start/end dates (for backward compatibility), treat as valid
        if (!::currentUserId.isInitialized) {
            return true
        }

        // Get schedule start date at beginning of day
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = schedule.startDate
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val startDateMillis = startCal.timeInMillis

        // First check: is the selected date on or after the start date?
        if (dateToCheckMillis < startDateMillis) {
            return false
        }

        // Second check: if there's an end date, is the selected date on or before it?
        if (schedule.endDate != null) {
            val endCal = Calendar.getInstance()
            endCal.timeInMillis = schedule.endDate
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)
            val endDateMillis = endCal.timeInMillis

            return dateToCheckMillis <= endDateMillis
        }

        // No end date means it's valid indefinitely after the start date
        return true
    }

    private fun createMedicationView(medication: ScheduledMedication): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_scheduled_medication, null)

        val nameTextView: TextView = view.findViewById(R.id.medicationNameTextView)
        val timeTextView: TextView = view.findViewById(R.id.medicationTimeTextView)
        val dosageTextView: TextView = view.findViewById(R.id.medicationDosageTextView)
        val notesTextView: TextView = view.findViewById(R.id.medicationNotesTextView)
        val cardView: CardView = view.findViewById(R.id.medicationCardView)

        nameTextView.text = medication.name
        timeTextView.text = medication.time
        dosageTextView.text = "Dosage: ${medication.dosage}"

        if (medication.notes.isNotEmpty()) {
            notesTextView.visibility = View.VISIBLE
            notesTextView.text = medication.notes
        } else {
            notesTextView.visibility = View.GONE
        }

        return view
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_calendar

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this@CalendarActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }

                R.id.nav_calendar -> {
                    // We're already here
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this@CalendarActivity, UserProfileActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // Renamed to make it clearer this loads user-specific data
    private fun loadUserFolders() {
        try {
            // Safety check - make sure we have a current user ID
            if (!::currentUserId.isInitialized || currentUserId.isEmpty()) {
                FirebaseAuth.getInstance().currentUser?.let {
                    currentUserId = it.uid
                } ?: run {
                    // No user found, returning empty list
                    folderList = mutableListOf()
                    return
                }
            }

            val gson = Gson()
            // Use user-specific key to load data
            val userSpecificKey = "${folderKey}_${currentUserId}"
            val json = sharedPreferences.getString(userSpecificKey, null)

            // Log retrieval key for debugging
            Log.d("CalendarActivity", "Loading user data with key: $userSpecificKey")

            val type = object : TypeToken<MutableList<FolderData>>() {}.type

            folderList = if (json != null) {
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }

            // Log how many folders were loaded
            Log.d("CalendarActivity", "Loaded ${folderList.size} folders for user $currentUserId")

        } catch (e: Exception) {
            Log.e("CalendarActivity", "Error loading folders: ${e.message}", e)
            folderList = mutableListOf()
        }
    }
}