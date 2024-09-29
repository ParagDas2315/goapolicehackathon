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

public class InsertCCTVActivity extends AppCompatActivity {

    private EditText snoInput, latitudeInput, longitudeInput, backupDaysInput, connectedToNetworkInput, contactNoInput, coverageInput, locationInput, ownerNameInput, ownershipInput, workStatusInput;
    private Button insertButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_cctv);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
            db.collection("cctvdatatesting").add(cctvData).addOnSuccessListener(documentReference -> {
                Toast.makeText(InsertCCTVActivity.this, "CCTV Data inserted", Toast.LENGTH_SHORT).show();
                clearInputFields();
                finish(); // Close the activity after successful insertion
            }).addOnFailureListener(e -> {
                Toast.makeText(InsertCCTVActivity.this, "Error inserting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
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
