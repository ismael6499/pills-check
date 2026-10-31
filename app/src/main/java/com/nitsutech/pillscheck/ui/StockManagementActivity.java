package com.nitsutech.pillscheck.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.nitsutech.pillscheck.receiver.NotificationActionReceiver;
import com.nitsutech.pillscheck.util.LocaleHelper;
import com.nitsutech.pillscheck.util.PreferenceHelper;

import java.util.List;
import java.util.concurrent.Executors;

public class StockManagementActivity extends AppCompatActivity {

    private StockManagementAdapter adapter;
    private AppDatabase db;

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadStockData();
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
        PreferenceHelper prefs = new PreferenceHelper(this);
        LocaleHelper.updateAppConfiguration(this, prefs.getAppLanguage());

        setContentView(R.layout.activity_stock_management);

        db = AppDatabase.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btn_back_stock);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_stock_management);
        adapter = new StockManagementAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        loadStockData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NotificationActionReceiver.ACTION_DATA_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dataReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dataReceiver, filter);
        }
        loadStockData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(dataReceiver);
        } catch (Exception ignored) {}
    }

    private void loadStockData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MedicationEntity> list = db.medicationDao().getAllMedications();
            runOnUiThread(() -> adapter.setItems(list));
        });
    }
}
