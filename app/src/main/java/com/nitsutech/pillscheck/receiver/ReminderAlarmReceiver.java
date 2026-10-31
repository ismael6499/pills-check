package com.nitsutech.pillscheck.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.ui.MainActivity;
import com.nitsutech.pillscheck.util.AlarmScheduler;
import com.nitsutech.pillscheck.util.PreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "pills_reminder_channel";
    public static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferenceHelper prefs = new PreferenceHelper(context);
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        if (prefs.isTakenForDate(todayStr) || prefs.isSilencedForDate(todayStr)) {
            // Already handled today; schedule next day
            AlarmScheduler.scheduleNextReminder(context);
            return;
        }

        showReminderNotification(context);

        // Schedule next repetition or tomorrow's reminder
        AlarmScheduler.scheduleNextReminder(context);
    }

    private void showReminderNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        createNotificationChannel(context, nm);

        // Content intent (opens app)
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action 1: Take Now
        Intent takeIntent = new Intent(context, NotificationActionReceiver.class);
        takeIntent.setAction(NotificationActionReceiver.ACTION_TAKE_NOW);
        PendingIntent takePendingIntent = PendingIntent.getBroadcast(
                context, 1, takeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action 2: Snooze 1h
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, 2, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action 3: Silence Today
        Intent silenceIntent = new Intent(context, NotificationActionReceiver.class);
        silenceIntent.setAction(NotificationActionReceiver.ACTION_SILENCE_TODAY);
        PendingIntent silencePendingIntent = PendingIntent.getBroadcast(
                context, 3, silenceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 300, 200, 300})
                .setContentIntent(contentPendingIntent)
                .addAction(0, context.getString(R.string.notif_action_take), takePendingIntent)
                .addAction(0, context.getString(R.string.notif_action_snooze), snoozePendingIntent)
                .addAction(0, context.getString(R.string.notif_action_silence), silencePendingIntent);

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    public static void createNotificationChannel(Context context, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            channel.enableLights(true);
            channel.setLightColor(Color.CYAN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});
            nm.createNotificationChannel(channel);
        }
    }
}
