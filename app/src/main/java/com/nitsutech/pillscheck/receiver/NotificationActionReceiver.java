package com.nitsutech.pillscheck.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;


import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.IntakeRecordEntity;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.nitsutech.pillscheck.util.AlarmScheduler;
import com.nitsutech.pillscheck.util.PreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_TAKE_NOW = "com.nitsutech.pillscheck.ACTION_TAKE_NOW";
    public static final String ACTION_SNOOZE = "com.nitsutech.pillscheck.ACTION_SNOOZE";
    public static final String ACTION_SILENCE_TODAY = "com.nitsutech.pillscheck.ACTION_SILENCE_TODAY";
    public static final String ACTION_DATA_CHANGED = "com.nitsutech.pillscheck.DATA_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(ReminderAlarmReceiver.NOTIFICATION_ID);
        }

        PreferenceHelper prefs = new PreferenceHelper(context);
        String action = intent.getAction();

        if (ACTION_TAKE_NOW.equals(action)) {
            handleTakeNow(context, prefs);
        } else if (ACTION_SNOOZE.equals(action)) {
            AlarmScheduler.scheduleSnooze(context, prefs.getRepeatIntervalMinutes());
            Toast.makeText(context, R.string.notif_snoozed_toast, Toast.LENGTH_SHORT).show();
        } else if (ACTION_SILENCE_TODAY.equals(action)) {
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            prefs.setSilencedDate(todayStr);
            AlarmScheduler.scheduleNextReminder(context);
            Toast.makeText(context, R.string.notif_silenced_toast, Toast.LENGTH_SHORT).show();
            notifyUiChanged(context);
        }
    }

    private void handleTakeNow(Context context, PreferenceHelper prefs) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<MedicationEntity> meds = db.medicationDao().getAllMedications();

            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());

            int folcresRemaining = 0;
            int minoxRemaining = 0;
            int vitDRemaining = 0;

            for (MedicationEntity med : meds) {
                boolean[] slots = med.getSlotsArray();
                // Consume the rightmost filled slot (matching physical taking order)
                for (int i = slots.length - 1; i >= 0; i--) {
                    if (slots[i]) {
                        slots[i] = false;
                        break;
                    }
                }
                med.setSlotsArray(slots);
                db.medicationDao().update(med);

                int remaining = med.getActivePillsCount();
                if ("folcres".equals(med.getKey())) folcresRemaining = remaining;
                else if ("minoxidil".equals(med.getKey())) minoxRemaining = remaining;
                else if ("vitamind".equals(med.getKey())) vitDRemaining = remaining;
            }

            // Record intake
            IntakeRecordEntity record = new IntakeRecordEntity(
                    System.currentTimeMillis(),
                    todayStr,
                    timeStr,
                    folcresRemaining,
                    minoxRemaining,
                    vitDRemaining,
                    "Notification Action"
            );
            db.intakeRecordDao().insert(record);

            prefs.setLastIntakeDate(todayStr);
            prefs.setLastIntakeTime(timeStr);

            AlarmScheduler.scheduleNextReminder(context);

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, R.string.notif_taken_toast, Toast.LENGTH_SHORT).show();
                notifyUiChanged(context);
            });
        });
    }

    private void notifyUiChanged(Context context) {
        Intent updateIntent = new Intent(ACTION_DATA_CHANGED);
        context.sendBroadcast(updateIntent);
    }
}
