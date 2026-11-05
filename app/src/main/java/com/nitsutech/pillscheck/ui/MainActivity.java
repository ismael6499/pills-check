package com.nitsutech.pillscheck.ui;

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

import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.IntakeRecordEntity;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.nitsutech.pillscheck.receiver.NotificationActionReceiver;
import com.nitsutech.pillscheck.util.AlarmScheduler;
import com.nitsutech.pillscheck.util.GoogleTasksManager;
import com.nitsutech.pillscheck.util.LocaleHelper;
import com.nitsutech.pillscheck.util.PreferenceHelper;
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
        if (prefs != null && prefs.isGoogleTasksEnabled()) {
            GoogleTasksManager.getInstance(this).syncFromGoogleTasks((success, message) -> {
                if (success) {
                    loadDashboardData();
                }
            });
        }
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

        ImageButton btnStock = findViewById(R.id.btn_open_stock);
        if (btnStock != null) btnStock.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StockManagementActivity.class)));
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
            applyV1011DataFixIfNeeded();
            applyV1013DataFixIfNeeded();
            db.medicationDao().removeDuplicateKeys();
            List<MedicationEntity> meds = db.medicationDao().getAllMedications();

            // Midnight rollover: if a new day arrived and yesterday was taken, update yesterdayCount
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String trackedDay = prefs.getCurrentDayTracker();
            if (trackedDay.isEmpty()) {
                prefs.setCurrentDayTracker(todayDate);
            } else if (!trackedDay.equals(todayDate)) {
                // New day! If yesterday was marked taken, active pills count at end of yesterday becomes yesterdayCount
                if (prefs.isTakenForDate(trackedDay)) {
                    for (MedicationEntity med : meds) {
                        med.setYesterdayCount(med.getActivePillsCount());
                        db.medicationDao().update(med);
                    }
                }
                prefs.setCurrentDayTracker(todayDate);
            }

            // Auto-detect if all pills were manually popped:
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            int pendingCount = 0;
            for (MedicationEntity med : meds) {
                if (med.isPendingToday(prefs, todayStr)) {
                    pendingCount++;
                }
            }
            boolean allTakenManually = (pendingCount == 0 && !meds.isEmpty());

            if (allTakenManually && !prefs.isTakenForDate(todayStr)) {
                String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                prefs.setLastIntakeDate(todayStr);
                prefs.setLastIntakeTime(timeStr);
                AlarmScheduler.scheduleNextReminder(MainActivity.this);
                GoogleTasksManager.getInstance(MainActivity.this).onAllMedicationsTaken();
            } else if (!allTakenManually && prefs.isTakenForDate(todayStr)) {
                // If user put a pill back manually, revert status to pending
                prefs.setLastIntakeDate("");
                prefs.setLastIntakeTime("");
                AlarmScheduler.scheduleNextReminder(MainActivity.this);
            }

            runOnUiThread(() -> {
                adapter.setItems(meds);
                updateIntakeStatusUI(meds);
                evaluatePharmacyAlerts(meds);
            });
        });
    }

    private void applyV1013DataFixIfNeeded() {
        if (!prefs.isV1013FixApplied()) {
            prefs.setV1013FixApplied(true);

            // Folcres: set to 7 active pills, yesterdayCount = 8
            MedicationEntity folcres = db.medicationDao().getMedicationByKey("folcres");
            if (folcres != null) {
                boolean[] slots = folcres.getSlotsArray();
                int count = 0;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i]) {
                        count++;
                        if (count > 7) {
                            slots[i] = false;
                        }
                    }
                }
                folcres.setSlotsArray(slots);
                folcres.setYesterdayCount(8);
                db.medicationDao().update(folcres);
            }

            // Minoxidil: set to 7 active pills, yesterdayCount = 8
            MedicationEntity minox = db.medicationDao().getMedicationByKey("minoxidil");
            if (minox != null) {
                boolean[] slots = minox.getSlotsArray();
                int count = 0;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i]) {
                        count++;
                        if (count > 7) {
                            slots[i] = false;
                        }
                    }
                }
                minox.setSlotsArray(slots);
                minox.setYesterdayCount(8);
                db.medicationDao().update(minox);
            }

            // Vitamin D: set to 7 active pills, yesterdayCount = 8
            MedicationEntity vitd = db.medicationDao().getMedicationByKey("vitamind");
            if (vitd != null) {
                boolean[] slots = vitd.getSlotsArray();
                int count = 0;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i]) {
                        count++;
                        if (count > 7) {
                            slots[i] = false;
                        }
                    }
                }
                vitd.setSlotsArray(slots);
                vitd.setYesterdayCount(8);
                db.medicationDao().update(vitd);
            }

            // Mark today as taken
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
            prefs.setLastIntakeDate(todayStr);
            prefs.setLastIntakeTime(timeStr);
            prefs.setMedicationTakenDate("folcres", todayStr);
            prefs.setMedicationTakenDate("minoxidil", todayStr);
            prefs.setMedicationTakenDate("vitamind", todayStr);

            IntakeRecordEntity todayRecord = new IntakeRecordEntity(
                    System.currentTimeMillis(),
                    todayStr,
                    timeStr,
                    7,
                    7,
                    7,
                    "Notification Action"
            );
            db.intakeRecordDao().insert(todayRecord);
        }
    }

    private void applyV1011DataFixIfNeeded() {
        if (!prefs.isV1011RenewedFixApplied()) {
            MedicationEntity minox = db.medicationDao().getMedicationByKey("minoxidil");
            if (minox != null) {
                boolean[] slots = new boolean[10];
                java.util.Arrays.fill(slots, true);
                minox.setSlotsArray(slots);
                minox.setYesterdayCount(10);
                db.medicationDao().update(minox);
            }
            MedicationEntity vitd = db.medicationDao().getMedicationByKey("vitamind");
            if (vitd != null) {
                boolean[] slots = new boolean[10];
                java.util.Arrays.fill(slots, true);
                vitd.setSlotsArray(slots);
                vitd.setYesterdayCount(10);
                db.medicationDao().update(vitd);
            }
            IntakeRecordEntity recordNov2 = db.intakeRecordDao().getRecordForDate("2026-11-02");
            if (recordNov2 != null) {
                recordNov2.setMinoxidilRemaining(10);
                recordNov2.setVitaminDRemaining(10);
                db.intakeRecordDao().insert(recordNov2);
            }
            prefs.setV1011RenewedFixApplied(true);
        }
    }

    private void updateIntakeStatusUI(List<MedicationEntity> meds) {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        boolean allTakenPref = prefs.isTakenForDate(todayStr);

        int totalMeds = (meds != null) ? meds.size() : 0;
        int pendingCount = 0;
        if (meds != null) {
            for (MedicationEntity med : meds) {
                if (med.isPendingToday(prefs, todayStr)) {
                    pendingCount++;
                }
            }
        }

        boolean allTaken = allTakenPref || (totalMeds > 0 && pendingCount == 0);

        if (allTaken) {
            tvTodayStatusBadge.setText(R.string.status_taken_all);
            tvTodayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            tvTodayTakenTime.setVisibility(View.VISIBLE);
            String takenTime = prefs.getLastIntakeTime();
            if (takenTime == null || takenTime.isEmpty()) {
                takenTime = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
            }
            tvTodayTakenTime.setText(getString(R.string.status_taken_at, takenTime));

            btnMainAction.setText(R.string.status_taken_all);
            btnMainAction.setEnabled(false);
            btnMainAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.bg_card_secondary));

            btnUndoIntake.setVisibility(View.VISIBLE);
            if (fabTakeAll != null) fabTakeAll.setVisibility(View.GONE);
        } else {
            tvTodayStatusBadge.setText(R.string.status_pending);
            tvTodayStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_amber));
            tvTodayTakenTime.setVisibility(View.GONE);

            String actionText;
            if (pendingCount < totalMeds) {
                if (pendingCount == 1) {
                    actionText = getString(R.string.btn_take_remaining_one);
                } else {
                    actionText = getString(R.string.btn_take_remaining, pendingCount);
                }
            } else {
                actionText = getString(R.string.btn_take_all_format, pendingCount);
            }

            btnMainAction.setText(actionText);
            btnMainAction.setEnabled(true);
            btnMainAction.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));

            if (fabTakeAll != null) {
                fabTakeAll.setText(actionText.toUpperCase(Locale.getDefault()));
                fabTakeAll.setVisibility(View.VISIBLE);
            }

            btnUndoIntake.setVisibility(View.GONE);
        }
    }

    private void handleMainActionClick() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MedicationEntity> meds = db.medicationDao().getAllMedications();
            // Save snapshot before intake
            List<String> slotSnapshots = new java.util.ArrayList<>();
            for (MedicationEntity m : meds) {
                slotSnapshots.add(m.getKey() + ":::" + m.getActiveSlotsJson());
            }
            prefs.setLastIntakeSnapshot(new com.google.gson.Gson().toJson(slotSnapshots));
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());

            int folcresRemaining = 0;
            int minoxRemaining = 0;
            int vitDRemaining = 0;

            for (MedicationEntity med : meds) {
                if (med.isPendingToday(prefs, todayStr)) {
                    med.consumeOnePill();
                    db.medicationDao().update(med);
                    prefs.setMedicationTakenDate(med.getKey(), todayStr);
                    GoogleTasksManager.getInstance(MainActivity.this).onMedicationTaken(med);
                }

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
            GoogleTasksManager.getInstance(MainActivity.this).onAllMedicationsTaken();

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
                        String snapshot = prefs.getLastIntakeSnapshot();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            try {
                                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                                List<String> list = new com.google.gson.Gson().fromJson(snapshot, listType);
                                if (list != null) {
                                    for (String item : list) {
                                        String[] parts = item.split(":::");
                                        if (parts.length == 2) {
                                            for (MedicationEntity med : meds) {
                                                if (med.getKey().equals(parts[0])) {
                                                    med.setActiveSlotsJson(parts[1]);
                                                    db.medicationDao().update(med);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                            prefs.setLastIntakeSnapshot("");
                        } else {
                            // Fallback if no snapshot
                            for (MedicationEntity med : meds) {
                                boolean[] slots = med.getSlotsArray();
                                for (int i = 0; i < slots.length; i++) {
                                    if (!slots[i]) {
                                        slots[i] = true;
                                        break;
                                    }
                                }
                                med.setSlotsArray(slots);
                                db.medicationDao().update(med);
                            }
                        }

                        for (MedicationEntity med : meds) { prefs.setMedicationTakenDate(med.getKey(), ""); }
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
