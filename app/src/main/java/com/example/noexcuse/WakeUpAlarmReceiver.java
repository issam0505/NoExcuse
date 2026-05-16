package com.example.noexcuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * WakeUpAlarmReceiver
 * Receives the AlarmManager broadcast → starts AlarmService (foreground).
 * AlarmService will use fullScreenIntent to force WakeUpAlarmActivity on screen.
 * NO notification is shown here — the activity opens directly.
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
            boolean qrMode  = intent.getBooleanExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, false);

            Intent serviceIntent = new Intent(ctx, AlarmService.class);
            serviceIntent.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, wakeTime);
            serviceIntent.putExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, qrMode);

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