package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 4000; // 4 seconds for total animation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find the Goa Police logo ImageView
        ImageView logo = findViewById(R.id.goapolice_logo);

        // Load the fade in-out animation
        Animation fadeInOut = AnimationUtils.loadAnimation(this, R.anim.fade_in_out);

        // Start the animation
        logo.startAnimation(fadeInOut);

        // Handler to transition to the next activity after the animation completes
        new Handler().postDelayed(() -> {
            // Start the LoginActivity after the animation finishes
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close the SplashActivity so it can't be returned to
        }, SPLASH_DURATION);
    }
}
