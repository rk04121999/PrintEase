package com.example.printease;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnCreateAccount;
    private TextView tvAlreadyAccount;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvAlreadyAccount = findViewById(R.id.tvAlreadyAccount);

        btnCreateAccount.setOnClickListener(v -> registerCustomer());

        tvAlreadyAccount.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });
    }

    private void registerCustomer() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter your full name");
            etName.requestFocus();
            return;
        }

        if (name.length() < 3) {
            etName.setError("Name must contain at least 3 characters");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (!phone.matches("[6-9][0-9]{9}")) {
            etPhone.setError("Enter a valid 10-digit Indian mobile number");
            etPhone.requestFocus();
            return;
        }

        if (password.length() < 8
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*[0-9].*")) {
            etPassword.setError("Use at least 8 characters with letters and numbers");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnCreateAccount.setEnabled(false);
        btnCreateAccount.setText("Creating Account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveCustomerProfile(firebaseUser, name, email, phone);
                        } else {
                            resetButton();
                            Toast.makeText(this, "Account creation failed", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        resetButton();
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveCustomerProfile(FirebaseUser firebaseUser, String name,
                                     String email, String phone) {

        String uid = firebaseUser.getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", "CUSTOMER");
        user.put("active", true);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();

//                    Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
                })
                .addOnFailureListener(e -> {
                    firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                        auth.signOut();
                        resetButton();
                        Toast.makeText(this,
                                "Could not save profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                });
    }

    private void resetButton() {
        btnCreateAccount.setEnabled(true);
        btnCreateAccount.setText("Create Account");
    }
}
