package com.nitsutech.pillscheck.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nitsutech.pillscheck.R;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.ViewHolder> {

    public interface OnStockChangeListener {
        void onStockChanged();
    }

    private final Context context;
    private final List<MedicationEntity> items = new ArrayList<>();
    private final OnStockChangeListener stockListener;

    public MedicationAdapter(Context context, OnStockChangeListener stockListener) {
        this.context = context;
        this.stockListener = stockListener;
    }

    public void setItems(List<MedicationEntity> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<MedicationEntity> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicationEntity med = items.get(position);
        holder.bind(med);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvSub;
        private final TextView tvBadge;
        private final TextView tvYesterdayPost;
        private final BlisterCanvasView blisterCanvas;
        private final TextView tvReserveInfo;
        private final TextView tvSupplyDays;
        private final MaterialButton btnOpenBlister;
        private final MaterialButton btnAddBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_med_name);
            tvSub = itemView.findViewById(R.id.tv_med_sub);
            tvBadge = itemView.findViewById(R.id.tv_med_badge);
            tvYesterdayPost = itemView.findViewById(R.id.tv_yesterday_post);
            blisterCanvas = itemView.findViewById(R.id.blister_canvas);
            tvReserveInfo = itemView.findViewById(R.id.tv_reserve_info);
            tvSupplyDays = itemView.findViewById(R.id.tv_supply_days);
            btnOpenBlister = itemView.findViewById(R.id.btn_open_blister);
            btnAddBox = itemView.findViewById(R.id.btn_add_box);
        }

        public void bind(MedicationEntity med) {
            tvName.setText(med.getName());
            tvSub.setText(med.getSubtitle());

            int activeCount = med.getActivePillsCount();
            tvBadge.setText(context.getString(R.string.label_active_blister, activeCount, med.getBlisterCapacity()));

            if (med.getYesterdayCount() > 0) {
                tvYesterdayPost.setText(context.getString(R.string.label_yesterday_post, med.getYesterdayCount()));
            } else {
                tvYesterdayPost.setText(context.getString(R.string.label_yesterday_none));
            }

            int reserveTotal = (med.getReserveBlisters() * med.getBlisterCapacity()) + med.getReserveLoosePills();
            tvReserveInfo.setText(context.getString(R.string.label_reserve_stock, med.getReserveBlisters(), reserveTotal));

            int totalDays = med.getTotalDaysSupply();
            tvSupplyDays.setText(context.getString(R.string.label_total_supply, totalDays));

            blisterCanvas.configure(med.getRows(), med.getCols(), med.getBlisterType(), med.getSlotsArray());

            // Highlight next pill to be taken today with a subtle thin border
            int nextSlotToTake = -1;
            String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
            com.nitsutech.pillscheck.util.PreferenceHelper prefs = new com.nitsutech.pillscheck.util.PreferenceHelper(context);
            if (!prefs.isTakenForDate(todayStr)) {
                boolean[] slots = med.getSlotsArray();
                for (int i = slots.length - 1; i >= 0; i--) {
                    if (slots[i]) {
                        nextSlotToTake = i;
                        break;
                    }
                }
            }
            blisterCanvas.setHighlightedSlotIndex(nextSlotToTake);

            blisterCanvas.setOnSlotClickListener((slotIndex, isFilled, remainingCount) -> {
                boolean[] currentSlots = blisterCanvas.getSlotStates();
                med.setSlotsArray(currentSlots);

                tvBadge.setText(context.getString(R.string.label_active_blister, remainingCount, med.getBlisterCapacity()));
                tvSupplyDays.setText(context.getString(R.string.label_total_supply, med.getTotalDaysSupply()));

                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getInstance(context).medicationDao().update(med);
                });

                if (stockListener != null) {
                    stockListener.onStockChanged();
                }
            });

            btnOpenBlister.setOnClickListener(v -> showOpenBlisterDialog(med));
            btnAddBox.setOnClickListener(v -> showAddStockDialog(med));
        }

        private void showOpenBlisterDialog(MedicationEntity med) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_open_blister_title)
                    .setMessage(context.getString(R.string.dialog_open_blister_msg, med.getBlisterCapacity(), med.getName()))
                    .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                        if (med.getReserveBlisters() > 0) {
                            med.setReserveBlisters(med.getReserveBlisters() - 1);
                        }
                        boolean[] newSlots = new boolean[med.getBlisterCapacity()];
                        for (int i = 0; i < newSlots.length; i++) {
                            newSlots[i] = true;
                        }
                        med.setSlotsArray(newSlots);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            AppDatabase.getInstance(context).medicationDao().update(med);
                        });

                        notifyItemChanged(getAdapterPosition());
                        if (stockListener != null) {
                            stockListener.onStockChanged();
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        }

        private void showAddStockDialog(MedicationEntity med) {
            String[] options = new String[]{
                    context.getString(R.string.dialog_add_one_box, med.getStandardBoxPills()),
                    context.getString(R.string.dialog_add_custom)
            };

            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.dialog_add_stock_title, med.getName()))
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Add 1 standard box
                            int blistersInBox = Math.max(1, med.getStandardBoxPills() / med.getBlisterCapacity());
                            med.setReserveBlisters(med.getReserveBlisters() + blistersInBox);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                AppDatabase.getInstance(context).medicationDao().update(med);
                            });
                            notifyItemChanged(getAdapterPosition());
                            if (stockListener != null) stockListener.onStockChanged();
                        } else {
                            // Custom amount input
                            promptCustomAmount(med);
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        }

        private void promptCustomAmount(MedicationEntity med) {
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint(R.string.dialog_custom_pills_hint);

            new AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_add_custom)
                    .setView(input)
                    .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                        String text = input.getText().toString().trim();
                        if (!text.isEmpty()) {
                            try {
                                int pills = Integer.parseInt(text);
                                int addedBlisters = pills / med.getBlisterCapacity();
                                int addedLoose = pills % med.getBlisterCapacity();
                                med.setReserveBlisters(med.getReserveBlisters() + addedBlisters);
                                med.setReserveLoosePills(med.getReserveLoosePills() + addedLoose);

                                Executors.newSingleThreadExecutor().execute(() -> {
                                    AppDatabase.getInstance(context).medicationDao().update(med);
                                });
                                notifyItemChanged(getAdapterPosition());
                                if (stockListener != null) stockListener.onStockChanged();
                            } catch (NumberFormatException e) {
                                Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        }
    }
}
