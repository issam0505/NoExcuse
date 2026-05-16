package com.example.noexcuse;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.example.noexcuse.database.AppDatabase;
import com.example.noexcuse.database.SleepSettings;

import java.util.Locale;
import java.util.concurrent.Executors;

public class AlarmActivity extends AppCompatActivity {

    private static final int CAMERA_REQ = 201;

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

        // ✅ Request "Display over other apps" permission — required to show alarm screen above lock
        requestOverlayPermissionIfNeeded();
    }

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

            // ✅ Schedule sleep reminder notification
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

    private void showQrInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📱 How does the QR alarm work?")
                .setMessage("1. The app provides a QR code sticker — print it and stick it in your bathroom or toilet.\n\n"
                        + "2. When your alarm fires, tap STOP to pause it.\n\n"
                        + "3. After 5 minutes, you'll get a notification: \"Are you awake?\"\n\n"
                        + "4. Tap YES to confirm — or the alarm will ring again!\n\n"
                        + "5. If you still don't respond, the alarm fires again — and this time you MUST scan the QR code in the bathroom to dismiss it.\n\n"
                        + "This guarantees you actually got out of bed! 💪")
                .setPositiveButton("Got it!", null)
                .show();
    }

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
}