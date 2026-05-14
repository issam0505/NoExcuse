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
        String  type   = getInputData().getString("type");    // TASK / EDU / GYM
        String  title  = getInputData().getString("title");
        String  time   = getInputData().getString("time");    // "07:30"
        boolean isNow  = getInputData().getBoolean("isNow", false);
        int     itemId = getInputData().getInt("itemId", 0);

        String baseChannel, notifTitle, notifBody;
        int icon;

        if ("GYM".equals(type)) {
            baseChannel = "ch_gym";
            icon        = R.drawable.ic_notification_gym;
            notifTitle  = isNow ? "💪 Time's up — " + title
                    : "💪 1 hour left — " + title;
            notifBody   = isNow ? "It's " + time + " · Prepare your gear!"
                    : "Gym at " + time + " · Get ready!";

        } else if ("EDU".equals(type)) {
            baseChannel = "ch_edu";
            icon        = R.drawable.ic_notification_edu;
            notifTitle  = isNow ? "📚 Time's up — " + title
                    : "📚 1 hour left — " + title;
            notifBody   = isNow ? "It's " + time + " · Start studying!"
                    : "Session at " + time + " · Get ready!";

        } else {                          // TASK (default)
            baseChannel = "ch_task";
            icon        = R.drawable.ic_notification_task;
            notifTitle  = isNow ? "🔔 Time's up — " + title
                    : "⏰ 1 hour left — " + title;
            notifBody   = isNow ? "It's " + time + " · Do it now!"
                    : "Task at " + time + " · Don't miss it!";
        }

        // unique notif id: itemId * 10 + (isNow ? 1 : 0)
        int notifId = itemId * 10 + (isNow ? 1 : 0);

        // isNow → last.mp3  |  !isNow → first.mp3
        NotificationHelper.send(
                getApplicationContext(),
                notifId,
                baseChannel,   // "ch_task" / "ch_edu" / "ch_gym"
                isNow,         // determines channel suffix + sound
                notifTitle,
                notifBody,
                icon);

        return Result.success();
    }
}