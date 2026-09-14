package com.vestigia.app.items;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vestigia.app.R;
import com.vestigia.app.models.LostItem;

import java.util.List;

public class LostItemAdapter extends RecyclerView.Adapter<LostItemAdapter.ViewHolder> {

    private final List<LostItem> items;

    public LostItemAdapter(List<LostItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lost_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LostItem item = items.get(position);
        holder.tvItemName.setText(item.getItemName());
        holder.tvLocation.setText(item.getLastSeenLocation() + " • " + item.getDateLost());
        holder.tvStatus.setText(item.getStatus());

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), LostItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<LostItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvLocation, tvStatus;
        ImageView ivThumb;

        ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivThumb = itemView.findViewById(R.id.ivThumb);
        }
    }
}