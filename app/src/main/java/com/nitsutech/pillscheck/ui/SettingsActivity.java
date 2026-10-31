package com.nitsutech.pillscheck.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.nitsutech.pillscheck.util.AlarmScheduler;
import com.nitsutech.pillscheck.util.GoogleTasksManager;
import com.nitsutech.pillscheck.util.LocaleHelper;
import com.nitsutech.pillscheck.util.PreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceHelper prefs;
    private GoogleTasksManager tasksManager;

    private TextView tvFirstReminderVal;
    private TextView tvRepeatIntervalVal;
    private TextView tvCutoffTimeVal;
    private TextView tvLowStockDaysVal;
    private SwitchMaterial switchSilenceToday;
    private RadioGroup rgLanguage;
    private RadioButton rbLangEn;
    private RadioButton rbLangEs;

    // Google Tasks Views
    private SwitchMaterial switchTasksSync;
    private LinearLayout layoutTasksContainer;
    private TextView tvTasksAccountStatus;
    private MaterialButton btnTasksConnect;
    private RadioGroup rgTasksMode;
    private RadioButton rbTasksModeSingle;
    private RadioButton rbTasksModeIndividual;
    private LinearLayout layoutTasksSingleMode;
    private EditText etTasksSingleName;
    private LinearLayout layoutTasksIndividualMode;
    private LinearLayout containerMedTaskRows;
    private MaterialButton btnTasksSyncNow;

    @Override
    protected void attachBaseContext(Context newBase) {
        PreferenceHelper p = new PreferenceHelper(newBase);
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, p.getAppLanguage()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferenceHelper(this);
        tasksManager = GoogleTasksManager.getInstance(this);
        LocaleHelper.updateAppConfiguration(this, prefs.getAppLanguage());

        setContentView(R.layout.activity_settings);

        initViews();
        loadValues();
        setupGoogleTasksSection();
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

        // Google Tasks Section Views
        switchTasksSync = findViewById(R.id.switch_tasks_sync);
        layoutTasksContainer = findViewById(R.id.layout_tasks_container);
        tvTasksAccountStatus = findViewById(R.id.tv_tasks_account_status);
        btnTasksConnect = findViewById(R.id.btn_tasks_connect);
        rgTasksMode = findViewById(R.id.rg_tasks_mode);
        rbTasksModeSingle = findViewById(R.id.rb_tasks_mode_single);
        rbTasksModeIndividual = findViewById(R.id.rb_tasks_mode_individual);
        layoutTasksSingleMode = findViewById(R.id.layout_tasks_single_mode);
        etTasksSingleName = findViewById(R.id.et_tasks_single_name);
        layoutTasksIndividualMode = findViewById(R.id.layout_tasks_individual_mode);
        containerMedTaskRows = findViewById(R.id.container_med_task_rows);
        btnTasksSyncNow = findViewById(R.id.btn_tasks_sync_now);

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

    private void setupGoogleTasksSection() {
        boolean isEnabled = prefs.isGoogleTasksEnabled();
        switchTasksSync.setChecked(isEnabled);
        layoutTasksContainer.setVisibility(isEnabled ? View.VISIBLE : View.GONE);

        switchTasksSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setGoogleTasksEnabled(isChecked);
            layoutTasksContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        updateGoogleTasksAccountUI();

        btnTasksConnect.setOnClickListener(v -> {
            if (tasksManager.isConnected()) {
                tasksManager.signOut(this, this::updateGoogleTasksAccountUI);
            } else {
                Intent signInIntent = tasksManager.getGoogleSignInClient(this).getSignInIntent();
                startActivityForResult(signInIntent, GoogleTasksManager.RC_SIGN_IN);
            }
        });

        String currentMode = prefs.getGoogleTasksMode();
        if (PreferenceHelper.SYNC_MODE_INDIVIDUAL.equals(currentMode)) {
            rbTasksModeIndividual.setChecked(true);
            layoutTasksSingleMode.setVisibility(View.GONE);
            layoutTasksIndividualMode.setVisibility(View.VISIBLE);
        } else {
            rbTasksModeSingle.setChecked(true);
            layoutTasksSingleMode.setVisibility(View.VISIBLE);
            layoutTasksIndividualMode.setVisibility(View.GONE);
        }

        rgTasksMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_tasks_mode_individual) {
                prefs.setGoogleTasksMode(PreferenceHelper.SYNC_MODE_INDIVIDUAL);
                layoutTasksSingleMode.setVisibility(View.GONE);
                layoutTasksIndividualMode.setVisibility(View.VISIBLE);
            } else {
                prefs.setGoogleTasksMode(PreferenceHelper.SYNC_MODE_SINGLE);
                layoutTasksSingleMode.setVisibility(View.VISIBLE);
                layoutTasksIndividualMode.setVisibility(View.GONE);
            }
        });

        etTasksSingleName.setText(prefs.getGlobalGoogleTaskName());
        etTasksSingleName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                prefs.setGlobalGoogleTaskName(s.toString().trim());
            }
        });

        populateIndividualMedicationRows();

        btnTasksSyncNow.setOnClickListener(v -> {
            btnTasksSyncNow.setEnabled(false);
            Toast.makeText(this, R.string.settings_google_tasks_syncing, Toast.LENGTH_SHORT).show();
            tasksManager.syncFromGoogleTasks((success, message) -> {
                btnTasksSyncNow.setEnabled(true);
                if (success) {
                    Toast.makeText(SettingsActivity.this, R.string.settings_google_tasks_sync_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, getString(R.string.settings_google_tasks_sync_error) + ": " + message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void updateGoogleTasksAccountUI() {
        if (tasksManager.isConnected()) {
            tvTasksAccountStatus.setText(getString(R.string.settings_google_tasks_connected, tasksManager.getConnectedAccountEmail()));
            tvTasksAccountStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            btnTasksConnect.setText(R.string.settings_google_tasks_btn_disconnect);
            btnTasksConnect.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.bg_card_secondary));
        } else {
            tvTasksAccountStatus.setText(R.string.settings_google_tasks_not_connected);
            tvTasksAccountStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            btnTasksConnect.setText(R.string.settings_google_tasks_btn_connect);
            btnTasksConnect.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        }
    }

    private void populateIndividualMedicationRows() {
        containerMedTaskRows.removeAllViews();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MedicationEntity> meds = AppDatabase.getInstance(this).medicationDao().getAllMedications();
            runOnUiThread(() -> {
                for (MedicationEntity med : meds) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(0, 8, 0, 8);

                    TextView tvMedLabel = new TextView(this);
                    tvMedLabel.setText(med.getName());
                    tvMedLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    tvMedLabel.setTextSize(13);
                    tvMedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                    row.addView(tvMedLabel);

                    EditText etMedTask = new EditText(this);
                    etMedTask.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (42 * getResources().getDisplayMetrics().density)
                    ));
                    etMedTask.setBackgroundResource(R.drawable.bg_badge_rounded);
                    etMedTask.setPadding((int) (10 * getResources().getDisplayMetrics().density), 0, (int) (10 * getResources().getDisplayMetrics().density), 0);
                    etMedTask.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    etMedTask.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    etMedTask.setTextSize(13);
                    etMedTask.setHint(med.getName());
                    etMedTask.setText(prefs.getMedicationGoogleTaskName(med.getKey(), med.getName()));

                    etMedTask.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override
                        public void afterTextChanged(Editable s) {
                            prefs.setMedicationGoogleTaskName(med.getKey(), s.toString().trim());
                        }
                    });

                    row.addView(etMedTask);
                    containerMedTaskRows.addView(row);
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GoogleTasksManager.RC_SIGN_IN) {
            boolean success = tasksManager.handleSignInResult(data);
            if (success) {
                updateGoogleTasksAccountUI();
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.settings_google_tasks_sync_error, Toast.LENGTH_LONG).show();
            }
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
