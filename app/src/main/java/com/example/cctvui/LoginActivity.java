package com.example.cctvui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize login button
        loginButton = findViewById(R.id.login_button);

        // Set click listener for login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the mapsui activity
                Intent intent = new Intent(LoginActivity.this, mapsui.class);
                startActivity(intent);
            }
        });
    }
}
