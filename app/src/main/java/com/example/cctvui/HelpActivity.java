package com.example.cctvui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.view.ViewGroup;
import android.view.View;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class HelpActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Set<String> uniqueDetailsSet = new HashSet<>(); // Use a Set to avoid duplicates
    private ProgressDialog progressDialog; // Progress dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        findViewById(R.id.back_arrow).setOnClickListener(v -> onBackPressed());
        db = FirebaseFirestore.getInstance();  // Initialize Firestore
        LinearLayout blockContainer = findViewById(R.id.block_container);

        // Initialize the progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching data...");
        progressDialog.setCancelable(false);

        // Get the list of lat-long pairs from the intent
        ArrayList<String> latLngList = getIntent().getStringArrayListExtra("LAT_LNG_LIST");

        // Check if data is received
        if (latLngList != null && !latLngList.isEmpty()) {
            progressDialog.show(); // Show progress dialog
            for (String latLng : latLngList) {
                // Modify parsing logic to handle "Lat: <latitude>, Lng: <longitude>" format
                try {
                    // Remove the "Lat:" and "Lng:" parts and split by comma
                    String[] parts = latLng.replace("Lat:", "").replace("Lng:", "").trim().split(",");
                    if (parts.length == 2) {
                        double latitude = Double.parseDouble(parts[0].trim());
                        double longitude = Double.parseDouble(parts[1].trim());

                        // Fetch details from Firestore for each lat-long
                        fetchCameraDetailsFromFirestore(latitude, longitude, blockContainer);
                    } else {
                        Log.e("HelpActivity", "Invalid format for lat-long pair: " + latLng);
                    }
                } catch (NumberFormatException e) {
                    Log.e("HelpActivity", "Invalid latitude/longitude format: " + latLng);
                }
            }
        } else {
            Toast.makeText(this, "No location data received.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCameraDetailsFromFirestore(double latitude, double longitude, LinearLayout blockContainer) {
        db.collection("cctvdatanew")
                .whereEqualTo("Latitude", latitude)
                .whereEqualTo("Longitude", longitude)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);  // Assuming only one match

                            // Fetch all available data from Firestore
                            Long sno = document.getLong("Sno");
                            String location = document.getString("Location");
                            String ownership = document.getString("Ownership");
                            String ownerName = document.getString("OwnerName");
                            Object contactNo = document.get("ContactNo");
                            String workStatus = document.getString("Workstatus");
                            String coverage = document.getString("Coverage");
                            Long backupDays = document.getLong("Backupdays");
                            String connectedToNetwork = document.getString("Connectedtonetwork");

                            // Prepare details array to maintain order
                            String[] detailsArray = {
                                    sno != null ? sno.toString() : "NA",
                                    location != null ? location : "NA",
                                    ownership != null ? ownership : "NA",
                                    ownerName != null ? ownerName : "NA",
                                    contactNo != null ? contactNo.toString() : "NA",
                                    workStatus != null ? workStatus : "NA",
                                    coverage != null ? coverage : "NA",
                                    backupDays != null ? backupDays.toString() : "NA",
                                    connectedToNetwork != null ? connectedToNetwork : "NA"
                            };

                            // Create a unique key to ensure no duplicates
                            String uniqueKey = String.join(",", detailsArray);

                            if (uniqueDetailsSet.add(uniqueKey)) {
                                addDetailBlock(detailsArray, blockContainer);
                            }
                        } else {
                            Log.e("HelpActivity", "No data found for location: " + latitude + ", " + longitude);
                        }
                    } else {
                        Log.e("HelpActivity", "Task failed. Error: ", task.getException());
                        Toast.makeText(this, "Error fetching data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    // Dismiss progress dialog when task is complete
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("HelpActivity", "Failure fetching data: " + e.getMessage());
                    Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss(); // Dismiss on failure
                });
    }

    private void addDetailBlock(String[] detailsArray, LinearLayout blockContainer) {
        // Create a new card for each entry
        CardView cardView = new CardView(this);
        cardView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        cardView.setRadius(12);
        cardView.setCardElevation(8);
        cardView.setPadding(16, 16, 16, 16);
        cardView.setUseCompatPadding(true);

        // Create a LinearLayout inside the card to hold the details
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);

        // Labels corresponding to details
        String[] labels = {"Sno", "Location", "Ownership", "Owner Name", "Contact No", "Work Status", "Coverage", "Backup Days", "Connected to Network"};

        for (int i = 0; i < detailsArray.length && i < labels.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(labels[i] + ": " + detailsArray[i].trim());
            textView.setPadding(4, 4, 4, 4);
            detailsLayout.addView(textView);
        }

        cardView.addView(detailsLayout);

        // Add the card to the container
        blockContainer.addView(cardView);

        // Add some spacing between cards
        View space = new View(this);
        space.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                16
        ));
        blockContainer.addView(space);
    }
}
