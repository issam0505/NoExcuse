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
import com.example.noexcuse.database.SleepDao;
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
    private View           cardWakeTime;
    private View           cardSleepTime;
    private View           viewStatusDot;
    private SwitchMaterial switchAlarm;
    private MaterialButton btnSave;
    private View           btnQrInfo;

    // Default values: Sleep 23:00, Wake 07:00
    private int     wakeHour    = 7;
    private int     wakeMinute  = 0;
    private int     sleepHour   = 23;
    private int     sleepMinute = 0;
    private boolean isAlarmEnabled = false;
    private boolean pendingCameraCheck = false;
    private boolean isLoadingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_sleep_alarm);

        tvWakeTime      = findViewById(R.id.tvWakeTime);
        tvSleepTime     = findViewById(R.id.tvSleepTime);
        tvAlarmStatus   = findViewById(R.id.tvAlarmStatus);
        tvSleepDuration = findViewById(R.id.tvSleepDuration);
        cardWakeTime    = findViewById(R.id.cardWakeTime);
        cardSleepTime   = findViewById(R.id.cardSleepTime);
        viewStatusDot   = findViewById(R.id.viewStatusDot);
        switchAlarm     = findViewById(R.id.switchAlarm);
        btnSave         = findViewById(R.id.btnSaveAlarm);
        btnQrInfo       = findViewById(R.id.btnQrInfo);

        // 1. Load data first to restore previous session or use defaults
        loadSavedSettings();

        tvWakeTime.setOnClickListener(v -> pickTime(true));
        tvSleepTime.setOnClickListener(v -> pickTime(false));
        if (cardWakeTime != null) cardWakeTime.setOnClickListener(v -> pickTime(true));
        if (cardSleepTime != null) cardSleepTime.setOnClickListener(v -> pickTime(false));
        btnSave.setOnClickListener(v -> saveSettings());

        if (btnQrInfo != null) {
            btnQrInfo.setOnClickListener(v -> showQrInfoDialog());
        }

        if (switchAlarm != null) {
            switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isLoadingData) return;
                isAlarmEnabled = isChecked;
                updateToggleUI();
                persistSelection(); // Save instantly
            });
        }

        requestOverlayPermissionIfNeeded();

        SharedPreferences prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_QR_INFO_SHOWN, false)) {
            prefs.edit().putBoolean(PREF_QR_INFO_SHOWN, true).apply();
            showQrInfoDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadSavedSettings();
    }

    private void updateToggleUI() {
        // Prevent recursive listener calls
        if (switchAlarm != null && switchAlarm.isChecked() != isAlarmEnabled) {
            isLoadingData = true;
            switchAlarm.setChecked(isAlarmEnabled);
            isLoadingData = false;
        }
        
        if (isAlarmEnabled) {
            tvAlarmStatus.setText("Alarm On");
            tvAlarmStatus.setTextColor(Color.parseColor("#4CAF50"));
            if (viewStatusDot != null) viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_on);
        } else {
            tvAlarmStatus.setText("Alarm Off");
            tvAlarmStatus.setTextColor(Color.parseColor("#888888"));
            if (viewStatusDot != null) viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot);
        }
        updateSleepDuration();
    }

    private void updateSleepDuration() {
        int sleepTotalMins = sleepHour * 60 + sleepMinute;
        int wakeTotalMins  = wakeHour  * 60 + wakeMinute;

        int diffMins = wakeTotalMins - sleepTotalMins;
        if (diffMins <= 0) diffMins += 24 * 60;

        int hours = diffMins / 60;
        int mins  = diffMins % 60;

        if (tvSleepDuration != null) {
            tvSleepDuration.setText(String.format(Locale.getDefault(), "%dh %02dm", hours, mins));
        }
    }

    /**
     * Saves the current UI selection to the database.
     */
    private void persistSelection() {
        if (isLoadingData) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            SleepDao dao = AppDatabase.getInstance(this).sleepDao();
            SleepSettings s = dao.getSleepSettings();
            if (s == null) {
                s = new SleepSettings();
                s.id = 1; // Dima record wahed
            }
            s.sleepTime = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute);
            s.wakeUpTime = String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute);
            s.isAlarmOn = isAlarmEnabled;
            s.isQRRequired = true;
            dao.insertSleepSettings(s);
        });
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
            persistSelection();
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

    private void applyAndSave(boolean alarmOn) {
        if (alarmOn) {
            WakeUpScheduler.schedule(this, wakeHour, wakeMinute);
            SleepReminderReceiver.schedule(this, sleepHour, sleepMinute);
            Toast.makeText(this, "⏰ Alarm scheduled!", Toast.LENGTH_SHORT).show();
        } else {
            WakeUpScheduler.cancel(this);
            SleepReminderReceiver.cancel(this);
            Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show();
        }
        persistSelection();
        finish();
    }

    private void loadSavedSettings() {
        isLoadingData = true;
        Executors.newSingleThreadExecutor().execute(() -> {
            SleepSettings saved = AppDatabase.getInstance(this).sleepDao().getSleepSettings();
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
                isLoadingData = false;
            });
        });
    }

    private void showQrInfoDialog() {
        int dp = (int) getResources().getDisplayMetrics().density;
        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(8 * dp, 4 * dp, 8 * dp, 4 * dp);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16 * dp, 8 * dp, 16 * dp, 8 * dp);
        TextView tv = new TextView(this);
        tv.setText("1. Take a screenshot of the QR code\n2. Print it & stick it in your bathroom\n3. Scan it to stop the alarm! 💪");
        tv.setTextSize(14f);
        layout.addView(tv);
        ImageView ivQr = new ImageView(this);
        ivQr.setImageResource(R.drawable.qr_code);
        int size = 220 * dp;
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(size, size);
        imgParams.gravity = Gravity.CENTER_HORIZONTAL;
        imgParams.topMargin = 24 * dp;
        ivQr.setLayoutParams(imgParams);
        layout.addView(ivQr);
        scrollView.addView(layout);
        new AlertDialog.Builder(this).setTitle("📱 QR Alarm Info").setView(scrollView).setPositiveButton("Got it!", null).show();
    }

    private void requestOverlayPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this).setTitle("Permission").setMessage("Allow overlay for alarm screen.").setPositiveButton("Settings", (d, w) -> {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }).show();
        }
    }

    private void showExactAlarmPermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingCameraCheck) { pendingCameraCheck = false; applyAndSave(true); }
        }
    }
}
