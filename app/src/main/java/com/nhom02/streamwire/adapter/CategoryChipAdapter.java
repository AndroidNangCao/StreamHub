package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemCategoryChipBinding;
import com.nhom02.streamwire.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    private final List<Category> categories = new ArrayList<>();
    private int selectedPosition = 0;
    private final OnCategoryClickListener listener;

    public CategoryChipAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Category> list) {
        this.categories.clear();
        if (list != null) {
            this.categories.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryChipBinding binding = ItemCategoryChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        boolean isSelected = (position == selectedPosition);

        holder.binding.tvChipName.setText(category.getName());

        if (isSelected) {
            holder.binding.tvChipName.setBackgroundResource(R.drawable.bg_button_primary);
            holder.binding.tvChipName.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_primary));
        } else {
            holder.binding.tvChipName.setBackgroundResource(R.drawable.bg_badge_pill);
            holder.binding.tvChipName.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onCategoryClick(category, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCategoryChipBinding binding;

        ViewHolder(ItemCategoryChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

