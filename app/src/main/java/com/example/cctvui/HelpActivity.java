package com.example.cctvui;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {

    private TextView helpContentTextView;
    private static final String TAG = "HelpActivity"; // For logging purposes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Initialize the TextView where Firestore data will be displayed
        helpContentTextView = findViewById(R.id.help_content_text_view);

        // Set the back arrow click listener to go back to the previous page
        findViewById(R.id.back_arrow).setOnClickListener(v -> onBackPressed());

        // Fetch Firestore data and display it in the TextView
        fetchFirestoreData();
    }

    // Method to fetch Firestore data as a string and process it
    private void fetchFirestoreData() {
        // Get the Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch all documents from the collection
        db.collection("cctvdatanew")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Initialize a string builder to store the data
                        StringBuilder completeData = new StringBuilder();
                        List<DocumentSnapshot> documentList = new ArrayList<>(task.getResult().getDocuments());

                        for (DocumentSnapshot document : documentList) {
                            // Convert the entire document data into a string
                            String documentData = document.getData().toString();

                            // Split the string by commas
                            String[] dataArray = documentData.split(",");

                            // Process each element in the array
                            for (String dataPiece : dataArray) {
                                // Trim any extra spaces
                                dataPiece = dataPiece.trim();

                                // Check if the data piece contains Latitude or Longitude
                                if (dataPiece.contains("Latitude") || dataPiece.contains("Longitude")) {
                                    Log.d(TAG, "Latitude/Longitude Data: " + dataPiece);

                                    // Add only Latitude and Longitude to the display
                                    completeData.append(dataPiece).append("\n");
                                }
                            }

                            // Log the full string data for debugging
                            Log.d(TAG, "Full Document Data: " + documentData);
                        }

                        // Set the text view with the Latitude/Longitude data
                        helpContentTextView.setText(completeData.toString());
                    } else {
                        helpContentTextView.setText("Failed to fetch data");
                        Log.e(TAG, "Fetch failed: ", task.getException());
                    }
                });
    }
}
