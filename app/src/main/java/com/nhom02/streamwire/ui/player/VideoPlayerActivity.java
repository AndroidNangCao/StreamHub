package com.nhom02.streamwire.ui.player;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.adapter.EpisodeAdapter;
import com.nhom02.streamwire.databinding.ActivityVideoPlayerBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.Episode;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.utils.SensorHelper;
import com.nhom02.streamwire.utils.SessionManager;

import java.util.List;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity implements SensorHelper.OnShakeListener {

    private static final String TAG = "VideoPlayerActivity";

    private ActivityVideoPlayerBinding binding;
    private ExoPlayer player;
    private Video currentVideo;
    private Episode currentEpisode;
    private long startPositionMs = 0L;

    private SensorHelper sensorHelper;
    private SessionManager sessionManager;
    private DatabaseManager dbManager;
    private EpisodeAdapter episodeAdapter;

    private boolean isFullscreen = false;
    private boolean isMuted = false;
    private boolean isFallbackAttempted = false;
    private boolean isUserSeeking = false;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = this::hideControls;

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying() && !isUserSeeking) {
                long current = player.getCurrentPosition();
                long total = player.getDuration();
                if (total > 0) {
                    int progress = (int) ((current * 1000) / total);
                    binding.customControls.playerSeekBar.setProgress(progress);
                    binding.customControls.tvCurrentTime.setText(formatTime(current));
                    binding.customControls.tvTotalTime.setText(formatTime(total));
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbManager = DatabaseManager.getInstance(this);
        sensorHelper = new SensorHelper(this);
        sensorHelper.setOnShakeListener(this);

        extractIntentData();
        setupPlayer();
        setupCustomControls();
        setupTouchGestures();
        setupEpisodesList();

        // Ban đầu hiển thị controls, sau 4s tự ẩn
        showControls();
    }

    private void extractIntentData() {
        if (getIntent().hasExtra("extra_video")) {
            currentVideo = (Video) getIntent().getSerializableExtra("extra_video");
        }
        if (currentVideo == null) {
            currentVideo = dbManager.getHeroVideo();
        }

        if (getIntent().hasExtra("extra_episode")) {
            currentEpisode = (Episode) getIntent().getSerializableExtra("extra_episode");
        }
        if (currentEpisode == null && currentVideo != null && currentVideo.getEpisodes() != null && !currentVideo.getEpisodes().isEmpty()) {
            currentEpisode = currentVideo.getEpisodes().get(0);
        }

        startPositionMs = getIntent().getLongExtra("extra_last_position", 0L);

        if (currentVideo != null) {
            binding.tvPlayerVideoTitle.setText(currentVideo.getTitle());
            if (currentEpisode != null) {
                binding.customControls.tvPlayerTopTitle.setText(currentVideo.getTitle() + " - " + currentEpisode.getTitle());
                binding.tvPlayerEpisodeDetail.setText("Tập " + currentEpisode.getEpisodeNumber() + ": " + currentEpisode.getTitle() + " (" + currentEpisode.getDuration() + ")");
                binding.tvPlayerSynopsis.setText(currentEpisode.getDescription());
            } else {
                binding.customControls.tvPlayerTopTitle.setText(currentVideo.getTitle());
                binding.tvPlayerEpisodeDetail.setText(currentVideo.getQuality() + " • " + currentVideo.getDuration());
                binding.tvPlayerSynopsis.setText(currentVideo.getDescription());
            }
        }
    }

    private void setupPlayer() {
        // Cấu hình HTTP DataSource có Browser User-Agent và timeout
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 StreamWire/1.0")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpFactory);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();

        binding.exoPlayerView.setPlayer(player);

        String videoUrl = currentEpisode != null && currentEpisode.getVideoUrl() != null && !currentEpisode.getVideoUrl().isEmpty() ?
                currentEpisode.getVideoUrl() : (currentVideo != null ? currentVideo.getVideoUrl() : "");

        if (videoUrl == null || videoUrl.isEmpty()) {
            videoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";
        }

        playMediaUrl(videoUrl, startPositionMs);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    binding.playerLoadingBar.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY) {
                    binding.playerLoadingBar.setVisibility(View.GONE);
                    resetAutoHideTimer();
                } else if (playbackState == Player.STATE_ENDED) {
                    binding.playerLoadingBar.setVisibility(View.GONE);
                    showControls();
                } else {
                    binding.playerLoadingBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    binding.customControls.btnPlayPause.setImageResource(R.drawable.ic_pause);
                    resetAutoHideTimer();
                } else {
                    binding.customControls.btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
                    showControls();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "ExoPlayer playback error: " + error.getMessage(), error);
                binding.playerLoadingBar.setVisibility(View.GONE);

                if (!isFallbackAttempted) {
                    isFallbackAttempted = true;
                    Toast.makeText(VideoPlayerActivity.this,
                            "Nguồn video trực tuyến không phản hồi. Đang phát bản dự phòng offline...",
                            Toast.LENGTH_LONG).show();

                    // Phát video cục bộ tích hợp sẵn trong res/raw/sample_video.mp4
                    Uri localUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sample_video);
                    MediaItem localItem = MediaItem.fromUri(localUri);
                    player.setMediaItem(localItem);
                    player.prepare();
                    player.play();
                } else {
                    Toast.makeText(VideoPlayerActivity.this,
                            "Không thể phát video: " + error.getErrorCodeName(),
                            Toast.LENGTH_SHORT).show();
                    showControls();
                }
            }
        });

        progressHandler.post(updateProgressRunnable);
    }

    private void playMediaUrl(String url, long seekPositionMs) {
        if (player == null) return;
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);

        if (seekPositionMs > 0) {
            player.seekTo(seekPositionMs);
            Toast.makeText(this, "Tiếp tục xem từ: " + formatTime(seekPositionMs), Toast.LENGTH_SHORT).show();
        }

        player.prepare();
        player.play();
    }

    private void setupCustomControls() {
        // Bấm vào nền mờ custom controls để ẩn controls
        binding.customControls.getRoot().setOnClickListener(v -> hideControls());

        // Nút Back
        binding.customControls.btnPlayerBack.setOnClickListener(v -> finish());

        // Nút Play/Pause
        binding.customControls.btnPlayPause.setOnClickListener(v -> {
            resetAutoHideTimer();
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            }
        });

        // Tua lùi 10s
        binding.customControls.btnRewind10.setOnClickListener(v -> {
            resetAutoHideTimer();
            seekRelative(-10000);
        });

        // Tua tới 10s
        binding.customControls.btnForward10.setOnClickListener(v -> {
            resetAutoHideTimer();
            seekRelative(10000);
        });

        // Nút Âm lượng (Mute / Unmute)
        binding.customControls.btnVolume.setOnClickListener(v -> {
            resetAutoHideTimer();
            if (player != null) {
                isMuted = !isMuted;
                player.setVolume(isMuted ? 0f : 1f);
                binding.customControls.btnVolume.setImageResource(
                        isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
                Toast.makeText(this, isMuted ? "Đã tắt tiếng" : "Đã bật tiếng", Toast.LENGTH_SHORT).show();
            }
        });

        // SeekBar
        binding.customControls.playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long targetMs = (player.getDuration() * progress) / 1000;
                    binding.customControls.tvCurrentTime.setText(formatTime(targetMs));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
                autoHideHandler.removeCallbacks(autoHideRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    long targetMs = (player.getDuration() * seekBar.getProgress()) / 1000;
                    player.seekTo(targetMs);
                }
                isUserSeeking = false;
                resetAutoHideTimer();
            }
        });

        // Chọn chất lượng phát
        binding.customControls.btnPlayerQuality.setOnClickListener(v -> {
            resetAutoHideTimer();
            String[] qualities = {"1080p Full HD", "720p HD", "480p SD", "Tự động (Auto)"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Chất lượng video")
                    .setItems(qualities, (dialog, which) -> {
                        binding.customControls.btnPlayerQuality.setText(qualities[which].split(" ")[0]);
                        Toast.makeText(this, "Đã chuyển sang chất lượng: " + qualities[which], Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        // Tốc độ phát (0.75x, 1x, 1.25x, 1.5x, 2x)
        binding.customControls.btnPlayerSpeed.setOnClickListener(v -> {
            resetAutoHideTimer();
            String[] speeds = {"0.75x", "1.0x (Chuẩn)", "1.25x", "1.5x", "2.0x"};
            float[] speedValues = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Tốc độ phát")
                    .setItems(speeds, (dialog, which) -> {
                        if (player != null) {
                            player.setPlaybackParameters(new PlaybackParameters(speedValues[which]));
                            binding.customControls.btnPlayerSpeed.setText(speedValues[which] + "x");
                        }
                    })
                    .show();
        });

        // Toàn màn hình (Fullscreen)
        binding.customControls.btnFullscreenToggle.setOnClickListener(v -> {
            resetAutoHideTimer();
            toggleFullscreen();
        });
    }

    private void setupTouchGestures() {
        // Cử chỉ chạm thông minh: Double tap 2 bên để tua 10s, Single tap để ẩn/hiện điều khiển
        GestureDetector leftDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (sessionManager.isSmartGesturesEnabled()) {
                    seekRelative(-10000);
                    Toast.makeText(VideoPlayerActivity.this, "⏪ Tua lùi 10s", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        GestureDetector rightDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (sessionManager.isSmartGesturesEnabled()) {
                    seekRelative(10000);
                    Toast.makeText(VideoPlayerActivity.this, "⏩ Tua tới 10s", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        binding.gestureLeftRewind.setOnTouchListener((v, event) -> {
            leftDetector.onTouchEvent(event);
            return true;
        });

        binding.gestureRightForward.setOnTouchListener((v, event) -> {
            rightDetector.onTouchEvent(event);
            return true;
        });
    }

    private void toggleControls() {
        if (binding.customControls.getRoot().getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        binding.customControls.getRoot().setVisibility(View.VISIBLE);
        resetAutoHideTimer();
    }

    private void hideControls() {
        binding.customControls.getRoot().setVisibility(View.GONE);
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }

    private void resetAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
        if (player != null && player.isPlaying()) {
            autoHideHandler.postDelayed(autoHideRunnable, 4000);
        }
    }

    private void setupEpisodesList() {
        episodeAdapter = new EpisodeAdapter((episode, position) -> {
            changeEpisode(episode);
        });
        binding.rvPlayerEpisodes.setAdapter(episodeAdapter);

        if (currentVideo != null) {
            List<Episode> eps = dbManager.getEpisodesByVideoId(currentVideo.getId());
            if (eps == null || eps.isEmpty()) {
                eps = currentVideo.getEpisodes();
            }
            if (eps != null) {
                episodeAdapter.setData(eps);
            }
        }
    }

    private void changeEpisode(Episode episode) {
        this.currentEpisode = episode;
        this.isFallbackAttempted = false;
        saveCurrentProgressToSQLite();

        String url = episode.getVideoUrl();
        if (url == null || url.isEmpty()) {
            url = "https://media.w3.org/2010/05/sintel/trailer.mp4";
        }
        playMediaUrl(url, 0L);

        binding.customControls.tvPlayerTopTitle.setText(currentVideo.getTitle() + " - " + episode.getTitle());
        binding.tvPlayerEpisodeDetail.setText("Tập " + episode.getEpisodeNumber() + ": " + episode.getTitle() + " (" + episode.getDuration() + ")");
        binding.tvPlayerSynopsis.setText(episode.getDescription());
    }

    private void seekRelative(long deltaMs) {
        if (player == null) return;
        long newPos = Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + deltaMs));
        player.seekTo(newPos);
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            binding.playerBottomScroll.setVisibility(View.GONE);
            ViewGroup.LayoutParams lp = binding.playerContainer.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            binding.playerContainer.setLayoutParams(lp);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            binding.playerBottomScroll.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = binding.playerContainer.getLayoutParams();
            lp.height = (int) (240 * getResources().getDisplayMetrics().density);
            binding.playerContainer.setLayoutParams(lp);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onShake() {
        if (!sessionManager.isShakeGestureEnabled()) return;

        runOnUiThread(() -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(150);
                }
            }

            seekRelative(10000);
            Toast.makeText(this, "📱 Cảm biến gia tốc: Lắc máy ➔ Đã tua tới 10s!", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveCurrentProgressToSQLite() {
        if (player == null || currentVideo == null) return;
        long current = player.getCurrentPosition();
        long total = player.getDuration();

        if (total <= 0) return;

        String epTitle = currentEpisode != null ? currentEpisode.getTitle() : "Tập 1";
        // Ghi thẳng vào SQLite
        dbManager.saveOrUpdateHistory(currentVideo.getId(), epTitle, current, total);
    }

    private String formatTime(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isShakeGestureEnabled() && sensorHelper.hasAccelerometerSensor()) {
            sensorHelper.registerAccelerometer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.unregisterAccelerometer();
        saveCurrentProgressToSQLite();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoHideHandler.removeCallbacksAndMessages(null);
        progressHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
        sensorHelper.unregisterAll();
    }
}
