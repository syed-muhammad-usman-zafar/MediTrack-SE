package com.usmanzafar.meditrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextView userNameTextView;
    private TextView bmiValueTextView;
    private TextView bmiStatusTextView;
    private CircularProgressIndicator bmiIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize TextViews and BMI indicator
        userNameTextView = findViewById(R.id.user_name);
        bmiValueTextView = findViewById(R.id.bmi_value);
        bmiStatusTextView = findViewById(R.id.bmi_status);

        //circle indicator intializer
        bmiIndicator = findViewById(R.id.bmi_indicator);
        // Set user name
        updateUserNameDisplay();

        // Update BMI display
        updateBMIDisplay();

        // Toolbar Setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Apply Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set click listeners for various features (BMI, Nutrition, Pharmacies)
        setupFeatureListeners();


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true; // Already here
            }

            Intent intent = null;
            if (id == R.id.nav_calendar) {
                intent = new Intent(MainActivity.this, CalendarActivity.class);
            }else if (id == R.id.nav_profile) {
                intent = new Intent(MainActivity.this, UserProfileActivity.class);
            }



            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // <--- smoother transition
                startActivity(intent);
                return true;
            }

            return false;
        });

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the user name display when resuming the activity
        updateUserNameDisplay();
        // Update BMI display when returning from BMI calculator
        updateBMIDisplay();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void updateBMIDisplay() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid(); // Get user-specific key

            SharedPreferences sharedPreferences = getSharedPreferences(BMIActivity.BMI_PREFS, MODE_PRIVATE);
            float bmiValue = sharedPreferences.getFloat(BMIActivity.BMI_VALUE + "_" + uid, -1);
            String bmiCategory = sharedPreferences.getString(BMIActivity.BMI_CATEGORY + "_" + uid, "");

            if (bmiValue >= 0) {
                bmiValueTextView.setText(String.format("BMI: %.1f", bmiValue));
                bmiStatusTextView.setText("Category: " + bmiCategory);

                int progressValue;
                if (bmiValue <= 16) {
                    progressValue = 0;
                } else if (bmiValue >= 35) {
                    progressValue = 100;
                } else {
                    progressValue = (int)((bmiValue - 16) * (100.0 / (35 - 16)));
                }
                bmiIndicator.setProgress(progressValue);

                int color;
                if (bmiCategory.equals("Normal")) {
                    color = ContextCompat.getColor(this, R.color.progress_healthy);
                } else if (bmiCategory.equals("Underweight") || bmiCategory.equals("Severely Underweight")) {
                    color = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
                } else {
                    color = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                }
                bmiIndicator.setIndicatorColor(color);

            } else {
                bmiValueTextView.setText("BMI: Not calculated");
                bmiStatusTextView.setText("Tap BMI Calculator to check your BMI");
                bmiIndicator.setProgress(0);
                bmiIndicator.setIndicatorColor(ContextCompat.getColor(this, R.color.progress_undefined));
            }
        }
    }


    private void updateUserNameDisplay() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            // If display name is available, use it; otherwise use email or a default text
            if (displayName != null && !displayName.isEmpty()) {
                userNameTextView.setText(displayName);
            } else if (currentUser.getEmail() != null) {
                // Use email up to @ symbol as fallback
                String email = currentUser.getEmail();
                int atIndex = email.indexOf('@');
                if (atIndex > 0) {
                    userNameTextView.setText(email.substring(0, atIndex));
                } else {
                    userNameTextView.setText(email);
                }
            } else {
                // Default text if neither display name nor email is available
                userNameTextView.setText("User");
            }
        } else {
            // YE HONA NAI CHAIYE BAS ADDITIONAL CHECK RKHA WA TO BE SURE
            // wapis login screen pr jaega
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu (this will add items to the action bar if it is present)
        getMenuInflater().inflate(R.menu.top_header_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logout) {
            // Handle Logout Action
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupFeatureListeners() {
        MaterialCardView bmiCardView = findViewById(R.id.card_bmi);
        bmiCardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BMIActivity.class);
            startActivity(intent);
        });

        MaterialCardView nutritionCardView = findViewById(R.id.card_nutrition);
        nutritionCardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NutritionActivity.class);
            startActivity(intent);
        });

        MaterialCardView pharmaciesCardView = findViewById(R.id.card_pharmacies);
        pharmaciesCardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PharmaciesActivity.class);
            startActivity(intent);
        });


        MaterialCardView trackingCardView = findViewById(R.id.card_track_medications);
        trackingCardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TrackMedicationsActivity.class);
            startActivity(intent);
        });

        MaterialCardView medicationsCardView = findViewById(R.id.card_pill_info);
        medicationsCardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Pill_InfoActivity.class);
            startActivity(intent);
        });


    }
}