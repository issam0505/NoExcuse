// utils/NotificationHelper.java
package com.example.noexcuse.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import com.example.noexcuse.R;

public class NotificationHelper {

    public static final String CH_TASK_PRE  = "ch_task_pre";
    public static final String CH_TASK_NOW  = "ch_task_now";
    public static final String CH_EDU_PRE   = "ch_edu_pre";
    public static final String CH_EDU_NOW   = "ch_edu_now";
    public static final String CH_GYM_PRE   = "ch_gym_pre";
    public static final String CH_GYM_NOW   = "ch_gym_now";

    // Keep old constants for backward compat (optional)
    public static final String CH_TASK  = CH_TASK_NOW;
    public static final String CH_EDU   = CH_EDU_NOW;
    public static final String CH_GYM   = CH_GYM_NOW;

    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        // Sound URIs from res/raw/
        Uri soundFirst = Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.first);
        Uri soundLast  = Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.last);

        AudioAttributes audioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        // Channel configs: { id, name, soundUri }
        String[]  ids   = { CH_TASK_PRE, CH_TASK_NOW, CH_EDU_PRE, CH_EDU_NOW, CH_GYM_PRE, CH_GYM_NOW };
        String[]  names = { "Tasks (1h before)", "Tasks (Now)", "Education (1h before)", "Education (Now)", "Gym (1h before)", "Gym (Now)" };
        Uri[]     sounds = { soundFirst, soundLast, soundFirst, soundLast, soundFirst, soundLast };

        for (int i = 0; i < ids.length; i++) {
            NotificationChannel ch = new NotificationChannel(
                    ids[i], names[i], NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(sounds[i], audioAttrs);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 300, 100, 300});
            nm.createNotificationChannel(ch);
        }
    }

    /**
     * @param isNow  true  → use "now" channel (last.mp3)
     *               false → use "pre" channel (first.mp3)
     */
    public static void send(Context ctx, int id,
                            String baseChannelId,  // CH_TASK / CH_EDU / CH_GYM (without _pre/_now)
                            boolean isNow,
                            String title, String body,
                            int smallIconRes) {

        // Pick the right channel based on isNow
        String channelId = baseChannelId + (isNow ? "_now" : "_pre");

        // Sound URI (used for pre-O devices)
        Uri soundUri = Uri.parse("android.resource://" + ctx.getPackageName()
                + "/" + (isNow ? R.raw.last : R.raw.first));

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(smallIconRes)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)                         // for < Android O
                .setVibrate(new long[]{0, 300, 100, 300})
                .setAutoCancel(true);

        nm.notify(id, builder.build());
    }
}