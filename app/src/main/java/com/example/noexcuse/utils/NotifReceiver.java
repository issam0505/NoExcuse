// utils/NotifReceiver.java
package com.example.noexcuse.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.noexcuse.R;

public class NotifReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String type       = intent.getStringExtra("type");
        String title      = intent.getStringExtra("title");
        String time       = intent.getStringExtra("time");
        boolean isNow     = intent.getBooleanExtra("isNow", false);
        int itemId        = intent.getIntExtra("itemId", 0);

        String baseChannel, notifTitle, notifBody;
        int icon;

        if ("GYM".equals(type)) {
            baseChannel = "ch_gym";
            icon        = R.drawable.ic_notification_gym;
            notifTitle  = isNow ? "💪 Time's up — " + title : "💪 1 hour left — " + title;
            notifBody   = isNow ? "It's " + time + " · Prepare your gear!" : "Gym at " + time + " · Get ready!";

        } else if ("EDU".equals(type)) {
            baseChannel = "ch_edu";
            icon        = R.drawable.ic_notification_edu;
            notifTitle  = isNow ? "📚 Time's up — " + title : "📚 1 hour left — " + title;
            notifBody   = isNow ? "It's " + time + " · Start studying!" : "Session at " + time + " · Get ready!";

        } else {
            baseChannel = "ch_task";
            icon        = R.drawable.ic_notification_task;
            notifTitle  = isNow ? "🔔 Time's up — " + title : "⏰ 1 hour left — " + title;
            notifBody   = isNow ? "It's " + time + " · Do it now!" : "Task at " + time + " · Don't miss it!";
        }

        int notifId = itemId * 10 + (isNow ? 1 : 0);

        NotificationHelper.send(ctx, notifId, baseChannel, isNow, notifTitle, notifBody, icon);
    }
}