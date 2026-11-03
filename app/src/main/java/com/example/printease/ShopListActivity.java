
package com.example.printease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ShopListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout shopsContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private boolean hasPrintDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        shopsContainer = findViewById(R.id.shopsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        hasPrintDetails = getIntent().hasExtra("pdfName");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadShops());

        loadShops();
    }

    private void loadShops() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        shopsContainer.removeAllViews();

        db.collection("shops")
                .whereEqualTo("verificationStatus", "APPROVED")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (querySnapshot.isEmpty()) {
                        tvEmpty.setText("No approved print shops available yet.");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        addShopCard(
                                document.getId(),
                                document.getString("shopName"),
                                document.getString("ownerName"),
                                document.getString("address"),
                                document.getString("phone")
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setText("Unable to load shops. Please try again.");
                    tvEmpty.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addShopCard(
            String shopId,
            String shopName,
            String ownerName,
            String address,
            String phone
    ) {
        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 16);

        card.setLayoutParams(cardParams);
        card.setRadius(18);
        card.setCardElevation(2);
        card.setStrokeWidth(1);
        card.setStrokeColor(
                ContextCompat.getColor(this, R.color.border_color)
        );
        card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.card_bg)
        );
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);

        TextView name = createText(
                safeText(shopName),
                19,
                R.color.primary_text,
                true
        );

        TextView owner = createText(
                "Owner: " + safeText(ownerName),
                14,
                R.color.secondary_text,
                false
        );

        TextView shopAddress = createText(
                "Address: " + safeText(address),
                14,
                R.color.secondary_text,
                false
        );

        TextView contact = createText(
                "Contact: " + safeText(phone),
                14,
                R.color.secondary_text,
                false
        );

        TextView status = createText(
                hasPrintDetails ? "Select this shop →" : "Verified Print Shop",
                14,
                R.color.accent_color,
                true
        );

        owner.setPadding(0, 12, 0, 0);
        shopAddress.setPadding(0, 8, 0, 0);
        contact.setPadding(0, 8, 0, 0);
        status.setPadding(0, 14, 0, 0);

        content.addView(name);
        content.addView(owner);
        content.addView(shopAddress);
        content.addView(contact);
        content.addView(status);

        card.addView(content);

        card.setOnClickListener(v -> {
            if (!hasPrintDetails) {
                Toast.makeText(
                        this,
                        "Select a PDF first from Upload & Print",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(
                    ShopListActivity.this,
                    OrderReviewActivity.class
            );

            intent.putExtra("shopId", shopId);
            intent.putExtra("shopName", safeText(shopName));
            intent.putExtra("shopAddress", safeText(address));
            intent.putExtra("shopPhone", safeText(phone));

            intent.putExtras(getIntent().getExtras());

            startActivity(intent);
        });

        shopsContainer.addView(card);
    }

    private TextView createText(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(ContextCompat.getColor(this, color));

        if (bold) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return textView;
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }

        return value;
    }
}
