package com.usmanzafar.meditrack

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var searchView: SearchView
    private var imageList = mutableListOf<ImageData>()
    private var filteredImageList = mutableListOf<ImageData>()
    private var folderName: String = ""
    private var currentPhotoPath: String = ""
    private val folderKey = "folderList"
    private lateinit var currentUserId: String

    // Create a temporary file for camera photos
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile(
            imageFileName, ".jpg", storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Activity result launcher for capturing images
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Save the captured image to MediaStore
            val mediaSavedPath = MediaStoreHelper.saveCapturedImageToMediaStore(
                this, folderName, currentPhotoPath
            )

            if (mediaSavedPath != null) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val newImage = ImageData(mediaSavedPath, "Image $timeStamp", timeStamp)
                imageList.add(newImage)

                // Add to filtered list if it matches the current query
                val query = searchView.query?.toString() ?: ""
                if (matchesSearchQuery(newImage, query)) {
                    filteredImageList.add(newImage)
                    imageAdapter.notifyItemInserted(filteredImageList.size - 1)
                }

                saveImagesToPreferences()
            }
        }
    }

    // Activity result launcher for picking images from gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Save the picked image to MediaStore
            val mediaSavedPath = MediaStoreHelper.saveImageToMediaStore(this, folderName, it)

            if (mediaSavedPath != null) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val newImage = ImageData(mediaSavedPath, "Image $timeStamp", timeStamp)
                imageList.add(newImage)

                // Add to filtered list if it matches the current query
                val query = searchView.query?.toString() ?: ""
                if (matchesSearchQuery(newImage, query)) {
                    filteredImageList.add(newImage)
                    imageAdapter.notifyItemInserted(filteredImageList.size - 1)
                }

                saveImagesToPreferences()
            }
        }
    }

    // Activity result launcher for picking files
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Save the picked file to MediaStore
            val mediaSavedPath = MediaStoreHelper.saveImageToMediaStore(this, folderName, it)

            if (mediaSavedPath != null) {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val newImage = ImageData(mediaSavedPath, "File $timeStamp", timeStamp)
                imageList.add(newImage)

                // Add to filtered list if it matches the current query
                val query = searchView.query?.toString() ?: ""
                if (matchesSearchQuery(newImage, query)) {
                    filteredImageList.add(newImage)
                    imageAdapter.notifyItemInserted(filteredImageList.size - 1)
                }

                saveImagesToPreferences()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        // Get the current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        currentUserId = currentUser.uid


        // Get folder name from intent
        folderName = intent.getStringExtra("folder_name") ?: "Unnamed Folder"
        val folderImages = intent.getStringArrayListExtra("folder_images") ?: arrayListOf()

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = folderName //Idhar jo medicne ka nam hoga uska nam toolbar pr ajye gha
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Convert the string list to image items
        imageList = folderImages.mapIndexed { index, path ->
            val timeStamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            ImageData(path, "Image $index", timeStamp)
        }.toMutableList()

        // Initialize filtered list with all images
        filteredImageList = imageList.toMutableList()

        // Initialize SearchView
        searchView = findViewById(R.id.search_bar)
        setupSearchView()

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerViewImages)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        filteredImageList = imageList.toMutableList()
        // adapter with delete functionality
        imageAdapter = ImageAdapter(
            filteredImageList,
            { position -> deleteImage(position) }, // onImageDelete
            { position ->  Toast.makeText(this, "Image $position clicked", Toast.LENGTH_SHORT).show() // onImageClick

            }
        )

        recyclerView.adapter = imageAdapter

        // Set up buttons
        findViewById<MaterialButton>(R.id.btnImportFiles).setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        findViewById<MaterialButton>(R.id.btnImportImages).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<MaterialButton>(R.id.btnCaptureImage).setOnClickListener {
            checkCameraPermission()
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                this,
                "com.usmanzafar.meditrack.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoURI)
        } catch (e: Exception) {
            Log.e("ImageActivity", "Camera error: ${e.message}", e)
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Updated method to delete a image
    private fun deleteImage(position: Int) {
        if (position >= 0 && position < filteredImageList.size) {
            val imageToDelete = filteredImageList[position]

            // Remove from filtered list
            filteredImageList.removeAt(position)

            // Remove from original list using the path (which is unique)
            val originalIndex = imageList.indexOfFirst { it.path == imageToDelete.path }
            if (originalIndex != -1) {
                imageList.removeAt(originalIndex)
            }

            imageAdapter.notifyItemRemoved(position)
            saveImagesToPreferences()

            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchView() {
        searchView.queryHint = "Search Images"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterImages(newText)
                return true
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterImages(query: String?) {
        filteredImageList.clear()

        if (query.isNullOrEmpty()) {
            filteredImageList.addAll(imageList)
        } else {
            for (image in imageList) {
                if (matchesSearchQuery(image, query)) {
                    filteredImageList.add(image)
                }
            }
        }
        imageAdapter.notifyDataSetChanged()
    }

    private fun matchesSearchQuery(image: ImageData, query: String): Boolean {
        val searchQuery = query.lowercase()
        return image.title.lowercase().contains(searchQuery) ||
                image.date.lowercase().contains(searchQuery)
    }

    // Fixed method to save images while preserving schedule information
    private fun saveImagesToPreferences() {
        val imagePaths = imageList.map { it.path }

        // Use SharedPreferences directly to save the updated list
        val sharedPreferences = getSharedPreferences("MediTrackPrefs", MODE_PRIVATE)

        // Get the current folder list with user-specific key
        val gson = Gson()
        val json = sharedPreferences.getString("${folderKey}_${currentUserId}", null)
        val type = object : TypeToken<MutableList<FolderData>>() {}.type
        val folderList: MutableList<FolderData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // Update the specific folder while preserving schedule
        val folderIndex = folderList.indexOfFirst { it.name == folderName }
        if (folderIndex != -1) {
            // Get the existing schedule
            val existingSchedule = folderList[folderIndex].schedule
            // Update existing folder with new images but keep schedule
            folderList[folderIndex] = FolderData(folderName, imagePaths, existingSchedule)
        } else {
            // Add new folder with empty schedule
            folderList.add(FolderData(folderName, imagePaths, mutableListOf()))
        }

        // Save back to SharedPreferences with user-specific key
        val editor = sharedPreferences.edit()
        val updatedJson = gson.toJson(folderList)
        editor.putString("${folderKey}_${currentUserId}", updatedJson)
        editor.apply()

        // Also prepare result intent for when activity finishes
        val resultIntent = Intent()
        resultIntent.putExtra("folder_name", folderName)
        resultIntent.putStringArrayListExtra("folder_images", ArrayList(imagePaths))
        setResult(RESULT_OK, resultIntent)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the back button on toolbar
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}