package com.example.noexcuse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * FocusMonitorService
 * Optimized with UsageEvents for real-time distraction detection.
 */
public class FocusMonitorService extends Service {

    private static final String TAG = "FocusMonitor";

    public static final String ACTION_START = "ACTION_FOCUS_START";
    public static final String ACTION_STOP  = "ACTION_FOCUS_STOP";
    public static final String EXTRA_MODULE = "EXTRA_MODULE_NAME";

    private static final String CHANNEL_FOCUS    = "focus_mode_channel";
    private static final String CHANNEL_REMINDER = "focus_reminder_channel";

    private static final int NOTIF_ID_FOREGROUND = 1001;
    private static final int NOTIF_ID_REMINDER   = 1002;

    private static final long POLL_INTERVAL_MS     = 10_000L;
    private static final long DISTRACTION_LIMIT_MS = 120_000L; // 2 minutes

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String  moduleName       = "your studies";
    private long    distractionStart = 0L;
    private boolean reminderFired    = false;
    private boolean isPolling        = false;

    // We keep track of the last known foreground app to avoid 'null' drops
    private String lastDetectedApp = null;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            checkForegroundApp();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            ensurePolling();
            return START_STICKY;
        }

        if (ACTION_STOP.equals(intent.getAction())) {
            stopFocusMode();
            return START_NOT_STICKY;
        }

        String newModule = intent.getStringExtra(EXTRA_MODULE);
        if (newModule != null) moduleName = newModule;

        startForeground(NOTIF_ID_FOREGROUND, buildForegroundNotification());
        ensurePolling();
        return START_STICKY;
    }

    private void ensurePolling() {
        if (!isPolling) {
            isPolling = true;
            handler.post(pollRunnable);
            Log.d(TAG, "▶️ Focus monitoring started");
        }
    }

    private void checkForegroundApp() {
        String currentPkg = getForegroundPackage();

        // Fix 1: If system returns null, retain the last known app instead of resetting
        if (currentPkg != null) {
            lastDetectedApp = currentPkg;
            Log.d(TAG, "📱 Current App Detected: " + currentPkg);
        } else {
            Log.w(TAG, "⚠️ App detection returned null. Retaining last known app: " + lastDetectedApp);
        }

        // Use the persistent package tracker for analysis
        boolean isAllowed = AllowedAppsConfig.isAllowed(lastDetectedApp);

        if (isAllowed) {
            if (distractionStart != 0L) {
                Log.d(TAG, "✅ Returned to allowed app. Resetting distraction timer.");
            }
            distractionStart = 0L;
            reminderFired    = false;
            cancelReminderNotification();
        } else {
            if (distractionStart == 0L) {
                distractionStart = System.currentTimeMillis();
                reminderFired    = false;
                Log.d(TAG, "🚨 Distraction started! User is on: " + lastDetectedApp);
            } else {
                long elapsed = System.currentTimeMillis() - distractionStart;
                Log.d(TAG, "⏱ Time away: " + (elapsed / 1000) + "s / 120s");

                if (elapsed >= DISTRACTION_LIMIT_MS && !reminderFired) {
                    fireReminderNotification();
                    reminderFired = true;
                }
            }
        }
    }

    private String getForegroundPackage() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return null;

        long now = System.currentTimeMillis();
        // Fix 2: Look back 5 minutes (300,000ms) instead of just 1 minute to ensure events capture
        UsageEvents events = usm.queryEvents(now - 300_000, now);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPkg = null;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPkg = event.getPackageName();
            }
        }
        return lastPkg;
    }

    private void fireReminderNotification() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi = PendingIntent.getActivity(this, 1,
                intent != null ? intent : new Intent(), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_school)
                .setContentTitle("⏰ Time to refocus!")
                .setContentText("You've been distracted for 2 minutes. Back to: " + moduleName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID_REMINDER, notif);
            handler.post(() -> Toast.makeText(getApplicationContext(), "⏰ Back to Study!", Toast.LENGTH_LONG).show());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_FOCUS, "Focus Mode", NotificationManager.IMPORTANCE_LOW));

            NotificationChannel reminderCh = new NotificationChannel(
                    CHANNEL_REMINDER, "Focus Reminders", NotificationManager.IMPORTANCE_HIGH);
            reminderCh.enableVibration(true);
            // Fix 3: Ensure notifications can bypass lockscreen/pop up dynamically
            reminderCh.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(reminderCh);
        }
    }

    private Notification buildForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_FOCUS)
                .setSmallIcon(R.drawable.ic_school)
                .setContentTitle("🎯 Focus Mode Active")
                .setContentText("Monitoring: " + moduleName)
                .setOngoing(true)
                .build();
    }

    private void cancelReminderNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID_REMINDER);
    }

    private void stopFocusMode() {
        isPolling = false;
        handler.removeCallbacks(pollRunnable);
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() { stopFocusMode(); super.onDestroy(); }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}