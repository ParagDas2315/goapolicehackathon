package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.view.View;
import android.widget.ProgressBar;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton, adminLoginButton;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        adminLoginButton = findViewById(R.id.admin_login_button);

        // Set click listener for regular login button
        loginButton.setOnClickListener(v -> loginUser(false));

        // Set click listener for admin login button
        adminLoginButton.setOnClickListener(v -> loginUser(true));
        progressBar = findViewById(R.id.progressBar);
    }

    private void loginUser(boolean isAdmin) {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(LoginActivity.this, "Please enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        findViewById(R.id.progress_card).setVisibility(View.VISIBLE);

        // If the login is not for admin, hash the password before comparison
        if (!isAdmin) {
            password = hashPassword(password);
            if (password == null) {
                Toast.makeText(LoginActivity.this, "Error hashing password", Toast.LENGTH_SHORT).show();
                findViewById(R.id.progress_card).setVisibility(View.GONE);
                return;
            }
        }

        // Query Firestore to check if the user exists and the password matches
        CollectionReference usersRef = isAdmin ? db.collection("admin") : db.collection("users");
        String finalPassword = password;
        usersRef.whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    findViewById(R.id.progress_card).setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            boolean isValid = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String storedPassword = document.getString("password");

                                // Make sure storedPassword is not null to avoid crashes
                                if (storedPassword != null && finalPassword.equals(storedPassword)) {
                                    isValid = true;

                                    // Display login success message
                                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                                    // If login is successful, navigate to the appropriate activity
                                    Intent intent;
                                    if (isAdmin) {
                                        intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                    } else {
                                        intent = new Intent(LoginActivity.this, mapsui.class);
                                    }

                                    startActivity(intent);
                                    finish();
                                    break;
                                }
                            }

                            if (!isValid) {
                                Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // No user found with the provided username
                            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle Firestore error
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Function to hash the password using SHA-256 (used for regular users only)
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes());
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper function to convert byte array to a hexadecimal string
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
