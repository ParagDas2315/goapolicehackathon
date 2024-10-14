package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button signOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set back arrow click listener to go back to mapsui page
        findViewById(R.id.back_arrow).setOnClickListener(v -> onBackPressed());

        // Find the Sign Out button
        signOutButton = findViewById(R.id.sign_out_button);

        // Set the Sign Out button click listener
        signOutButton.setOnClickListener(v -> {
            // Sign out the current user
            mAuth.signOut();

            // Redirect to LoginActivity after logging out
            Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close the current activity
        });
    }
}
