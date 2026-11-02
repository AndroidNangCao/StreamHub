package com.nhom02.streamwire.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ItemAdminUserBinding;
import com.nhom02.streamwire.model.User;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    public interface OnAdminUserActionListener {
        void onChangeRole(User user, int position);
        void onToggleLock(User user, int position);
    }

    private final List<User> userList = new ArrayList<>();
    private final OnAdminUserActionListener listener;

    public AdminUserAdapter(OnAdminUserActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<User> list) {
        this.userList.clear();
        if (list != null) {
            this.userList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        holder.binding.tvAdminUserName.setText(user.getFullName());
        holder.binding.tvAdminUserUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));
        holder.binding.tvAdminUserRole.setText(user.getRole());

        // Status
        if (user.isLocked()) {
            holder.binding.tvAdminUserStatus.setText("Đã khóa");
            holder.binding.tvAdminUserStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_red));
            holder.binding.btnAdminToggleLock.setText("Mở khóa");
            holder.binding.btnAdminToggleLock.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            holder.binding.btnAdminToggleLock.setIconResource(R.drawable.ic_lock_open);
            holder.binding.btnAdminToggleLock.setIconTintResource(R.color.accent_green);
        } else {
            holder.binding.tvAdminUserStatus.setText("Hoạt động");
            holder.binding.tvAdminUserStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            holder.binding.btnAdminToggleLock.setText("Khóa tài khoản");
            holder.binding.btnAdminToggleLock.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_red));
            holder.binding.btnAdminToggleLock.setIconResource(R.drawable.ic_lock);
            holder.binding.btnAdminToggleLock.setIconTintResource(R.color.accent_red);
        }

        holder.binding.btnAdminChangeRole.setOnClickListener(v -> {
            if (listener != null) listener.onChangeRole(user, holder.getAdapterPosition());
        });

        holder.binding.btnAdminToggleLock.setOnClickListener(v -> {
            if (listener != null) listener.onToggleLock(user, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminUserBinding binding;

        ViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

