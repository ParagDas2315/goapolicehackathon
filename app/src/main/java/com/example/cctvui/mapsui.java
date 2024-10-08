package com.example.cctvui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Looper;

public class mapsui extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "mapsui";
    private ImageView filterIcon;
    private LinearLayout settingsButton, helpButton, uploadButton, accountButton;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;  // Firestore instance
    private View selectedButton;

    private static final int REQUEST_LOCATION_PERMISSION = 113;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int FILTER_REQUEST_CODE = 2;  // New request code for the filter

    private Button showCamerasButton;
    private boolean areCamerasVisible = false;  // To toggle visibility state
    private List<Marker> cameraMarkers = new ArrayList<>();  // Store camera markers
    private int currentMapType = GoogleMap.MAP_TYPE_NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        setContentView(R.layout.mapsui);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY_HERE");
        }

        db = FirebaseFirestore.getInstance();  // Initialize Firestore

        filterIcon = findViewById(R.id.filter_icon);
        settingsButton = findViewById(R.id.settings_button);
        helpButton = findViewById(R.id.help_button);
        uploadButton = findViewById(R.id.upload_button);
        accountButton = findViewById(R.id.account_button);

        showCamerasButton = findViewById(R.id.show_cameras_button);

        setupBottomMenuClickListeners();
        setupSearchListener();
        setupFilterListener();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error initializing map!", Toast.LENGTH_SHORT).show();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            // Implement location updates here if needed
        };

        // Check location permission on start
        checkLocationPermission();

        // Show/Hide Cameras button logic
        showCamerasButton.setOnClickListener(v -> toggleCamerasVisibility());
    }

    private void startLocationUpdates() {
        // Create a LocationRequest object for accurate location updates
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);  // Update interval of 10 seconds
        locationRequest.setFastestInterval(5000);  // Fastest update interval of 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check if permission is granted before accessing location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission not granted", e);
            }
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap == null) {
            Toast.makeText(this, "Google Map initialization failed!", Toast.LENGTH_SHORT).show();
            return;
        }

        mMap = googleMap;
        mMap.setMapType(currentMapType);

        // Check location permissions and set up the map's location features
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Use existing dimension values for padding
            int topPadding = getResources().getDimensionPixelSize(R.dimen.map_top_padding);  // Padding for My Location button
            int bottomPadding = getResources().getDimensionPixelSize(R.dimen.map_bottom_padding);  // Padding for Zoom controls

            // Apply padding to Google Maps UI
            mMap.setPadding(0, topPadding, 0, bottomPadding);  // Left, Top, Right, Bottom padding

            startLocationUpdates();

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                }
            });

            // Set a click listener for the markers
            mMap.setOnMarkerClickListener(marker -> {
                LatLng markerPosition = marker.getPosition();
                fetchCameraDetailsFromFirestore(markerPosition.latitude, markerPosition.longitude);
                return true;
            });
        }
    }



    private void fetchCameraDetailsFromFirestore(double latitude, double longitude) {
        // Query Firestore using the latitude and longitude
        db.collection("cctvdatanew")
                .whereEqualTo("Latitude", latitude)
                .whereEqualTo("Longitude", longitude)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);  // Assuming only one match
                        String owner = document.getString("Owner");
                        Long backupDays = document.getLong("BackupDays");

                        // Show bottom sheet with fetched data
                        showBottomSheet(latitude, longitude);
                    } else {
                        Toast.makeText(mapsui.this, "No data found for this location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showBottomSheet(double latitude, double longitude) {
        // Query Firestore using the latitude and longitude
        Query query = db.collection("cctvdatanew")
                .whereEqualTo("Latitude", latitude);
        query  // Ensure latitude is a String for comparison
                .whereEqualTo("Longitude", longitude)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);  // Assuming only one match

                        // Fetch all available data from Firestore
                        Long sno = document.getLong("Sno");
                        String location = document.getString("Location");
                        String ownership = document.getString("Ownership");
                        String ownerName = document.getString("OwnerName");
                        Object contactNo = document.get("ContactNo");  // Fetch as Object to handle different types
                        String workStatus = document.getString("Workstatus");
                        String coverage = document.getString("Coverage");
                        Long backupDays = document.getLong("Backupdays");
                        String connectedToNetwork = document.getString("Connectedtonetwork");

                        // Show bottom sheet with fetched data
                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);

                        // Find views inside bottom sheet and set data
                        TextView tvSno = bottomSheetView.findViewById(R.id.tv_sno);
                        TextView tvLocation = bottomSheetView.findViewById(R.id.tv_location);
                        TextView tvOwnership = bottomSheetView.findViewById(R.id.tv_ownership);
                        TextView tvOwnerName = bottomSheetView.findViewById(R.id.tv_owner_name);
                        TextView tvContactNo = bottomSheetView.findViewById(R.id.tv_contact_no);
                        TextView tvWorkStatus = bottomSheetView.findViewById(R.id.tv_work_status);
                        TextView tvCoverage = bottomSheetView.findViewById(R.id.tv_coverage);
                        TextView tvBackupDays = bottomSheetView.findViewById(R.id.tv_backup_days);
                        TextView tvConnectedToNetwork = bottomSheetView.findViewById(R.id.tv_connected_to_network);

                        // Set the data in the views
                        tvSno.setText("S.No: " + sno);
                        tvLocation.setText("Location: " + location);
                        tvOwnership.setText("Ownership: " + ownership);
                        tvOwnerName.setText("Owner Name: " + ownerName);

                        // Handle ContactNo of different types (String, Long, etc.)
                        if (contactNo != null) {
                            if (contactNo instanceof Long) {
                                tvContactNo.setText("Contact No: " + ((Long) contactNo).toString());
                            } else if (contactNo instanceof String) {
                                tvContactNo.setText("Contact No: " + (String) contactNo);
                            } else {
                                tvContactNo.setText("Contact No: Unknown format");
                            }
                        } else {
                            tvContactNo.setText("Contact No: Not available");
                        }

                        tvWorkStatus.setText("Work Status: " + workStatus);
                        tvCoverage.setText("Coverage: " + coverage);
                        tvBackupDays.setText("Backup Days: " + backupDays);
                        tvConnectedToNetwork.setText("Connected to Network: " + connectedToNetwork);

                        bottomSheetDialog.setContentView(bottomSheetView);
                        bottomSheetDialog.show();

                    } else {
                        Toast.makeText(mapsui.this, "No data found for this location.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(mapsui.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required to show your location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupBottomMenuClickListeners() {
        settingsButton.setOnClickListener(v -> {
            highlightSelected(settingsButton);
            toggleMapTerrain();
        });
        helpButton.setOnClickListener(v -> {
            highlightSelected(helpButton);
            openHelpPage();
        });
        uploadButton.setOnClickListener(v -> {
            highlightSelected(uploadButton);
            saveDataToKml();
        });
        accountButton.setOnClickListener(v -> {
            highlightSelected(accountButton);
            openAccountPage();
        });
    }

    private void setupSearchListener() {
        findViewById(R.id.searchBar).setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });
    }

    private void setupFilterListener() {
        filterIcon.setOnClickListener(v -> {
            Intent intent = new Intent(mapsui.this, filter_activity.class);
            startActivityForResult(intent, FILTER_REQUEST_CODE);  // Start filter activity
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                LatLng selectedLocation = place.getLatLng();

                if (selectedLocation != null && mMap != null) {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(selectedLocation).title(place.getName()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Toast.makeText(this, "An error occurred: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // Handle the result from the filter activity
        if (requestCode == FILTER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Get the distance, ownership, and backupdays filter from the filter activity
            String distanceStr = data.getStringExtra("distance");
            String ownership = data.getStringExtra("ownership");
            String backupDaysStr = data.getStringExtra("backup");  // Fetch the backup days from the intent

            // Parse backup days as integer
            int backupDays = 0;
            if (backupDaysStr != null && !backupDaysStr.isEmpty()) {
                try {
                    backupDays = Integer.parseInt(backupDaysStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid backup days value", Toast.LENGTH_SHORT).show();
                }
            }

            if (distanceStr != null && !distanceStr.isEmpty()) {
                try {
                    double radiusInKm = Double.parseDouble(distanceStr);
                    if (radiusInKm > 0) {
                        // Call the function to filter cameras by radius, ownership, and backup days
                        filterCameras(radiusInKm);

                    } else {
                        Toast.makeText(this, "Invalid radius", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show();
                }
            }

            // Log the ownership and backup days (if needed for further processing)
            Log.d(TAG, "Ownership: " + ownership);
            Log.d(TAG, "Backup Days: " + backupDays);
        }
    }

    private void fetchFirestoreData() {
        db.collection("cctvdatanew")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Clear any existing markers from the map
                        cameraMarkers.clear();
                        mMap.clear();

                        // Fetch all documents from the Firestore collection
                        List<DocumentSnapshot> documentList = new ArrayList<>(task.getResult().getDocuments());

                        // Iterate over each document
                        for (DocumentSnapshot document : documentList) {
                            // Convert the entire document data into a string
                            String documentData = document.getData().toString();

                            // Split the string by commas
                            String[] dataArray = documentData.split(",");

                            // Temporary variables for latitude and longitude
                            Double latitude = null;
                            Double longitude = null;

                            // Process each element in the array
                            for (String dataPiece : dataArray) {
                                dataPiece = dataPiece.trim();

                                // Extract Latitude
                                if (dataPiece.contains("Latitude")) {
                                    try {
                                        latitude = Double.parseDouble(dataPiece.split("=")[1].trim());
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing Latitude: " + dataPiece, e);
                                    }
                                }

                                // Extract Longitude
                                if (dataPiece.contains("Longitude")) {
                                    try {
                                        longitude = Double.parseDouble(dataPiece.split("=")[1].trim());
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing Longitude: " + dataPiece, e);
                                    }
                                }

                                // If both latitude and longitude are available, add the marker
                                if (latitude != null && longitude != null) {
                                    LatLng location = new LatLng(latitude, longitude);

                                    // Add yellow marker to the map and store in the list
                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(location)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                            .title("CCTV Location"));

                                    cameraMarkers.add(marker); // Add marker to the list

                                    // Reset latitude and longitude for the next iteration
                                    latitude = null;
                                    longitude = null;
                                }
                            }
                        }

                        // Set camera markers to visible initially
                        for (Marker marker : cameraMarkers) {
                            marker.setVisible(true);
                        }

                        areCamerasVisible = true;
                        showCamerasButton.setText("Hide Cameras");

                    } else {
                        Log.e(TAG, "Error fetching data", task.getException());
                    }
                });
    }


    private void openSettingsPage() {

    }

    private void openHelpPage() {
        Intent intent = new Intent(mapsui.this, HelpActivity.class);
        startActivity(intent);
    }


    private void openAccountPage() {
        Intent intent = new Intent(mapsui.this, AccountActivity.class);
        startActivity(intent);
    }

    private void highlightSelected(View selectedButton) {
        if (this.selectedButton != null) {
            this.selectedButton.setBackground(null);
        }
        selectedButton.setBackgroundResource(R.drawable.highlight_background);
        this.selectedButton = selectedButton;
    }

    // Function to toggle visibility of camera markers
    private void toggleCamerasVisibility() {
        if (areCamerasVisible) {
            // Hide markers
            for (Marker marker : cameraMarkers) {
                marker.setVisible(false);
            }
            showCamerasButton.setText("Show Cameras");
        } else {
            // Show markers
            if (cameraMarkers.isEmpty()) {
                fetchFirestoreData();  // Fetch and add markers if not done before
            } else {
                for (Marker marker : cameraMarkers) {
                    marker.setVisible(true);
                }
            }
            showCamerasButton.setText("Hide Cameras");
        }
        areCamerasVisible = !areCamerasVisible;  // Toggle the state
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000;  // Convert to kilometers
    }

    private void saveDataToKml() {
        // Ensure that the cameraMarkers list is not empty
        if (cameraMarkers.isEmpty()) {
            Toast.makeText(this, "No camera markers to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // KML Header
        StringBuilder kmlData = new StringBuilder();
        kmlData.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kmlData.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kmlData.append("<Document>\n");
        kmlData.append("<name>CCTV Locations</name>\n");

        // Iterate over the markers and add their coordinates to the KML data
        for (Marker marker : cameraMarkers) {
            LatLng position = marker.getPosition();
            kmlData.append("<Placemark>\n");
            kmlData.append("<name>CCTV Location</name>\n");
            kmlData.append("<Point>\n");
            kmlData.append("<coordinates>").append(position.longitude).append(",").append(position.latitude).append("</coordinates>\n");
            kmlData.append("</Point>\n");
            kmlData.append("</Placemark>\n");
        }

        // KML Footer
        kmlData.append("</Document>\n");
        kmlData.append("</kml>\n");

        // Save the KML file to the Documents directory
        saveKmlFile(kmlData.toString());
    }

    private void saveKmlFile(String kmlContent) {
        // Check if external storage is writable
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage is not writable.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a file in the Documents directory
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }

        // Define the KML file
        File kmlFile = new File(documentsDir, "cctv_locations.kml");

        // Write the KML data to the file
        try (FileOutputStream fos = new FileOutputStream(kmlFile)) {
            fos.write(kmlContent.getBytes());
            Toast.makeText(this, "KML file saved: " + kmlFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving KML file.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error writing KML file", e);
        }
    }

    // Helper method to check if external storage is writable
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void toggleMapTerrain() {
        switch (currentMapType) {
            case GoogleMap.MAP_TYPE_NORMAL:
                currentMapType = GoogleMap.MAP_TYPE_TERRAIN;
                Toast.makeText(this, "Terrain View Enabled", Toast.LENGTH_SHORT).show();
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                currentMapType = GoogleMap.MAP_TYPE_SATELLITE;
                Toast.makeText(this, "Satellite View Enabled", Toast.LENGTH_SHORT).show();
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                currentMapType = GoogleMap.MAP_TYPE_HYBRID;
                Toast.makeText(this, "Hybrid View Enabled", Toast.LENGTH_SHORT).show();
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                currentMapType = GoogleMap.MAP_TYPE_NORMAL;
                Toast.makeText(this, "Normal View Enabled", Toast.LENGTH_SHORT).show();
                break;
        }

        // Apply the new map type
        if (mMap != null) {
            mMap.setMapType(currentMapType);
        }
    }

    // Filter and show cameras within the given radius
    // Filter and show cameras within the given radius
    // Fetch and log camera data (latitude and longitude)
    private void filterCameras(Double radiusInKm) {
        // Check permission before accessing location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        double userLat = location.getLatitude();
                        double userLng = location.getLongitude();

                        // Log user's current location
                        Log.d(TAG, "User location - Lat: " + userLat + ", Lng: " + userLng);

                        // Clear previous markers and list
                        cameraMarkers.clear();
                        mMap.clear();

                        // Fetch all cameras from Firestore
                        db.collection("cctvdatanew").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> documentList = new ArrayList<>(task.getResult().getDocuments());

                                for (DocumentSnapshot document : documentList) {
                                    // Convert the entire document data into a string
                                    String documentData = document.getData().toString();

                                    // Split the string by commas
                                    String[] dataArray = documentData.split(",");

                                    // Temporary variables for latitude and longitude
                                    Double latitude = null;
                                    Double longitude = null;

                                    // Process each element in the array
                                    for (String dataPiece : dataArray) {
                                        dataPiece = dataPiece.trim();

                                        // Extract Latitude
                                        if (dataPiece.contains("Latitude")) {
                                            try {
                                                latitude = Double.parseDouble(dataPiece.split("=")[1].trim());
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Error parsing Latitude: " + dataPiece, e);
                                            }
                                        }

                                        // Extract Longitude
                                        if (dataPiece.contains("Longitude")) {
                                            try {
                                                longitude = Double.parseDouble(dataPiece.split("=")[1].trim());
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Error parsing Longitude: " + dataPiece, e);
                                            }
                                        }

                                        // If both latitude and longitude are available, perform filtering
                                        if (latitude != null && longitude != null) {
                                            // Calculate distance using the Haversine formula
                                            double distanceToCamera = calculateDistance(userLat, userLng, latitude, longitude);
                                            Log.d(TAG, "Distance to camera: " + distanceToCamera + " km");

                                            // If the camera is within the radius, plot the marker
                                            if (distanceToCamera <= radiusInKm) {
                                                LatLng cameraLocation = new LatLng(latitude, longitude);

                                                // Add yellow marker to the map
                                                Marker marker = mMap.addMarker(new MarkerOptions()
                                                        .position(cameraLocation)
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                        .title("CCTV Location"));

                                                // Add marker to the list
                                                cameraMarkers.add(marker);

                                                Log.d(TAG, "Camera within radius, plotting marker at: Lat: " + latitude + ", Lng: " + longitude);
                                            } else {
                                                Log.d(TAG, "Camera outside radius, skipping");
                                            }

                                            // Reset latitude and longitude for the next iteration
                                            latitude = null;
                                            longitude = null;
                                        }
                                    }
                                }

                                // Set all markers visible initially
                                for (Marker marker : cameraMarkers) {
                                    marker.setVisible(true);
                                }

                                areCamerasVisible = true;
                                showCamerasButton.setText("Hide Cameras");

                            } else {
                                Log.e(TAG, "Error fetching data", task.getException());
                            }
                        });
                    } else {
                        Toast.makeText(mapsui.this, "Unable to retrieve user location.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission not granted", e);
            }
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

}