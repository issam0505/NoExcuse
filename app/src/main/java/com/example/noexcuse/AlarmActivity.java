package com.example.noexcuse;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    private static final int    CAMERA_REQ         = 201;
    private static final String PREF_FILE          = "noexcuse_prefs";
    private static final String PREF_QR_INFO_SHOWN = "qr_info_shown";

    private TextView       tvWakeTime;
    private TextView       tvSleepTime;
    private TextView       tvAlarmStatus;
    private TextView       tvSleepDuration;
    private SwitchMaterial switchAlarm;
    private MaterialButton btnSave;
    private View           btnQrInfo;

    private int     wakeHour    = 7;
    private int     wakeMinute  = 0;
    private int     sleepHour   = 23;
    private int     sleepMinute = 0;
    private boolean isAlarmEnabled = false;
    private boolean pendingCameraCheck = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sleep_alarm);

        tvWakeTime      = findViewById(R.id.tvWakeTime);
        tvSleepTime     = findViewById(R.id.tvSleepTime);
        tvAlarmStatus   = findViewById(R.id.tvAlarmStatus);
        tvSleepDuration = findViewById(R.id.tvSleepDuration);
        switchAlarm     = findViewById(R.id.switchAlarm);
        btnSave         = findViewById(R.id.btnSaveAlarm);
        btnQrInfo       = findViewById(R.id.btnQrInfo);

        loadSavedSettings();

        tvWakeTime.setOnClickListener(v -> pickTime(true));
        tvSleepTime.setOnClickListener(v -> pickTime(false));
        btnSave.setOnClickListener(v -> saveSettings());

        if (btnQrInfo != null) {
            btnQrInfo.setOnClickListener(v -> showQrInfoDialog());
        }

        isAlarmEnabled = WakeUpScheduler.isEnabled(this);
        updateToggleUI();

        if (switchAlarm != null) {
            switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isAlarmEnabled = isChecked;
                updateToggleUI();
            });
        }

        // Overlay permission
        requestOverlayPermissionIfNeeded();

        // First time opening → show QR info dialog automatically
        SharedPreferences prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_QR_INFO_SHOWN, false)) {
            prefs.edit().putBoolean(PREF_QR_INFO_SHOWN, true).apply();
            showQrInfoDialog();
        }
    }

    private void updateToggleUI() {
        if (switchAlarm != null && switchAlarm.isChecked() != isAlarmEnabled) {
            switchAlarm.setChecked(isAlarmEnabled);
        }
        
        if (isAlarmEnabled) {
            tvAlarmStatus.setText("ON");
            tvAlarmStatus.setTextColor(Color.parseColor("#4CAF50")); // Greenish
        } else {
            tvAlarmStatus.setText("OFF");
            tvAlarmStatus.setTextColor(Color.parseColor("#888888"));
        }
        updateSleepDuration();
    }

    private void updateSleepDuration() {
        int sleepTotalMins = sleepHour * 60 + sleepMinute;
        int wakeTotalMins  = wakeHour  * 60 + wakeMinute;

        int diffMins = wakeTotalMins - sleepTotalMins;
        if (diffMins <= 0) diffMins += 24 * 60; // overnight wrap

        int hours = diffMins / 60;
        int mins  = diffMins % 60;

        if (tvSleepDuration != null) {
            String duration = String.format(Locale.getDefault(), "%dh %02dm", hours, mins);
            tvSleepDuration.setText(duration);
        }
    }

    private void showQrInfoDialog() {
        int dp = (int) getResources().getDisplayMetrics().density;

        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(8 * dp, 4 * dp, 8 * dp, 4 * dp);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16 * dp, 8 * dp, 16 * dp, 8 * dp);

        TextView tv = new TextView(this);
        tv.setText(
                "1. Take a screenshot of the QR code below 📸\n\n"
                        + "2. Print it and stick it in your bathroom or toilet 🚽\n\n"
                        + "3. When your alarm fires, tap STOP to pause it.\n\n"
                        + "4. After 5 minutes, you'll get a notification: \"Are you awake?\"\n\n"
                        + "5. Tap YES to confirm — or the alarm will ring again!\n\n"
                        + "6. If you still don't respond, the alarm fires again — and this time "
                        + "you MUST go to the bathroom and scan the QR sticker to stop it.\n\n"
                        + "This guarantees you actually got out of bed! 💪"
        );
        tv.setTextSize(14f);
        tv.setLineSpacing(4f, 1f);
        layout.addView(tv);

        ImageView ivQr = new ImageView(this);
        ivQr.setImageResource(R.drawable.qr_code);

        int size = 220 * dp;
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(size, size);
        imgParams.gravity    = Gravity.CENTER_HORIZONTAL;
        imgParams.topMargin  = 24 * dp;
        imgParams.bottomMargin = 4 * dp;
        ivQr.setLayoutParams(imgParams);
        ivQr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layout.addView(ivQr);

        TextView caption = new TextView(this);
        caption.setText("📌 Screenshot this QR → Print it → Stick it in your bathroom");
        caption.setTextSize(12f);
        caption.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams capParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        capParams.topMargin    = 4 * dp;
        capParams.bottomMargin = 8 * dp;
        caption.setLayoutParams(capParams);
        layout.addView(caption);

        scrollView.addView(layout);

        new AlertDialog.Builder(this)
                .setTitle("📱 How does the QR alarm work?")
                .setView(scrollView)
                .setPositiveButton("Got it!", null)
                .show();
    }

    private void requestOverlayPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Permission Required")
                        .setMessage(
                                "To show the alarm screen instantly — even when your phone is locked "
                                        + "or the app is closed — please enable \"Display over other apps\" for NoExcuse.\n\n"
                                        + "Settings → Apps → NoExcuse → Display over other apps → Allow")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            startActivity(new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName())));
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
            updateSleepDuration();
        }, hour, minute, true).show();
    }

    private void saveSettings() {
        if (isAlarmEnabled) {
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

        applyAndSave(isAlarmEnabled);
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ Permission Required")
                .setMessage("To ensure your alarm rings on time (even when the phone is asleep), "
                        + "please allow 'Alarms & Reminders' in Settings.\n\n"
                        + "Settings → Apps → NoExcuse → Alarms & Reminders → Allow")
                .setPositiveButton("Open Settings", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        startActivity(new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())));
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
            s.id                = 1;
            s.sleepTime         = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute);
            s.wakeUpTime        = String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute);
            s.isAlarmOn         = alarmOn;
            s.isQRRequired      = true;
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
                    isAlarmEnabled = saved.isAlarmOn;
                } catch (Exception ignored) {}
            }
            runOnUiThread(() -> {
                tvWakeTime.setText(String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute));
                tvSleepTime.setText(String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute));
                updateToggleUI();
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && pendingCameraCheck) {
                pendingCameraCheck = false;
                applyAndSave(true);
            } else {
                Toast.makeText(this,
                        "Camera permission is required for QR scan alarm.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() { super.onResume(); }
}
