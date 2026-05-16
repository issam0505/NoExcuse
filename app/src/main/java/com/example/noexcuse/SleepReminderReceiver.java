package com.example.noexcuse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * SleepReminderReceiver
 * Fires at the user's configured sleep time.
 * Shows a gentle notification: "Time to sleep 😴"
 */
public class SleepReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_SLEEP_REMINDER = "com.example.noexcuse.ACTION_SLEEP_REMINDER";
    public static final int    REQUEST_CODE           = 6001;

    private static final String CH_SLEEP = "ch_sleep_reminder";
    private static final int    NOTIF_ID = 7777;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!ACTION_SLEEP_REMINDER.equals(intent.getAction())) return;
        showSleepNotification(ctx);
    }

    private void showSleepNotification(Context ctx) {
        createChannel(ctx);

        Notification notif = new NotificationCompat.Builder(ctx, CH_SLEEP)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("😴 Time to sleep!")
                .setContentText("Rest up — a good night's sleep makes tomorrow better 💪")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("It's time to wind down 🌙\n"
                                + "Getting enough sleep will help you stay productive and energized tomorrow. "
                                + "Good night! 💪"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build();

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, notif);
    }

    // ── Static helpers to schedule / cancel ────────────────────────────────

    public static void schedule(Context ctx, int hour, int minute) {
        long triggerAt = nextOccurrence(hour, minute);

        android.app.AlarmManager am =
                (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(ctx, SleepReminderReceiver.class);
        intent.setAction(ACTION_SLEEP_REMINDER);

        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                ctx, REQUEST_CODE, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        | android.app.PendingIntent.FLAG_IMMUTABLE);

        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }

    public static void cancel(Context ctx) {
        Intent intent = new Intent(ctx, SleepReminderReceiver.class);
        intent.setAction(ACTION_SLEEP_REMINDER);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                ctx, REQUEST_CODE, intent,
                android.app.PendingIntent.FLAG_NO_CREATE
                        | android.app.PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }

    private static long nextOccurrence(int hour, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH_SLEEP) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_SLEEP, "Sleep Reminder",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(ch);
            }
        }
    }
}