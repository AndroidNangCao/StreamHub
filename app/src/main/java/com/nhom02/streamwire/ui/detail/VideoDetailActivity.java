package com.nhom02.streamwire.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.adapter.CastAdapter;
import com.nhom02.streamwire.adapter.EpisodeAdapter;
import com.nhom02.streamwire.databinding.ActivityVideoDetailBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.Episode;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.ui.player.VideoPlayerActivity;

public class VideoDetailActivity extends AppCompatActivity {

    private ActivityVideoDetailBinding binding;
    private DatabaseManager dbManager;
    private Video currentVideo;
    private CastAdapter castAdapter;
    private EpisodeAdapter episodeAdapter;
    private boolean isDescriptionExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbManager = DatabaseManager.getInstance(this);

        // Nhận Video từ Intent hoặc tải từ SQLite theo ID
        String videoId = getIntent().getStringExtra("extra_video_id");
        if (videoId != null) {
            currentVideo = dbManager.getVideoById(videoId);
        }

        if (currentVideo == null && getIntent().hasExtra("extra_video")) {
            currentVideo = (Video) getIntent().getSerializableExtra("extra_video");
            if (currentVideo != null && (currentVideo.getEpisodes() == null || currentVideo.getEpisodes().isEmpty())) {
                Video fullV = dbManager.getVideoById(currentVideo.getId());
                if (fullV != null) currentVideo = fullV;
            }
        }

        if (currentVideo == null) {
            currentVideo = dbManager.getHeroVideo();
        }

        if (currentVideo == null || !"ACTIVE".equals(currentVideo.getStatus())) {
            Toast.makeText(this, "Video không còn khả dụng", Toast.LENGTH_SHORT).show(); finish(); return;
        }
        setupToolbar();
        setupViews();
        setupCastRecyclerView();
        setupEpisodeRecyclerView();
        setupActionButtons();
    }

    private void setupToolbar() {
        binding.toolbarDetail.setNavigationOnClickListener(v -> finish());
        binding.btnTopShare.setOnClickListener(v -> shareVideo());
        binding.btnTopDownload.setOnClickListener(v -> downloadVideo());
    }

    private void setupViews() {
        binding.tvDetailTitle.setText(currentVideo.getTitle());
        binding.tvRatingBadge.setText(currentVideo.getRating() > 0 ? "★ " + currentVideo.getRating() : "Chưa có đánh giá");
        binding.tvYearBadge.setText(String.valueOf(currentVideo.getReleaseYear()));
        binding.tvAgeBadge.setText(currentVideo.getAgeRating() != null ? currentVideo.getAgeRating() : "16+");
        binding.tvDurationBadge.setText(currentVideo.getDuration() != null ? currentVideo.getDuration() : "Chưa xác định");
        binding.tvQualityBadge.setText(currentVideo.getQuality() != null ? currentVideo.getQuality() : "Nguồn gốc");
        binding.tvCategoryTags.setText(currentVideo.getCategory());
        binding.tvDetailDescription.setText(currentVideo.getDescription());

        // Thumbnail Preview
        Glide.with(this)
                .load(currentVideo.getThumbnailUrl())
                .placeholder(R.drawable.bg_search_input)
                .error(R.drawable.ic_play_circle)
                .into(binding.ivDetailPreview);

        // Toggle Xem thêm mô tả
        binding.tvToggleDescription.setOnClickListener(v -> {
            isDescriptionExpanded = !isDescriptionExpanded;
            if (isDescriptionExpanded) {
                binding.tvDetailDescription.setMaxLines(Integer.MAX_VALUE);
                binding.tvToggleDescription.setText("Thu gọn ▲");
            } else {
                binding.tvDetailDescription.setMaxLines(3);
                binding.tvToggleDescription.setText("Xem thêm ▼");
            }
        });
    }

    private void setupCastRecyclerView() {
        castAdapter = new CastAdapter();
        binding.rvCast.setAdapter(castAdapter);

        if (currentVideo.getCastList() != null && !currentVideo.getCastList().isEmpty()) {
            castAdapter.setData(currentVideo.getCastList());
        } else {
            castAdapter.setData(dbManager.getCastByVideoId(currentVideo.getId()));
        }
    }

    private void setupEpisodeRecyclerView() {
        episodeAdapter = new EpisodeAdapter((episode, position) -> {
            playVideo(episode, 0L);
        });
        binding.rvDetailEpisodes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDetailEpisodes.setAdapter(episodeAdapter);

        if (currentVideo.getEpisodes() != null && !currentVideo.getEpisodes().isEmpty()) {
            episodeAdapter.setData(currentVideo.getEpisodes());
        } else {
            episodeAdapter.setData(dbManager.getEpisodesByVideoId(currentVideo.getId()));
        }

        binding.tvSeasonDropdown.setText("Danh sách tập");
    }

    private void setupActionButtons() {
        // Nút Phát Video lớn
        binding.btnMainPlay.setOnClickListener(v -> {
            Episode ep = null;
            if (currentVideo.getEpisodes() != null && !currentVideo.getEpisodes().isEmpty()) {
                ep = currentVideo.getEpisodes().get(0);
            }
            playVideo(ep, 0L);
        });

        // Xem thử trên card
        binding.cardPreviewPlayer.setOnClickListener(v -> {
            Episode ep = null;
            if (currentVideo.getEpisodes() != null && !currentVideo.getEpisodes().isEmpty()) {
                ep = currentVideo.getEpisodes().get(0);
            }
            playVideo(ep, 0L);
        });

        binding.btnAddWatchlist.setText(dbManager.isFavorite(currentVideo.getId()) ? "Bỏ yêu thích" : "Yêu thích");
        binding.btnAddWatchlist.setOnClickListener(v -> {
            boolean favorite = !dbManager.isFavorite(currentVideo.getId());
            if (dbManager.setFavorite(currentVideo.getId(), favorite)) {
                binding.btnAddWatchlist.setText(favorite ? "Bỏ yêu thích" : "Yêu thích");
            } else Toast.makeText(this, "Không thể lưu yêu thích", Toast.LENGTH_SHORT).show();
        });

        binding.btnActionShare.setOnClickListener(v -> shareVideo());
        binding.btnActionDownload.setOnClickListener(v -> downloadVideo());
    }

    private void playVideo(Episode episode, long lastPositionMs) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("extra_video", currentVideo);
        if (episode != null) {
            intent.putExtra("extra_episode", episode);
        }
        intent.putExtra("extra_last_position", lastPositionMs);
        startActivity(intent);
    }

    private void shareVideo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentVideo.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Đang xem phim siêu đỉnh trên StreamWire: "
                + currentVideo.getTitle() + "\n" + currentVideo.getVideoUrl());
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ video qua"));
    }

    private void downloadVideo() {
        String url = currentVideo.getVideoUrl();
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, "Chỉ hỗ trợ tải đường dẫn HTTP/HTTPS", Toast.LENGTH_SHORT).show(); return;
        }
        try {
            android.app.DownloadManager downloads = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url))
                    .setTitle(currentVideo.getTitle())
                    .setNotificationVisibility(new com.nhom02.streamwire.utils.SessionManager(this).isNotificationsEnabled() ? android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED : android.app.DownloadManager.Request.VISIBILITY_HIDDEN)
                    .setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_MOVIES, currentVideo.getId() + "_" + System.currentTimeMillis() + ".mp4");
            long id = downloads.enqueue(request);
            dbManager.saveDownload(id, currentVideo.getId());
            Toast.makeText(this, "Đã đưa video vào hàng đợi tải xuống", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Không thể bắt đầu tải video", Toast.LENGTH_SHORT).show();
        }
    }
}
