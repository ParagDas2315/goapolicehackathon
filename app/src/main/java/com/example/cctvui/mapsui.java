package com.example.cctvui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.QuerySnapshot;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;


public class mapsui extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "mapsui";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;  // Firestore instance
    private List<Marker> cameraMarkers = new ArrayList<>();  // Store camera markers
    private boolean areCamerasShown = false;

    private static final int REQUEST_LOCATION_PERMISSION = 113;
    private static final double RADIUS_IN_KM = 100;  // Fixed radius of 100 km
    private ImageView saveKmlButton;
    private CardView progressCard;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapsui);

        db = FirebaseFirestore.getInstance();  // Initialize Firestore
        Button showCamerasButton = findViewById(R.id.show_cameras_button);
        ImageView filterButton = findViewById(R.id.filter_icon);
        saveKmlButton = findViewById(R.id.upload_icon);
        progressCard = findViewById(R.id.progress_card);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            // Implement location updates here if needed
        };

        // Check location permission on start
        checkLocationPermission();

        filterButton.setOnClickListener(v -> {
            Intent intent = new Intent(mapsui.this, filter_activity.class);
            startActivityForResult(intent, 1);  // 1 is the request code for filter
        });

        // Show/Hide Cameras button logic
        showCamerasButton.setOnClickListener(v -> {
            if (areCamerasShown) {
                hideCameras(); // Hide cameras if they are currently shown
                showCamerasButton.setText("Show Cameras");
            } else {
                fetchAllCameras(); // Show cameras if they are currently hidden
                showCamerasButton.setText("Hide Cameras");
            }
            areCamerasShown = !areCamerasShown; // Toggle the state
        });
        saveKmlButton.setOnClickListener(v -> saveDataToKML());
    }

    private void hideCameras() {
        if (mMap != null) {
            // Clear all markers from the map
            mMap.clear();
        }
    }

    // Show loading bar
    private void showLoading() {
        if (progressCard != null) {
            progressCard.setVisibility(View.VISIBLE);
        }
    }

    // Hide loading bar
    private void hideLoading() {
        if (progressCard != null) {
            progressCard.setVisibility(View.GONE);
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);  // Update interval of 10 seconds
        locationRequest.setFastestInterval(5000);  // Fastest update interval of 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission not granted", e);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void saveDataToKML() {
        List<Marker> markers = cameraMarkers;  // Get the current list of displayed markers
        if (markers.isEmpty()) {
            Toast.makeText(this, "No markers available to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the KML content
        StringBuilder kmlContent = new StringBuilder();
        kmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kmlContent.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kmlContent.append("<Document>\n");

        // Add each marker's coordinates to the KML content
        for (Marker marker : markers) {
            LatLng position = marker.getPosition();
            kmlContent.append("<Placemark>\n");
            kmlContent.append("<name>").append(marker.getTitle()).append("</name>\n");
            kmlContent.append("<Point>\n");
            kmlContent.append("<coordinates>").append(position.longitude).append(",").append(position.latitude).append("</coordinates>\n");
            kmlContent.append("</Point>\n");
            kmlContent.append("</Placemark>\n");
        }

        kmlContent.append("</Document>\n");
        kmlContent.append("</kml>");

        // Create a timestamp for the filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "CCTV_Markers_" + timeStamp + ".kml";

        // Save the KML content to a file in the documents directory
        try {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "KMLFiles");
            if (!directory.exists()) {
                directory.mkdirs(); // Create the directory if it doesn't exist
            }
            File kmlFile = new File(directory, fileName);
            FileWriter writer = new FileWriter(kmlFile);
            writer.write(kmlContent.toString());
            writer.flush();
            writer.close();

            Toast.makeText(this, "KML file saved successfully at " + kmlFile.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving KML file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error saving KML file", e);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String distanceStr = data.getStringExtra("distance");
            String backupStr = data.getStringExtra("backup");

            // Provide default values if inputs are empty
            Double radius = null;  // Default radius is null (meaning no radius filter)
            Integer backupDays = null;  // Default backup days is null (fetch all)

            ArrayList<String> ownershipList = data.getStringArrayListExtra("ownership");

            // Check if distance is provided, if not, leave radius as null
            if (distanceStr != null && !distanceStr.isEmpty()) {
                radius = Double.parseDouble(distanceStr);
            }

            // Check if backup is provided, if not, leave backupDays as null
            if (backupStr != null && !backupStr.isEmpty()) {
                backupDays = Integer.parseInt(backupStr);
            }

            // Now, call fetchFilteredCameras with the dynamic parameters
            fetchFilteredCameras(radius, backupDays, ownershipList);
        }
    }


    private void fetchFilteredCameras(@Nullable Double radius, @Nullable Integer backupDays, List<String> ownershipList) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    showLoading();
                    double userLat = location.getLatitude();
                    double userLng = location.getLongitude();

                    // Start building the query with ownership filters
                    Query query = db.collection("cctvdatanew")
                            .whereIn("Ownership", ownershipList); // Use whereIn for multiple ownership options

                    // Only apply the backupDays filter if a value is provided (non-null)
                    if (backupDays != null) {
                        query = query.whereEqualTo("Backupdays", backupDays);
                    }

                    // Execute the query
                    query.get().addOnCompleteListener(task -> {
                        hideLoading();
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documentList = task.getResult().getDocuments();
                            mMap.clear(); // Clear the map
                            cameraMarkers.clear(); // Clear existing markers

                            if (documentList.isEmpty()) {
                                Toast.makeText(this, "No cameras found with the specified filters.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for (DocumentSnapshot document : documentList) {
                                try {
                                    // Fetch latitude and longitude as numbers
                                    Double latitude = document.getDouble("Latitude");
                                    Double longitude = document.getDouble("Longitude");

                                    if (latitude != null && longitude != null) {
                                        if (radius != null) {
                                            // If radius is provided, filter based on distance
                                            double distanceToCamera = calculateDistance(userLat, userLng, latitude, longitude);
                                            if (distanceToCamera <= radius) {
                                                LatLng cameraLocation = new LatLng(latitude, longitude);
                                                Marker marker = mMap.addMarker(new MarkerOptions()
                                                        .position(cameraLocation)
                                                        .title("CCTV Location"));
                                                cameraMarkers.add(marker);
                                            }
                                        } else {
                                            // If no radius is provided, add all cameras that match the filters
                                            LatLng cameraLocation = new LatLng(latitude, longitude);
                                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                    .position(cameraLocation)
                                                    .title("CCTV Location"));
                                            cameraMarkers.add(marker);
                                        }
                                    } else {
                                        Log.e(TAG, "Latitude/Longitude is null for document: " + document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing document: " + document.getId(), e);
                                }
                            }

                            if (cameraMarkers.isEmpty()) {
                                Toast.makeText(this, "No cameras found within the specified radius.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Error fetching data", task.getException());
                        }
                    });
                } else {
                    Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(this, "Error accessing location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
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
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // Default map type

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);  // Enable location tracking
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false); // Disable default location button
            mMap.setPadding(0,0,0,250);

            // Custom "My Location" Button
            ImageView customLocationButton = findViewById(R.id.custom_location_button);
            customLocationButton.setOnClickListener(v -> moveToUserLocation());

            // Terrain Toggle Button
            ImageView terrainToggleButton = findViewById(R.id.terrain_button);
            terrainToggleButton.setOnClickListener(v -> toggleMapTerrain());

            // Start location updates
            startLocationUpdates();

            // Move the map to user location on load
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                }
            });

            // Set a click listener for markers
            mMap.setOnMarkerClickListener(marker -> {
                LatLng markerPosition = marker.getPosition();
                fetchCameraDetailsFromFirestore(markerPosition.latitude, markerPosition.longitude);
                return true;
            });
        }
    }

    // Move the map to user's current location
    private void moveToUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                } else {
                    Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Function to toggle between different map types (Normal, Terrain, Satellite, Hybrid)
    private void toggleMapTerrain() {
        int currentMapType = mMap.getMapType();
        switch (currentMapType) {
            case GoogleMap.MAP_TYPE_NORMAL:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                Toast.makeText(this, "Terrain View", Toast.LENGTH_SHORT).show();
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                Toast.makeText(this, "Satellite View", Toast.LENGTH_SHORT).show();
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                Toast.makeText(this, "Hybrid View", Toast.LENGTH_SHORT).show();
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                Toast.makeText(this, "Normal View", Toast.LENGTH_SHORT).show();
                break;
            default:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                Toast.makeText(this, "Normal View", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    private void toggleMapView() {
        if (mMap != null) {
            int currentType = mMap.getMapType();
            switch (currentType) {
                case GoogleMap.MAP_TYPE_NORMAL:
                    changeMapView(GoogleMap.MAP_TYPE_SATELLITE);
                    Toast.makeText(this, "Satellite View", Toast.LENGTH_SHORT).show();
                    break;
                case GoogleMap.MAP_TYPE_SATELLITE:
                    changeMapView(GoogleMap.MAP_TYPE_TERRAIN);
                    Toast.makeText(this, "Terrain View", Toast.LENGTH_SHORT).show();
                    break;
                case GoogleMap.MAP_TYPE_TERRAIN:
                    changeMapView(GoogleMap.MAP_TYPE_HYBRID);
                    Toast.makeText(this, "Hybrid View", Toast.LENGTH_SHORT).show();
                    break;
                case GoogleMap.MAP_TYPE_HYBRID:
                    changeMapView(GoogleMap.MAP_TYPE_NORMAL);
                    Toast.makeText(this, "Normal View", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void changeMapView(int viewType) {
        if (mMap != null) {
            mMap.setMapType(viewType);
        }
    }



    private void fetchAllCameras() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    showLoading();
                    double userLat = location.getLatitude();
                    double userLng = location.getLongitude();

                    // Fetch all camera locations from Firestore
                    db.collection("cctvdatanew")
                            .get()
                            .addOnCompleteListener(task -> {
                                hideLoading();
                                if (task.isSuccessful()) {
                                    List<DocumentSnapshot> documentList = task.getResult().getDocuments();
                                    mMap.clear();  // Clear any previous markers
                                    cameraMarkers.clear();

                                    if (documentList.isEmpty()) {
                                        Log.d(TAG, "No cameras found.");
                                        Toast.makeText(this, "No cameras found.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    for (DocumentSnapshot document : documentList) {
                                        try {
                                            // Fetch Latitude and Longitude
                                            Object latObj = document.get("Latitude");
                                            Object lngObj = document.get("Longitude");

                                            Double latitude = null;
                                            Double longitude = null;

                                            if (latObj instanceof Double) {
                                                latitude = (Double) latObj;
                                            } else if (latObj instanceof String) {
                                                latitude = Double.parseDouble((String) latObj);
                                            }

                                            if (lngObj instanceof Double) {
                                                longitude = (Double) lngObj;
                                            } else if (lngObj instanceof String) {
                                                longitude = Double.parseDouble((String) lngObj);
                                            }

                                            if (latitude != null && longitude != null) {
                                                double distanceToCamera = calculateDistance(userLat, userLng, latitude, longitude);

                                                if (distanceToCamera <= RADIUS_IN_KM) {
                                                    LatLng cameraLocation = new LatLng(latitude, longitude);
                                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                                            .position(cameraLocation)
                                                            .title("CCTV Location"));

                                                    cameraMarkers.add(marker);
                                                }
                                            } else {
                                                Log.e(TAG, "Latitude/Longitude is null or missing for document: " + document.getId());
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error processing document: " + document.getId(), e);
                                        }
                                    }

                                    if (cameraMarkers.isEmpty()) {
                                        Toast.makeText(this, "No cameras found within the specified radius.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e(TAG, "Error fetching data", task.getException());
                                }
                            });
                } else {
                    Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(this, "Error accessing location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
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

                        // Show the details in a BottomSheetDialog
                        showBottomSheet(sno, location, ownership, ownerName, contactNo, workStatus, coverage, backupDays, connectedToNetwork);
                    } else {
                        Toast.makeText(mapsui.this, "No data found for this location.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(mapsui.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBottomSheet(Long sno, String location, String ownership, String ownerName, Object contactNo, String workStatus, String coverage, Long backupDays, String connectedToNetwork) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);

        // Find views inside the bottom sheet and set the data
        TextView tvSno = bottomSheetView.findViewById(R.id.tv_sno);
        TextView tvLocation = bottomSheetView.findViewById(R.id.tv_location);
        TextView tvOwnership = bottomSheetView.findViewById(R.id.tv_ownership);
        TextView tvOwnerName = bottomSheetView.findViewById(R.id.tv_owner_name);
        TextView tvContactNo = bottomSheetView.findViewById(R.id.tv_contact_no);
        TextView tvWorkStatus = bottomSheetView.findViewById(R.id.tv_work_status);
        TextView tvCoverage = bottomSheetView.findViewById(R.id.tv_coverage);
        TextView tvBackupDays = bottomSheetView.findViewById(R.id.tv_backup_days);
        TextView tvConnectedToNetwork = bottomSheetView.findViewById(R.id.tv_connected_to_network);

        // Set the fetched data into views
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
    }

    // Helper method to calculate distance between two coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000;  // Convert to kilometers
    }
}
