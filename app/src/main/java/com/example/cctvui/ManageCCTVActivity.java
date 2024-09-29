package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ManageCCTVActivity extends AppCompatActivity {

    private Button insertCCTVButton, deleteCCTVButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cctv);

        // Initialize buttons
        insertCCTVButton = findViewById(R.id.insert_cctv_button);
        deleteCCTVButton = findViewById(R.id.delete_cctv_button);

        // Set onClick listener for the Insert button to open InsertCCTVActivity
        insertCCTVButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManageCCTVActivity.this, InsertCCTVActivity.class);
            startActivity(intent);
        });

        // Set onClick listener for the Delete button to open DeleteCCTVActivity
        deleteCCTVButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManageCCTVActivity.this, DeleteCCTVActivity.class);
            startActivity(intent);
        });
    }
}
