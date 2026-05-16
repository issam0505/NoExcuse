package com.example.noexcuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * WakeUpAlarmReceiver
 * ──────────────────────────────────────────────────────────────────
 * Receives the AlarmManager broadcast.
 * FIXED: Starts AlarmService (Foreground Service) to ensure sound plays
 * and activity is forced to front via fullScreenIntent.
 */
public class WakeUpAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_WAKE_ALARM = "com.example.noexcuse.ACTION_WAKE_ALARM";
    public static final int    REQUEST_CODE      = 7001;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        Log.d("WakeUpAlarmReceiver", "Alarm fired! Action: " + action);

        if (ACTION_WAKE_ALARM.equals(action)) {
            String wakeTime = intent.getStringExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME);

            Intent serviceIntent = new Intent(ctx, AlarmService.class);
            serviceIntent.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, wakeTime);

            // Start Foreground Service (Required for Android 10+ to launch activity from background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(serviceIntent);
            } else {
                ctx.startService(serviceIntent);
            }
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            WakeUpScheduler.rescheduleFromPrefs(ctx);
        }
    }
}