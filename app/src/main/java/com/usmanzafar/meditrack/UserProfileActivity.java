package com.usmanzafar.meditrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private TextView nameTextView, emailInput, dobTextView, ageTextView;
    private Button forgotPasswordBtn;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        nameTextView = findViewById(R.id.profile_name);
        emailInput = findViewById(R.id.profile_email);
        dobTextView = findViewById(R.id.profile_dob);
        ageTextView = findViewById(R.id.profile_age);
        forgotPasswordBtn = findViewById(R.id.forgot_password);

        forgotPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(UserProfileActivity.this, "Please enter your email first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                sendPasswordResetEmail(email);
            }
        });

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        loadUserProfile();


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.nav_home) {
                intent = new Intent(UserProfileActivity.this, MainActivity.class);
            }
            else if (id == R.id.nav_calendar) {
                intent = new Intent(UserProfileActivity.this, CalendarActivity.class);
            }else if (id == R.id.nav_profile) {
                return true; // Already here
            }
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // <--- smoother transition
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {


            // Get user data from Firebase Database
            mDatabase.child("users").child(user.getUid()).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                            if (dataSnapshot.exists()) {
                                User userProfile = dataSnapshot.getValue(User.class);

                                if (userProfile != null) {
                                    // Set user profile data
                                    nameTextView.setText(userProfile.getName());
                                    emailInput.setText(userProfile.getEmail());

                                    String dob = userProfile.getDateOfBirth();
                                    dobTextView.setText(dob);

                                    // Calculate and display age
                                    int age = calculateAgeFromDOB(dob);
                                    ageTextView.setText(age + " years old");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle errors
                        }
                    }
            );
        }
    }

    private int calculateAgeFromDOB(String dobString) {
        try {
            // Parse the DOB string to Date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(dobString);

            // Get current date
            Calendar today = Calendar.getInstance();

            // Get birth date in Calendar
            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);

            // Calculate age
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            // Adjust age if birthday hasn't occurred yet this year
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Return 0 if there's an error parsing the date
        }
    }


    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(UserProfileActivity.this,
                                    "Password reset email sent",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UserProfileActivity.this,
                                    "Failed to send reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}