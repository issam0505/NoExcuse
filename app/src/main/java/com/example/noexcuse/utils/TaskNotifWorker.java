// workers/TaskNotifWorker.java
package com.example.noexcuse.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.noexcuse.R;
import com.example.noexcuse.utils.NotificationHelper;

public class TaskNotifWorker extends Worker {

    public TaskNotifWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull @Override
    public Result doWork() {
        String type    = getInputData().getString("type");    // TASK / EDU / GYM
        String title   = getInputData().getString("title");
        String time    = getInputData().getString("time");    // "07:30"
        boolean isNow  = getInputData().getBoolean("isNow", false);
        int     itemId = getInputData().getInt("itemId", 0);

        String channel, notifTitle, notifBody;
        int icon;

        if ("GYM".equals(type)) {
            channel    = NotificationHelper.CH_GYM;
            icon       = R.drawable.ic_notification_gym; // crée une ic simple
            notifTitle = isNow ? "💪 Time's up — " + title
                    : "💪 1 hour left — " + title;
            notifBody  = isNow ? "It's " + time + " · Prepare your gear!"
                    : "Gym at " + time + " · Get ready!";
        } else if ("EDU".equals(type)) {
            channel    = NotificationHelper.CH_EDU;
            icon       = R.drawable.ic_notification_edu;
            notifTitle = isNow ? "📚 Time's up — " + title
                    : "📚 1 hour left — " + title;
            notifBody  = isNow ? "It's " + time + " · Start studying!"
                    : "Session at " + time + " · Get ready!";
        } else {
            channel    = NotificationHelper.CH_TASK;
            icon       = R.drawable.ic_notification_task;
            notifTitle = isNow ? "🔔 Time's up — " + title
                    : "⏰ 1 hour left — " + title;
            notifBody  = isNow ? "It's " + time + " · Do it now!"
                    : "Task at " + time + " · Don't miss it!";
        }

        // unique notif id: itemId * 10 + (isNow ? 1 : 0)
        int notifId = itemId * 10 + (isNow ? 1 : 0);

        NotificationHelper.send(getApplicationContext(),
                notifId, channel, notifTitle, notifBody, icon);

        return Result.success();
    }
}