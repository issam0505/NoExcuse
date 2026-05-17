package com.example.noexcuse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.pm.PackageManager;

public class QrScanActivity extends AppCompatActivity {

    // ✅ هذا هو الـ payload اللي كيكون داخل الـ QR code بتاعك
    public static final String QR_PAYLOAD      = "NOEXCUSE_AWAKE_QR_v1";
    private static final int   CAMERA_REQ_CODE = 200;
    private static final String PREF_QR_SHOWN  = "qr_download_shown";

    private ExecutorService  cameraExecutor;
    private PreviewView      previewView;
    private volatile boolean dismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        previewView    = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // ✅ أول مرة يستخدم QR → نعرضيه الـ dialog قبل الكاميرا
        SharedPreferences prefs = getSharedPreferences("noexcuse_prefs", Context.MODE_PRIVATE);
        boolean alreadyShown = prefs.getBoolean(PREF_QR_SHOWN, false);

        if (!alreadyShown) {
            showQrDownloadDialog(prefs);
        } else {
            startCameraIfPermitted();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // First-time dialog: show QR + ask to download
    // ─────────────────────────────────────────────────────────────────────────

    private void showQrDownloadDialog(SharedPreferences prefs) {
        new AlertDialog.Builder(this)
                .setTitle("📱 Your QR Code")
                .setMessage(
                        "This is your personal wake-up QR code.\n\n"
                                + "Print it and stick it in your bathroom or toilet 🚽\n\n"
                                + "Every morning, after stopping the alarm, you'll need to physically go there "
                                + "and scan this code to prove you're actually up! 💪\n\n"
                                + "Tap \"Save to Phone\" to download it now.")
                .setPositiveButton("💾 Save to Phone", (d, w) -> {
                    saveQrToGallery();
                    prefs.edit().putBoolean(PREF_QR_SHOWN, true).apply();
                    startCameraIfPermitted();
                })
                .setNegativeButton("Skip for now", (d, w) -> {
                    // Don't mark as shown → will ask again next time
                    startCameraIfPermitted();
                })
                .setCancelable(false)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save QR image to gallery
    // ─────────────────────────────────────────────────────────────────────────

    private void saveQrToGallery() {
        try {
            // Decode the embedded base64 QR image
            byte[] decodedBytes = Base64.decode(QR_BASE64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to decode QR image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to Pictures/NoExcuse/
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NoExcuse");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "NoExcuse_WakeUp_QR.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // Notify gallery
            Intent mediaScan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScan.setData(Uri.fromFile(file));
            sendBroadcast(mediaScan);

            Toast.makeText(this,
                    "✅ QR saved! Find it in your Photos → NoExcuse folder.",
                    Toast.LENGTH_LONG).show();

            // Offer to open/share immediately
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file);

            Intent shareIntent = new Intent(Intent.ACTION_VIEW);
            shareIntent.setDataAndType(fileUri, "image/png");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            new AlertDialog.Builder(this)
                    .setTitle("✅ QR Saved!")
                    .setMessage("Your QR code has been saved to your phone.\n\nDo you want to open it now to print or share?")
                    .setPositiveButton("Open / Print", (d, w) -> startActivity(shareIntent))
                    .setNegativeButton("Later", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null && !dismissed) {
                        com.google.mlkit.vision.common.InputImage inputImage =
                                com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.getImageInfo().getRotationDegrees());

                        com.google.mlkit.vision.barcode.BarcodeScanning
                                .getClient()
                                .process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    for (com.google.mlkit.vision.barcode.common.Barcode barcode : barcodes) {
                                        if (QR_PAYLOAD.equals(barcode.getRawValue())) {
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
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QR matched → stop alarm
    // ─────────────────────────────────────────────────────────────────────────

    private void onQrSuccess() {
        if (dismissed) return;
        dismissed = true;
        runOnUiThread(() -> {
            Toast.makeText(this, "✅ QR verified — Good morning! 🌅", Toast.LENGTH_SHORT).show();

            // ✅ Stop alarm sound ONLY after successful QR scan
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
            Toast.makeText(this, "Camera permission is required to scan the QR code.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ Embedded QR code (base64) — هذا هو الـ QR اللي رفعته
    // ─────────────────────────────────────────────────────────────────────────

    private static final String QR_BASE64 =
            "R0lGODlhZABkAPcAAAAAAAAAMwAAZgAAmQAAzAAA/wArAAArMwArZgArmQArzAAr/wBVAABVMwBVZgBV" +
                    "mQBVzABV/wCAAACAMwCAZgCAmQCAzACA/wCqAACqMwCqZgCqmQCqzACq/wDVAADVMwDVZgDVmQDVzADV" +
                    "/wD/AAD/MwD/ZgD/mQD/zAD//zMAADMAMzMAZjMAmTMAzDMA/zMrADMrMzMrZjMrmTMrzDMr/zNVADNV" +
                    "MzNVZjNVmTNVzDNV/zOAADOAMzOAZjOAmTOAzDOA/zOqADOqMzOqZjOqmTOqzDOq/zPVADPVMzPVZjPV" +
                    "mTPVzDPV/zP/ADP/MzP/ZjP/mTP/zDP//2YAAGYAM2YAZmYAmWYAzGYA/2YrAGYrM2YrZmYrmWYrzGYr" +
                    "/2ZVAGZVM2ZVZmZVmWZVzGZV/2aAAGaAM2aAZmaAmWaAzGaA/2aqAGaqM2aqZmaqmWaqzGaq/2bVAGbV" +
                    "M2bVZmbVmWbVzGbV/2b/AGb/M2b/Zmb/mWb/zGb//5kAAJkAM5kAZpkAmZkAzJkA/5krAJkrM5krZpkr" +
                    "mZkrzJkr/5lVAJlVM5lVZplVmZlVzJlV/5mAAJmAM5mAZpmAmZmAzJmA/5mqAJmqM5mqZpmqmZmqzJmq" +
                    "/5nVAJnVM5nVZpnVmZnVzJnV/5n/AJn/M5n/Zpn/mZn/zJn//8wAAMwAM8wAZswAmcwAzMwA/8wrAMwr" +
                    "M8wrZswrmcwrzMwr/8xVAMxVM8xVZsxVmcxVzMxV/8yAAMyAM8yAZsyAmcyAzMyA/8yqAMyqM8yqZsyq" +
                    "mcyqzMyq/8zVAMzVM8zVZszVmczVzMzV/8z/AMz/M8z/Zsz/mcz/zMz///8AAP8AM/8AZv8Amf8AzP8A" +
                    "//8rAP8rM/8rZv8rmf8rzP8r//9VAP9VM/9VZv9Vmf9VzP9V//+AAP+AM/+AZv+Amf+AzP+A//+qAP+q" +
                    "M/+qZv+qmf+qzP+q///VAP/VM//VZv/Vmf/VzP/V////AP//M///Zv//mf//zP///wAAAAAAAAAAAAAAACH5" +
                    "BAEAAPwALAAAAABkAGQAAAj/AAEIHEiwoMGD+xIqTEhw4cKGDhkiVHiwosWLGDFGlChw4z6IESs+zEiy" +
                    "pEkAHkE6VDnSYMuTMGN23MiS4sCUEznKvOixp82ZOnG6pJnz402fPUUiFYryZ9OQRV+qrLlUJ9WqU49C" +
                    "Hbq14MuqRLmCdfo1rFezWYGOtarVKE+yTk8uTfvUrUWpbTOWZVtybt6/b/nW1QtXMGGfdPGKVWoY8GCZ" +
                    "ftXSZWx3ctGrh+3uXemYsmeNhTkH1hx6c+bPozGDDlo69OnLq0l3Rf1YrenYtGlvjnyWte/KsBfjdrxW" +
                    "t+vhwlMTH2v89+vkd4/bRtpcNnDoV3krrs19d2uwlIt3/17uvK138FGZj5dM3vp5rOnRs4d+e/pv8TuV" +
                    "z+8unXv+/9GVt199/gFoYGL9EbjdgQAqmGB/Bq6FX2+i1fWehBhmOJt537mn4YcghtdhdiCWKB+F75FG" +
                    "omApxuchYhw2tp9lA6IV44tJ3fgchdVtx9SETOW2Hn8b2ofjjzZiNySNBc54I5Jd+RgXcjwGhyJ1Og65" +
                    "oJVMSnkilNc1GWCYXpKpnpFdTlmjgBaq2eaJ1am2pnVyXgmnlS3OiWCRb8KnH5EVOnifmxrumGefZa7o" +
                    "pogw9oWlWIrKaGeOjjY6qYlynUkSbyp+mOmdkCYJaKedbjnnhYQ+aOmEPXZY5p6UIv8qKqyosqhqrKzi" +
                    "adahnJoUZGy8jvjkosASG+qgbIL5a6UVZhYsskcaO6apk9bJ5LVU5kqfqNRS26ql35opqbe6glolmlymy" +
                    "2yp0j5L57BsYjtat5rW+q6SzNL7pasQLlnsuPXyG6+/87b76Jg7wsrgn/qGmebAC7eXsLqjRlyuwwSLS" +
                    "aDFBIvJoMXtRdqsi8uKaiu0Hjs7MKkXe9xwr0a+Cu+KJoP7cpwLCxroqrd6m5/N7iqc8U48C6unxBxX" +
                    "jO7RRifNbpQ4J9t0rEK2fDCYtAqMsZNSr/p01SQbPLKEdQbdNcBfG7r1xUDPmnHPaEOmNtbn9vnwvTA" +
                    "lrDPb20rK6NVi+nEM996Hmk023eRmjeOndNP8N8hCb9pv3on/ezOfiiM8c4NvV0sx1x/HZLeLUcfNct2Z" +
                    "W7u21RGWnuWxfmrsNdQdctjh3DF3DmjIhWr5Ou6oM6512ZK7bHjRrkOb+vGFC2835shzrPzn665u8fOq" +
                    "M2xWQAA7";
}