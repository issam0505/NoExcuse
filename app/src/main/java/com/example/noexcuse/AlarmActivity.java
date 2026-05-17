package com.example.noexcuse;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.example.noexcuse.database.AppDatabase;
import com.example.noexcuse.database.SleepSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AlarmActivity extends AppCompatActivity {

    private static final int    CAMERA_REQ     = 201;
    private static final String PREF_QR_SHOWN  = "qr_download_shown";

    private TextView       tvWakeTime;
    private TextView       tvSleepTime;
    private SwitchMaterial swAlarm;
    private MaterialButton btnSave;
    private MaterialButton btnQrInfo;

    private int     wakeHour    = 7;
    private int     wakeMinute  = 0;
    private int     sleepHour   = 23;
    private int     sleepMinute = 0;
    private boolean pendingCameraCheck = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sleep_alarm);

        tvWakeTime  = findViewById(R.id.tvWakeTime);
        tvSleepTime = findViewById(R.id.tvSleepTime);
        swAlarm     = findViewById(R.id.swAlarmEnabled);
        btnSave     = findViewById(R.id.btnSaveAlarm);
        btnQrInfo   = findViewById(R.id.btnQrInfo);

        loadSavedSettings();

        tvWakeTime.setOnClickListener(v -> pickTime(true));
        tvSleepTime.setOnClickListener(v -> pickTime(false));
        btnSave.setOnClickListener(v -> saveSettings());

        if (btnQrInfo != null) {
            btnQrInfo.setOnClickListener(v -> showQrInfoDialog());
        }

        swAlarm.setChecked(WakeUpScheduler.isEnabled(this));

        // ✅ Request overlay permission
        requestOverlayPermissionIfNeeded();

        // ✅ First time opening this page → show QR download dialog
        SharedPreferences prefs = getSharedPreferences("noexcuse_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_QR_SHOWN, false)) {
            showQrDownloadDialog(prefs);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ First-time QR download dialog
    // ─────────────────────────────────────────────────────────────────────────

    private void showQrDownloadDialog(SharedPreferences prefs) {
        new AlertDialog.Builder(this)
                .setTitle("📱 Your Wake-Up QR Code")
                .setMessage(
                        "To make sure you actually get out of bed, NoExcuse uses a QR code system.\n\n"
                                + "Here's how it works:\n"
                                + "1. Save the QR code to your phone.\n"
                                + "2. Print it and stick the sticker in your bathroom or toilet 🚽\n"
                                + "3. Every morning, when the alarm rings, you'll need to physically go there and scan it to stop the alarm.\n\n"
                                + "This guarantees you actually got out of bed! 💪\n\n"
                                + "Tap \"Save QR Code\" to download it now.")
                .setPositiveButton("💾 Save QR Code", (d, w) -> {
                    prefs.edit().putBoolean(PREF_QR_SHOWN, true).apply();
                    saveQrToPhone();
                })
                .setNegativeButton("Later", (d, w) -> {
                    // Don't mark shown — will remind again next time
                })
                .setCancelable(false)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ Save QR image to phone storage
    // ─────────────────────────────────────────────────────────────────────────

    private void saveQrToPhone() {
        try {
            byte[]  bytes  = Base64.decode(QR_BASE64, Base64.DEFAULT);
            Bitmap  bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to decode QR image.", Toast.LENGTH_SHORT).show();
                return;
            }

            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NoExcuse");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "NoExcuse_WakeUp_QR.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // Scan into gallery
            Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scan.setData(Uri.fromFile(file));
            sendBroadcast(scan);

            // Offer to open/share immediately
            Uri fileUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", file);

            new AlertDialog.Builder(this)
                    .setTitle("✅ QR Code Saved!")
                    .setMessage(
                            "Your QR code has been saved to your phone 📁\n\n"
                                    + "Print it out and stick it in your bathroom or toilet — "
                                    + "so every morning you'll have to go there to scan it and stop the alarm! 🚽💪\n\n"
                                    + "Want to open it now to print or share?")
                    .setPositiveButton("📤 Open / Print", (d, w) -> {
                        Intent open = new Intent(Intent.ACTION_VIEW);
                        open.setDataAndType(fileUri, "image/png");
                        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(open);
                    })
                    .setNegativeButton("Later", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overlay permission
    // ─────────────────────────────────────────────────────────────────────────

    private void requestOverlayPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Permission Required")
                        .setMessage(
                                "To show the alarm screen instantly — even when your phone is locked or the app is closed — "
                                        + "please enable \"Display over other apps\" for NoExcuse.\n\n"
                                        + "How to enable:\n"
                                        + "Settings → Apps → NoExcuse → Display over other apps → Allow")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            Intent i = new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Time picker
    // ─────────────────────────────────────────────────────────────────────────

    private void pickTime(boolean isWakeUp) {
        int hour   = isWakeUp ? wakeHour   : sleepHour;
        int minute = isWakeUp ? wakeMinute : sleepMinute;

        new TimePickerDialog(this, (tp, h, m) -> {
            if (isWakeUp) {
                wakeHour   = h;
                wakeMinute = m;
                tvWakeTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            } else {
                sleepHour   = h;
                sleepMinute = m;
                tvSleepTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            }
        }, hour, minute, true).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save settings
    // ─────────────────────────────────────────────────────────────────────────

    private void saveSettings() {
        boolean alarmOn = swAlarm.isChecked();

        if (alarmOn) {
            if (WakeUpScheduler.needsExactAlarmPermission(this)) {
                showExactAlarmPermissionDialog();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingCameraCheck = true;
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQ);
                return;
            }
        }

        applyAndSave(alarmOn);
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ Permission Required")
                .setMessage("To ensure your alarm rings on time (even when the phone is asleep), "
                        + "please allow 'Alarms & Reminders' in Settings.\n\n"
                        + "Settings → Apps → NoExcuse → Alarms & Reminders → Allow")
                .setPositiveButton("Open Settings", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyAndSave(boolean alarmOn) {
        if (alarmOn) {
            WakeUpScheduler.schedule(this, wakeHour, wakeMinute);
            Toast.makeText(this,
                    String.format(Locale.getDefault(),
                            "⏰ Alarm set for %02d:%02d", wakeHour, wakeMinute),
                    Toast.LENGTH_SHORT).show();
            SleepReminderReceiver.schedule(this, sleepHour, sleepMinute);
        } else {
            WakeUpScheduler.cancel(this);
            SleepReminderReceiver.cancel(this);
            Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show();
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            SleepSettings s = new SleepSettings();
            s.sleepTime         = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute);
            s.wakeUpTime        = String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute);
            s.isAlarmOn         = alarmOn;
            s.isQRRequired      = true;
            s.lastSleepDuration = 0;
            AppDatabase.getInstance(this).sleepDao().insertSleepSettings(s);
        });

        finish();
    }

    private void loadSavedSettings() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SleepSettings saved = AppDatabase.getInstance(this)
                    .sleepDao().getSleepSettings();

            if (saved != null) {
                try {
                    String[] wake  = saved.wakeUpTime.split(":");
                    String[] sleep = saved.sleepTime.split(":");
                    wakeHour    = Integer.parseInt(wake[0]);
                    wakeMinute  = Integer.parseInt(wake[1]);
                    sleepHour   = Integer.parseInt(sleep[0]);
                    sleepMinute = Integer.parseInt(sleep[1]);
                } catch (Exception ignored) {}
            }

            runOnUiThread(() -> {
                tvWakeTime.setText(String.format(Locale.getDefault(),
                        "%02d:%02d", wakeHour, wakeMinute));
                tvSleepTime.setText(String.format(Locale.getDefault(),
                        "%02d:%02d", sleepHour, sleepMinute));
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ Updated QR info dialog — includes sticker instruction
    // ─────────────────────────────────────────────────────────────────────────

    private void showQrInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📱 How does the QR alarm work?")
                .setMessage(
                        "1. Save the QR code to your phone (you were asked on first open).\n\n"
                                + "2. Print it out and stick it as a sticker in your bathroom or toilet 🚽\n\n"
                                + "3. When your alarm fires, tap STOP to pause it.\n\n"
                                + "4. After 5 minutes, you'll get a notification: \"Are you awake?\"\n\n"
                                + "5. Tap YES to confirm — or the alarm will ring again!\n\n"
                                + "6. If you still don't respond, the alarm fires again — and this time you MUST "
                                + "physically go to the bathroom and scan the QR sticker to dismiss it.\n\n"
                                + "This guarantees you actually got out of bed! 💪")
                .setPositiveButton("Got it!", null)
                .setNeutralButton("💾 Save QR Again", (d, w) -> saveQrToPhone())
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions result
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                if (pendingCameraCheck) {
                    pendingCameraCheck = false;
                    applyAndSave(true);
                }
            } else {
                Toast.makeText(this,
                        "Camera permission is required for QR scan alarm. Please enable it in Settings.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ Embedded QR code (same as QrScanActivity)
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
                    "omApxuchYhw2tp9lA6IV44tJ3fgchdVtx9FOTOW2Hn8b2ofjjzZiNySNBc54I5Jd+RgXcjwGhyJ1Og65" +
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