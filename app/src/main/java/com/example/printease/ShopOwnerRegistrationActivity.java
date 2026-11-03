
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

public class ShopOwnerRegistrationActivity extends AppCompatActivity {

    private EditText etOwnerName, etShopName, etShopEmail, etShopPhone;
    private EditText etShopAddress, etShopPassword, etShopConfirmPassword;
    private Button btnRegisterShop;
    private TextView tvShopLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_owner_registration);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etOwnerName = findViewById(R.id.etOwnerName);
        etShopName = findViewById(R.id.etShopName);
        etShopEmail = findViewById(R.id.etShopEmail);
        etShopPhone = findViewById(R.id.etShopPhone);
        etShopAddress = findViewById(R.id.etShopAddress);
        etShopPassword = findViewById(R.id.etShopPassword);
        etShopConfirmPassword = findViewById(R.id.etShopConfirmPassword);
        btnRegisterShop = findViewById(R.id.btnRegisterShop);
        tvShopLogin = findViewById(R.id.tvShopLogin);

        btnRegisterShop.setOnClickListener(v -> registerShopOwner());

        tvShopLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void registerShopOwner() {
        String ownerName = etOwnerName.getText().toString().trim();
        String shopName = etShopName.getText().toString().trim();
        String email = etShopEmail.getText().toString().trim();
        String phone = etShopPhone.getText().toString().trim();
        String address = etShopAddress.getText().toString().trim();
        String password = etShopPassword.getText().toString();
        String confirmPassword = etShopConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(ownerName) || ownerName.length() < 3) {
            etOwnerName.setError("Enter a valid owner name");
            etOwnerName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(shopName)) {
            etShopName.setError("Enter your shop name");
            etShopName.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etShopEmail.setError("Enter a valid email address");
            etShopEmail.requestFocus();
            return;
        }

        if (!phone.matches("[6-9][0-9]{9}")) {
            etShopPhone.setError("Enter a valid 10-digit mobile number");
            etShopPhone.requestFocus();
            return;
        }

        if (address.length() < 10) {
            etShopAddress.setError("Enter a complete shop address");
            etShopAddress.requestFocus();
            return;
        }

        if (password.length() < 8
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*[0-9].*")) {
            etShopPassword.setError("Use at least 8 characters with letters and numbers");
            etShopPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etShopConfirmPassword.setError("Passwords do not match");
            etShopConfirmPassword.requestFocus();
            return;
        }

        btnRegisterShop.setEnabled(false);
        btnRegisterShop.setText("Registering...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveShopOwner(
                                    firebaseUser,
                                    ownerName,
                                    shopName,
                                    email,
                                    phone,
                                    address
                            );
                        } else {
                            resetButton();
                            Toast.makeText(
                                    this,
                                    "Registration failed",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    } else {
                        resetButton();

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void saveShopOwner(
            FirebaseUser firebaseUser,
            String ownerName,
            String shopName,
            String email,
            String phone,
            String address
    ) {

        String uid = firebaseUser.getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", ownerName);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", "SHOP_OWNER");
        user.put("active", true);
        user.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> shop = new HashMap<>();
        shop.put("ownerId", uid);
        shop.put("ownerName", ownerName);
        shop.put("shopName", shopName);
        shop.put("email", email);
        shop.put("phone", phone);
        shop.put("address", address);
        shop.put("verificationStatus", "PENDING");
        shop.put("createdAt", FieldValue.serverTimestamp());

        db.runBatch(batch -> {
            batch.set(db.collection("users").document(uid), user);
            batch.set(db.collection("shops").document(uid), shop);
        }).addOnSuccessListener(unused -> {

            auth.signOut();

            Toast.makeText(
                    this,
                    "Shop registered. Waiting for admin approval.",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent = new Intent(
                    ShopOwnerRegistrationActivity.this,
                    MainActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {

            firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                auth.signOut();
                resetButton();

                Toast.makeText(
                        this,
                        "Could not save shop: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            });
        });
    }

    private void resetButton() {
        btnRegisterShop.setEnabled(true);
        btnRegisterShop.setText("Register Shop");
    }
}
