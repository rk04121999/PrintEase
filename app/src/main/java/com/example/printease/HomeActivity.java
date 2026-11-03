package com.example.printease;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserEmail;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        loadCustomerDetails();

        findViewById(R.id.btnUploadPrint).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, UploadPrintActivity.class)));

        findViewById(R.id.btnFindShops).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ShopListActivity.class))
        );

        findViewById(R.id.btnMyOrders).setOnClickListener(v ->
                Toast.makeText(this, "My Orders feature coming next", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navHome).setOnClickListener(v -> {});

        findViewById(R.id.navOrders).setOnClickListener(v ->
                Toast.makeText(this, "My Orders coming next", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navShops).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ShopListActivity.class))
        );

        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "Customer Profile coming next", Toast.LENGTH_SHORT).show());

        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show());
    }

    private void loadCustomerDetails() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {
                            tvGreeting.setText("Hello, " + name + "!");
                        } else {
                            tvGreeting.setText("Hello, Customer!");
                        }
                    } else {
                        tvGreeting.setText("Hello, Customer!");
                    }
                })
                .addOnFailureListener(e ->
                        tvGreeting.setText("Hello, Customer!"));
    }

    private void openLogin() {
        auth.signOut();
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
        finish();
    }
}
