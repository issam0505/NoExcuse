package com.example.noexcuse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * WakeUpAlarmReceiver
 * ──────────────────────────────────────────────────────────────────
 * Receives the AlarmManager broadcast at wake-up time.
 * Launches WakeUpAlarmActivity (over lock screen).
 *
 * Also used to re-fire when REBOOT happens (BOOT_COMPLETED):
 * re-schedule pending alarms from SharedPreferences.
 */
public class WakeUpAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_WAKE_ALARM = "com.example.noexcuse.ACTION_WAKE_ALARM";
    public static final int    REQUEST_CODE      = 7001;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();

        if (ACTION_WAKE_ALARM.equals(action)) {
            String wakeTime = intent.getStringExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME);

            Intent launch = new Intent(ctx, WakeUpAlarmActivity.class);
            launch.putExtra(WakeUpAlarmActivity.EXTRA_QR_MODE, false);
            launch.putExtra(WakeUpAlarmActivity.EXTRA_WAKE_TIME, wakeTime);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ctx.startActivity(launch);
        }

        // BOOT_COMPLETED: re-schedule alarm from SharedPreferences
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            WakeUpScheduler.rescheduleFromPrefs(ctx);
        }
    }
}