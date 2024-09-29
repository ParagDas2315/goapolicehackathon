package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ManageUserDataActivity extends AppCompatActivity {

    private Button insertUserButton, deleteUserButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user);

        // Initialize buttons
        insertUserButton = findViewById(R.id.insert_user_button);
        deleteUserButton = findViewById(R.id.delete_user_button);

        // Set onClick listener for the Insert button to open InsertUserActivity
        insertUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManageUserDataActivity.this, InsertUserActivity.class);
            startActivity(intent);
        });

        // Set onClick listener for the Delete button to open DeleteUserActivity
        deleteUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManageUserDataActivity.this, DeleteUserActivity.class);
            startActivity(intent);
        });
    }
}
