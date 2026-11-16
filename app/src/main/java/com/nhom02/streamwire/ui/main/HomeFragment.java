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
    private int currentSort;
    private String currentCategory = "Tất cả";

    private boolean syncReceiverRegistered;
    private final android.content.BroadcastReceiver syncReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            if (binding != null) { loadDataFromDatabase(); updateSyncStatus(); }
        }
    };

    @Override public void onStart() {
        super.onStart();
        androidx.core.content.ContextCompat.registerReceiver(requireContext(), syncReceiver,
                new android.content.IntentFilter(com.nhom02.streamwire.cloud.FirebaseSyncWorker.ACTION), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        syncReceiverRegistered = true;
    }

    @Override public void onStop() {
        if (syncReceiverRegistered) { requireContext().unregisterReceiver(syncReceiver); syncReceiverRegistered = false; }
        super.onStop();
    }

    private void updateSyncStatus() {
        if (binding == null) return;
        boolean configured = com.nhom02.streamwire.cloud.FirebaseRuntime.configured(requireContext());
        binding.btnSyncNow.setEnabled(configured);
        if (!configured) { binding.tvSyncStatus.setText("Đang lưu trên thiết bị. Chưa cấu hình Firebase."); return; }
        com.nhom02.streamwire.cloud.SyncStore store = new com.nhom02.streamwire.cloud.SyncStore(requireContext());
        com.nhom02.streamwire.utils.SessionManager session = new com.nhom02.streamwire.utils.SessionManager(requireContext());
        String uid = session.isGuest() || session.getUser() == null ? "guest" : session.getUser().getId();
        android.net.ConnectivityManager network = (android.net.ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkCapabilities capabilities = network.getNetworkCapabilities(network.getActiveNetwork());
        boolean online = capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        String status = online ? store.state("status:" + uid) : "Ngoại tuyến: đang dùng dữ liệu trên máy.";
        if (status.isEmpty()) status = "Sẵn sàng đồng bộ Firebase";
        int pending = store.pendingCount(uid);
        binding.tvSyncStatus.setText(status + (pending > 0 ? " • " + pending + " thay đổi chờ gửi" : ""));
    }

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
        setupSwipeRefresh();
        binding.btnSyncNow.setOnClickListener(v -> {
            com.nhom02.streamwire.cloud.SyncScheduler.request(requireContext());
            updateSyncStatus();
        });
        updateSyncStatus();
        com.nhom02.streamwire.cloud.SyncScheduler.request(requireContext());

        // 1. Tải trước từ SQLite cache để hiển thị ngay lập tức (Offline-First)
        loadDataFromDatabase();


    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeResources(R.color.secondary, R.color.primary);
        binding.swipeRefreshHome.setOnRefreshListener(() -> { loadDataFromDatabase(); updateSyncStatus(); com.nhom02.streamwire.cloud.SyncScheduler.request(requireContext()); binding.swipeRefreshHome.setRefreshing(false); });
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
            currentSort = 3;
            applyFilters();
            Toast.makeText(requireContext(), "Đang hiển thị toàn bộ video trong kho", Toast.LENGTH_SHORT).show();
        });

        binding.tvSeeMoreRecommended.setOnClickListener(v -> {
            recommendedAdapter.setData(dbManager.getRecommendedVideos());
            Toast.makeText(requireContext(), "Đã tải danh sách đề xuất", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDataFromDatabase() {
        if (!isAdded() || binding == null || categoryAdapter == null) return;

        // 1. Categories
        List<Category> categories = dbManager.getCategories();
        categoryAdapter.setData(categories);

        // 2. Hero Video
        heroVideo = dbManager.getHeroVideo();
        binding.cardHeroToday.setVisibility(heroVideo == null ? View.GONE : View.VISIBLE);
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
        fullTrendingList = dbManager.getPublicVideos(0);
        trendingAdapter.setData(fullTrendingList);

        fullRecommendedList = dbManager.getRecommendedVideos();
        recommendedAdapter.setData(fullRecommendedList);
        applyFilters();
        binding.tvEmptyLibrary.setVisibility(fullTrendingList.isEmpty() ? View.VISIBLE : View.GONE);
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
            String[] filters = {"Mới nhất", "Lượt xem cao", "Đánh giá cao (★)", "Tất cả kho", "Yêu thích"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Bộ lọc sắp xếp")
                    .setItems(filters, (dialog, which) -> {
                        currentSort = which;
                        applyFilters();
                        Toast.makeText(requireContext(), "Đã áp dụng: " + filters[which], Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    private void filterVideosByCategory(String categoryName) {
        currentCategory = categoryName;
        applyFilters();
    }

    private void filterVideosByQuery(String query) {
        applyFilters();
    }

    private void applyFilters() {
        String query = binding.etSearchHome.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
        List<Video> source = currentSort == 4 ? dbManager.getFavoriteVideos() : dbManager.getPublicVideos(currentSort);
        List<Video> filtered = new ArrayList<>();
        for (Video video : source) {
            String category = video.getCategory() == null ? "" : video.getCategory();
            boolean categoryMatches = "Tất cả".equals(currentCategory) || category.contains(currentCategory);
            String text = (video.getTitle() + " " + category).toLowerCase(java.util.Locale.ROOT);
            if (categoryMatches && text.contains(query)) filtered.add(video);
        }
        trendingAdapter.setData(filtered);
        recommendedAdapter.setData(filtered);
    }

    private void openVideoDetail(Video video) {
        Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
        intent.putExtra("extra_video", video);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Luôn cập nhật lại dữ liệu mới nhất từ SQLite mỗi khi quay lại Trang chủ
        loadDataFromDatabase();
        updateSyncStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
