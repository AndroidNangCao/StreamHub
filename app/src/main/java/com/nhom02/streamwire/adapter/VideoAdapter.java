package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemVideoCardBinding;
import com.nhom02.streamwire.databinding.ItemVideoHorizontalBinding;
import com.nhom02.streamwire.model.Video;

import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_CARD = 1;
    public static final int VIEW_TYPE_HORIZONTAL = 2;

    public interface OnVideoClickListener {
        void onVideoClick(Video video);
    }

    private final List<Video> videoList = new ArrayList<>();
    private final int viewTypeMode;
    private final OnVideoClickListener listener;

    public VideoAdapter(int viewTypeMode, OnVideoClickListener listener) {
        this.viewTypeMode = viewTypeMode;
        this.listener = listener;
    }

    public void setData(List<Video> list) {
        this.videoList.clear();
        if (list != null) {
            this.videoList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return viewTypeMode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HORIZONTAL) {
            ItemVideoHorizontalBinding binding = ItemVideoHorizontalBinding.inflate(inflater, parent, false);
            return new HorizontalViewHolder(binding);
        } else {
            ItemVideoCardBinding binding = ItemVideoCardBinding.inflate(inflater, parent, false);
            return new CardViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Video video = videoList.get(position);

        if (holder instanceof CardViewHolder) {
            CardViewHolder vh = (CardViewHolder) holder;
            vh.binding.tvVideoTitle.setText(video.getTitle());
            vh.binding.tvDuration.setText(video.getDuration() != null ? video.getDuration() : "45:00");

            String meta = video.getCategory() + " • " + video.getViews() + " • " + video.getPublishedAt();
            vh.binding.tvVideoMeta.setText(meta);

            Glide.with(vh.itemView.getContext())
                    .load(video.getThumbnailUrl())
                    .placeholder(R.drawable.bg_search_input)
                    .error(R.drawable.ic_play_circle)
                    .into(vh.binding.ivThumbnail);

            vh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onVideoClick(video);
            });

        } else if (holder instanceof HorizontalViewHolder) {
            HorizontalViewHolder vh = (HorizontalViewHolder) holder;
            vh.binding.tvSmallTitle.setText(video.getTitle());
            vh.binding.tvSmallCategory.setText(video.getCategory() + " • " + video.getViews());

            Glide.with(vh.itemView.getContext())
                    .load(video.getThumbnailUrl())
                    .placeholder(R.drawable.bg_search_input)
                    .error(R.drawable.ic_play_circle)
                    .into(vh.binding.ivSmallThumb);

            vh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onVideoClick(video);
            });
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final ItemVideoCardBinding binding;

        CardViewHolder(ItemVideoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class HorizontalViewHolder extends RecyclerView.ViewHolder {
        final ItemVideoHorizontalBinding binding;

        HorizontalViewHolder(ItemVideoHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

