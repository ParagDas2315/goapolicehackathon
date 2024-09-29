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
            openSettingsPage();
        });
        helpButton.setOnClickListener(v -> {
            highlightSelected(helpButton);
            openHelpPage();
        });
        uploadButton.setOnClickListener(v -> {
            highlightSelected(uploadButton);
            openUploadPage();
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
                        filterCameras(radiusInKm, ownership, backupDays);
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



    private void openSettingsPage() {
        Intent intent = new Intent(mapsui.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openHelpPage() {
        Intent intent = new Intent(mapsui.this, HelpActivity.class);
        startActivity(intent);
    }

    private void openUploadPage() {
        Intent intent = new Intent(mapsui.this, UploadActivity.class);
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

    // Filter and show cameras within the given radius
    // Filter and show cameras within the given radius
    private void filterCameras(Double radiusInKm, @Nullable String ownership, @Nullable Integer backupDays) {
        // Check permission before accessing location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        double userLat = location.getLatitude();
                        double userLng = location.getLongitude();

                        // Clear previous markers and list
                        cameraMarkers.clear();
                        mMap.clear();

                        db.collection("cctvdatanew").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> documentList = new ArrayList<>(task.getResult().getDocuments());
                                List<MarkerOptions> markersToPlot = new ArrayList<>();

                                for (DocumentSnapshot document : documentList) {
                                    // Temporary variables for latitude, longitude, ownership, and backupdays
                                    Double latitude = null;
                                    Double longitude = null;
                                    String ownerData = null;
                                    Integer documentBackupDays = null;

                                    // Process each element in the document
                                    for (String key : document.getData().keySet()) {
                                        Object dataPiece = document.get(key);  // Get the field value as an Object

                                        // Ensure the value is a string before splitting
                                        if (dataPiece instanceof String) {
                                            String[] parts = ((String) dataPiece).split("=");

                                            // Check if the split resulted in two parts before accessing them
                                            if (parts.length == 2) {
                                                if (key.equals("Latitude")) {
                                                    latitude = Double.parseDouble(parts[1].trim());
                                                } else if (key.equals("Longitude")) {
                                                    longitude = Double.parseDouble(parts[1].trim());
                                                } else if (key.equals("Owner")) {
                                                    ownerData = parts[1].trim();
                                                } else if (key.equals("BackupDays")) {
                                                    documentBackupDays = Integer.parseInt(parts[1].trim());
                                                }
                                            } else {
                                                // Log a warning or handle the case where split doesn't have two parts
                                                Log.w(TAG, "Unexpected format for " + key + ": " + dataPiece);
                                            }
                                        }
                                    }


                                    // If latitude and longitude are available, start filtering
                                    if (latitude != null && longitude != null) {
                                        boolean passesFilters = true;

                                        // 1. Check the distance filter (if radius is provided)
                                        if (radiusInKm != null) {
                                            double distanceToCamera = calculateDistance(userLat, userLng, latitude, longitude);
                                            if (distanceToCamera > radiusInKm) {
                                                passesFilters = false;
                                            }
                                        }

                                        // 2. Check the ownership filter (if ownership is provided)
                                        if (ownership != null && !ownership.equals("All")) {
                                            if (ownerData == null || !ownerData.equals(ownership)) {
                                                passesFilters = false;
                                            }
                                        }

                                        // 3. Check the backup days filter (if backupDays is provided)
                                        if (backupDays != null) {
                                            if (documentBackupDays == null || !documentBackupDays.equals(backupDays)) {
                                                passesFilters = false;
                                            }
                                        }

                                        // If the camera passes all active filters, prepare to plot it
                                        if (passesFilters) {
                                            LatLng cameraLocation = new LatLng(latitude, longitude);
                                            markersToPlot.add(new MarkerOptions()
                                                    .position(cameraLocation)
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                    .title("CCTV Location"));
                                        }
                                    }
                                }

                                // Plot all the filtered markers
                                for (MarkerOptions markerOptions : markersToPlot) {
                                    cameraMarkers.add(mMap.addMarker(markerOptions));
                                }

                                // Update camera visibility state
                                areCamerasVisible = !cameraMarkers.isEmpty();
                                showCamerasButton.setText(areCamerasVisible ? "Hide Cameras" : "Show Cameras");

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




    // Fetch and log camera data (latitude and longitude)
    private void fetchFirestoreData() {
        db.collection("cctvdatanew")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Initialize a list to store the LatLng coordinates
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

                                // Check if the data piece contains Latitude or Longitude
                                if (dataPiece.contains("Latitude")) {
                                    latitude = Double.parseDouble(dataPiece.split("=")[1].trim());
                                } else if (dataPiece.contains("Longitude")) {
                                    longitude = Double.parseDouble(dataPiece.split("=")[1].trim());
                                }

                                // If both latitude and longitude are available, add the marker
                                if (latitude != null && longitude != null) {
                                    LatLng location = new LatLng(latitude, longitude);

                                    // Add yellow marker to the map and store in the list
                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(location)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                            .title("CCTV Location"));

                                    cameraMarkers.add(marker);  // Add marker to the list

                                    // Reset latitude and longitude for next iteration
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
}