// utils/NotifScheduler.java
package com.example.noexcuse.utils;

import android.content.Context;
import androidx.work.*;
import com.example.noexcuse.workers.TaskNotifWorker;
import java.util.concurrent.TimeUnit;

public class NotifScheduler {

    /** Schedule 2 notifications: -1h and on-time */
    public static void schedule(Context ctx,
                                String type,      // "TASK" / "EDU" / "GYM"
                                int    itemId,
                                String title,
                                long   triggerMillis,  // exact time dyal task
                                String displayTime) {  // "07:30" for body text

        long now = System.currentTimeMillis();

        // --- 1H BEFORE ---
        long delayMinus1h = (triggerMillis - TimeUnit.HOURS.toMillis(1)) - now;
        if (delayMinus1h > 0) {
            enqueue(ctx, type, itemId, title, displayTime, false, delayMinus1h);
        }

        // --- ON TIME ---
        long delayNow = triggerMillis - now;
        if (delayNow > 0) {
            enqueue(ctx, type, itemId, title, displayTime, true, delayNow);
        }
    }

    private static void enqueue(Context ctx, String type, int itemId,
                                String title, String time,
                                boolean isNow, long delayMs) {
        Data data = new Data.Builder()
                .putString("type",   type)
                .putString("title",  title)
                .putString("time",   time)
                .putBoolean("isNow", isNow)
                .putInt("itemId",    itemId)
                .build();

        String tag = type + "_" + itemId + (isNow ? "_now" : "_pre");

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(TaskNotifWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build();

        WorkManager.getInstance(ctx).enqueueUniqueWork(
                tag,
                ExistingWorkPolicy.REPLACE,
                req);
    }

    /** Cancel notifications ila user delete task */
    public static void cancel(Context ctx, String type, int itemId) {
        WorkManager wm = WorkManager.getInstance(ctx);
        wm.cancelAllWorkByTag(type + "_" + itemId + "_pre");
        wm.cancelAllWorkByTag(type + "_" + itemId + "_now");
    }
}