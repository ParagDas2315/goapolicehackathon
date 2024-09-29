package com.example.cctvui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InsertUserActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button submitButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_user);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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

            // Call the function to insert data into Firestore
            insertUserData(username, password);
        });
    }

    // Function to insert user data into Firestore
    private void insertUserData(String username, String password) {
        // Create a new user data object
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", password);

        // Insert data into Firestore
        db.collection("users").add(userData).addOnSuccessListener(documentReference -> {
            Toast.makeText(InsertUserActivity.this, "User Data inserted", Toast.LENGTH_SHORT).show();
            clearInputFields();
            finish(); // Close the activity after successful insertion
        }).addOnFailureListener(e -> {
            Toast.makeText(InsertUserActivity.this, "Error inserting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Helper function to clear input fields after inserting data
    private void clearInputFields() {
        usernameInput.setText("");
        passwordInput.setText("");
    }
}
