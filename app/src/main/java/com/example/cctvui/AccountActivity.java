package com.example.cctvui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button signOutButton;
    private TextView tvName, tvDesignation, tvDistrict, tvOutpostLocation;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set back arrow click listener to go back to mapsui page
        findViewById(R.id.back_arrow).setOnClickListener(v -> onBackPressed());

        // Find the Sign Out button and TextViews
        signOutButton = findViewById(R.id.sign_out_button);
        tvName = findViewById(R.id.name_text_view);
        tvDesignation = findViewById(R.id.designation_text_view);
        tvDistrict = findViewById(R.id.district_text_view);
        tvOutpostLocation = findViewById(R.id.outpost_location_text_view);

        // Set the Sign Out button click listener
        signOutButton.setOnClickListener(v -> {
            // Sign out the current user
            mAuth.signOut();
            clearUserInfoFromPreferences();

            // Redirect to LoginActivity after logging out
            Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close the current activity
        });

        // Try to load data from SharedPreferences first
        if (!loadUserInfoFromPreferences()) {
            // If data is not found, show progress dialog and fetch from Firestore
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Loading user information...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Fetch and display user information
            fetchUserInfo();
        }
    }

    private void fetchUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Fetch user information from Firestore
            db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                // Dismiss the progress dialog
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Retrieve data or set "NA" if not available
                        String name = document.getString("name") != null ? document.getString("name") : "NA";
                        String designation = document.getString("designation") != null ? document.getString("designation") : "NA";
                        String district = document.getString("district") != null ? document.getString("district") : "NA";
                        String outpostLocation = document.getString("outpostLocation") != null ? document.getString("outpostLocation") : "NA";

                        // Display data
                        tvName.setText(name);
                        tvDesignation.setText(designation);
                        tvDistrict.setText(district);
                        tvOutpostLocation.setText(outpostLocation);

                        // Save data in SharedPreferences
                        saveUserInfoToPreferences(name, designation, district, outpostLocation);
                    }
                } else {
                    // Handle the error (optional)
                    // Display a Toast or Log the error message
                }
            });
        } else {
            // Dismiss the progress dialog if no user is logged in
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    private void saveUserInfoToPreferences(String name, String designation, String district, String outpostLocation) {
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.putString("designation", designation);
        editor.putString("district", district);
        editor.putString("outpostLocation", outpostLocation);
        editor.apply();
    }

    private boolean loadUserInfoFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String name = prefs.getString("name", null);
        String designation = prefs.getString("designation", null);
        String district = prefs.getString("district", null);
        String outpostLocation = prefs.getString("outpostLocation", null);

        // Check if data exists
        if (name != null && designation != null && district != null && outpostLocation != null) {
            // Display stored data
            tvName.setText(name);
            tvDesignation.setText(designation);
            tvDistrict.setText(district);
            tvOutpostLocation.setText(outpostLocation);
            return true; // Data was loaded from SharedPreferences
        }
        return false; // No data found, need to fetch from Firestore
    }
    private void clearUserInfoFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Remove all stored data
        editor.apply();
    }
}
