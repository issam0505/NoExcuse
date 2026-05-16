package com.example.noexcuse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "CRITICAL_ALARM_FORCE_V8";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String wakeTime = intent.getStringExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME);
        boolean qrMode = intent.getBooleanExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, false);

        // ✅ 1. تحضير واجهة المنبه (STOP Screen)
        Intent activityIntent = new Intent(this, WakeUpAlarmActivity.class);
        activityIntent.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, wakeTime);
        activityIntent.putExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, qrMode);
        
        // فلاتر قوية جداً لجلب التطبيق للمقدمة حتى لو كان مغلقاً أو في الخلفية
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        // ✅ 2. المحاولة "الانتحارية" لتشغيل النشاط فوراً (تنجح إذا تم منح صلاحية Draw Over Apps)
        try {
            startActivity(activityIntent);
            Log.d(TAG, "Activity launch requested successfully");
        } catch (Exception e) {
            Log.e(TAG, "Direct launch failed: " + e.getMessage());
        }

        PendingIntent pi = PendingIntent.getActivity(this, 2000,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ✅ 3. إنشاء إشعار "درع" غير قابل للمسح أبداً (Ongoing + No Clear)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🚨 ALARM IS ACTIVE")
                .setContentText("Tap to return to stop screen")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true) 
                .setOngoing(true)       // ✅ يمنع المسح باليد (Swipe)
                .setAutoCancel(false)    // يمنع الاختفاء التلقائي
                .setSilent(true)        // صامت لأننا نتحكم بالصوت يدوياً
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        // التوافق مع أندرويد 14
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(7002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(7002, notification);
        }

        startAlarmMedia();
        return START_STICKY;
    }

    private void startAlarmMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] pattern = {0, 1000, 500, 1000};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Media error: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Critical Emergency Alarm", NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) vibrator.cancel();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
