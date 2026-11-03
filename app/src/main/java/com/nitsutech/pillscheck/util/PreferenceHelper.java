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

    // Google Tasks Integration Preferences
    public static final String SYNC_MODE_SINGLE = "single";
    public static final String SYNC_MODE_INDIVIDUAL = "individual";

    private static final String KEY_GOOGLE_TASKS_ENABLED = "google_tasks_enabled";
    private static final String KEY_GOOGLE_TASKS_ACCOUNT = "google_tasks_account";
    private static final String KEY_GOOGLE_TASKS_MODE = "google_tasks_mode";
    private static final String KEY_GOOGLE_TASKS_GLOBAL_NAME = "google_tasks_global_name";
    private static final String KEY_GOOGLE_TASKS_MED_PREFIX = "google_tasks_med_";

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

    public boolean isGoogleTasksEnabled() {
        return prefs.getBoolean(KEY_GOOGLE_TASKS_ENABLED, false);
    }

    public void setGoogleTasksEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GOOGLE_TASKS_ENABLED, enabled).apply();
    }

    public String getGoogleTasksAccount() {
        return prefs.getString(KEY_GOOGLE_TASKS_ACCOUNT, "");
    }

    public void setGoogleTasksAccount(String account) {
        prefs.edit().putString(KEY_GOOGLE_TASKS_ACCOUNT, account).apply();
    }

    public String getGoogleTasksMode() {
        return prefs.getString(KEY_GOOGLE_TASKS_MODE, SYNC_MODE_SINGLE);
    }

    public void setGoogleTasksMode(String mode) {
        prefs.edit().putString(KEY_GOOGLE_TASKS_MODE, mode).apply();
    }

    public String getGlobalGoogleTaskName() {
        return prefs.getString(KEY_GOOGLE_TASKS_GLOBAL_NAME, "Medication");
    }

    public void setGlobalGoogleTaskName(String name) {
        prefs.edit().putString(KEY_GOOGLE_TASKS_GLOBAL_NAME, name).apply();
    }

    public String getMedicationGoogleTaskName(String medKey, String defaultName) {
        return prefs.getString(KEY_GOOGLE_TASKS_MED_PREFIX + medKey, defaultName);
    }

    public void setMedicationGoogleTaskName(String medKey, String taskName) {
        prefs.edit().putString(KEY_GOOGLE_TASKS_MED_PREFIX + medKey, taskName).apply();
    }

    private static final String KEY_LAST_INTAKE_SNAPSHOT = "last_intake_snapshot";
    private static final String KEY_CURRENT_DAY_TRACKER = "current_day_tracker";

    public String getLastIntakeSnapshot() {
        return prefs.getString(KEY_LAST_INTAKE_SNAPSHOT, "");
    }

    public void setLastIntakeSnapshot(String snapshot) {
        prefs.edit().putString(KEY_LAST_INTAKE_SNAPSHOT, snapshot).apply();
    }

    public String getCurrentDayTracker() {
        return prefs.getString(KEY_CURRENT_DAY_TRACKER, "");
    }

    public void setCurrentDayTracker(String dateStr) {
        prefs.edit().putString(KEY_CURRENT_DAY_TRACKER, dateStr).apply();
    }

    private static final String KEY_MED_TAKEN_PREFIX = "med_taken_date_";
    private static final String KEY_V1011_RENEWED_FIX = "v1011_renewed_fix_applied";

    public String getMedicationTakenDate(String medKey) {
        return prefs.getString(KEY_MED_TAKEN_PREFIX + medKey, "");
    }

    public void setMedicationTakenDate(String medKey, String dateStr) {
        prefs.edit().putString(KEY_MED_TAKEN_PREFIX + medKey, dateStr).apply();
    }

    public boolean isV1011RenewedFixApplied() {
        return prefs.getBoolean(KEY_V1011_RENEWED_FIX, false);
    }

    public void setV1011RenewedFixApplied(boolean applied) {
        prefs.edit().putBoolean(KEY_V1011_RENEWED_FIX, applied).apply();
    }
}
