package com.example.cctvui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

            // Hash the password before storing it
            String encryptedPassword = hashPassword(password);

            if (encryptedPassword != null) {
                // Call the function to insert data into Firestore
                insertUserData(username, encryptedPassword);
            } else {
                Toast.makeText(InsertUserActivity.this, "Error encrypting password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to insert user data into Firestore
    private void insertUserData(String username, String password) {
        // Create a new user data object
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", password); // Insert encrypted password

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

    // Function to hash the password using SHA-256
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
