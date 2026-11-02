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

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.adapter.CategoryChipAdapter;
import com.nhom02.streamwire.adapter.VideoAdapter;
import com.nhom02.streamwire.databinding.FragmentHomeBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.Category;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.ui.detail.VideoDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DatabaseManager dbManager;
    private CategoryChipAdapter categoryAdapter;
    private VideoAdapter trendingAdapter;
    private VideoAdapter recommendedAdapter;

    private List<Video> fullTrendingList = new ArrayList<>();
    private List<Video> fullRecommendedList = new ArrayList<>();
    private Video heroVideo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = DatabaseManager.getInstance(requireContext());

        setupAdapters();
        setupSearchAndFilter();
        loadDataFromDatabase();
    }

    private void setupAdapters() {
        // Categories horizontal
        categoryAdapter = new CategoryChipAdapter((category, position) -> {
            filterVideosByCategory(category.getName());
        });
        binding.rvCategories.setAdapter(categoryAdapter);

        // Trending videos (Card mode)
        trendingAdapter = new VideoAdapter(VideoAdapter.VIEW_TYPE_CARD, this::openVideoDetail);
        binding.rvTrending.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTrending.setAdapter(trendingAdapter);

        // Recommended videos (Horizontal compact mode)
        recommendedAdapter = new VideoAdapter(VideoAdapter.VIEW_TYPE_HORIZONTAL, this::openVideoDetail);
        binding.rvRecommended.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecommended.setAdapter(recommendedAdapter);

        binding.tvSeeAllTrending.setOnClickListener(v -> {
            trendingAdapter.setData(dbManager.getAllVideos());
            Toast.makeText(requireContext(), "Đã hiển thị toàn bộ video trong kho SQLite", Toast.LENGTH_SHORT).show();
        });

        binding.tvSeeMoreRecommended.setOnClickListener(v -> {
            recommendedAdapter.setData(dbManager.getRecommendedVideos());
            Toast.makeText(requireContext(), "Đã tải danh sách đề xuất từ SQLite", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDataFromDatabase() {
        // 1. Categories
        List<Category> categories = dbManager.getCategories();
        categoryAdapter.setData(categories);

        // 2. Hero Video
        heroVideo = dbManager.getHeroVideo();
        if (heroVideo != null) {
            binding.tvHeroTitle.setText(heroVideo.getTitle());
            binding.tvHeroDesc.setText(heroVideo.getDescription());

            Glide.with(this)
                    .load(heroVideo.getThumbnailUrl())
                    .placeholder(R.drawable.bg_search_input)
                    .error(R.drawable.ic_play_circle)
                    .into(binding.ivHeroThumbnail);

            binding.cardHeroToday.setOnClickListener(v -> openVideoDetail(heroVideo));
        }

        // 3. Trending & Recommended Videos from SQLite
        fullTrendingList = dbManager.getTrendingVideos();
        trendingAdapter.setData(fullTrendingList);

        fullRecommendedList = dbManager.getRecommendedVideos();
        recommendedAdapter.setData(fullRecommendedList);
    }

    private void setupSearchAndFilter() {
        binding.etSearchHome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVideosByQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnFilterHome.setOnClickListener(v -> {
            String[] filters = {"Mới nhất", "Lượt xem cao", "Đánh giá cao (★)", "Tất cả kho"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Bộ lọc sắp xếp")
                    .setItems(filters, (dialog, which) -> {
                        if (which == 3) {
                            trendingAdapter.setData(dbManager.getAllVideos());
                        } else {
                            trendingAdapter.setData(dbManager.getTrendingVideos());
                        }
                        Toast.makeText(requireContext(), "Đã áp dụng: " + filters[which], Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    private void filterVideosByCategory(String categoryName) {
        if ("Tất cả".equalsIgnoreCase(categoryName)) {
            trendingAdapter.setData(fullTrendingList);
            recommendedAdapter.setData(fullRecommendedList);
            return;
        }

        List<Video> filtered = dbManager.getVideosByCategory(categoryName);
        trendingAdapter.setData(filtered);
        recommendedAdapter.setData(filtered);
    }

    private void filterVideosByQuery(String query) {
        if (query.trim().isEmpty()) {
            trendingAdapter.setData(fullTrendingList);
            recommendedAdapter.setData(fullRecommendedList);
            return;
        }

        List<Video> searchResults = dbManager.searchVideos(query);
        trendingAdapter.setData(searchResults);
        recommendedAdapter.setData(searchResults);
    }

    private void openVideoDetail(Video video) {
        Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
        intent.putExtra("extra_video_id", video.getId());
        intent.putExtra("extra_video", video);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại dữ liệu từ SQLite nếu Admin có thêm/sửa/xóa video
        loadDataFromDatabase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
