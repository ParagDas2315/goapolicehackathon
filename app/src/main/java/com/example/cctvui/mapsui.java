package com.example.cctvui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.gms.maps.model.MarkerOptions;
import android.os.Looper;

public class mapsui extends AppCompatActivity implements OnMapReadyCallback {

    private EditText searchBar;
    private ImageView searchIcon, filterIcon;
    private LinearLayout settingsButton, helpButton, uploadButton, accountButton;
    private View selectedButton;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private static final int REQUEST_LOCATION_PERMISSION = 113;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initially make the status bar transparent
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        setContentView(R.layout.mapsui);

        // Initialize the search bar, filter button, and bottom menu buttons
        searchBar = findViewById(R.id.searchBar);
        searchIcon = findViewById(R.id.search_icon);
        filterIcon = findViewById(R.id.filter_icon);

        settingsButton = findViewById(R.id.settings_button);
        helpButton = findViewById(R.id.help_button);
        uploadButton = findViewById(R.id.upload_button);
        accountButton = findViewById(R.id.account_button);

        // Set click listener for filter button
        filterIcon.setOnClickListener(v -> openFilterPage());

        // Initialize bottom menu buttons
        setupBottomMenuClickListeners();

        // Initialize Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the location callback
        locationCallback = new LocationCallback() {
            // Implement location updates here if needed
        };

        checkLocationPermission();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            startLocationUpdates();
        } else {
            // Request the location permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            startLocationUpdates();

            // Show user's current location
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15)); // Zoom level 15 for a closer view

                    // Change the status bar color to white and text to dark when the map is ready
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    getWindow().setStatusBarColor(android.graphics.Color.WHITE); // Set the status bar color to white
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Call to super

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location updates
                startLocationUpdates();
            } else {
                // Permission denied, handle as needed (e.g., show a message)
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

    private void openFilterPage() {
        Intent intent = new Intent(mapsui.this, filter_activity.class);
        startActivity(intent);
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

    // Fix to clear focus from search bar and bottom menu selection when clicking outside
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                View focusedView = getCurrentFocus();
                int[] screenCoords = new int[2];
                focusedView.getLocationOnScreen(screenCoords);
                float x = event.getRawX() + focusedView.getLeft() - screenCoords[0];
                float y = event.getRawY() + focusedView.getTop() - screenCoords[1];

                if (x < focusedView.getLeft() || x > focusedView.getRight() || y < focusedView.getTop() || y > focusedView.getBottom()) {
                    hideKeyboard(view);
                    view.clearFocus();
                }
            }

            // Deselect the bottom menu button when clicked outside the menu
            if (selectedButton != null) {
                int[] location = new int[2];
                selectedButton.getLocationOnScreen(location);
                float x = event.getRawX();
                float y = event.getRawY();

                if (x < location[0] || x > location[0] + selectedButton.getWidth()
                        || y < location[1] || y > location[1] + selectedButton.getHeight()) {
                    selectedButton.setBackground(null);
                    selectedButton = null;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
