package com.usmanzafar.meditrack;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;

public class BMIActivity extends AppCompatActivity {
    private TextInputEditText weightInput, heightFeetInput, heightInchesInput;
    private TextView resultText, suggestionText;
    private Button calculateButton;

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
            if (BMI < 16) {
                suggestion = "Severely underweight! Please consult a doctor immediately.";
            } else if (BMI < 18.5) {
                suggestion = "You are underweight. Consider increasing your calorie intake with nutritious foods.";
            } else if (BMI < 25) {
                suggestion = "You have a healthy BMI. Keep maintaining a balanced diet and regular exercise!";
            } else if (BMI < 30) {
                suggestion = "You are overweight. Consider a regular exercise routine and a healthier diet.";
            } else if (BMI < 35) {
                suggestion = "You are in the obese category. A structured fitness and nutrition plan is recommended.";
            } else {
                suggestion = "Severely obese! Please seek medical advice as soon as possible.";
            }

            // Apply text
            suggestionText.setText(suggestion);

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