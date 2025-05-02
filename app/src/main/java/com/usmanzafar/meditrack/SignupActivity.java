package com.usmanzafar.meditrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;

public class SignupActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, DOBInput, passwordInput, confirmPasswordInput;
    private Button signupButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        signupButton = findViewById(R.id.signup_button);
        DOBInput = findViewById(R.id.dob_input);

        DOBInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Date of Birth")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .setCalendarConstraints(
                                new CalendarConstraints.Builder()
                                        .setValidator(DateValidatorPointBackward.now()) // No future dates
                                        .build()
                        )
                        .build();

                datePicker.addOnPositiveButtonClickListener(selection -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String formattedDate = sdf.format(new Date(selection));
                    DOBInput.setText(formattedDate);
                });

                datePicker.show(getSupportFragmentManager(), "dob_picker");
            }
        });

        // Set up signup button click listener
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();
                String dob = DOBInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();

                // Validate inputs
                if (name.isEmpty() || email.isEmpty() || dob.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please fill in all fields",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignupActivity.this, "Passwords do not match",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 8) {
                    Toast.makeText(SignupActivity.this,
                            "Password should be at least 8 characters",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                createAccount(name, email, password, dob);
            }
        });

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));


        TextView loginRedirect = findViewById(R.id.login_redirect);
        String text = "Already have an account? Sign in";
        SpannableString spannable = new SpannableString(text);
        int startIndex = text.indexOf("Sign in");
        int endIndex = startIndex + "Sign in".length();
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Open LoginActivity
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);        // Underline
                ds.setColor(Color.parseColor("#0000EE")); // Link blue
            }
        };
        spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginRedirect.setText(spannable);
        loginRedirect.setMovementMethod(LinkMovementMethod.getInstance());
        loginRedirect.setHighlightColor(Color.TRANSPARENT); // Optional: removes background highlight


    }

    private void createAccount(final String name, String email, String password, final String dob) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign up success, update user profile
                    FirebaseUser user = mAuth.getCurrentUser();
                    String userId = user.getUid();

                    // Set display name
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // Create User object with DOB
                                        User userProfile = new User(userId, name, email, dob);

                                        //DB k andr user nam ka object
                                        mDatabase.child("users").child(userId).setValue(userProfile)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(SignupActivity.this,
                                                                    "Account created successfully",
                                                                    Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(SignupActivity.this,
                                                                    "Failed to save user data: " + task.getException().getMessage(),
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }
                                }
                            });
                } else {
                    // If sign up fails, display a message to the user
                    Toast.makeText(SignupActivity.this, "Authentication failed: "
                                    + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}