package com.nhom02.streamwire.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.nhom02.streamwire.api.ApiClient;
import com.nhom02.streamwire.api.ApiService;
import com.nhom02.streamwire.databinding.FragmentHomeBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.Category;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.ui.detail.VideoDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private DatabaseManager dbManager;
    private ApiService apiService;
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
        apiService = ApiClient.getService(requireContext());

        setupAdapters();
        setupSearchAndFilter();
        setupSwipeRefresh();

        // 1. Tải trước từ SQLite cache để hiển thị ngay lập tức (Offline-First)
        loadDataFromDatabase();

        // 2. Tự động gọi Public REST API qua mạng để cập nhật danh mục video mới nhất
        fetchDataFromApi();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeResources(R.color.secondary, R.color.primary);
        binding.swipeRefreshHome.setOnRefreshListener(this::fetchDataFromApi);
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
            Toast.makeText(requireContext(), "Đang hiển thị toàn bộ video trong kho", Toast.LENGTH_SHORT).show();
        });

        binding.tvSeeMoreRecommended.setOnClickListener(v -> {
            recommendedAdapter.setData(dbManager.getRecommendedVideos());
            Toast.makeText(requireContext(), "Đã tải danh sách đề xuất", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Gọi Public REST API qua Retrofit để lấy danh mục và danh sách video mới nhất từ máy chủ
     */
    private void fetchDataFromApi() {
        if (!isAdded()) return;
        binding.swipeRefreshHome.setRefreshing(true);

        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> catResponse) {
                final List<Category> fetchedCategories = catResponse.isSuccessful() ? catResponse.body() : null;

                apiService.getVideos().enqueue(new Callback<List<Video>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Video>> call, @NonNull Response<List<Video>> vidResponse) {
                        if (!isAdded()) return;
                        binding.swipeRefreshHome.setRefreshing(false);

                        if (vidResponse.isSuccessful() && vidResponse.body() != null) {
                            List<Video> fetchedVideos = vidResponse.body();
                            // Lưu đồng bộ dữ liệu API vào SQLite cục bộ
                            dbManager.syncFromApi(fetchedCategories, fetchedVideos);
                            // Hiển thị dữ liệu mới lên giao diện
                            loadDataFromDatabase();
                            Toast.makeText(requireContext(), "✓ Đã đồng bộ " + fetchedVideos.size() + " video từ Public Cloud API", Toast.LENGTH_SHORT).show();
                        } else {
                            loadDataFromDatabase();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Video>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Lỗi kết nối API videos: " + t.getMessage());
                        binding.swipeRefreshHome.setRefreshing(false);
                        loadDataFromDatabase();
                        Toast.makeText(requireContext(), "Đang sử dụng dữ liệu ngoại tuyến (Offline SQLite)", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Lỗi kết nối API categories: " + t.getMessage());
                binding.swipeRefreshHome.setRefreshing(false);
                loadDataFromDatabase();
            }
        });
    }

    private void loadDataFromDatabase() {
        if (!isAdded()) return;

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
        intent.putExtra("extra_video", video);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
