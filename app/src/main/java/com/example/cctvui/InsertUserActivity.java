package com.example.cctvui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InsertUserActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button submitButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_user);

        // Initialize Firestore and Firebase Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize input fields and button
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        submitButton = findViewById(R.id.submit_button);

        // Set onClick listener for submit button
        submitButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(InsertUserActivity.this, "Both fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call the function to create the user using Firebase Authentication
            createUser(username, password);
        });
    }

    // Function to create a user using Firebase Authentication
    private void createUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User created successfully
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Add any additional user data in Firestore
                            insertUserData(user.getEmail(), user.getUid());
                        }
                    } else {
                        // Handle errors like user already exists
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(InsertUserActivity.this, "User already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(InsertUserActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Function to insert user data into Firestore
    private void insertUserData(String email, String uid) {
        // Create a new user data object
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);  // Save email
        userData.put("uid", uid);      // Save Firebase Authentication UID (Unique ID)

        // Insert data into Firestore (optional)
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(InsertUserActivity.this, "User data inserted", Toast.LENGTH_SHORT).show();
                    clearInputFields();
                    finish(); // Close the activity after successful insertion
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(InsertUserActivity.this, "Error inserting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper function to clear input fields after inserting data
    private void clearInputFields() {
        usernameInput.setText("");
        passwordInput.setText("");
    }
}
