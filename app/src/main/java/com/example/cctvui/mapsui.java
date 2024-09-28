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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
            startLocationUpdates();

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                }
            });
        }
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
            // Get the distance and ownership filter from the filter activity
            String distanceStr = data.getStringExtra("distance");
            String ownership = data.getStringExtra("ownership");

            if (distanceStr != null && !distanceStr.isEmpty()) {
                try {
                    double radiusInKm = Double.parseDouble(distanceStr);
                    if (radiusInKm > 0) {
                        // Call the function to filter cameras by radius
                        filterCamerasWithinRadius(radiusInKm);
                    } else {
                        Toast.makeText(this, "Invalid radius", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show();
                }
            }

            // Ownership filtering logic (optional, if needed)
            // You can handle ownership filtering here if needed
            Log.d(TAG, "Ownership: " + ownership);  // This is just a placeholder for ownership filtering
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
    private void filterCamerasWithinRadius(double radiusInKm) {
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
                                            double distanceToCamera = calculateDistance(userLat, userLng, latitude, longitude);

                                            // Only show markers within the specified radius
                                            if (distanceToCamera <= radiusInKm) {
                                                LatLng cameraLocation = new LatLng(latitude, longitude);
                                                Marker marker = mMap.addMarker(new MarkerOptions()
                                                        .position(cameraLocation)
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                        .title("CCTV Location"));

                                                cameraMarkers.add(marker);  // Add marker to the list
                                            }

                                            // Reset latitude and longitude for next iteration
                                            latitude = null;
                                            longitude = null;
                                        }
                                    }
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

