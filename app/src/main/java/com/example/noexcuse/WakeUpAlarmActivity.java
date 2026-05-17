package com.example.noexcuse;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WakeUpAlarmActivity extends AppCompatActivity {

    public static final String EXTRA_QR_MODE   = "qrMode";
    public static final String EXTRA_WAKE_TIME = "wakeTime";

    private boolean qrMode;
    private String  wakeTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupLockScreenFlags();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wake_up_alarm);
        handleIntent(getIntent());
        setupUI();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        setupUI();
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            qrMode   = intent.getBooleanExtra(EXTRA_QR_MODE, false);
            wakeTime = intent.getStringExtra(EXTRA_WAKE_TIME);
            if (wakeTime == null) wakeTime = "--:--";
        }
    }

    private void setupUI() {
        TextView tvTime  = findViewById(R.id.tvAlarmTime);
        TextView tvLabel = findViewById(R.id.tvAlarmLabel);
        Button   btnStop = findViewById(R.id.btnStopAlarm);

        if (tvTime  != null) tvTime.setText(wakeTime);
        if (tvLabel != null) {
            tvLabel.setText(qrMode
                    ? "🚽 Go scan the QR code in the bathroom to stop the alarm!"
                    : "Good morning! Time to wake up 🌅");
        }

        if (btnStop != null) {
            // ✅ In QR mode: button label makes it clear
            btnStop.setText(qrMode ? "📷 Scan QR to Stop" : "STOP");

            btnStop.setOnClickListener(v -> {
                if (qrMode) {
                    // ✅ QR mode: open camera ONLY — sound keeps playing until QR scanned
                    openQrScanner();
                } else {
                    // Normal mode: stop sound + schedule "are you awake?" check
                    stopAlarmService();
                    scheduleConfirmationNotif();
                    finish();
                }
            });
        }
    }

    private void setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void stopAlarmService() {
        stopService(new Intent(this, AlarmService.class));
    }

    private void scheduleConfirmationNotif() {
        long triggerAt = System.currentTimeMillis() + (5 * 60 * 1000L);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmConfirmReceiver.class);
        intent.setAction(AlarmConfirmReceiver.ACTION_CONFIRM);

        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                AlarmConfirmReceiver.REQUEST_CODE_CONFIRM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }

    private void openQrScanner() {
        // ✅ Sound is NOT stopped here — AlarmService keeps running
        // Sound will only stop inside QrScanActivity.onQrSuccess()
        Intent intent = new Intent(this, QrScanActivity.class);
        startActivity(intent);
        // Do NOT call finish() here — keep this activity in stack
        // so user can't go back and bypass the QR
        finish();
    }

    @Override
    public void onBackPressed() {
        // Block back button — user must scan QR or tap STOP
    }
}