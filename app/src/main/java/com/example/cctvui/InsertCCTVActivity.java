package com.example.cctvui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InsertCCTVActivity extends AppCompatActivity {

    private EditText snoInput, latitudeInput, longitudeInput, backupDaysInput, connectedToNetworkInput, contactNoInput, coverageInput, locationInput, ownerNameInput, ownershipInput, workStatusInput;
    private Button insertButton, currentLocationButton;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_cctv);

        // Initialize Firestore and location client
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize input fields
        snoInput = findViewById(R.id.input_sno);
        latitudeInput = findViewById(R.id.input_latitude);
        longitudeInput = findViewById(R.id.input_longitude);
        backupDaysInput = findViewById(R.id.input_backupdays);
        connectedToNetworkInput = findViewById(R.id.input_connected_to_network);
        contactNoInput = findViewById(R.id.input_contact_no);
        coverageInput = findViewById(R.id.input_coverage);
        locationInput = findViewById(R.id.input_location);
        ownerNameInput = findViewById(R.id.input_owner_name);
        ownershipInput = findViewById(R.id.input_ownership);
        workStatusInput = findViewById(R.id.input_work_status);

        insertButton = findViewById(R.id.insert_cctv_button);
        currentLocationButton = findViewById(R.id.current_location_button); // Button to get current location

        // Set onClick listener for current location button
        currentLocationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                getCurrentLocation();
            }
        });

        // Set onClick listeners for insert button
        insertButton.setOnClickListener(v -> {
            String snoStr = snoInput.getText().toString().trim();
            String contactNoStr = contactNoInput.getText().toString().trim();
            String latitude = latitudeInput.getText().toString().trim();
            String longitude = longitudeInput.getText().toString().trim();
            String backupDays = backupDaysInput.getText().toString().trim();
            String connectedToNetwork = connectedToNetworkInput.getText().toString().trim();
            String coverage = coverageInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String ownerName = ownerNameInput.getText().toString().trim();
            String ownership = ownershipInput.getText().toString().trim();
            String workStatus = workStatusInput.getText().toString().trim();

            // Validate required fields
            if (TextUtils.isEmpty(snoStr) || TextUtils.isEmpty(latitude) || TextUtils.isEmpty(longitude)) {
                Toast.makeText(InsertCCTVActivity.this, "Sno, Latitude, and Longitude are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse Sno and ContactNo to integers
            int sno = Integer.parseInt(snoStr);
            long contactNo = Long.parseLong(contactNoStr); // Use long in case contact numbers are large

            // Create a new CCTV data object
            Map<String, Object> cctvData = new HashMap<>();
            cctvData.put("Sno", sno);  // Save as an integer
            cctvData.put("Latitude", Double.parseDouble(latitude));
            cctvData.put("Longitude", Double.parseDouble(longitude));
            cctvData.put("Backupdays", Integer.parseInt(backupDays));
            cctvData.put("Connectedtonetwork", connectedToNetwork);
            cctvData.put("ContactNo", contactNo);  // Save as a long
            cctvData.put("Coverage", coverage);
            cctvData.put("Location", location);
            cctvData.put("OwnerName", ownerName);
            cctvData.put("Ownership", ownership);
            cctvData.put("Workstatus", workStatus);

            // Insert data into Firestore
            db.collection("cctvdatanew").add(cctvData).addOnSuccessListener(documentReference -> {
                Toast.makeText(InsertCCTVActivity.this, "CCTV Data inserted", Toast.LENGTH_SHORT).show();
                clearInputFields();
                finish(); // Close the activity after successful insertion
            }).addOnFailureListener(e -> {
                Toast.makeText(InsertCCTVActivity.this, "Error inserting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void getCurrentLocation() {
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with getting the location
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    latitudeInput.setText(String.valueOf(latitude));
                    longitudeInput.setText(String.valueOf(longitude));
                    Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, now you can request the location
                getCurrentLocation();
            } else {
                // Permission was denied, show a message to the user
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Helper function to clear input fields after inserting data
    private void clearInputFields() {
        snoInput.setText("");
        latitudeInput.setText("");
        longitudeInput.setText("");
        backupDaysInput.setText("");
        connectedToNetworkInput.setText("");
        contactNoInput.setText("");
        coverageInput.setText("");
        locationInput.setText("");
        ownerNameInput.setText("");
        ownershipInput.setText("");
        workStatusInput.setText("");
    }
}
