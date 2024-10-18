package com.example.cctvui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class OtpLoginActivity extends AppCompatActivity {

    private EditText phoneInput, otpInput;
    private Button sendOtpButton, verifyOtpButton;
    private FirebaseAuth mAuth;
    private String verificationId;

    // Progress bar
    private ProgressDialog progressDialog;
    private ProgressDialog otpVerificationProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_login); // Set the first layout for sending OTP

        mAuth = FirebaseAuth.getInstance();

        // Initialize phone input and send OTP button
        phoneInput = findViewById(R.id.phone_input);
        sendOtpButton = findViewById(R.id.send_otp_button);

        // Initialize ProgressDialogs
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending OTP...");
        progressDialog.setCancelable(false);

        otpVerificationProgressDialog = new ProgressDialog(this);
        otpVerificationProgressDialog.setMessage("Verifying OTP...");
        otpVerificationProgressDialog.setCancelable(false);

        // Set the Send OTP button click listener
        sendOtpButton.setOnClickListener(v -> {
            String phoneNumber = phoneInput.getText().toString().trim();

            // Add country code automatically if not present
            if (!phoneNumber.startsWith("+91")) {
                phoneNumber = "+91" + phoneNumber;
            }

            // Validate the length of the phone number after adding country code
            if (phoneNumber.length() != 13) {  // 13 because +91 + 10 digits of the phone number
                Toast.makeText(OtpLoginActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;  // Exit the method if the format is incorrect
            }

            // Show progress bar while checking the phone number
            if (!isFinishing()) {
                progressDialog.show();
            }

            // Check if the phone number exists in Firestore before sending OTP
            checkPhoneNumberInDatabase(phoneNumber);
        });
    }

    private void checkPhoneNumberInDatabase(String phoneNumber) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Assuming you have a collection named 'users' that stores phone numbers
        db.collection("users")
                .whereEqualTo("phone", phoneNumber)  // Make sure phone numbers are stored in a 'phone' field
                .get()
                .addOnCompleteListener(task -> {
                    if (!isFinishing()) {
                        progressDialog.dismiss(); // Hide progress bar when the database operation is complete
                    }

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Phone number found, proceed with sending OTP
                        sendOtp(phoneNumber);
                    } else {
                        // Phone number not found in the database
                        Toast.makeText(OtpLoginActivity.this, "This phone number is not registered.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(OtpLoginActivity.this, "Error checking phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendOtp(String phoneNumber) {
        // Show progress while sending OTP
        if (!isFinishing()) {
            progressDialog.show();
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    if (!isFinishing()) {
                        progressDialog.dismiss(); // Hide progress when verification is completed
                    }
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    if (!isFinishing()) {
                        progressDialog.dismiss(); // Hide progress when verification fails
                    }
                    Toast.makeText(OtpLoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    if (!isFinishing()) {
                        progressDialog.dismiss();  // Hide progress after OTP is sent
                    }
                    OtpLoginActivity.this.verificationId = verificationId;
                    showOtpCard();
                    Toast.makeText(OtpLoginActivity.this, "OTP sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void showOtpCard() {
        setContentView(R.layout.otp_verification_card); // Switch to the OTP verification layout

        otpInput = findViewById(R.id.otp_input);
        verifyOtpButton = findViewById(R.id.verify_otp_button);

        verifyOtpButton.setOnClickListener(v -> {
            String code = otpInput.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(OtpLoginActivity.this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isFinishing()) {
                otpVerificationProgressDialog.show(); // Show progress when verifying OTP
            }
            verifyOtp(code);
        });

        Button resendOtpButton = findViewById(R.id.resend_otp_button);
        resendOtpButton.setOnClickListener(v -> {
            String phoneNumber = phoneInput.getText().toString().trim();
            if (!TextUtils.isEmpty(phoneNumber)) {
                sendOtp(phoneNumber); // Resend OTP
            } else {
                Toast.makeText(OtpLoginActivity.this, "Phone number missing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!isFinishing()) {
                        otpVerificationProgressDialog.dismiss(); // Hide progress after verification is done
                    }
                    if (task.isSuccessful()) {
                        // OTP verified successfully
                        Toast.makeText(OtpLoginActivity.this, "OTP Verified", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        // OTP verification failed
                        Toast.makeText(OtpLoginActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(OtpLoginActivity.this, mapsui.class);
        startActivity(intent);
        finish();
    }
}
