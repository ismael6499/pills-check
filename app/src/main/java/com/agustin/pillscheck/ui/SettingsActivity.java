package com.agustin.pillscheck.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agustin.pillscheck.R;
import com.agustin.pillscheck.util.AlarmScheduler;
import com.agustin.pillscheck.util.LocaleHelper;
import com.agustin.pillscheck.util.PreferenceHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceHelper prefs;

    private TextView tvFirstReminderVal;
    private TextView tvRepeatIntervalVal;
    private TextView tvCutoffTimeVal;
    private TextView tvLowStockDaysVal;
    private SwitchMaterial switchSilenceToday;
    private RadioGroup rgLanguage;
    private RadioButton rbLangEn;
    private RadioButton rbLangEs;

    @Override
    protected void attachBaseContext(Context newBase) {
        PreferenceHelper p = new PreferenceHelper(newBase);
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, p.getAppLanguage()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferenceHelper(this);
        LocaleHelper.updateAppConfiguration(this, prefs.getAppLanguage());

        setContentView(R.layout.activity_settings);

        initViews();
        loadValues();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back_settings);
        btnBack.setOnClickListener(v -> finish());

        RelativeLayout rowFirstReminder = findViewById(R.id.row_first_reminder);
        RelativeLayout rowRepeatInterval = findViewById(R.id.row_repeat_interval);
        RelativeLayout rowCutoffTime = findViewById(R.id.row_cutoff_time);
        RelativeLayout rowLowStockDays = findViewById(R.id.row_low_stock_days);

        tvFirstReminderVal = findViewById(R.id.tv_first_reminder_val);
        tvRepeatIntervalVal = findViewById(R.id.tv_repeat_interval_val);
        tvCutoffTimeVal = findViewById(R.id.tv_cutoff_time_val);
        tvLowStockDaysVal = findViewById(R.id.tv_low_stock_days_val);
        switchSilenceToday = findViewById(R.id.switch_silence_today);

        rgLanguage = findViewById(R.id.rg_language);
        rbLangEn = findViewById(R.id.rb_lang_en);
        rbLangEs = findViewById(R.id.rb_lang_es);

        rowFirstReminder.setOnClickListener(v -> showTimePicker(true));
        rowCutoffTime.setOnClickListener(v -> showTimePicker(false));
        rowRepeatInterval.setOnClickListener(v -> showRepeatIntervalPicker());
        rowLowStockDays.setOnClickListener(v -> showLowStockDaysPicker());

        switchSilenceToday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            if (isChecked) {
                prefs.setSilencedDate(todayStr);
            } else {
                prefs.setSilencedDate("");
            }
            AlarmScheduler.scheduleNextReminder(SettingsActivity.this);
        });

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedLang = (checkedId == R.id.rb_lang_es) ? "es" : "en";
            if (!selectedLang.equals(prefs.getAppLanguage())) {
                prefs.setAppLanguage(selectedLang);
                LocaleHelper.updateAppConfiguration(this, selectedLang);
                // Restart activity
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
    }

    private void loadValues() {
        int rHour = prefs.getReminderHour();
        int rMin = prefs.getReminderMinute();
        tvFirstReminderVal.setText(String.format(Locale.US, "%02d:%02d", rHour, rMin));

        int interval = prefs.getRepeatIntervalMinutes();
        tvRepeatIntervalVal.setText(interval + " min");

        int cHour = prefs.getCutoffHour();
        int cMin = prefs.getCutoffMinute();
        tvCutoffTimeVal.setText(String.format(Locale.US, "%02d:%02d", cHour, cMin));

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        switchSilenceToday.setChecked(prefs.isSilencedForDate(todayStr));

        tvLowStockDaysVal.setText(prefs.getLowStockDaysThreshold() + " days");

        String currentLang = prefs.getAppLanguage();
        if ("es".equalsIgnoreCase(currentLang)) {
            rbLangEs.setChecked(true);
        } else {
            rbLangEn.setChecked(true);
        }
    }

    private void showTimePicker(boolean isReminderTime) {
        int initialHour = isReminderTime ? prefs.getReminderHour() : prefs.getCutoffHour();
        int initialMin = isReminderTime ? prefs.getReminderMinute() : prefs.getCutoffMinute();

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if (isReminderTime) {
                prefs.setReminderHour(hourOfDay);
                prefs.setReminderMinute(minute);
                tvFirstReminderVal.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
            } else {
                prefs.setCutoffHour(hourOfDay);
                prefs.setCutoffMinute(minute);
                tvCutoffTimeVal.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
            }
            AlarmScheduler.scheduleNextReminder(SettingsActivity.this);
        }, initialHour, initialMin, true);
        dialog.show();
    }

    private void showRepeatIntervalPicker() {
        String[] intervals = new String[]{"15 min", "30 min", "60 min (1h)", "120 min (2h)"};
        int[] minutes = new int[]{15, 30, 60, 120};
        int selectedIndex = 2; // default 60 min
        int current = prefs.getRepeatIntervalMinutes();
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] == current) selectedIndex = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_repeat_interval)
                .setSingleChoiceItems(intervals, selectedIndex, (dialog, which) -> {
                    int chosen = minutes[which];
                    prefs.setRepeatIntervalMinutes(chosen);
                    tvRepeatIntervalVal.setText(chosen + " min");
                    AlarmScheduler.scheduleNextReminder(SettingsActivity.this);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showLowStockDaysPicker() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(prefs.getLowStockDaysThreshold()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_low_stock_days)
                .setView(input)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    String str = input.getText().toString().trim();
                    if (!str.isEmpty()) {
                        try {
                            int days = Math.max(1, Integer.parseInt(str));
                            prefs.setLowStockDaysThreshold(days);
                            tvLowStockDaysVal.setText(days + " days");
                            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException ignored) {
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
