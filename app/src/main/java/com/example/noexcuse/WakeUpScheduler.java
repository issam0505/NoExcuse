package com.example.noexcuse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * WakeUpScheduler — FIXED
 * ──────────────────────────────────────────────────────────────────
 * FIX 1: check canScheduleExactAlarms() avant setAlarmClock()
 * FIX 2: setExactAndAllowWhileIdle() kfallback ila makaynach permission
 * FIX 3: log clair bach nshfu wach alarm tregistrat
 */
public class WakeUpScheduler {

    private static final String TAG      = "WakeUpScheduler";
    private static final String PREFS    = "noexcuse_alarm_prefs";
    private static final String KEY_HOUR    = "alarm_hour";
    private static final String KEY_MINUTE  = "alarm_minute";
    private static final String KEY_ENABLED = "alarm_enabled";

    public static void schedule(Context ctx, int hour, int minute) {
        long triggerAt = nextOccurrence(hour, minute);
        String displayTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);

        Log.d(TAG, "Scheduling alarm for " + displayTime + " → triggerAt=" + triggerAt
                + " (now=" + System.currentTimeMillis() + ", diff="
                + ((triggerAt - System.currentTimeMillis()) / 1000) + "s)");

        Intent intent = new Intent(ctx, WakeUpAlarmReceiver.class);
        intent.setAction(WakeUpAlarmReceiver.ACTION_WAKE_ALARM);
        intent.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, displayTime);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                WakeUpAlarmReceiver.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // ✅ FIX: Android 12+ kaykhaṣ check explicit permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                AlarmManager.AlarmClockInfo clockInfo =
                        new AlarmManager.AlarmClockInfo(triggerAt, pi);
                am.setAlarmClock(clockInfo, pi);
                Log.d(TAG, "✅ setAlarmClock() registered (exact alarms allowed)");
            } else {
                // Fallback: setExactAndAllowWhileIdle — kayer walakin machi
                // 100% guaranteed f Doze. Kdir toast bach user imchi l settings.
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                Log.w(TAG, "⚠️ Exact alarm permission NOT granted — using setExactAndAllowWhileIdle fallback");
            }
        } else {
            // Android < 12 → setAlarmClock() direct
            AlarmManager.AlarmClockInfo clockInfo =
                    new AlarmManager.AlarmClockInfo(triggerAt, pi);
            am.setAlarmClock(clockInfo, pi);
            Log.d(TAG, "✅ setAlarmClock() registered (pre-Android 12)");
        }

        // Persist bach BOOT_COMPLETED yreschedule
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .putBoolean(KEY_ENABLED, true)
                .apply();
    }

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
            Log.d(TAG, "Alarm cancelled");
        }

        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, false)
                .apply();
    }

    public static void rescheduleFromPrefs(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, false)) return;

        int hour   = prefs.getInt(KEY_HOUR, 7);
        int minute = prefs.getInt(KEY_MINUTE, 0);
        Log.d(TAG, "BOOT_COMPLETED → rescheduling alarm " + hour + ":" + minute);
        schedule(ctx, hour, minute);
    }

    public static int getSavedHour(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HOUR, -1);
    }

    public static int getSavedMinute(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MINUTE, 0);
    }

    public static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    /**
     * ✅ FIX: check ila Android 12+ o permission machi granted
     * Rja3 true → user khas imchi l Settings bach isma7
     */
    public static boolean needsExactAlarmPermission(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            return !am.canScheduleExactAlarms();
        }
        return false;
    }

    private static long nextOccurrence(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }
}