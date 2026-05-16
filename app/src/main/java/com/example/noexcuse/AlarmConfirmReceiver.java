package com.example.noexcuse;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * AlarmConfirmReceiver
 * ──────────────────────────────────────────────────────────────────
 * هذا المستقبل يعالج مرحلتين:
 * 1. ACTION_CONFIRM: يظهر إشعار "هل أنت مستيقظ" ويجدول منبه الـ QR بعد 5 دقائق.
 * 2. ACTION_QR_ALARM: يشغل منبه الـ QR إذا لم يضغط المستخدم على YES.
 */
public class AlarmConfirmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmConfirmReceiver";
    public static final String ACTION_CONFIRM = "com.example.noexcuse.ACTION_CONFIRM_AWAKE";
    public static final String ACTION_YES     = "com.example.noexcuse.ACTION_YES_AWAKE";
    public static final String ACTION_QR_ALARM= "com.example.noexcuse.ACTION_QR_ALARM";

    public static final int REQUEST_CODE_CONFIRM = 9001;
    public static final int REQUEST_CODE_YES     = 9002;
    public static final int REQUEST_CODE_QR      = 9003;

    static final String CH_CONFIRM = "ch_alarm_confirm";
    static final int    NOTIF_ID   = 8888;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "onReceive: " + action);

        switch (action) {
            case ACTION_CONFIRM:
                // هذه المرحلة تأتي بعد 5 دقائق من ضغط STOP
                showConfirmNotification(ctx);
                // جدولة منبه الـ QR بعد 5 دقائق إضافية من الآن
                scheduleQrFallback(ctx);
                break;

            case ACTION_YES:
                // المستخدم أكد استيقاظه - نلغي كل شيء
                cancelQrFallback(ctx);
                dismissNotification(ctx);
                break;

            case ACTION_QR_ALARM:
                // مرت 5 دقائق على الإشعار ولم يتم ضغط YES - تشغيل المنبه الصاخب مع QR
                dismissNotification(ctx);
                launchAlarmServiceWithQr(ctx);
                break;
        }
    }

    private void showConfirmNotification(Context ctx) {
        createChannel(ctx);

        Intent yesIntent = new Intent(ctx, AlarmConfirmReceiver.class);
        yesIntent.setAction(ACTION_YES);
        PendingIntent yesPi = PendingIntent.getBroadcast(
                ctx, REQUEST_CODE_YES, yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CH_CONFIRM)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("⏰ Are you awake?")
                        .setContentText("Tap YES to confirm you're up — or the alarm will ring again in 5 mins!")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .addAction(R.mipmap.ic_launcher, "✅  YES, I'M UP!", yesPi);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, builder.build());
    }

    private void scheduleQrFallback(Context ctx) {
        // الانتظار 5 دقائق أخرى قبل تفعيل الـ QR Alarm
        long triggerAt = System.currentTimeMillis() + (5 * 60 * 1000L);

        Intent intent = new Intent(ctx, AlarmConfirmReceiver.class);
        intent.setAction(ACTION_QR_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, REQUEST_CODE_QR, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }

    private void cancelQrFallback(Context ctx) {
        Intent intent = new Intent(ctx, AlarmConfirmReceiver.class);
        intent.setAction(ACTION_QR_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, REQUEST_CODE_QR, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }

    private void launchAlarmServiceWithQr(Context ctx) {
        Intent serviceIntent = new Intent(ctx, AlarmService.class);
        serviceIntent.putExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, true);
        serviceIntent.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, "Wake up!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(serviceIntent);
        } else {
            ctx.startService(serviceIntent);
        }
    }

    private void dismissNotification(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID);
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH_CONFIRM) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_CONFIRM,
                        "Wake-up Confirmation",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(ch);
            }
        }
    }
}
