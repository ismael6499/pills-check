package com.nitsutech.pillscheck.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.nitsutech.pillscheck.receiver.NotificationActionReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class StockManagementAdapter extends RecyclerView.Adapter<StockManagementAdapter.ViewHolder> {

    private final Context context;
    private final List<MedicationEntity> items = new ArrayList<>();

    public StockManagementAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<MedicationEntity> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_management_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvSub;
        private final TextView tvTotalDays;
        private final TextView tvActiveInfo;
        private final MaterialButton btnOpenBlister;
        private final TextView tvBoxDetail;
        private final TextView tvBoxCount;
        private final MaterialButton btnMinusBox;
        private final MaterialButton btnPlusBox;
        private final TextView tvBlisterTotalPills;
        private final TextView tvBlisterCount;
        private final MaterialButton btnMinusBlister;
        private final MaterialButton btnPlusBlister;
        private final MaterialButton btnDirectEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_stock_med_name);
            tvSub = itemView.findViewById(R.id.tv_stock_med_sub);
            tvTotalDays = itemView.findViewById(R.id.tv_stock_total_days);
            tvActiveInfo = itemView.findViewById(R.id.tv_stock_active_info);
            btnOpenBlister = itemView.findViewById(R.id.btn_stock_open_blister);
            tvBoxDetail = itemView.findViewById(R.id.tv_stock_box_detail);
            tvBoxCount = itemView.findViewById(R.id.tv_box_count);
            btnMinusBox = itemView.findViewById(R.id.btn_minus_box);
            btnPlusBox = itemView.findViewById(R.id.btn_plus_box);
            tvBlisterTotalPills = itemView.findViewById(R.id.tv_stock_blister_total_pills);
            tvBlisterCount = itemView.findViewById(R.id.tv_blister_count);
            btnMinusBlister = itemView.findViewById(R.id.btn_minus_blister);
            btnPlusBlister = itemView.findViewById(R.id.btn_plus_blister);
            btnDirectEdit = itemView.findViewById(R.id.btn_stock_direct_edit);
        }

        public void bind(MedicationEntity med) {
            tvName.setText(med.getName());
            tvSub.setText(med.getSubtitle());

            updateDisplay(med);

            int blistersPerBox = Math.max(1, med.getStandardBoxPills() / med.getBlisterCapacity());

            btnPlusBox.setOnClickListener(v -> {
                med.setReserveBlisters(med.getReserveBlisters() + blistersPerBox);
                saveAndNotify(med);
            });

            btnMinusBox.setOnClickListener(v -> {
                int next = Math.max(0, med.getReserveBlisters() - blistersPerBox);
                med.setReserveBlisters(next);
                saveAndNotify(med);
            });

            btnPlusBlister.setOnClickListener(v -> {
                med.setReserveBlisters(med.getReserveBlisters() + 1);
                saveAndNotify(med);
            });

            btnMinusBlister.setOnClickListener(v -> {
                int next = Math.max(0, med.getReserveBlisters() - 1);
                med.setReserveBlisters(next);
                saveAndNotify(med);
            });

            btnOpenBlister.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_open_blister_title)
                        .setMessage(context.getString(R.string.dialog_open_blister_msg, med.getBlisterCapacity(), med.getName()))
                        .setPositiveButton(R.string.dialog_open_blister_taken, (dialog, which) -> {
                            openReserveBlister(med, true);
                        })
                        .setNeutralButton(R.string.dialog_open_blister_pending, (dialog, which) -> {
                            openReserveBlister(med, false);
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
            });

            btnDirectEdit.setOnClickListener(v -> showDirectEditDialog(med));
        }

        private void updateDisplay(MedicationEntity med) {
            tvTotalDays.setText(context.getString(R.string.label_total_supply, med.getTotalDaysSupply()));
            tvActiveInfo.setText(context.getString(R.string.stock_active_status, med.getActivePillsCount(), med.getBlisterCapacity()));

            int blistersPerBox = Math.max(1, med.getStandardBoxPills() / med.getBlisterCapacity());
            tvBoxDetail.setText(context.getString(R.string.stock_box_detail_format, blistersPerBox, med.getStandardBoxPills()));

            int estimatedBoxes = med.getReserveBlisters() / blistersPerBox;
            tvBoxCount.setText(estimatedBoxes + " box");

            int totalReservePills = (med.getReserveBlisters() * med.getBlisterCapacity()) + med.getReserveLoosePills();
            tvBlisterCount.setText(med.getReserveBlisters() + " bl.");
            tvBlisterTotalPills.setText("Total: " + totalReservePills + " pills");
        }

        private void saveAndNotify(MedicationEntity med) {
            updateDisplay(med);
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(context).medicationDao().update(med);
                Intent changedIntent = new Intent(NotificationActionReceiver.ACTION_DATA_CHANGED);
                changedIntent.setPackage(context.getPackageName());
                context.sendBroadcast(changedIntent);
            });
        }

        private void openReserveBlister(MedicationEntity med, boolean alreadyTakenToday) {
            if (med.getReserveBlisters() > 0) {
                med.setReserveBlisters(med.getReserveBlisters() - 1);
            }
            boolean[] newSlots = new boolean[med.getBlisterCapacity()];
            java.util.Arrays.fill(newSlots, true);
            med.setSlotsArray(newSlots);
            med.setYesterdayCount(med.getBlisterCapacity());

            com.nitsutech.pillscheck.util.PreferenceHelper prefs = new com.nitsutech.pillscheck.util.PreferenceHelper(context);
            String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
            if (alreadyTakenToday) {
                prefs.setMedicationTakenDate(med.getKey(), todayStr);
            } else {
                prefs.setMedicationTakenDate(med.getKey(), "");
            }
            saveAndNotify(med);
        }

        private void showDirectEditDialog(MedicationEntity med) {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (18 * context.getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad / 2, pad, pad / 2);

            TextView tvBlisterLabel = new TextView(context);
            tvBlisterLabel.setText(R.string.dialog_exact_blisters_hint);
            tvBlisterLabel.setTextSize(13);
            layout.addView(tvBlisterLabel);

            final EditText etBlisters = new EditText(context);
            etBlisters.setInputType(InputType.TYPE_CLASS_NUMBER);
            etBlisters.setText(String.valueOf(med.getReserveBlisters()));
            etBlisters.setSelection(etBlisters.getText().length());
            layout.addView(etBlisters);

            TextView tvLooseLabel = new TextView(context);
            tvLooseLabel.setText(R.string.dialog_exact_loose_hint);
            tvLooseLabel.setTextSize(13);
            tvLooseLabel.setPadding(0, pad / 2, 0, 0);
            layout.addView(tvLooseLabel);

            final EditText etLoose = new EditText(context);
            etLoose.setInputType(InputType.TYPE_CLASS_NUMBER);
            etLoose.setText(String.valueOf(med.getReserveLoosePills()));
            etLoose.setSelection(etLoose.getText().length());
            layout.addView(etLoose);

            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.dialog_exact_stock_title, med.getName()))
                    .setView(layout)
                    .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                        String bStr = etBlisters.getText().toString().trim();
                        String lStr = etLoose.getText().toString().trim();
                        try {
                            if (!bStr.isEmpty()) med.setReserveBlisters(Math.max(0, Integer.parseInt(bStr)));
                            if (!lStr.isEmpty()) med.setReserveLoosePills(Math.max(0, Integer.parseInt(lStr)));
                            saveAndNotify(med);
                        } catch (NumberFormatException ignored) {}
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        }
    }
}
