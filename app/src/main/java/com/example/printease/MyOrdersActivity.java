package com.example.printease;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyOrdersActivity extends AppCompatActivity {

    private LinearLayout ordersContainer;
    private TextView tvOrderCount, tvEmpty;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ordersContainer = findViewById(R.id.ordersContainer);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadOrders());

        loadOrders();
    }

    private void loadOrders() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            auth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        ordersContainer.removeAllViews();
        tvEmpty.setVisibility(View.GONE);
        tvOrderCount.setText("Loading your orders...");

        db.collection("orders")
                .whereEqualTo("customerId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> orderList =
                            new ArrayList<>(querySnapshot.getDocuments());

                    Collections.sort(orderList, (first, second) -> {
                        Timestamp firstTime = first.getTimestamp("createdAt");
                        Timestamp secondTime = second.getTimestamp("createdAt");

                        if (firstTime == null && secondTime == null) return 0;
                        if (firstTime == null) return 1;
                        if (secondTime == null) return -1;

                        return secondTime.compareTo(firstTime);
                    });

                    int count = orderList.size();
                    tvOrderCount.setText(count + (count == 1
                            ? " order found"
                            : " orders found"));

                    if (count == 0) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (DocumentSnapshot document : orderList) {
                        addOrderCard(document);
                    }
                })
                .addOnFailureListener(e -> {
                    tvOrderCount.setText("Unable to load orders");
                    Toast.makeText(
                            this,
                            "Failed to load orders: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addOrderCard(DocumentSnapshot document) {
        String orderId = document.getId();
        String status = getStringValue(document, "status", "UNKNOWN");

        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(15);

        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.glass_bg)
        );
        card.setRadius(dp(22));
        card.setCardElevation(dp(4));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(
                ContextCompat.getColor(this, R.color.glass_border)
        );

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView tvShop = createText(
                getStringValue(document, "shopName", "Print shop"),
                18,
                true,
                R.color.primary_text
        );
        content.addView(tvShop);

        TextView tvStatus = createText(
                getStatusLabel(status),
                12,
                true,
                R.color.button_text
        );
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(dp(13), dp(7), dp(13), dp(7));

        GradientDrawable statusBackground = new GradientDrawable();
        statusBackground.setCornerRadius(dp(20));
        statusBackground.setColor(getStatusColor(status));
        tvStatus.setBackground(statusBackground);

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(10);
        content.addView(tvStatus, statusParams);

        addDivider(content);

        addProgressTracker(content, status);

        addDivider(content);

        addDetail(content, "Document",
                getStringValue(document, "pdfName", "Not available"));

        addDetail(content, "Pages", String.valueOf(
                document.getLong("pageCount") != null
                        ? document.getLong("pageCount") : 0
        ));

        addDetail(content, "Paper size",
                getStringValue(document, "paperSize", "Not available"));

        addDetail(content, "Print type",
                getStringValue(document, "printType", "Not available"));

        addDetail(content, "Printing sides",
                getStringValue(document, "printSide", "Not available"));

        addDetail(content, "Copies", String.valueOf(
                document.getLong("copies") != null
                        ? document.getLong("copies") : 1
        ));

        Number price = document.get("estimatedPrice") instanceof Number
                ? (Number) document.get("estimatedPrice") : 0;

        DecimalFormat format = new DecimalFormat("0.00");
        addDetail(content, "Estimated price", "₹" + format.format(price.doubleValue()));

        Timestamp timestamp = document.getTimestamp("createdAt");
        String date = timestamp == null
                ? "Not available"
                : new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        ).format(timestamp.toDate());

        addDetail(content, "Order date", date);

        String shopAddress = getStringValue(document, "shopAddress", "");
        if (!shopAddress.isEmpty()) {
            addDetail(content, "Shop address", shopAddress);
        }

        String shopPhone = getStringValue(document, "shopPhone", "");
        if (!shopPhone.isEmpty()) {
            addDetail(content, "Shop phone", shopPhone);
        }

        boolean documentUploaded = Boolean.TRUE.equals(
                document.getBoolean("documentUploaded")
        );

        TextView tvDocument = createText(
                documentUploaded
                        ? "Document uploaded"
                        : "Document upload pending",
                12,
                true,
                R.color.secondary_text
        );

        LinearLayout.LayoutParams documentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        documentParams.topMargin = dp(12);
        content.addView(tvDocument, documentParams);

        if ("COMPLETED".equals(status)) {
            addFeedbackSection(content, orderId);
        }

        card.addView(content);
        ordersContainer.addView(card);
    }

    private void addProgressTracker(LinearLayout parent, String status) {

        TextView heading = createText(
                "Order progress",
                15,
                true,
                R.color.primary_text
        );

        heading.setPadding(0, dp(5), 0, dp(12));
        parent.addView(heading);

        if ("REJECTED".equals(status)) {
            TextView rejected = createText(
                    "This order was rejected by the shop.",
                    14,
                    true,
                    R.color.secondary_text
            );
            parent.addView(rejected);
            return;
        }

        String[] steps = {
                "Order Accepted",
                "Printing Started",
                "Ready for Pickup",
                "Completed"
        };

        int currentStep;

        switch (status) {
            case "ACCEPTED":
                currentStep = 0;
                break;
            case "PRINTING":
                currentStep = 1;
                break;
            case "READY":
                currentStep = 2;
                break;
            case "COMPLETED":
                currentStep = 3;
                break;
            default:
                currentStep = -1;
                break;
        }

        if (currentStep == -1) {
            TextView pending = createText(
                    "Waiting for the shop to accept your order.",
                    13,
                    false,
                    R.color.secondary_text
            );
            parent.addView(pending);
            return;
        }

        for (int i = 0; i < steps.length; i++) {

            LinearLayout stepRow = new LinearLayout(this);
            stepRow.setOrientation(LinearLayout.HORIZONTAL);
            stepRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView circle = createText(
                    i <= currentStep ? "✓" : String.valueOf(i + 1),
                    13,
                    true,
                    R.color.button_text
            );

            circle.setGravity(Gravity.CENTER);
            circle.setTextColor(Color.WHITE);

            GradientDrawable circleBackground = new GradientDrawable();
            circleBackground.setShape(GradientDrawable.OVAL);
            circleBackground.setColor(
                    i <= currentStep
                            ? ContextCompat.getColor(this, R.color.button_bg)
                            : ContextCompat.getColor(this, R.color.border_color)
            );
            circle.setBackground(circleBackground);

            LinearLayout.LayoutParams circleParams =
                    new LinearLayout.LayoutParams(dp(30), dp(30));
            circleParams.rightMargin = dp(12);

            stepRow.addView(circle, circleParams);

            TextView label = createText(
                    steps[i],
                    13,
                    i == currentStep,
                    i <= currentStep
                            ? R.color.primary_text
                            : R.color.secondary_text
            );

            stepRow.addView(label);
            parent.addView(stepRow);

            if (i < steps.length - 1) {
                View connector = new View(this);
                connector.setBackgroundColor(
                        i < currentStep
                                ? ContextCompat.getColor(this, R.color.button_bg)
                                : ContextCompat.getColor(this, R.color.border_color)
                );

                LinearLayout.LayoutParams connectorParams =
                        new LinearLayout.LayoutParams(dp(2), dp(15));
                connectorParams.leftMargin = dp(14);

                parent.addView(connector, connectorParams);
            }
        }
    }

    private void addFeedbackSection(LinearLayout parent, String orderId) {

        addDivider(parent);

        TextView heading = createText(
                "Rate your experience",
                15,
                true,
                R.color.primary_text
        );

        parent.addView(heading);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        String customerId = user.getUid();

        db.collection("orders")
                .document(orderId)
                .collection("feedback")
                .document(customerId)
                .get()
                .addOnSuccessListener(feedback -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (feedback.exists()) {
                        TextView submitted = createText(
                                "Thank you! You have already submitted feedback.",
                                13,
                                true,
                                R.color.secondary_text
                        );
                        parent.addView(submitted);
                    } else {
                        TextView button = createText(
                                "Submit your feedback",
                                14,
                                true,
                                R.color.button_text
                        );

                        button.setGravity(Gravity.CENTER);
                        button.setPadding(dp(14), dp(12), dp(14), dp(12));

                        GradientDrawable background = new GradientDrawable();
                        background.setCornerRadius(dp(12));
                        background.setColor(
                                ContextCompat.getColor(this, R.color.button_bg)
                        );
                        button.setBackground(background);

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                        params.topMargin = dp(10);

                        parent.addView(button, params);

                        button.setOnClickListener(v ->
                                showFeedbackDialog(orderId, customerId));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to check feedback: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
    }

    private void showFeedbackDialog(String orderId, String customerId) {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(5));

        TextView ratingLabel = new TextView(this);
        ratingLabel.setText("Rate your printing experience");
        ratingLabel.setTextSize(14);
        ratingLabel.setTextColor(getColor(R.color.primary_text));

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);
        ratingBar.setRating(5);

        EditText commentInput = new EditText(this);
        commentInput.setHint("Write your feedback (optional)");
        commentInput.setTextSize(14);
        commentInput.setTextColor(getColor(R.color.primary_text));
        commentInput.setHintTextColor(getColor(R.color.secondary_text));
        commentInput.setMinLines(3);
        commentInput.setGravity(Gravity.TOP);
        commentInput.setBackgroundResource(R.drawable.bg_input);
        commentInput.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout.LayoutParams ratingParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        ratingParams.topMargin = dp(10);

        LinearLayout.LayoutParams commentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        commentParams.topMargin = dp(15);

        layout.addView(ratingLabel);
        layout.addView(ratingBar, ratingParams);
        layout.addView(commentInput, commentParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Order Feedback")
                .setView(layout)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener(v -> {

            int rating = Math.round(ratingBar.getRating());
            String comment = commentInput.getText().toString().trim();

            if (rating < 1 || rating > 5) {
                Toast.makeText(
                        this,
                        "Please select a rating.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            saveFeedback(orderId, customerId, rating, comment, dialog);
        }));

        dialog.show();
    }

    private void saveFeedback(
            String orderId,
            String customerId,
            int rating,
            String comment,
            AlertDialog dialog
    ) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || !user.getUid().equals(customerId)) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        db.collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(order -> {

                    if (!order.exists()
                            || !customerId.equals(order.getString("customerId"))
                            || !"COMPLETED".equals(order.getString("status"))) {
                        Toast.makeText(
                                this,
                                "Feedback is not available for this order.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String shopId = order.getString("shopId");

                    if (shopId == null || shopId.trim().isEmpty()) {
                        Toast.makeText(
                                this,
                                "Shop information is missing.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    Map<String, Object> feedbackData = new HashMap<>();
                    feedbackData.put("orderId", orderId);
                    feedbackData.put("customerId", customerId);
                    feedbackData.put("shopId", shopId);
                    feedbackData.put("rating", rating);
                    feedbackData.put("comment", comment);
                    feedbackData.put("createdAt", FieldValue.serverTimestamp());

                    Map<String, Object> publicReviewData = new HashMap<>();
                    publicReviewData.put("rating", rating);
                    publicReviewData.put("comment", comment);
                    publicReviewData.put("createdAt", FieldValue.serverTimestamp());

                    com.google.firebase.firestore.DocumentReference privateFeedbackRef =
                            db.collection("orders")
                                    .document(orderId)
                                    .collection("feedback")
                                    .document(customerId);

                    com.google.firebase.firestore.DocumentReference publicReviewRef =
                            db.collection("shops")
                                    .document(shopId)
                                    .collection("reviews")
                                    .document(orderId);

                    WriteBatch batch = db.batch();

                    batch.set(privateFeedbackRef, feedbackData);
                    batch.set(publicReviewRef, publicReviewData);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                dialog.dismiss();

                                Toast.makeText(
                                        this,
                                        "Thank you for your feedback!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadOrders();
                            })
                            .addOnFailureListener(e -> {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                Toast.makeText(
                                        this,
                                        "Unable to submit feedback: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to verify order: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show());
    }

    private void addDetail(LinearLayout parent, String label, String value) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView title = createText(
                label,
                13,
                false,
                R.color.secondary_text
        );

        TextView detail = createText(
                value,
                13,
                true,
                R.color.primary_text
        );
        detail.setGravity(Gravity.END);

        row.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        row.addView(detail, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        parent.addView(row);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(
                ContextCompat.getColor(this, R.color.border_color)
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        params.topMargin = dp(13);
        params.bottomMargin = dp(8);

        parent.addView(divider, params);
    }

    private TextView createText(
            String value,
            int size,
            boolean bold,
            int colorResource
    ) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(
                ContextCompat.getColor(this, colorResource)
        );

        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private String getStringValue(
            DocumentSnapshot document,
            String field,
            String defaultValue
    ) {
        String value = document.getString(field);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case "AWAITING_DOCUMENT":
                return "AWAITING DOCUMENT";
            case "ACCEPTED":
                return "ACCEPTED";
            case "REJECTED":
                return "REJECTED";
            case "PRINTING":
                return "PRINTING";
            case "READY":
                return "READY FOR PICKUP";
            case "COMPLETED":
                return "COMPLETED";
            default:
                return status.replace("_", " ");
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "ACCEPTED":
            case "COMPLETED":
            case "READY":
                return Color.rgb(46, 125, 50);
            case "REJECTED":
                return Color.rgb(198, 40, 40);
            case "PRINTING":
                return Color.rgb(21, 101, 192);
            default:
                return Color.rgb(230, 126, 34);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
