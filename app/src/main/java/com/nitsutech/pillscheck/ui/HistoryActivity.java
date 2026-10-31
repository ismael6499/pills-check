package com.nitsutech.pillscheck.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.IntakeRecordEntity;
import com.nitsutech.pillscheck.util.LocaleHelper;
import com.nitsutech.pillscheck.util.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private HistoryAdapter adapter;

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

        setContentView(R.layout.activity_history);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rv_history);
        tvEmpty = findViewById(R.id.tv_empty_history);

        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<IntakeRecordEntity> records = AppDatabase.getInstance(this).intakeRecordDao().getAllRecords();
            runOnUiThread(() -> {
                if (records == null || records.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                    adapter.setItems(records);
                }
            });
        });
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<IntakeRecordEntity> list = new ArrayList<>();

        public void setItems(List<IntakeRecordEntity> items) {
            list.clear();
            if (items != null) list.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_record, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            IntakeRecordEntity record = list.get(position);
            holder.tvDateTime.setText(getString(R.string.history_taken_format, record.getDateString(), record.getTimeString()));

            String details = getString(R.string.history_pill_detail, "Folcres", record.getFolcresRemaining()) + "\n"
                    + getString(R.string.history_pill_detail, "Minoxidil", record.getMinoxidilRemaining()) + "\n"
                    + getString(R.string.history_pill_detail, "Vitamin D", record.getVitaminDRemaining());
            holder.tvDetails.setText(details);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvDateTime;
            final TextView tvDetails;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDateTime = itemView.findViewById(R.id.tv_history_date_time);
                tvDetails = itemView.findViewById(R.id.tv_history_details);
            }
        }
    }
}
