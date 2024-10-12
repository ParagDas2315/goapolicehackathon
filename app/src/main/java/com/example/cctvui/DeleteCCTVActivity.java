package com.example.cctvui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteCCTVActivity extends AppCompatActivity {

    private EditText snoInput;
    private Button deleteButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_cctv);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize input field and button
        snoInput = findViewById(R.id.input_sno_delete);
        deleteButton = findViewById(R.id.delete_button);

        // Set click listener for delete button
        deleteButton.setOnClickListener(v -> {
            String snoToDelete = snoInput.getText().toString().trim();

            if (TextUtils.isEmpty(snoToDelete)) {
                Toast.makeText(DeleteCCTVActivity.this, "Please enter Sno to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call the function to delete data by Sno
            deleteCCTVDataBySno(snoToDelete);
        });
    }

    // Function to delete data based on Sno
    private void deleteCCTVDataBySno(String sno) {
        // Reference to the CCTV data collection
        CollectionReference cctvDataRef = db.collection("cctvdatatesting");

        // Query Firestore for the document with the specified Sno
        cctvDataRef.whereEqualTo("Sno", Integer.parseInt(sno))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // If a document with the Sno is found, delete it
                        queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(DeleteCCTVActivity.this, "CCTV Data deleted successfully", Toast.LENGTH_SHORT).show();
                                    snoInput.setText("");  // Clear the input field
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(DeleteCCTVActivity.this, "Error deleting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // If no document is found with the given Sno
                        Toast.makeText(DeleteCCTVActivity.this, "No data found with Sno: " + sno, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DeleteCCTVActivity.this, "Error finding data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
