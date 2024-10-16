package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.view.View;
import android.widget.ProgressBar;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.app.ProgressDialog;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton, adminLoginButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView otpLoginLink; // Added for OTP login link
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        adminLoginButton = findViewById(R.id.admin_login_button);
        otpLoginLink = findViewById(R.id.otp_login_link); // OTP Login Link
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, navigate to main activity
            navigateToMain();
        }

        // Set click listener for regular login button
        loginButton.setOnClickListener(v -> loginUser(false));

        // Set click listener for admin login button
        adminLoginButton.setOnClickListener(v -> loginUser(true));

        // Set click listener for OTP login link
        otpLoginLink.setOnClickListener(v -> {
            // Open OtpLoginActivity
            Intent intent = new Intent(LoginActivity.this, OtpLoginActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(boolean isAdmin) {
        String email = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (isAdmin) {
            // Admin login via Firestore with encrypted password
            checkAdminLogin(email, password);
        } else {
            // Regular user login via Firebase Authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            // Login successful
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            // If login fails
                            Toast.makeText(LoginActivity.this, "Authentication Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void checkAdminLogin(String username, String password) {
        // Hash the password using SHA-256
        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            Toast.makeText(LoginActivity.this, "Error hashing password", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Log the username and hashed password for debugging
        Log.d("AdminLogin", "Username: " + username + ", Hashed Password: " + hashedPassword);

        // Query Firestore to check if admin exists and the hashed password matches
        CollectionReference adminRef = db.collection("admin");
        adminRef.whereEqualTo("username", username)  // Query using 'username' field instead of 'email'
                .get()
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        boolean isValid = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String storedPassword = document.getString("password");

                            // Log the stored password for comparison
                            Log.d("AdminLogin", "Stored Password: " + storedPassword);

                            // Compare the hashed input password with the stored hashed password
                            if (storedPassword != null && storedPassword.equals(hashedPassword)) {
                                isValid = true;
                                Toast.makeText(LoginActivity.this, "Admin Login Successful", Toast.LENGTH_SHORT).show();
                                navigateToAdminDashboard();
                                break;
                            }
                        }
                        if (!isValid) {
                            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, mapsui.class);
        startActivity(intent);
        finish();
    }

    private void navigateToAdminDashboard() {
        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        startActivity(intent);
        finish();
    }

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
