
package com.example.printease;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OrderReviewActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String shopId;
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private String pdfName;
    private String paperSize;
    private String printType;
    private String printSide;

    private int pageCount;
    private int copies;
    private double estimatedPrice;

    private MaterialButton btnPlaceOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_review);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        shopId = getIntent().getStringExtra("shopId");
        shopName = getIntent().getStringExtra("shopName");
        shopAddress = getIntent().getStringExtra("shopAddress");
        shopPhone = getIntent().getStringExtra("shopPhone");

        pdfName = getIntent().getStringExtra("pdfName");
        paperSize = getIntent().getStringExtra("paperSize");
        printType = getIntent().getStringExtra("printType");
        printSide = getIntent().getStringExtra("printSide");

        pageCount = getIntent().getIntExtra("pageCount", 0);
        copies = getIntent().getIntExtra("copies", 1);
        estimatedPrice = getIntent().getDoubleExtra("estimatedPrice", 0);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        displayOrderDetails();

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void displayOrderDetails() {
        setValue(R.id.tvShopName, shopName);
        setValue(R.id.tvShopAddress, shopAddress);
        setValue(R.id.tvShopPhone, shopPhone);
        setValue(R.id.tvPdfName, pdfName);
        setValue(R.id.tvPageCount, pageCount + " pages");
        setValue(R.id.tvPaperSize, paperSize);
        setValue(R.id.tvPrintType, printType);
        setValue(R.id.tvPrintSide, printSide);
        setValue(R.id.tvCopies, String.valueOf(copies));
        setValue(R.id.tvTotalPrice, String.format("₹%.2f", estimatedPrice));
    }

    private void setValue(int viewId, String value) {
        TextView textView = findViewById(viewId);
        textView.setText(value == null ? "Not available" : value);
    }

    private void placeOrder() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (shopId == null || pdfName == null || pageCount <= 0
                || estimatedPrice <= 0 || copies < 1) {
            Toast.makeText(
                    this,
                    "Order details are incomplete. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Placing order...");

        String customerId = auth.getCurrentUser().getUid();
        String customerEmail = auth.getCurrentUser().getEmail();

        db.collection("users")
                .document(customerId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    String customerName = userDocument.getString("name");

                    if (customerName == null || customerName.trim().isEmpty()) {
                        customerName = customerEmail == null
                                ? "Customer"
                                : customerEmail;
                    }

                    saveOrder(customerId, customerName, customerEmail);
                })
                .addOnFailureListener(e ->
                        saveOrder(customerId, customerEmail, customerEmail)
                );
    }

    private void saveOrder(
            String customerId,
            String customerName,
            String customerEmail
    ) {
        Map<String, Object> order = new HashMap<>();

        order.put("customerId", customerId);
        order.put("customerName", customerName);
        order.put("customerEmail", customerEmail);

        order.put("shopId", shopId);
        order.put("shopName", shopName);
        order.put("shopAddress", shopAddress);
        order.put("shopPhone", shopPhone);

        order.put("pdfName", pdfName);
        order.put("pageCount", pageCount);
        order.put("paperSize", paperSize);
        order.put("printType", printType);
        order.put("printSide", printSide);
        order.put("copies", copies);
        order.put("estimatedPrice", estimatedPrice);

        order.put("documentUploaded", false);
        order.put("status", "AWAITING_DOCUMENT");
        order.put("createdAt", FieldValue.serverTimestamp());

        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(
                            this,
                            "Order request saved successfully",
                            Toast.LENGTH_LONG
                    ).show();

                    Intent intent = new Intent(
                            OrderReviewActivity.this,
                            HomeActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText("Place Order Request");

                    Toast.makeText(
                            this,
                            "Unable to place order: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
