package com.example.medirappel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedAdapter extends RecyclerView.Adapter<MedAdapter.VH> {

    public interface OnEdit { void onEdit(Medication m); }
    public interface OnDelete { void onDelete(Medication m); }

    private final List<Medication> items;
    private final OnEdit editListener;
    private final OnDelete deleteListener;

    public MedAdapter(List<Medication> items, OnEdit editListener, OnDelete deleteListener) {
        this.items = items;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, details;
        final ImageButton edit, delete;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            details = v.findViewById(R.id.tvDetails);
            edit = v.findViewById(R.id.btnEdit);
            delete = v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_med, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Medication medication = items.get(position);
        holder.name.setText(medication.name);

        String description = medication.dose == null ? "" : medication.dose.trim();
        String scheduled = holder.itemView.getContext().getString(R.string.item_scheduled);
        holder.details.setText(description.isEmpty() ? scheduled : description + "  •  " + scheduled);

        holder.edit.setOnClickListener(v -> editListener.onEdit(medication));
        holder.delete.setOnClickListener(v -> deleteListener.onDelete(medication));
    }

    @Override
    public int getItemCount() { return items.size(); }
}
