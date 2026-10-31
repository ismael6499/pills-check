package com.nitsutech.pillscheck;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.receiver.ReminderAlarmReceiver;
import com.nitsutech.pillscheck.util.AlarmScheduler;
import com.nitsutech.pillscheck.util.LocaleHelper;
import com.nitsutech.pillscheck.util.PreferenceHelper;

import java.util.concurrent.Executors;

public class PillsCheckApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        PreferenceHelper prefs = new PreferenceHelper(base);
        super.attachBaseContext(LocaleHelper.applyLocale(base, prefs.getAppLanguage()));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceHelper prefs = new PreferenceHelper(this);
        LocaleHelper.updateAppConfiguration(this, prefs.getAppLanguage());

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            ReminderAlarmReceiver.createNotificationChannel(this, nm);
        }

        // Initialize DB and ensure seed data is loaded
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (db.medicationDao().getAllMedications().isEmpty()) {
                AppDatabase.seedFromAssets(this, db.medicationDao());
            }
        });

        // Set up alarm scheduler
        AlarmScheduler.scheduleNextReminder(this);
    }
}
