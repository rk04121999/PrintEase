package com.example.printease;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp, tvShopOwner;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvShopOwner = findViewById(R.id.tvShopOwner);

        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> resetPassword());

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SignUpActivity.class)));

        tvShopOwner.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ShopOwnerRegistrationActivity.class)));

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Please wait...");
            openUserDashboard(currentUser.getUid());
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Please wait...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            openUserDashboard(user.getUid());
                        } else {
                            resetLoginButton();
                            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        resetLoginButton();
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openUserDashboard(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()) {
                        auth.signOut();
                        resetLoginButton();
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = document.getString("role");

                    if ("CUSTOMER".equals(role)) {
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();

                    } else if ("SHOP_OWNER".equals(role)) {
                        checkShopApproval(uid);

                    } else {
                        auth.signOut();
                        resetLoginButton();
                        Toast.makeText(this, "Invalid account role", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    auth.signOut();
                    resetLoginButton();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkShopApproval(String uid) {
        db.collection("shops").document(uid).get()
                .addOnSuccessListener(shop -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String status = shop.exists()
                            ? shop.getString("verificationStatus")
                            : null;

                    if ("APPROVED".equals(status)) {
                        startActivity(new Intent(MainActivity.this,
                                ShopOwnerDashboardActivity.class));
                        finish();
                    } else {
                        auth.signOut();
                        resetLoginButton();

                        if ("REJECTED".equals(status)) {
                            Toast.makeText(this,
                                    "Your shop registration was rejected. Please contact support.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Your shop is waiting for admin approval.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    auth.signOut();
                    resetLoginButton();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your registered email first");
            etEmail.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
    }
}