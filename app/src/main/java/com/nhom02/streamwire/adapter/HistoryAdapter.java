package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemWatchHistoryBinding;
import com.nhom02.streamwire.model.WatchHistory;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnHistoryActionListener {
        void onResumeWatch(WatchHistory history);
        void onDeleteItem(WatchHistory history, int position);
    }

    private final List<WatchHistory> historyList = new ArrayList<>();
    private final OnHistoryActionListener listener;

    public HistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<WatchHistory> list) {
        this.historyList.clear();
        if (list != null) {
            this.historyList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < historyList.size()) {
            historyList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearAll() {
        historyList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWatchHistoryBinding binding = ItemWatchHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchHistory item = historyList.get(position);

        if (item.getVideo() != null) {
            holder.binding.tvHistoryTitle.setText(item.getVideo().getTitle());
            holder.binding.tvHistorySubinfo.setText(item.getEpisodeTitle() + " • " + item.getVideo().getCategory());
            holder.binding.tvHistoryDuration.setText(item.getVideo().getDuration() != null ? item.getVideo().getDuration() : "45:00");

            Glide.with(holder.itemView.getContext())
                    .load(item.getVideo().getThumbnailUrl())
                    .placeholder(R.drawable.bg_search_input)
                    .error(R.drawable.ic_play_circle)
                    .into(holder.binding.ivHistoryThumb);
        }

        holder.binding.tvHistoryTimestamp.setText(item.getUpdatedAt());

        int percent = item.getProgressPercent();
        holder.binding.tvProgressPercent.setText("Đã xem " + percent + "%");
        holder.binding.tvProgressTime.setText(item.getProgressTimeText());
        holder.binding.progressHistory.setProgress(percent);

        holder.binding.btnResumePlay.setOnClickListener(v -> {
            if (listener != null) listener.onResumeWatch(item);
        });

        holder.binding.btnDeleteSingleHistory.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (listener != null) listener.onDeleteItem(item, pos);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWatchHistoryBinding binding;

        ViewHolder(ItemWatchHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

