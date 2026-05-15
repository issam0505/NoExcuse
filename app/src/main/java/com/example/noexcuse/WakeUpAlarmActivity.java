package com.example.noexcuse;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.noexcuse.R;

/**
 * WakeUpAlarmActivity
 * ──────────────────────────────────────────────────────────────────
 * Flow:
 *  1. Screen fires → alarm sound + vibration
 *  2. User taps STOP → sound stops, activity finishes
 *  3. 5 minutes later → AlarmConfirmReceiver fires a "Are you awake?" notification
 *  4a. User taps YES  → done ✅
 *  4b. No response (5 min) → AlarmConfirmReceiver fires QR alarm (via WakeUpAlarmReceiver with qrMode=true)
 *  4c. User still ignores  → QR scan required (WakeUpAlarmActivity launched again with qrMode=true)
 */
public class WakeUpAlarmActivity extends AppCompatActivity {

    public static final String EXTRA_QR_MODE   = "qrMode";
    public static final String EXTRA_WAKE_TIME = "wakeTime"; // display string "07:00"

    private MediaPlayer  mediaPlayer;
    private Vibrator     vibrator;
    private Handler      handler = new Handler(Looper.getMainLooper());
    private boolean      qrMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_wake_up_alarm);

        qrMode = getIntent().getBooleanExtra(EXTRA_QR_MODE, false);
        String wakeTime = getIntent().getStringExtra(EXTRA_WAKE_TIME);

        TextView tvTime  = findViewById(R.id.tvAlarmTime);
        TextView tvLabel = findViewById(R.id.tvAlarmLabel);
        Button   btnStop = findViewById(R.id.btnStopAlarm);

        if (tvTime != null && wakeTime != null) tvTime.setText(wakeTime);
        if (tvLabel != null) {
            tvLabel.setText(qrMode
                    ? "Scan the QR code to dismiss ⚠️"
                    : "Good morning! Time to wake up 🌅");
        }

        // Start alarm
        startAlarmSound();
        startVibration();

        btnStop.setOnClickListener(v -> {
            if (qrMode) {
                // Must scan QR — launch scanner
                openQrScanner();
            } else {
                // Normal stop → schedule confirmation notification in 5 min
                stopAlarm();
                scheduleConfirmationNotif();
                finish();
            }
        });
    }

    private void startAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;
        long[] pattern = {0, 500, 300, 500, 300};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    /** Schedule an "Are you awake?" notification 5 minutes after STOP is tapped */
    private void scheduleConfirmationNotif() {
        long triggerAt = System.currentTimeMillis() + (5 * 60 * 1000L); // +5 min
        android.app.AlarmManager am =
                (android.app.AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmConfirmReceiver.class);
        intent.setAction(AlarmConfirmReceiver.ACTION_CONFIRM);

        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                this,
                AlarmConfirmReceiver.REQUEST_CODE_CONFIRM,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                        android.app.PendingIntent.FLAG_IMMUTABLE);

        android.app.AlarmManager.AlarmClockInfo clockInfo =
                new android.app.AlarmManager.AlarmClockInfo(triggerAt, pi);
        am.setAlarmClock(clockInfo, pi);
    }

    /** Launch the QR scanner activity */
    private void openQrScanner() {
        stopAlarm();
        Intent intent = new Intent(this, QrScanActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
        handler.removeCallbacksAndMessages(null);
    }
}