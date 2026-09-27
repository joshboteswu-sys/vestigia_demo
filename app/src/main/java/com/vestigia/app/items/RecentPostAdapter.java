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
import com.vestigia.app.models.RecentPost;

import java.util.List;

public class RecentPostAdapter extends RecyclerView.Adapter<RecentPostAdapter.ViewHolder> {

    private final List<RecentPost> posts;

    public RecentPostAdapter(List<RecentPost> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_post_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentPost post = posts.get(position);
        holder.tvBadge.setText(post.getItemType().toUpperCase());
        boolean isFound = post.getItemType().equals("Found");
        holder.tvBadge.setBackgroundResource(R.drawable.bg_status_pill);
        holder.tvBadge.setTextColor(isFound ? 0xFF0F766E : 0xFFEF4444);

        holder.tvItemName.setText(post.getItemName());
        holder.tvLocation.setText(post.getLocation());
        holder.tvReporter.setText("By " + post.getReporterName());

        Glide.with(holder.itemView.getContext())
                .load(post.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivImage);

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if (isFound) {
                intent = new Intent(v.getContext(), ItemDetailActivity.class);
            } else {
                intent = new Intent(v.getContext(), LostItemDetailActivity.class);
            }
            intent.putExtra("item_id", post.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updateData(List<RecentPost> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvBadge, tvItemName, tvLocation, tvReporter;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvReporter = itemView.findViewById(R.id.tvReporter);
        }
    }
}