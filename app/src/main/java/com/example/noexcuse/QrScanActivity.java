package com.example.noexcuse;

import android.Manifest;
import android.content.pm.PackageManager;
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

/**
 * QrScanActivity
 * ──────────────────────────────────────────────────────────────────
 * Opens the camera and scans for the special app QR code.
 * If the correct QR is found → finish (alarm dismissed ✅).
 * If camera permission is denied → go back to MainActivity.
 *
 * The QR payload must match: "NOEXCUSE_AWAKE_QR_v1"
 * (This is what the app generates when showing the QR sticker setup screen)
 *
 * Uses CameraX + ML Kit Barcode Scanning.
 * Add these to build.gradle:
 *   implementation "androidx.camera:camera-camera2:1.3.1"
 *   implementation "androidx.camera:camera-lifecycle:1.3.1"
 *   implementation "androidx.camera:camera-view:1.3.1"
 *   implementation "com.google.mlkit:barcode-scanning:17.2.0"
 */
public class QrScanActivity extends AppCompatActivity {

    public static final String QR_PAYLOAD       = "NOEXCUSE_AWAKE_QR_v1";
    private static final int   CAMERA_REQ_CODE  = 200;

    private ExecutorService cameraExecutor;
    private PreviewView     previewView;
    private volatile boolean dismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        previewView   = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

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
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    // ML Kit barcode scanning
                    @SuppressWarnings("UnnecessaryLocalVariable")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null && !dismissed) {
                        com.google.mlkit.vision.common.InputImage inputImage =
                                com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                        mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        com.google.mlkit.vision.barcode.BarcodeScanning
                                .getClient()
                                .process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    for (com.google.mlkit.vision.barcode.common.Barcode barcode : barcodes) {
                                        String value = barcode.getRawValue();
                                        if (QR_PAYLOAD.equals(value)) {
                                            onQrSuccess();
                                        }
                                    }
                                })
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                provider.unbindAll();
                provider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void onQrSuccess() {
        if (dismissed) return;
        dismissed = true;
        runOnUiThread(() -> {
            Toast.makeText(this, "✅ QR verified — Good morning!", Toast.LENGTH_SHORT).show();
            // Stop alarm confirmation loop — user is truly awake
            sendBroadcast(new android.content.Intent(this, AlarmConfirmReceiver.class)
                    .setAction(AlarmConfirmReceiver.ACTION_YES));
            finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Camera permission needed to scan QR. Going back to main screen.",
                        Toast.LENGTH_LONG).show();
                // Go to MainActivity (which will re-ask permission next alarm interaction)
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}