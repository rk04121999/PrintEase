
package com.example.printease;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ShopOwnerDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvShopName, tvOrderCount, tvEmpty;
    private LinearLayout ordersContainer;
    private ProgressBar progressBar;

    private String shopId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_owner_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvShopName = findViewById(R.id.tvShopName);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        ordersContainer = findViewById(R.id.ordersContainer);
        progressBar = findViewById(R.id.progressBar);

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        shopId = user.getUid();

        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadOrders());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            openLogin();
        });

        loadShopDetails();
        loadOrders();
    }

    private void loadShopDetails() {
        db.collection("shops").document(shopId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()
                            || !"APPROVED".equals(document.getString("verificationStatus"))) {
                        auth.signOut();
                        openLogin();
                        return;
                    }

                    String name = document.getString("shopName");
                    tvShopName.setText(name != null ? name : "Shop Owner Dashboard");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        ordersContainer.removeAllViews();

        db.collection("orders")
                .whereEqualTo("shopId", shopId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    int count = querySnapshot.size();
                    tvOrderCount.setText("Total Orders: " + count);

                    if (count == 0) {
                        tvEmpty.setText("No orders received yet.");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        addOrderCard(document);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setText("Unable to load orders.");
                    tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addOrderCard(DocumentSnapshot document) {

        String orderId = document.getId();
        String customerName = getText(document, "customerName", "Customer");
        String customerEmail = getText(document, "customerEmail", "");
        String pdfName = getText(document, "pdfName", "PDF not selected");
        String paperSize = getText(document, "paperSize", "");
        String printType = getText(document, "printType", "");
        String printSide = getText(document, "printSide", "");
        String status = getText(document, "status", "UNKNOWN");

        Long pageCountValue = document.getLong("pageCount");
        Long copiesValue = document.getLong("copies");

        Number priceValue = document.get("estimatedPrice") instanceof Number
                ? (Number) document.get("estimatedPrice") : null;

        long pageCount = pageCountValue != null ? pageCountValue : 0;
        long copies = copiesValue != null ? copiesValue : 1;
        double price = priceValue != null ? priceValue.doubleValue() : 0;

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        cardParams.setMargins(0, 0, 0, dpToPx(16));

        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(22));
        card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(getColor(R.color.glass_bg));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(getColor(R.color.glass_border));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(18));

        TextView tvCustomer = createText("Customer: " + customerName, 18, true);
        TextView tvEmail = createText(customerEmail, 13, false);
        TextView tvFile = createText("PDF: " + pdfName, 15, true);
        TextView tvPages = createText(
                "Pages: " + pageCount + "    Copies: " + copies, 14, false
        );
        TextView tvPaper = createText("Paper: " + paperSize, 14, false);
        TextView tvPrint = createText(printType + " | " + printSide, 14, false);

        TextView tvPrice = createText(
                String.format(Locale.getDefault(), "Estimated Price: ₹%.2f", price),
                16, true
        );

        TextView tvStatus = createText(
                "Status: " + status.replace("_", " "), 15, true
        );

        tvEmail.setTextColor(getColor(R.color.secondary_text));
        tvPages.setTextColor(getColor(R.color.secondary_text));
        tvPaper.setTextColor(getColor(R.color.secondary_text));
        tvPrint.setTextColor(getColor(R.color.secondary_text));
        tvPrice.setTextColor(getColor(R.color.accent_color));

        setStatusColor(tvStatus, status);

        content.addView(tvCustomer);
        content.addView(tvEmail);
        content.addView(tvFile);
        content.addView(tvPages);
        content.addView(tvPaper);
        content.addView(tvPrint);
        content.addView(tvPrice);
        content.addView(tvStatus);

        addStatusButtons(content, orderId, status);

        card.addView(content);
        ordersContainer.addView(card);
    }

    private void addStatusButtons(
            LinearLayout content,
            String orderId,
            String status
    ) {

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        buttonRow.setPadding(0, dpToPx(16), 0, 0);

        if ("AWAITING_DOCUMENT".equals(status)) {

            MaterialButton acceptButton = createButton(
                    "Accept Order",
                    getColor(R.color.button_bg)
            );

            MaterialButton rejectButton = createButton(
                    "Reject Order",
                    Color.rgb(198, 40, 40)
            );

            LinearLayout.LayoutParams acceptParams =
                    new LinearLayout.LayoutParams(0, dpToPx(52), 1);

            acceptParams.setMargins(0, 0, dpToPx(6), 0);

            LinearLayout.LayoutParams rejectParams =
                    new LinearLayout.LayoutParams(0, dpToPx(52), 1);

            rejectParams.setMargins(dpToPx(6), 0, 0, 0);

            acceptButton.setLayoutParams(acceptParams);
            rejectButton.setLayoutParams(rejectParams);

            acceptButton.setOnClickListener(v ->
                    showOrderConfirmation(orderId, "ACCEPTED"));

            rejectButton.setOnClickListener(v ->
                    showOrderConfirmation(orderId, "REJECTED"));

            buttonRow.addView(acceptButton);
            buttonRow.addView(rejectButton);

        } else {

            String nextStatus = getNextStatus(status);

            if (nextStatus == null) {
                return;
            }

            String buttonText;

            switch (nextStatus) {
                case "PRINTING":
                    buttonText = "Start Printing";
                    break;
                case "READY":
                    buttonText = "Mark Ready";
                    break;
                case "COMPLETED":
                    buttonText = "Mark Completed";
                    break;
                default:
                    return;
            }

            MaterialButton nextButton = createButton(
                    buttonText,
                    getColor(R.color.button_bg)
            );

            nextButton.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(52)
                    )
            );

            nextButton.setOnClickListener(v ->
                    showOrderConfirmation(orderId, nextStatus));

            buttonRow.addView(nextButton);
        }

        content.addView(buttonRow);
    }

    private String getNextStatus(String currentStatus) {

        switch (currentStatus) {
            case "ACCEPTED":
                return "PRINTING";

            case "PRINTING":
                return "READY";

            case "READY":
                return "COMPLETED";

            default:
                return null;
        }
    }

    private void setStatusColor(TextView textView, String status) {

        switch (status) {
            case "ACCEPTED":
                textView.setTextColor(Color.rgb(46, 125, 50));
                break;

            case "PRINTING":
                textView.setTextColor(Color.rgb(21, 101, 192));
                break;

            case "READY":
                textView.setTextColor(Color.rgb(0, 121, 107));
                break;

            case "COMPLETED":
                textView.setTextColor(Color.rgb(46, 125, 50));
                break;

            case "REJECTED":
                textView.setTextColor(Color.rgb(198, 40, 40));
                break;

            default:
                textView.setTextColor(getColor(R.color.accent_color));
                break;
        }
    }

    private MaterialButton createButton(String text, int backgroundColor) {

        MaterialButton button = new MaterialButton(this);

        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);

        button.setBackgroundTintList(
                ColorStateList.valueOf(backgroundColor)
        );

        button.setCornerRadius(dpToPx(12));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(dpToPx(52));
        button.setMinimumHeight(dpToPx(52));
        button.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        return button;
    }

    private void showOrderConfirmation(String orderId, String newStatus) {

        String title;
        String message;

        switch (newStatus) {
            case "ACCEPTED":
                title = "Accept Order";
                message = "Are you sure you want to accept this order?";
                break;

            case "REJECTED":
                title = "Reject Order";
                message = "Are you sure you want to reject this order?";
                break;

            case "PRINTING":
                title = "Start Printing";
                message = "Have you started printing this order?";
                break;

            case "READY":
                title = "Mark Order Ready";
                message = "Confirm that this order is ready for collection?";
                break;

            case "COMPLETED":
                title = "Complete Order";
                message = "Confirm that this order has been completed?";
                break;

            default:
                return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) ->
                        updateOrderStatus(orderId, newStatus))
                .setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void updateOrderStatus(String orderId, String newStatus) {

        db.collection("orders").document(orderId)
                .update(
                        "status", newStatus,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {

                    String message;

                    switch (newStatus) {
                        case "ACCEPTED":
                            message = "Order accepted successfully";
                            break;

                        case "REJECTED":
                            message = "Order rejected successfully";
                            break;

                        case "PRINTING":
                            message = "Printing started successfully";
                            break;

                        case "READY":
                            message = "Order marked as ready";
                            break;

                        case "COMPLETED":
                            message = "Order completed successfully";
                            break;

                        default:
                            message = "Order status updated";
                            break;
                    }

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to update order: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
    }

    private String getText(
            DocumentSnapshot document,
            String field,
            String defaultValue
    ) {
        String value = document.getString(field);
        return value != null ? value : defaultValue;
    }

    private TextView createText(String value, int size, boolean bold) {

        TextView textView = new TextView(this);

        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(getColor(R.color.primary_text));
        textView.setPadding(0, dpToPx(4), 0, dpToPx(4));

        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }

    private void openLogin() {

        Intent intent = new Intent(this, MainActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
