package com.example.cctvui;

import android.app.ProgressDialog;
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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class InsertUserActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput, nameInput, designationInput, districtInput, outpostLocationInput;
    private Button submitButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Spinner designationSpinner, districtSpinner;
    private ProgressDialog progressDialog;

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
        nameInput = findViewById(R.id.name_input);
        designationSpinner = findViewById(R.id.designation_spinner);
        districtSpinner = findViewById(R.id.district_spinner);
        outpostLocationInput = findViewById(R.id.outpost_location_input);
        submitButton = findViewById(R.id.submit_button);

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Inserting Data");
        progressDialog.setCancelable(false);

        // Set onClick listener for submit button
        submitButton.setOnClickListener(v -> {
            try {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String name = nameInput.getText().toString().trim();
                String designation = designationSpinner.getSelectedItem().toString();
                String district = districtSpinner.getSelectedItem().toString();
                String outpostLocation = outpostLocationInput.getText().toString().trim();

                // Validate all fields
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name) ||
                        TextUtils.isEmpty(outpostLocation) || designation.equals("Select Designation") ||
                        district.equals("Select District")) {
                    Toast.makeText(InsertUserActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                progressDialog.show();

                // Call the function to create the user using Firebase Authentication
                createUser(username, password, name, designation, district, outpostLocation);

            } catch (Exception e) {
                // Handle any unexpected errors and show a Toast
                Toast.makeText(InsertUserActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter<CharSequence> designationAdapter = ArrayAdapter.createFromResource(this,
                R.array.designation_options, android.R.layout.simple_spinner_item);
        designationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        designationSpinner.setAdapter(designationAdapter);

// Set up the district spinner
        ArrayAdapter<CharSequence> districtAdapter = ArrayAdapter.createFromResource(this,
                R.array.district_options, android.R.layout.simple_spinner_item);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtSpinner.setAdapter(districtAdapter);
    }

    // Function to create a user using Firebase Authentication
    private void createUser(String email, String password, String name, String designation, String district, String outpostLocation) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User created successfully
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Add any additional user data in Firestore
                            insertUserData(user.getEmail(), user.getUid(), name, designation, district, outpostLocation);
                        }
                    } else {
                        progressDialog.dismiss();
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
    private void insertUserData(String email, String uid, String name, String designation, String district, String outpostLocation) {
        // Create a new user data object
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);          // Save email
        userData.put("uid", uid);              // Save Firebase Authentication UID (Unique ID)
        userData.put("name", name);            // Save name
        userData.put("designation", designation); // Save designation
        userData.put("district", district);    // Save district
        userData.put("outpostLocation", outpostLocation); // Save outpost location

        // Insert data into Firestore (optional)
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(InsertUserActivity.this, "User data inserted", Toast.LENGTH_SHORT).show();
                    clearInputFields();
                    finish(); // Close the activity after successful insertion
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(InsertUserActivity.this, "Error inserting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper function to clear input fields after inserting data
    private void clearInputFields() {
        usernameInput.setText("");
        passwordInput.setText("");
        nameInput.setText("");
        designationInput.setText("");
        districtInput.setText("");
        outpostLocationInput.setText("");
    }
}
