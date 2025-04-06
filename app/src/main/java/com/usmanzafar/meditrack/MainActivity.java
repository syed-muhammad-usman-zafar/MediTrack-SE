package com.usmanzafar.meditrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextView userNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize TextView for user name
        userNameTextView = findViewById(R.id.user_name);

        // Set user name
        updateUserNameDisplay();

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

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the user name display when resuming the activity
        updateUserNameDisplay();
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
            // This shouldn't happen since we check for logged-in user in LoginActivity
            // But just in case, go back to login screen
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
    }
}