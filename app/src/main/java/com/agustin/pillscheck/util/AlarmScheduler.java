package com.agustin.pillscheck.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.agustin.pillscheck.receiver.ReminderAlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmScheduler {

    private static final int REQUEST_CODE_REMINDER = 1001;

    public static void scheduleNextReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PreferenceHelper prefs = new PreferenceHelper(context);
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        // Check if already taken or silenced for today
        boolean takenToday = prefs.isTakenForDate(todayStr);
        boolean silencedToday = prefs.isSilencedForDate(todayStr);

        Calendar now = Calendar.getInstance();

        Calendar reminderTime = Calendar.getInstance();
        reminderTime.set(Calendar.HOUR_OF_DAY, prefs.getReminderHour());
        reminderTime.set(Calendar.MINUTE, prefs.getReminderMinute());
        reminderTime.set(Calendar.SECOND, 0);
        reminderTime.set(Calendar.MILLISECOND, 0);

        Calendar cutoffTime = Calendar.getInstance();
        cutoffTime.set(Calendar.HOUR_OF_DAY, prefs.getCutoffHour());
        cutoffTime.set(Calendar.MINUTE, prefs.getCutoffMinute());
        cutoffTime.set(Calendar.SECOND, 0);
        cutoffTime.set(Calendar.MILLISECOND, 0);

        long triggerAtMillis;

        if (takenToday || silencedToday || now.after(cutoffTime)) {
            // Schedule for tomorrow at reminder time
            reminderTime.add(Calendar.DAY_OF_YEAR, 1);
            triggerAtMillis = reminderTime.getTimeInMillis();
        } else if (now.before(reminderTime)) {
            // Today before initial reminder
            triggerAtMillis = reminderTime.getTimeInMillis();
        } else {
            // Between reminder time and cutoff time, and not taken yet
            int intervalMin = prefs.getRepeatIntervalMinutes();
            if (intervalMin <= 0) intervalMin = 60;
            Calendar nextRepeat = Calendar.getInstance();
            nextRepeat.add(Calendar.MINUTE, intervalMin);

            if (nextRepeat.after(cutoffTime)) {
                // If next repeat exceeds cutoff, set for tomorrow
                reminderTime.add(Calendar.DAY_OF_YEAR, 1);
                triggerAtMillis = reminderTime.getTimeInMillis();
            } else {
                triggerAtMillis = nextRepeat.getTimeInMillis();
            }
        }

        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void scheduleSnooze(Context context, int minutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar snoozeTime = Calendar.getInstance();
        snoozeTime.add(Calendar.MINUTE, minutes);

        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = snoozeTime.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
}
