package com.usmanzafar.meditrack

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.view.LayoutInflater
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class TrackMedicationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var folderAdapter: FolderAdapter
    private var folderList = mutableListOf<FolderData>()
    private var filteredFolderList = mutableListOf<FolderData>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var searchView: SearchView
    private val folderKey = "folderList"
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_medications)


        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserId = currentUser?.uid ?: "anonymous"

        sharedPreferences = getSharedPreferences("MediTrackPrefs", MODE_PRIVATE);

        loadFolders()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Create a copy of the folder list for filtering
        filteredFolderList = folderList.toMutableList()

        // Updated adapter with delete functionality and long click for sharing
        folderAdapter = FolderAdapter(
            filteredFolderList,
            { folder -> onFolderClicked(folder) },
            { position -> deleteFolder(position) },
            { folder -> showFolderOptions(folder) } // Changed to options menu
        )

        recyclerView.adapter = folderAdapter

        // Initialize SearchView and set up its functionality
        searchView = findViewById(R.id.search_bar)
        setupSearchView()

        val fabAddFolder: FloatingActionButton = findViewById(R.id.fab_add_folder)
        fabAddFolder.setOnClickListener {
            showFolderCreationDialog()
        }

        // Setup bottom navigation
        setupBottomNavigation()

        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_calendar

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this@TrackMedicationsActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }

                R.id.nav_calendar -> {
                    val intent = Intent(this@TrackMedicationsActivity, CalendarActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterFolders(newText)
                return true
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterFolders(query: String?) {
        filteredFolderList.clear()

        if (query.isNullOrEmpty()) {
            filteredFolderList.addAll(folderList)
        } else {
            val searchQuery = query.lowercase()
            for (folder in folderList) {
                if (folder.name.lowercase().contains(searchQuery)) {
                    filteredFolderList.add(folder)
                }
            }
        }
        folderAdapter.notifyDataSetChanged()
    }

    private val folderActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val folderName = data.getStringExtra("folder_name") ?: return@let
                val folderImages = data.getStringArrayListExtra("folder_images") ?: return@let

                // Update folder in the list
                val folderIndex = folderList.indexOfFirst { it.name == folderName }
                if (folderIndex != -1) {
                    // Preserve the schedule when updating images
                    val currentSchedule = folderList[folderIndex].schedule
                    folderList[folderIndex] = FolderData(folderName, folderImages, currentSchedule)

                    // Update filtered list if folder is there
                    val filteredIndex = filteredFolderList.indexOfFirst { it.name == folderName }
                    if (filteredIndex != -1) {
                        filteredFolderList[filteredIndex] = FolderData(folderName, folderImages, currentSchedule)
                        folderAdapter.notifyItemChanged(filteredIndex)
                    }
                    saveFolders()
                }
            }
        }
    }

    private fun onFolderClicked(folder: FolderData) {
        // Pass the folder details to the FolderActivity
        val intent = Intent(this, ImageActivity::class.java)
        // You can pass the folder's name and images as extras
        intent.putExtra("folder_name", folder.name)
        intent.putStringArrayListExtra("folder_images", ArrayList(folder.images))
        folderActivityLauncher.launch(intent)
    }

    // method to show options when a folder is selected (long-pressed)
    private fun showFolderOptions(folder: FolderData) {
        val options = arrayOf("Schedule Medication", "Share")

        AlertDialog.Builder(this)
            .setTitle("${folder.name} Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showScheduleDialog(folder)
                    1 -> showSharingOptions(folder)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // New method to show medication scheduling dialog
    private fun showScheduleDialog(folder: FolderData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_medication, null)

        val timeTextView: TextView = dialogView.findViewById(R.id.timeTextView)
        val dosageInput: EditText = dialogView.findViewById(R.id.dosageInput)
        val notesInput: EditText = dialogView.findViewById(R.id.notesInput)
        val daysChipGroup: ChipGroup = dialogView.findViewById(R.id.daysChipGroup)

        // Set current time
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        timeTextView.text = formatTime(hour, minute)

        // Set time picker on click
        timeTextView.setOnClickListener {
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                timeTextView.text = formatTime(selectedHour, selectedMinute)
            }, hour, minute, false).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Schedule ${folder.name}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Collect selected days
                val selectedDays = mutableListOf<Int>()
                for (i in 0 until daysChipGroup.childCount) {
                    val chip = daysChipGroup.getChildAt(i) as Chip
                    if (chip.isChecked) {
                        // Convert day position to Calendar day constant (Sunday=1, Monday=2, etc.)
                        selectedDays.add(i + 1)
                    }
                }

                if (selectedDays.isEmpty()) {
                    Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val time = timeTextView.text.toString()
                val dosage = dosageInput.text.toString().ifEmpty { "1 tablet" }
                val notes = notesInput.text.toString()

                // Create a new schedule
                val newSchedule = MedicationSchedule(time = time, daysOfWeek = selectedDays, dosage = dosage, notes = notes)

                // Update the folder with the new schedule
                updateFolderSchedule(folder, newSchedule)

                Toast.makeText(this, "${folder.name} scheduled for ${selectedDays.size} days", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun updateFolderSchedule(folder: FolderData, newSchedule: MedicationSchedule) {
        // Find the folder in the original list
        val folderIndex = folderList.indexOfFirst { it.name == folder.name }
        if (folderIndex != -1) {
            // Create a new schedule list with the existing schedules plus the new one
            val updatedSchedules = folder.schedule.toMutableList()
            updatedSchedules.add(newSchedule)

            // Create updated folder with new schedule
            val updatedFolder = FolderData(folder.name, folder.images, updatedSchedules)

            // Update in both lists
            folderList[folderIndex] = updatedFolder

            val filteredIndex = filteredFolderList.indexOfFirst { it.name == folder.name }
            if (filteredIndex != -1) {
                filteredFolderList[filteredIndex] = updatedFolder
                folderAdapter.notifyItemChanged(filteredIndex)
            }

            saveFolders()
        }
    }

    // Method to share via specific app package
    private fun shareViaApp(folder: FolderData, packageName: String) {
        if (folder.images.isEmpty()) {
            Toast.makeText(this, "No images to share", Toast.LENGTH_SHORT).show()
            return
        }

        // Create an intent to share
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"

            // Convert string paths to Uri objects
            val imageUris = ArrayList<Uri>()
            for (imagePath in folder.images) {
                imageUris.add(Uri.parse(imagePath))
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)

            putExtra(Intent.EXTRA_SUBJECT, "Shared Medical Prescription")
            putExtra(Intent.EXTRA_TEXT, "Prescription details shared using MediTrack. This may include important medical information. Please handle with care.")
            setPackage(packageName)
        }

        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed or sharing not supported", Toast.LENGTH_SHORT).show()
            // Fallback to general sharing if specific app isn't available
            val fallbackIntent = Intent.createChooser(shareIntent.setPackage(null), "Share via")
            startActivity(fallbackIntent)
        }
    }

    // Method specifically for Bluetooth sharing
    private fun shareViaBluetooth(folder: FolderData) {
        if (folder.images.isEmpty()) {
            Toast.makeText(this, "No images to share", Toast.LENGTH_SHORT).show()
            return
        }

        // Create an intent to share via Bluetooth
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"

            // Convert string paths to Uri objects
            val imageUris = ArrayList<Uri>()
            for (imagePath in folder.images) {
                imageUris.add(Uri.parse(imagePath))
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)

            // Set Bluetooth package
            setPackage("com.android.bluetooth")
        }

        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback if Bluetooth package isn't available or varies by device
            val fallbackIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                val imageUris = ArrayList<Uri>()
                for (imagePath in folder.images) {
                    imageUris.add(Uri.parse(imagePath))
                }
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            }
            startActivity(Intent.createChooser(fallbackIntent, "Share via Bluetooth"))
        }
    }

    // New method to show sharing options when a folder is selected (long-pressed)
    private fun showSharingOptions(folder: FolderData) {
        if (folder.images.isEmpty()) {
            Toast.makeText(this, "No images to share in ${folder.name}", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a dialog with sharing options
        val options = arrayOf("Facebook", "WhatsApp", "Bluetooth", "Gmail")

        AlertDialog.Builder(this)
            .setTitle("Share ${folder.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareViaApp(folder, "com.facebook.katana")
                    1 -> shareViaApp(folder, "com.whatsapp")
                    2 -> shareViaBluetooth(folder)
                    3 -> shareViaApp(folder, "com.google.android.gm")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFolderCreationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_folder_creation, null)
        val folderNameInput = dialogView.findViewById<TextInputEditText>(R.id.folderNameInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add new medicine")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val folderName = folderNameInput.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    val newFolder = FolderData(folderName, listOf())  // Create folder with no images for now
                    addFolder(newFolder)
                    Toast.makeText(this, "prescription : $folderName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Medicine name can't be empty!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addFolder(folder: FolderData) {
        folderList.add(folder)
        // Add to filtered list as well if it should be visible
        val query = searchView.query?.toString() ?: ""
        if (query.isEmpty() || folder.name.lowercase().contains(query.lowercase())) {
            filteredFolderList.add(folder)
        }
        folderAdapter.notifyDataSetChanged()
        saveFolders()
    }

    // Updated method to delete a folder
    private fun deleteFolder(position: Int) {
        if (position >= 0 && position < filteredFolderList.size) {
            val folder = filteredFolderList[position]
            val folderName = folder.name

            // Remove from the filtered list
            filteredFolderList.removeAt(position)

            // Also remove from the original list
            folderList.remove(folder)

            folderAdapter.notifyItemRemoved(position)
            saveFolders()
            Toast.makeText(this, "Medicine '$folderName' deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFolders() {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(folderList)
        // Use user-specific key
        editor.putString("${folderKey}_${currentUserId}", json)
        editor.apply()
    }

    private fun loadFolders() {
        val gson = Gson()
        // Use user-specific key
        val json = sharedPreferences.getString("${folderKey}_${currentUserId}", null)
        val type = object : TypeToken<MutableList<FolderData>>() {}.type
        folderList = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}