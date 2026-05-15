package com.example.noexcuse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * WakeUpScheduler
 * ──────────────────────────────────────────────────────────────────
 * Schedules the daily wake-up alarm using AlarmManager.setAlarmClock()
 * Persists hour/minute to SharedPreferences so it can be re-scheduled
 * after device reboot (BOOT_COMPLETED in WakeUpAlarmReceiver).
 */
public class WakeUpScheduler {

    private static final String PREFS        = "noexcuse_alarm_prefs";
    private static final String KEY_HOUR     = "alarm_hour";
    private static final String KEY_MINUTE   = "alarm_minute";
    private static final String KEY_ENABLED  = "alarm_enabled";

    /** Schedule the alarm for the next occurrence of hour:minute */
    public static void schedule(Context ctx, int hour, int minute) {
        long triggerAt = nextOccurrence(hour, minute);

        String displayTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);

        Intent intent = new Intent(ctx, WakeUpAlarmReceiver.class);
        intent.setAction(WakeUpAlarmReceiver.ACTION_WAKE_ALARM);
        intent.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, displayTime);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                WakeUpAlarmReceiver.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(triggerAt, pi);
        am.setAlarmClock(clockInfo, pi);

        // Persist
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .putBoolean(KEY_ENABLED, true)
                .apply();
    }

    /** Cancel the scheduled alarm */
    public static void cancel(Context ctx) {
        Intent intent = new Intent(ctx, WakeUpAlarmReceiver.class);
        intent.setAction(WakeUpAlarmReceiver.ACTION_WAKE_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                WakeUpAlarmReceiver.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (pi != null) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            am.cancel(pi);
            pi.cancel();
        }

        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, false)
                .apply();
    }

    /** Called on BOOT_COMPLETED to restore the alarm */
    public static void rescheduleFromPrefs(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, false)) return;

        int hour   = prefs.getInt(KEY_HOUR, 7);
        int minute = prefs.getInt(KEY_MINUTE, 0);
        schedule(ctx, hour, minute);
    }

    /** Get saved hour (-1 if none) */
    public static int getSavedHour(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HOUR, -1);
    }

    public static int getSavedMinute(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MINUTE, 0);
    }

    public static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    /** Returns the next epoch millis for the given hour:minute (tomorrow if already passed today) */
    private static long nextOccurrence(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1); // Already passed today → tomorrow
        }
        return cal.getTimeInMillis();
    }
}