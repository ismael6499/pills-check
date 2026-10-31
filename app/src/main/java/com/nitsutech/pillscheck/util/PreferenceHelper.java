package com.nitsutech.pillscheck.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {

    private static final String PREF_NAME = "pills_check_prefs";

    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_REPEAT_INTERVAL_MIN = "repeat_interval_min";
    private static final String KEY_CUTOFF_HOUR = "cutoff_hour";
    private static final String KEY_CUTOFF_MINUTE = "cutoff_minute";
    private static final String KEY_LOW_STOCK_DAYS = "low_stock_days";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_SILENCED_DATE = "silenced_date";
    private static final String KEY_LAST_INTAKE_DATE = "last_intake_date";
    private static final String KEY_LAST_INTAKE_TIME = "last_intake_time";

    private final SharedPreferences prefs;

    public PreferenceHelper(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 14); // default 14:00 (2 PM)
    }

    public void setReminderHour(int hour) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).apply();
    }

    public int getReminderMinute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0);
    }

    public void setReminderMinute(int minute) {
        prefs.edit().putInt(KEY_REMINDER_MINUTE, minute).apply();
    }

    public int getRepeatIntervalMinutes() {
        return prefs.getInt(KEY_REPEAT_INTERVAL_MIN, 60); // default every 1 hour
    }

    public void setRepeatIntervalMinutes(int minutes) {
        prefs.edit().putInt(KEY_REPEAT_INTERVAL_MIN, minutes).apply();
    }

    public int getCutoffHour() {
        return prefs.getInt(KEY_CUTOFF_HOUR, 22); // default 22:00
    }

    public void setCutoffHour(int hour) {
        prefs.edit().putInt(KEY_CUTOFF_HOUR, hour).apply();
    }

    public int getCutoffMinute() {
        return prefs.getInt(KEY_CUTOFF_MINUTE, 0);
    }

    public void setCutoffMinute(int minute) {
        prefs.edit().putInt(KEY_CUTOFF_MINUTE, minute).apply();
    }

    public int getLowStockDaysThreshold() {
        return prefs.getInt(KEY_LOW_STOCK_DAYS, 7); // default 7 days margin
    }

    public void setLowStockDaysThreshold(int days) {
        prefs.edit().putInt(KEY_LOW_STOCK_DAYS, days).apply();
    }

    public String getAppLanguage() {
        return prefs.getString(KEY_APP_LANGUAGE, "en"); // default English
    }

    public void setAppLanguage(String lang) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply();
    }

    public String getSilencedDate() {
        return prefs.getString(KEY_SILENCED_DATE, "");
    }

    public void setSilencedDate(String dateStr) {
        prefs.edit().putString(KEY_SILENCED_DATE, dateStr).apply();
    }

    public boolean isSilencedForDate(String dateStr) {
        return dateStr != null && dateStr.equals(getSilencedDate());
    }

    public String getLastIntakeDate() {
        return prefs.getString(KEY_LAST_INTAKE_DATE, "");
    }

    public void setLastIntakeDate(String dateStr) {
        prefs.edit().putString(KEY_LAST_INTAKE_DATE, dateStr).apply();
    }

    public String getLastIntakeTime() {
        return prefs.getString(KEY_LAST_INTAKE_TIME, "");
    }

    public void setLastIntakeTime(String timeStr) {
        prefs.edit().putString(KEY_LAST_INTAKE_TIME, timeStr).apply();
    }

    public boolean isTakenForDate(String dateStr) {
        return dateStr != null && dateStr.equals(getLastIntakeDate());
    }
}
