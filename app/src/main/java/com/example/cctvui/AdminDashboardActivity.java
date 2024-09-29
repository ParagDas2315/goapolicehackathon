package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button manageCCTVButton, manageUserDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize buttons
        manageCCTVButton = findViewById(R.id.manage_cctv_button);
        manageUserDataButton = findViewById(R.id.manage_user_data_button);

        // Set onClick listeners for buttons
        manageCCTVButton.setOnClickListener(v -> {
            // Redirect to Manage CCTV Data Activity
            Intent intent = new Intent(AdminDashboardActivity.this, ManageCCTVActivity.class);
            startActivity(intent);
        });

        manageUserDataButton.setOnClickListener(v -> {
            // Redirect to Manage User Data Activity
            Intent intent = new Intent(AdminDashboardActivity.this, ManageUserDataActivity.class);
            startActivity(intent);
        });
    }
}
