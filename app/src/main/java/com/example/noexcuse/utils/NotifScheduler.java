// utils/NotifScheduler.java
package com.example.noexcuse.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.concurrent.TimeUnit;

public class NotifScheduler {

    /**
     * Schedule 2 notifications: -1h (first.mp3) and on-time (last.mp3)
     * AlarmManager.setAlarmClock() → guaranteed even in Doze/sleep mode
     */
    public static void schedule(Context ctx,
                                String type,         // "TASK" / "EDU" / "GYM"
                                int    itemId,
                                String title,
                                long   triggerMillis, // exact time dyal task
                                String displayTime) { // "07:30" for body text

        long now = System.currentTimeMillis();

        // --- 1H BEFORE ---
        long timeMinus1h = triggerMillis - TimeUnit.HOURS.toMillis(1);
        if (timeMinus1h > now) {
            setAlarm(ctx, type, itemId, title, displayTime, false, timeMinus1h);
        }

        // --- ON TIME ---
        if (triggerMillis > now) {
            setAlarm(ctx, type, itemId, title, displayTime, true, triggerMillis);
        }
    }

    private static void setAlarm(Context ctx, String type, int itemId,
                                 String title, String time,
                                 boolean isNow, long triggerAtMillis) {

        Intent intent = new Intent(ctx, NotifReceiver.class);
        intent.putExtra("type",   type);
        intent.putExtra("title",  title);
        intent.putExtra("time",   time);
        intent.putExtra("isNow",  isNow);
        intent.putExtra("itemId", itemId);

        // Unique requestCode per notification
        int requestCode = itemId * 10 + (isNow ? 1 : 0);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // setAlarmClock → wakens device even in Doze, shows clock icon in status bar
        AlarmManager.AlarmClockInfo clockInfo =
                new AlarmManager.AlarmClockInfo(triggerAtMillis, pi);
        am.setAlarmClock(clockInfo, pi);
    }

    /** Cancel both notifications for a task */
    public static void cancel(Context ctx, String type, int itemId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // Cancel _pre (1h before)
        cancelPending(ctx, am, type, itemId, false);
        // Cancel _now (on time)
        cancelPending(ctx, am, type, itemId, true);
    }

    private static void cancelPending(Context ctx, AlarmManager am,
                                      String type, int itemId, boolean isNow) {
        Intent intent = new Intent(ctx, NotifReceiver.class);
        int requestCode = itemId * 10 + (isNow ? 1 : 0);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}