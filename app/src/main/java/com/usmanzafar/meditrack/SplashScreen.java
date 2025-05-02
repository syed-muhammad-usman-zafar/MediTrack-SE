package com.usmanzafar.meditrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.view.Window;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Initialize UI components
        CardView logoContainer = findViewById(R.id.logoContainer);
        ImageView logoImageView = findViewById(R.id.logoImageView);
        TextView appNameTextView = findViewById(R.id.appNameTextView);
        TextView taglineTextView = findViewById(R.id.taglineTextView);
        LinearProgressIndicator loadingIndicator = findViewById(R.id.loadingIndicator);

        // Load animations
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation);
        Animation loadingAnimation = AnimationUtils.loadAnimation(this, R.anim.loading_animation);

        // Apply animations with slight delay between elements for a cascade effect
        logoContainer.startAnimation(logoAnimation);

        loadingIndicator.startAnimation(loadingAnimation);

        // Navigate to LoginActivity after animation completes
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }


}