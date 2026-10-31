package com.agustin.pillscheck;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

import com.agustin.pillscheck.data.AppDatabase;
import com.agustin.pillscheck.receiver.ReminderAlarmReceiver;
import com.agustin.pillscheck.util.AlarmScheduler;
import com.agustin.pillscheck.util.LocaleHelper;
import com.agustin.pillscheck.util.PreferenceHelper;

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
