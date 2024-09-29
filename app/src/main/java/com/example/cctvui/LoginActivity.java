package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton, adminLoginButton;
    private FirebaseFirestore db;

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

        // Query Firestore to check if the user exists and the password matches
        CollectionReference usersRef = isAdmin ? db.collection("admin") : db.collection("users");
        usersRef.whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean userFound = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String storedPassword = document.getString("password");

                            // Compare input password with stored password
                            if (password.equals(storedPassword)) {
                                userFound = true;

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

                        if (!userFound) {
                            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
