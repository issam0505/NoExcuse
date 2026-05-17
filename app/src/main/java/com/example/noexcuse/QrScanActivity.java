package com.example.noexcuse;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.pm.PackageManager;
import android.util.Log;

public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "QrScanActivity";

    // ✅ FIXED: must match the actual content of your QR code
    public static final String QR_PAYLOAD     = "ALARME_NOEXCUSE_001";
    private static final int   CAMERA_REQ_CODE = 200;

    private ExecutorService  cameraExecutor;
    private PreviewView      previewView;
    private volatile boolean dismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        previewView    = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        startCameraIfPermitted();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera
    // ─────────────────────────────────────────────────────────────────────────

    private void startCameraIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQ_CODE);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (dismissed) {
                        imageProxy.close();
                        return;
                    }

                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage == null) {
                        imageProxy.close();
                        return;
                    }

                    com.google.mlkit.vision.common.InputImage inputImage =
                            com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.getImageInfo().getRotationDegrees());

                    // ✅ Use BarcodeScannerOptions to specifically look for QR codes
                    com.google.mlkit.vision.barcode.BarcodeScannerOptions options =
                            new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(
                                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                                    .build();

                    com.google.mlkit.vision.barcode.BarcodeScanning
                            .getClient(options)
                            .process(inputImage)
                            .addOnSuccessListener(barcodes -> {
                                for (com.google.mlkit.vision.barcode.common.Barcode barcode : barcodes) {
                                    String raw = barcode.getRawValue();
                                    Log.d(TAG, "Scanned: " + raw);
                                    if (QR_PAYLOAD.equals(raw)) {
                                        onQrSuccess();
                                        return;
                                    }
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Scan error: " + e.getMessage()))
                            .addOnCompleteListener(task -> imageProxy.close());

                });

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera start error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ QR matched → stop alarm
    // ─────────────────────────────────────────────────────────────────────────

    private void onQrSuccess() {
        if (dismissed) return;
        dismissed = true;

        runOnUiThread(() -> {
            Toast.makeText(this, "✅ QR verified — Good morning! 🌅", Toast.LENGTH_SHORT).show();

            // Stop alarm sound
            stopService(new Intent(this, AlarmService.class));

            // Cancel any pending confirmation alarms
            sendBroadcast(new Intent(this, AlarmConfirmReceiver.class)
                    .setAction(AlarmConfirmReceiver.ACTION_YES));

            finish();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this,
                    "Camera permission is required to scan the QR code.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Block back — user must scan QR to stop alarm
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}