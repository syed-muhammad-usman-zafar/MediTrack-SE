package com.usmanzafar.meditrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BMIActivity extends AppCompatActivity {
    private TextInputEditText weightInput, heightFeetInput, heightInchesInput;
    private TextView resultText, suggestionText;
    private Button calculateButton;

    // Constants for SharedPreferences
    public static final String BMI_PREFS = "BMIPrefs";
    public static final String BMI_VALUE = "BMIValue";
    public static final String BMI_CATEGORY = "BMICategory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

        // Set up toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        weightInput = findViewById(R.id.weight);
        heightFeetInput = findViewById(R.id.heightFeet);
        heightInchesInput = findViewById(R.id.heightInches);
        resultText = findViewById(R.id.result);
        suggestionText = findViewById(R.id.suggestion);
        calculateButton = findViewById(R.id.btnSubmit);

        // Set click listener for calculate button
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBMI();
            }
        });
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.nav_home) {
                intent = new Intent(BMIActivity.this, MainActivity.class);
            }
            else if (id == R.id.nav_calendar) {
                intent = new Intent(BMIActivity.this, CalendarActivity.class);
            }else if (id == R.id.nav_profile) {
                intent = new Intent(BMIActivity.this, UserProfileActivity.class);
            }



            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // <--- smoother transition
                startActivity(intent);
                return true;
            }

            return false;
        });
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    private void calculateBMI() {
        // Validate input
        if (weightInput.getText().toString().trim().isEmpty() ||
                heightFeetInput.getText().toString().trim().isEmpty() ||
                heightInchesInput.getText().toString().trim().isEmpty()) {
            resultText.setText("Please enter all values");
            suggestionText.setText("");
            return;
        }

        try {
            // Convert input
            double weight = Double.parseDouble(weightInput.getText().toString().trim());
            int feet = Integer.parseInt(heightFeetInput.getText().toString().trim());
            double inches = Double.parseDouble(heightInchesInput.getText().toString().trim());

            // Convert height to meters
            double heightMeters = (feet * 0.3048) + (inches * 0.0254);

            // Avoid division by zero
            if (heightMeters <= 0) {
                resultText.setText("Height must be greater than zero");
                suggestionText.setText("");
                return;
            }

            // Calculate BMI
            double BMI = weight / (heightMeters * heightMeters);
            resultText.setText(String.format("BMI: %.1f", BMI));

            // Determine category & apply suggestion
            String suggestion;
            String category;

            if (BMI < 16) {
                suggestion = "Severely underweight! Please consult a doctor immediately.";
                category = "Severely Underweight";
            } else if (BMI < 18.5) {
                suggestion = "You are underweight. Consider increasing your calorie intake with nutritious foods.";
                category = "Underweight";
            } else if (BMI < 25) {
                suggestion = "You have a healthy BMI. Keep maintaining a balanced diet and regular exercise!";
                category = "Normal";
            } else if (BMI < 30) {
                suggestion = "You are overweight. Consider a regular exercise routine and a healthier diet.";
                category = "Overweight";
            } else if (BMI < 35) {
                suggestion = "You are in the obese category. A structured fitness and nutrition plan is recommended.";
                category = "Obese";
            } else {
                suggestion = "Severely obese! Please seek medical advice as soon as possible.";
                category = "Severely Obese";
            }

            // Apply text
            suggestionText.setText(suggestion);

            // Save BMI value and category to SharedPreferences with UID
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid(); // Unique user ID

                SharedPreferences sharedPreferences = getSharedPreferences(BMI_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat(BMI_VALUE + "_" + uid, (float) BMI);
                editor.putString(BMI_CATEGORY + "_" + uid, category);
                editor.apply();
            }

        } catch (NumberFormatException e) {
            resultText.setText("Please enter valid numbers");
            suggestionText.setText("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the back button on toolbar
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}