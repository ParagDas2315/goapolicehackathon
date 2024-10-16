package com.example.cctvui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.CameraPosition;
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
import android.app.ProgressDialog;


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
    private ImageView terminal;
    private ProgressDialog progressDialog;
    private Geocoder geocoder;
    private Marker lastSearchMarker;

    private ArrayList<String> latLngListForTerminal = new ArrayList<>();




    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapsui);

        db = FirebaseFirestore.getInstance();  // Initialize Firestore
        Button showCamerasButton = findViewById(R.id.show_cameras_button);
        ImageView filterButton = findViewById(R.id.filter_icon);
        saveKmlButton = findViewById(R.id.upload_icon);
        terminal = findViewById(R.id.help_icon);
        LinearLayout account = findViewById(R.id.account_button);
        geocoder = new Geocoder(this, Locale.getDefault());
        EditText placeSearchInput = findViewById(R.id.place_search_input);
        ImageView searchIcon = findViewById(R.id.search_icon);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching Locations...");
        progressDialog.setCancelable(false);


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
        placeSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null &&
                    event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                // Perform the search when the Enter key or search button is pressed
                String placeName = placeSearchInput.getText().toString().trim();
                searchForPlace(placeName);

                // Optionally hide the soft keyboard after search
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(placeSearchInput.getWindowToken(), 0);
                }

                return true;  // Return true to indicate the event is handled
            }
            return false;
        });

        terminal.setOnClickListener(v -> {
            Intent intent = new Intent(mapsui.this, HelpActivity.class);
            intent.putStringArrayListExtra("LAT_LNG_LIST", latLngListForTerminal);
            startActivity(intent);
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

        account.setOnClickListener(v -> {
            Intent intent = new Intent(mapsui.this, AccountActivity.class);
            startActivity(intent);
        });

    }

    private void searchForPlace(String placeName) {
        if (placeName == null || placeName.isEmpty()) {
            Toast.makeText(this, "Please enter a place name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Trim the input string to remove unnecessary spaces
        placeName = placeName.trim();

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // Fetch addresses based on the place name
            List<Address> addresses = geocoder.getFromLocationName(placeName, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Move camera to the searched location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                // Remove the previous marker if it exists
                if (lastSearchMarker != null) {
                    lastSearchMarker.remove();
                }

                // Add a new marker at the searched location and save its reference
                lastSearchMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(placeName));
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error searching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }






    private void hideCameras() {
        if (mMap != null) {
            // Clear all markers from the map
            mMap.clear();
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
                    progressDialog.show();
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
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documentList = task.getResult().getDocuments();
                            mMap.clear(); // Clear the map
                            cameraMarkers.clear(); // Clear existing markers
                            latLngListForTerminal.clear(); // Clear the previous data

                            if (documentList.isEmpty()) {
                                Toast.makeText(this, "No cameras found with the specified filters.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for (DocumentSnapshot document : documentList) {
                                try {
                                    // Fetch Latitude and Longitude as numbers
                                    Double latitude = null;
                                    Double longitude = null;

                                    Object latObj = document.get("Latitude");
                                    Object lngObj = document.get("Longitude");

                                    if (latObj instanceof Double) {
                                        latitude = (Double) latObj;
                                    }

                                    if (lngObj instanceof Double) {
                                        longitude = (Double) lngObj;
                                    }

                                    // Ensure both latitude and longitude are not null
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

                                                // Add to latLngListForTerminal
                                                String cameraDetails = String.format("Lat: %f, Lng: %f", latitude, longitude);
                                                latLngListForTerminal.add(cameraDetails);
                                            }
                                        } else {
                                            // If no radius is provided, add all cameras that match the filters
                                            LatLng cameraLocation = new LatLng(latitude, longitude);
                                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                    .position(cameraLocation)
                                                    .title("CCTV Location"));
                                            cameraMarkers.add(marker);

                                            // Add to latLngListForTerminal
                                            String cameraDetails = String.format("Lat: %f, Lng: %f", latitude, longitude);
                                            latLngListForTerminal.add(cameraDetails);
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
                progressDialog.dismiss();
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
            mMap.getUiSettings().setCompassEnabled(false);
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
                    progressDialog.show();
                    double userLat = location.getLatitude();
                    double userLng = location.getLongitude();

                    // Fetch all camera locations from Firestore
                    db.collection("cctvdatanew").get().addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documentList = task.getResult().getDocuments();
                            mMap.clear();  // Clear any previous markers
                            cameraMarkers.clear();
                            latLngListForTerminal.clear(); // Clear the previous data

                            if (documentList.isEmpty()) {
                                Log.d(TAG, "No cameras found.");
                                Toast.makeText(this, "No cameras found.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for (DocumentSnapshot document : documentList) {
                                try {
                                    // Fetch Latitude and Longitude as numbers
                                    Double latitude = null;
                                    Double longitude = null;

                                    Object latObj = document.get("Latitude");
                                    Object lngObj = document.get("Longitude");

                                    if (latObj instanceof Double) {
                                        latitude = (Double) latObj;
                                    }

                                    if (lngObj instanceof Double) {
                                        longitude = (Double) lngObj;
                                    }

                                    // Ensure both latitude and longitude are not null
                                    if (latitude != null && longitude != null) {
                                        double distanceToCamera = calculateDistance(userLat, userLng, latitude, longitude);

                                        if (distanceToCamera <= RADIUS_IN_KM) {
                                            LatLng cameraLocation = new LatLng(latitude, longitude);
                                            Marker marker = mMap.addMarker(new MarkerOptions()
                                                    .position(cameraLocation)
                                                    .title("CCTV Location"));

                                            cameraMarkers.add(marker);

                                            // Add to latLngListForTerminal
                                            String cameraDetails = String.format("Lat: %f, Lng: %f", latitude, longitude);
                                            latLngListForTerminal.add(cameraDetails);
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
                progressDialog.dismiss();
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
        tvSno.setTextColor(Color.BLACK);
        TextView tvLocation = bottomSheetView.findViewById(R.id.tv_location);
        tvLocation.setTextColor(Color.BLACK);
        TextView tvOwnership = bottomSheetView.findViewById(R.id.tv_ownership);
        tvOwnership.setTextColor(Color.BLACK);
        TextView tvOwnerName = bottomSheetView.findViewById(R.id.tv_owner_name);
        tvOwnerName.setTextColor(Color.BLACK);
        TextView tvContactNo = bottomSheetView.findViewById(R.id.tv_contact_no);
        tvContactNo.setTextColor(Color.BLACK);
        TextView tvWorkStatus = bottomSheetView.findViewById(R.id.tv_work_status);
        tvWorkStatus.setTextColor(Color.BLACK);
        TextView tvCoverage = bottomSheetView.findViewById(R.id.tv_coverage);
        tvCoverage.setTextColor(Color.BLACK);
        TextView tvBackupDays = bottomSheetView.findViewById(R.id.tv_backup_days);
        tvBackupDays.setTextColor(Color.BLACK);
        TextView tvConnectedToNetwork = bottomSheetView.findViewById(R.id.tv_connected_to_network);
        tvConnectedToNetwork.setTextColor(Color.BLACK);

        // Set the fetched data into views
        tvSno.setText(sno.toString());
        tvLocation.setText(location);
        tvOwnership.setText(ownership);
        tvOwnerName.setText(ownerName);

        // Handle ContactNo of different types (String, Long, etc.)
        if (contactNo != null) {
            if (contactNo instanceof Long) {
                tvContactNo.setText(((Long) contactNo).toString());
            } else if (contactNo instanceof String) {
                tvContactNo.setText((String) contactNo);
            } else {
                tvContactNo.setText("Contact No: Unknown format");
            }
        } else {
            tvContactNo.setText("Contact No: Not available");
        }

        tvWorkStatus.setText(workStatus);
        tvCoverage.setText(coverage);
        tvBackupDays.setText(backupDays.toString());
        tvConnectedToNetwork.setText(connectedToNetwork);

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
