package com.agustin.pillscheck.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agustin.pillscheck.R;
import com.agustin.pillscheck.data.AppDatabase;
import com.agustin.pillscheck.data.entity.IntakeRecordEntity;
import com.agustin.pillscheck.data.entity.MedicationEntity;
import com.agustin.pillscheck.receiver.NotificationActionReceiver;
import com.agustin.pillscheck.util.AlarmScheduler;
import com.agustin.pillscheck.util.LocaleHelper;
import com.agustin.pillscheck.util.PreferenceHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements MedicationAdapter.OnStockChangeListener {

    private static final int PERMISSION_REQUEST_NOTIF = 101;

    private TextView tvDate;
    private TextView tvTodayStatusBadge;
    private TextView tvTodayTakenTime;
    private MaterialButton btnMainAction;
    private MaterialButton btnUndoIntake;
    private ExtendedFloatingActionButton fabTakeAll;
    private LinearLayout layoutPharmacyAlert;
    private TextView tvPharmacyDesc;
    private RecyclerView rvMedications;
    private MedicationAdapter adapter;

    private PreferenceHelper prefs;
    private AppDatabase db;

    private final BroadcastReceiver dataChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadDashboardData();
        }
    };

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

        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        initViews();
        setupRecyclerView();
        checkPermissions();
        AlarmScheduler.scheduleNextReminder(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NotificationActionReceiver.ACTION_DATA_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dataChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dataChangeReceiver, filter);
        }
        loadDashboardData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(dataChangeReceiver);
        } catch (Exception ignored) {
        }
    }

    private void initViews() {
        tvDate = findViewById(R.id.tv_dashboard_date);
        tvTodayStatusBadge = findViewById(R.id.tv_today_status_badge);
        tvTodayTakenTime = findViewById(R.id.tv_today_taken_time);
        btnMainAction = findViewById(R.id.btn_main_action);
        btnUndoIntake = findViewById(R.id.btn_undo_intake);
        fabTakeAll = findViewById(R.id.fab_take_all);
        layoutPharmacyAlert = findViewById(R.id.layout_pharmacy_alert);
        tvPharmacyDesc = findViewById(R.id.tv_pharmacy_desc);
        rvMedications = findViewById(R.id.rv_medications);

        ImageButton btnHistory = findViewById(R.id.btn_open_history);
        ImageButton btnSettings = findViewById(R.id.btn_open_settings);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        tvDate.setText(dateFormat.format(new Date()));

        btnHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        btnMainAction.setOnClickListener(v -> handleMainActionClick());
        fabTakeAll.setOnClickListener(v -> handleMainActionClick());
        btnUndoIntake.setOnClickListener(v -> handleUndoIntakeClick());
    }

    private void setupRecyclerView() {
        adapter = new MedicationAdapter(this, this);
        rvMedications.setLayoutManager(new LinearLayoutManager(this));
        rvMedications.setAdapter(adapter);
    }

    private void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.medicationDao().removeDuplicateKeys();
            List<MedicationEntity> meds = db.medicationDao().getAllMedications();

            // Auto-detect if all 3 pills were manually popped:
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            boolean allTakenManually = true;
            for (MedicationEntity med : meds) {
                if (med.getActivePillsCount() >= med.getYesterdayCount()) {
                    allTakenManually = false;
                    break;
                }
            }

            if (allTakenManually && !meds.isEmpty() && !prefs.isTakenForDate(todayStr)) {
                String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                prefs.setLastIntakeDate(todayStr);
                prefs.setLastIntakeTime(timeStr);
                AlarmScheduler.scheduleNextReminder(MainActivity.this);
            } else if (!allTakenManually && prefs.isTakenForDate(todayStr)) {
                // If user put a pill back manually, revert status to pending
                prefs.setLastIntakeDate("");
                prefs.setLastIntakeTime("");
                AlarmScheduler.scheduleNextReminder(MainActivity.this);
            }

            runOnUiThread(() -> {
                adapter.setItems(meds);
                updateIntakeStatusUI();
                evaluatePharmacyAlerts(meds);
            });
        });
    }

    private void updateIntakeStatusUI() {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        boolean taken = prefs.isTakenForDate(todayStr);

        if (taken) {
            tvTodayStatusBadge.setText(R.string.status_taken_all);
            tvTodayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            tvTodayTakenTime.setVisibility(View.VISIBLE);
            tvTodayTakenTime.setText(getString(R.string.status_taken_at, prefs.getLastIntakeTime()));

            btnMainAction.setText(R.string.status_taken_all);
            btnMainAction.setEnabled(false);
            btnMainAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.bg_card_secondary));

            btnUndoIntake.setVisibility(View.VISIBLE);
            if (fabTakeAll != null) fabTakeAll.setVisibility(View.GONE);
        } else {
            tvTodayStatusBadge.setText(R.string.status_pending);
            tvTodayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_amber));
            tvTodayTakenTime.setVisibility(View.GONE);

            btnMainAction.setText(R.string.btn_take_all);
            btnMainAction.setEnabled(true);
            btnMainAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));

            btnUndoIntake.setVisibility(View.GONE);
            if (fabTakeAll != null) fabTakeAll.setVisibility(View.VISIBLE);
        }
    }

    private void handleMainActionClick() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MedicationEntity> meds = db.medicationDao().getAllMedications();
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());

            int folcresRemaining = 0;
            int minoxRemaining = 0;
            int vitDRemaining = 0;

            for (MedicationEntity med : meds) {
                boolean[] slots = med.getSlotsArray();
                // Consume rightmost filled pill
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

            IntakeRecordEntity record = new IntakeRecordEntity(
                    System.currentTimeMillis(),
                    todayStr,
                    timeStr,
                    folcresRemaining,
                    minoxRemaining,
                    vitDRemaining,
                    "Manual Dashboard"
            );
            db.intakeRecordDao().insert(record);

            prefs.setLastIntakeDate(todayStr);
            prefs.setLastIntakeTime(timeStr);

            AlarmScheduler.scheduleNextReminder(MainActivity.this);

            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, R.string.notif_taken_toast, Toast.LENGTH_SHORT).show();
                loadDashboardData();
            });
        });
    }

    private void handleUndoIntakeClick() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_undo_title)
                .setMessage(R.string.dialog_undo_msg)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                        IntakeRecordEntity todayRecord = db.intakeRecordDao().getRecordForDate(todayStr);
                        if (todayRecord != null) {
                            db.intakeRecordDao().delete(todayRecord);
                        }

                        List<MedicationEntity> meds = db.medicationDao().getAllMedications();
                        for (MedicationEntity med : meds) {
                            boolean[] slots = med.getSlotsArray();
                            // Restore first empty slot
                            for (int i = 0; i < slots.length; i++) {
                                if (!slots[i]) {
                                    slots[i] = true;
                                    break;
                                }
                            }
                            med.setSlotsArray(slots);
                            db.medicationDao().update(med);
                        }

                        prefs.setLastIntakeDate("");
                        prefs.setLastIntakeTime("");
                        AlarmScheduler.scheduleNextReminder(MainActivity.this);

                        runOnUiThread(this::loadDashboardData);
                    });
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void evaluatePharmacyAlerts(List<MedicationEntity> meds) {
        int thresholdDays = prefs.getLowStockDaysThreshold();
        MedicationEntity lowestMed = null;
        int minDays = Integer.MAX_VALUE;

        for (MedicationEntity med : meds) {
            int days = med.getTotalDaysSupply();
            if (days < thresholdDays && days < minDays) {
                minDays = days;
                lowestMed = med;
            }
        }

        if (lowestMed != null) {
            layoutPharmacyAlert.setVisibility(View.VISIBLE);
            tvPharmacyDesc.setText(getString(R.string.pharmacy_alert_desc, lowestMed.getName(), minDays));
        } else {
            layoutPharmacyAlert.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStockChanged() {
        loadDashboardData();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIF
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIF) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AlarmScheduler.scheduleNextReminder(this);
            }
        }
    }
}
