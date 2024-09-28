package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class filter_activity extends AppCompatActivity {

    private EditText distanceInput, backupInput;
    private CheckBox allCheckbox, publicCheckbox, privateCheckbox, schoolsCheckbox, religiousCheckbox, residentsCheckbox, banksCheckbox;
    private Button clearButton, applyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // Initialize filters and buttons
        distanceInput = findViewById(R.id.distance_input);
        backupInput = findViewById(R.id.backup_input);

        allCheckbox = findViewById(R.id.all_checkbox);
        publicCheckbox = findViewById(R.id.public_checkbox);
        privateCheckbox = findViewById(R.id.private_checkbox);
        schoolsCheckbox = findViewById(R.id.schools_checkbox);
        religiousCheckbox = findViewById(R.id.religious_checkbox);
        residentsCheckbox = findViewById(R.id.residents_checkbox);
        banksCheckbox = findViewById(R.id.banks_checkbox);

        clearButton = findViewById(R.id.clear_button);
        applyButton = findViewById(R.id.apply_button);

        // Back Arrow Click Listener
        findViewById(R.id.back_arrow).setOnClickListener(v -> onBackPressed());

        // Clear Button: Resets all filters
        clearButton.setOnClickListener(v -> clearFilters());

        // Apply Button: Applies the filters
        applyButton.setOnClickListener(v -> applyFilters());

        // Set 'All' checkbox functionality
        allCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // If 'All' is checked, uncheck the others
                publicCheckbox.setChecked(false);
                privateCheckbox.setChecked(false);
                schoolsCheckbox.setChecked(false);
                religiousCheckbox.setChecked(false);
                residentsCheckbox.setChecked(false);
                banksCheckbox.setChecked(false);
            }
        });

        // Uncheck 'All' if any of the other checkboxes are selected
        publicCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) allCheckbox.setChecked(false);
        });
        privateCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) allCheckbox.setChecked(false);
        });
        schoolsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) allCheckbox.setChecked(false);
        });
        religiousCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) allCheckbox.setChecked(false);
        });
        residentsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) allCheckbox.setChecked(false);
        });
        banksCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) allCheckbox.setChecked(false);
        });
    }

    // Method to clear all filters
    private void clearFilters() {
        // Clear the text input fields
        distanceInput.setText("");
        backupInput.setText("");

        // Reset all checkboxes
        allCheckbox.setChecked(true);
        publicCheckbox.setChecked(false);
        privateCheckbox.setChecked(false);
        schoolsCheckbox.setChecked(false);
        religiousCheckbox.setChecked(false);
        residentsCheckbox.setChecked(false);
        banksCheckbox.setChecked(false);

        Toast.makeText(this, "Filters Cleared", Toast.LENGTH_SHORT).show();
    }

    // Method to apply the selected filters
    private void applyFilters() {
        // Get the distance and backup duration values
        String distance = distanceInput.getText().toString();
        String backup = backupInput.getText().toString();

        // Get ownership selection
        StringBuilder ownership = new StringBuilder();
        if (allCheckbox.isChecked()) {
            ownership.append("All");
        } else {
            if (publicCheckbox.isChecked()) ownership.append("Public ");
            if (privateCheckbox.isChecked()) ownership.append("Private ");
            if (schoolsCheckbox.isChecked()) ownership.append("Schools ");
            if (religiousCheckbox.isChecked()) ownership.append("Religious ");
            if (residentsCheckbox.isChecked()) ownership.append("Residents ");
            if (banksCheckbox.isChecked()) ownership.append("Banks ");
        }

        // Pass the filter data back to the previous activity (mapsui)
        Intent intent = new Intent();
        intent.putExtra("distance", distance);  // Send the distance filter
        intent.putExtra("ownership", ownership.toString().trim());  // Send ownership filter
        setResult(RESULT_OK, intent); // Pass the data back to mapsui activity
        finish(); // Close the filter activity
    }
}
