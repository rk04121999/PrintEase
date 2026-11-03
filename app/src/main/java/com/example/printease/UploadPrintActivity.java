
package com.example.printease;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.DecimalFormat;

public class UploadPrintActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;

    private TextView tvFileName, tvPageCount, tvEstimatedPrice;
    private Spinner spPaperSize, spPrintType, spPrintSide;
    private EditText etCopies;
    private Button btnSelectPdf, btnCalculate, btnDecrease, btnIncrease;

    private Uri selectedPdfUri;
    private int pageCount = 0;
    private double estimatedPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_print);

        tvFileName = findViewById(R.id.tvFileName);
        tvPageCount = findViewById(R.id.tvPageCount);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);

        spPaperSize = findViewById(R.id.spPaperSize);
        spPrintType = findViewById(R.id.spPrintType);
        spPrintSide = findViewById(R.id.spPrintSide);

        etCopies = findViewById(R.id.etCopies);

        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);

        setupSpinners();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        btnSelectPdf.setOnClickListener(v -> selectPdf());
        btnDecrease.setOnClickListener(v -> changeCopies(-1));
        btnIncrease.setOnClickListener(v -> changeCopies(1));
        btnCalculate.setOnClickListener(v -> calculatePrice());

        findViewById(R.id.btnContinueToShops).setOnClickListener(v -> {
            if (!calculatePrice()) {
                return;
            }

            Intent intent = new Intent(
                    UploadPrintActivity.this,
                    ShopListActivity.class
            );

            intent.putExtra("pdfName", getFileName(selectedPdfUri));
            intent.putExtra("pdfUri", selectedPdfUri.toString());
            intent.putExtra("pageCount", pageCount);
            intent.putExtra("paperSize", spPaperSize.getSelectedItem().toString());
            intent.putExtra("printType", spPrintType.getSelectedItem().toString());
            intent.putExtra("printSide", spPrintSide.getSelectedItem().toString());
            intent.putExtra("copies", getCopies());
            intent.putExtra("estimatedPrice", estimatedPrice);

            startActivity(intent);
        });
    }

    private void setupSpinners() {
        String[] paperSizes = {"A4", "A3", "Letter"};
        String[] printTypes = {"Black & White", "Color"};
        String[] printSides = {"Single-sided", "Double-sided"};

        setSpinnerAdapter(spPaperSize, paperSizes);
        setSpinnerAdapter(spPrintType, printTypes);
        setSpinnerAdapter(spPrintSide, printSides);
    }

    private void setSpinnerAdapter(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void selectPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_PDF);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();

            if (uri == null) {
                Toast.makeText(this, "Unable to select PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedPdfUri = uri;

            try {
                int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;

                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (SecurityException ignored) {
            }

            readPdfDetails(uri);
        }
    }

    private void readPdfDetails(Uri uri) {
        try {
            ParcelFileDescriptor fileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");

            if (fileDescriptor == null) {
                Toast.makeText(this, "Unable to open PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            try (ParcelFileDescriptor descriptor = fileDescriptor;
                 PdfRenderer renderer = new PdfRenderer(descriptor)) {

                pageCount = renderer.getPageCount();

                tvFileName.setText(getFileName(uri));
                tvPageCount.setText(pageCount + " pages");
                tvEstimatedPrice.setText("₹0.00");
                estimatedPrice = 0;
            }

            Toast.makeText(
                    this,
                    "PDF selected successfully",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (IOException | SecurityException e) {
            selectedPdfUri = null;
            pageCount = 0;
            estimatedPrice = 0;

            tvFileName.setText("Unable to read PDF");
            tvPageCount.setText("0 pages");

            Toast.makeText(
                    this,
                    "Unable to read this PDF file",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "Selected PDF";

        try (Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                if (index >= 0) {
                    String name = cursor.getString(index);

                    if (name != null && !name.isEmpty()) {
                        fileName = name;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return fileName;
    }

    private void changeCopies(int change) {
        int copies = getCopies() + change;
        copies = Math.max(1, Math.min(copies, 100));

        etCopies.setText(String.valueOf(copies));
        estimatedPrice = 0;
        tvEstimatedPrice.setText("₹0.00");
    }

    private int getCopies() {
        String value = etCopies.getText().toString().trim();

        if (value.isEmpty()) {
            return 1;
        }

        try {
            int copies = Integer.parseInt(value);
            return Math.max(1, Math.min(copies, 100));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private boolean calculatePrice() {
        if (selectedPdfUri == null || pageCount == 0) {
            Toast.makeText(
                    this,
                    "Please select a PDF first",
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        int copies = getCopies();
        etCopies.setText(String.valueOf(copies));

        String paperSize = spPaperSize.getSelectedItem().toString();
        String printType = spPrintType.getSelectedItem().toString();

        double rate;

        if (paperSize.equals("A3")) {
            rate = printType.equals("Color") ? 20.0 : 4.0;
        } else {
            rate = printType.equals("Color") ? 10.0 : 2.0;
        }

        estimatedPrice = pageCount * rate * copies;

        DecimalFormat format = new DecimalFormat("0.00");
        tvEstimatedPrice.setText("₹" + format.format(estimatedPrice));

        return true;
    }
}
