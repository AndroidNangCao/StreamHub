package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemCastBinding;
import com.nhom02.streamwire.model.CastMember;

import java.util.ArrayList;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.ViewHolder> {

    private final List<CastMember> castList = new ArrayList<>();

    public void setData(List<CastMember> list) {
        this.castList.clear();
        if (list != null) {
            this.castList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCastBinding binding = ItemCastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CastMember item = castList.get(position);
        holder.binding.tvCastName.setText(item.getName());
        holder.binding.tvCastRole.setText(item.getRole());

        if (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getAvatarUrl())
                    .placeholder(R.drawable.bg_circle_icon)
                    .into(holder.binding.ivCastAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCastBinding binding;

        ViewHolder(ItemCastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

