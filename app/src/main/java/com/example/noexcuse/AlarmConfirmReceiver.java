package com.example.noexcuse;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.noexcuse.R;

/**
 * AlarmConfirmReceiver
 * ──────────────────────────────────────────────────────────────────
 * Fired 5 minutes after the user taps STOP on WakeUpAlarmActivity.
 *
 * Sends a "Are you awake?" notification with a YES button.
 * If user doesn't press YES within 5 min → fires QR alarm.
 *
 * Actions:
 *  ACTION_CONFIRM  → show the "awake?" notification + schedule QR fallback
 *  ACTION_YES      → user confirmed awake → cancel QR fallback
 */
public class AlarmConfirmReceiver extends BroadcastReceiver {

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

        switch (action) {
            case ACTION_CONFIRM:
                showConfirmNotification(ctx);
                scheduleQrFallback(ctx);
                break;

            case ACTION_YES:
                // User confirmed awake — cancel the QR fallback alarm
                cancelQrFallback(ctx);
                dismissNotification(ctx);
                break;

            case ACTION_QR_ALARM:
                // 5 min elapsed with no YES → launch QR alarm activity
                dismissNotification(ctx);
                launchQrAlarm(ctx);
                break;
        }
    }

    // ── Step 1: show the notification with YES button ─────────────────────

    private void showConfirmNotification(Context ctx) {
        createChannel(ctx);

        // YES pending intent
        Intent yesIntent = new Intent(ctx, AlarmConfirmReceiver.class);
        yesIntent.setAction(ACTION_YES);
        PendingIntent yesPi = PendingIntent.getBroadcast(
                ctx, REQUEST_CODE_YES, yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CH_CONFIRM)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("⏰ Are you awake?")
                        .setContentText("Tap YES to confirm you're up — or the alarm will ring again!")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .addAction(0, "✅  YES, I'm up!", yesPi);

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, builder.build());
    }

    // ── Step 2: schedule QR fallback in 5 more minutes ────────────────────

    private void scheduleQrFallback(Context ctx) {
        long triggerAt = System.currentTimeMillis() + (5 * 60 * 1000L);

        Intent intent = new Intent(ctx, AlarmConfirmReceiver.class);
        intent.setAction(ACTION_QR_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, REQUEST_CODE_QR, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo clockInfo =
                new AlarmManager.AlarmClockInfo(triggerAt, pi);
        am.setAlarmClock(clockInfo, pi);
    }

    private void cancelQrFallback(Context ctx) {
        Intent intent = new Intent(ctx, AlarmConfirmReceiver.class);
        intent.setAction(ACTION_QR_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, REQUEST_CODE_QR, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            am.cancel(pi);
            pi.cancel();
        }
    }

    private void launchQrAlarm(Context ctx) {
        Intent launch = new Intent(ctx, WakeUpAlarmActivity.class);
        launch.putExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, true);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ctx.startActivity(launch);
    }

    private void dismissNotification(Context ctx) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIF_ID);
    }

    // ── Channel creation ──────────────────────────────────────────────────

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CH_CONFIRM) != null) return;

        NotificationChannel ch = new NotificationChannel(
                CH_CONFIRM,
                "Wake-up Confirmation",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Confirms the user is actually awake");
        nm.createNotificationChannel(ch);
    }
}