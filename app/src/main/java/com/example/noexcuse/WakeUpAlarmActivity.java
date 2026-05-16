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
    private String wakeTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ إعدادات القوة: الظهور فوق القفل واستدعاء الشاشة فوراً
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
            qrMode = intent.getBooleanExtra(EXTRA_QR_MODE, false);
            wakeTime = intent.getStringExtra(EXTRA_WAKE_TIME);
            if (wakeTime == null) wakeTime = "--:--";
        }
    }

    private void setupUI() {
        TextView tvTime  = findViewById(R.id.tvAlarmTime);
        TextView tvLabel = findViewById(R.id.tvAlarmLabel);
        Button   btnStop = findViewById(R.id.btnStopAlarm);

        if (tvTime != null) tvTime.setText(wakeTime);
        if (tvLabel != null) {
            tvLabel.setText(qrMode ? "Scan QR Code to Stop ⚠️" : "Good morning! Time to wake up 🌅");
        }

        btnStop.setOnClickListener(v -> {
            if (qrMode) {
                // ✅ في وضع QR، نفتح الكاميرا ولا نوقف الصوت هنا أبداً
                openQrScanner();
            } else {
                stopAlarmService();
                scheduleConfirmationNotif();
                finish();
            }
        });
    }

    private void setupLockScreenFlags() {
        // ✅ تفعيل الأعلام اللازمة لتخطي القفل وتشغيل الشاشة بقوة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void stopAlarmService() {
        stopService(new Intent(this, AlarmService.class));
    }

    private void scheduleConfirmationNotif() {
        // ✅ جدولة إشعار "هل أنت مستيقظ؟" بعد 5 دقائق من إيقاف المنبه الأول
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
        Intent intent = new Intent(this, QrScanActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // منع زر العودة لضمان عدم إغلاق الواجهة إلا عبر STOP أو QR
    }
}
