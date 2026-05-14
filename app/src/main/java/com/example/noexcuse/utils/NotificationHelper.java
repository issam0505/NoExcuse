// utils/NotificationHelper.java
package com.example.noexcuse.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import com.example.noexcuse.R;

public class NotificationHelper {

    public static final String CH_TASK  = "ch_task";
    public static final String CH_EDU   = "ch_education";
    public static final String CH_GYM   = "ch_gym";

    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        // silent channels (no sound, vibration only)
        int[] types = {0, 1, 2};
        String[] ids    = {CH_TASK, CH_EDU, CH_GYM};
        String[] names  = {"Tasks", "Education", "Gym"};

        for (int i = 0; i < 3; i++) {
            NotificationChannel ch = new NotificationChannel(
                    ids[i], names[i], NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);           // no sound
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 300, 100, 300});
            nm.createNotificationChannel(ch);
        }
    }

    public static void send(Context ctx, int id,
                            String channelId, String title, String body,
                            int smallIconRes) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(smallIconRes)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(null)                // no sound
                .setVibrate(new long[]{0, 300, 100, 300})
                .setAutoCancel(true);

        nm.notify(id, builder.build());
    }
}