package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemEpisodeBinding;
import com.nhom02.streamwire.model.Episode;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode, int position);
    }

    private final List<Episode> episodes = new ArrayList<>();
    private int currentPlayingPosition = 0;
    private final OnEpisodeClickListener listener;

    public EpisodeAdapter(OnEpisodeClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Episode> list) {
        this.episodes.clear();
        if (list != null) {
            this.episodes.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setCurrentPlayingPosition(int position) {
        int oldPos = currentPlayingPosition;
        currentPlayingPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(currentPlayingPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEpisodeBinding binding = ItemEpisodeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode episode = episodes.get(position);
        boolean isPlaying = (position == currentPlayingPosition);

        holder.binding.tvEpisodeNumber.setText("Tập " + episode.getEpisodeNumber());
        holder.binding.tvEpisodeTitle.setText(episode.getTitle());
        holder.binding.tvEpisodeDesc.setText(episode.getDescription());
        holder.binding.tvEpDuration.setText(episode.getDuration());

        if (isPlaying) {
            holder.binding.tvPlayingBadge.setVisibility(View.VISIBLE);
            holder.binding.cardEpisode.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary));
            holder.binding.cardEpisode.setStrokeWidth(3);
        } else {
            holder.binding.tvPlayingBadge.setVisibility(View.GONE);
            holder.binding.cardEpisode.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.divider));
            holder.binding.cardEpisode.setStrokeWidth(1);
        }

        Glide.with(holder.itemView.getContext())
                .load(episode.getThumbnailUrl())
                .placeholder(R.drawable.bg_search_input)
                .error(R.drawable.ic_play_circle)
                .into(holder.binding.ivEpisodeThumb);

        holder.itemView.setOnClickListener(v -> {
            int oldPos = currentPlayingPosition;
            currentPlayingPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(currentPlayingPosition);

            if (listener != null) {
                listener.onEpisodeClick(episode, currentPlayingPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemEpisodeBinding binding;

        ViewHolder(ItemEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

