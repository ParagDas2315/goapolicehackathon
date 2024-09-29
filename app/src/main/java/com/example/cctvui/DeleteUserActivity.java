package com.example.cctvui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteUserActivity extends AppCompatActivity {

    private EditText usernameInput;
    private Button deleteButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_user);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize input field and button
        usernameInput = findViewById(R.id.username_input_delete);
        deleteButton = findViewById(R.id.delete_button);

        // Set click listener for delete button
        deleteButton.setOnClickListener(v -> {
            String usernameToDelete = usernameInput.getText().toString().trim();

            if (TextUtils.isEmpty(usernameToDelete)) {
                Toast.makeText(DeleteUserActivity.this, "Please enter a username to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call the function to delete data by username
            deleteUserDataByUsername(usernameToDelete);
        });
    }

    // Function to delete data based on username
    private void deleteUserDataByUsername(String username) {
        // Reference to the users collection
        CollectionReference usersRef = db.collection("users");

        // Query Firestore for the document with the specified username
        usersRef.whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // If a document with the username is found, delete it
                        queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(DeleteUserActivity.this, "User data deleted successfully", Toast.LENGTH_SHORT).show();
                                    usernameInput.setText("");  // Clear the input field
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(DeleteUserActivity.this, "Error deleting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // If no document is found with the given username
                        Toast.makeText(DeleteUserActivity.this, "No user found with username: " + username, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DeleteUserActivity.this, "Error finding data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
