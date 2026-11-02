package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemAdminVideoBinding;
import com.nhom02.streamwire.model.Video;

import java.util.ArrayList;
import java.util.List;

public class AdminVideoAdapter extends RecyclerView.Adapter<AdminVideoAdapter.ViewHolder> {

    public interface OnAdminVideoActionListener {
        void onEdit(Video video, int position);
        void onToggleStatus(Video video, int position);
        void onDelete(Video video, int position);
    }

    private final List<Video> videoList = new ArrayList<>();
    private final OnAdminVideoActionListener listener;

    public AdminVideoAdapter(OnAdminVideoActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Video> list) {
        this.videoList.clear();
        if (list != null) {
            this.videoList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < videoList.size()) {
            videoList.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminVideoBinding binding = ItemAdminVideoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Video video = videoList.get(position);

        holder.binding.tvAdminVideoId.setText("ID: #" + (position + 1));
        holder.binding.tvAdminVideoTitle.setText(video.getTitle());
        holder.binding.tvAdminVideoMeta.setText(video.getCategory() + " • " + video.getViews());

        // Status badge
        String status = video.getStatus();
        if ("PENDING".equalsIgnoreCase(status)) {
            holder.binding.tvAdminVideoStatus.setText("Chờ duyệt");
            holder.binding.tvAdminVideoStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_yellow));
            holder.binding.btnAdminToggleStatus.setText("Duyệt ngay");
            holder.binding.btnAdminToggleStatus.setIconResource(R.drawable.ic_visibility);
        } else if ("REPORTED".equalsIgnoreCase(status)) {
            holder.binding.tvAdminVideoStatus.setText("Báo cáo vi phạm");
            holder.binding.tvAdminVideoStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_red));
            holder.binding.btnAdminToggleStatus.setText("Khóa video");
            holder.binding.btnAdminToggleStatus.setIconResource(R.drawable.ic_lock);
        } else {
            holder.binding.tvAdminVideoStatus.setText("Đang hiển thị");
            holder.binding.tvAdminVideoStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            holder.binding.btnAdminToggleStatus.setText("Ẩn / Khóa");
            holder.binding.btnAdminToggleStatus.setIconResource(R.drawable.ic_visibility_off);
        }

        Glide.with(holder.itemView.getContext())
                .load(video.getThumbnailUrl())
                .placeholder(R.drawable.bg_search_input)
                .error(R.drawable.ic_play_circle)
                .into(holder.binding.ivAdminVideoThumb);

        holder.binding.btnAdminEditVideo.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(video, holder.getAdapterPosition());
        });

        holder.binding.btnAdminToggleStatus.setOnClickListener(v -> {
            if (listener != null) listener.onToggleStatus(video, holder.getAdapterPosition());
        });

        holder.binding.btnAdminDeleteVideo.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(video, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminVideoBinding binding;

        ViewHolder(ItemAdminVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

