package com.vestigia.app.items;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vestigia.app.R;
import com.vestigia.app.models.FoundItem;

import java.util.List;

public class FoundItemAdapter extends RecyclerView.Adapter<FoundItemAdapter.ViewHolder> {

    private final List<FoundItem> items;

    public FoundItemAdapter(List<FoundItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_found_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoundItem item = items.get(position);
        holder.tvItemName.setText(item.getItemName());
        holder.tvLocation.setText(item.getLocation() + " • " + item.getDateFound());
        holder.tvStatus.setText(item.getStatus());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<FoundItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvLocation, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
