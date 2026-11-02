package com.nhom02.streamwire.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.adapter.HistoryAdapter;
import com.nhom02.streamwire.databinding.FragmentHistoryBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.WatchHistory;
import com.nhom02.streamwire.ui.player.VideoPlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private DatabaseManager dbManager;
    private HistoryAdapter adapter;
    private List<WatchHistory> fullList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = DatabaseManager.getInstance(requireContext());

        setupRecyclerView();
        setupFilters();
        setupClearAll();
        loadHistoryDataFromSQLite();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onResumeWatch(WatchHistory history) {
                Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                intent.putExtra("extra_video", history.getVideo());
                intent.putExtra("extra_last_position", history.getLastPositionMs());
                startActivity(intent);
            }

            @Override
            public void onDeleteItem(WatchHistory history, int position) {
                // Xóa khỏi SQLite
                dbManager.deleteHistory(history.getId());
                fullList.remove(history);
                adapter.removeItem(position);
                updateCount();
                Toast.makeText(requireContext(), "Đã xóa 1 mục khỏi lịch sử SQLite", Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvWatchHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWatchHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearchAndChip(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.chipGroupHistory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterBySearchAndChip(binding.etSearchHistory.getText().toString());
        });
    }

    private void setupClearAll() {
        binding.btnClearAllHistory.setOnClickListener(v -> {
            if (fullList.isEmpty()) {
                Toast.makeText(requireContext(), "Lịch sử xem đang trống", Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa toàn bộ lịch sử?")
                    .setMessage("Bạn có chắc chắn muốn xóa toàn bộ lịch sử xem không? Dữ liệu trong SQLite sẽ được xóa vĩnh viễn.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                        // Xóa sạch trong SQLite
                        dbManager.clearAllHistory();
                        fullList.clear();
                        adapter.clearAll();
                        updateCount();
                        Toast.makeText(requireContext(), "Đã xóa toàn bộ lịch sử xem khỏi SQLite", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    private void loadHistoryDataFromSQLite() {
        fullList = dbManager.getWatchHistories();
        adapter.setData(fullList);
        updateCount();
    }

    private void filterBySearchAndChip(String query) {
        int checkedChipId = binding.chipGroupHistory.getCheckedChipId();
        List<WatchHistory> result = new ArrayList<>();

        for (WatchHistory h : fullList) {
            boolean matchSearch = query.isEmpty() || (h.getVideo() != null
                    && h.getVideo().getTitle().toLowerCase().contains(query.toLowerCase()));

            boolean matchChip = true;
            if (checkedChipId == R.id.chipUnfinished) {
                matchChip = (h.getProgressPercent() < 90);
            } else if (checkedChipId == R.id.chipFinished) {
                matchChip = (h.getProgressPercent() >= 90);
            }

            if (matchSearch && matchChip) {
                result.add(h);
            }
        }

        adapter.setData(result);
    }

    private void updateCount() {
        if (binding == null) return;
        int size = fullList.size();
        binding.tvHistoryCount.setText(size + " nội dung đang theo dõi");

        if (size == 0) {
            binding.layoutEmptyHistory.setVisibility(View.VISIBLE);
            binding.rvWatchHistory.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyHistory.setVisibility(View.GONE);
            binding.rvWatchHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryDataFromSQLite();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
