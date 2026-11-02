package com.nhom02.streamwire.model;

import java.io.Serializable;
import java.util.Locale;

public class WatchHistory implements Serializable {
    private String id;
    private Video video;
    private String episodeTitle;
    private long lastPositionMs;
    private long totalDurationMs;
    private String updatedAt;

    public WatchHistory() {
    }

    public WatchHistory(String id, Video video, String episodeTitle, long lastPositionMs, long totalDurationMs, String updatedAt) {
        this.id = id;
        this.video = video;
        this.episodeTitle = episodeTitle;
        this.lastPositionMs = lastPositionMs;
        this.totalDurationMs = totalDurationMs;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public String getEpisodeTitle() {
        return episodeTitle != null ? episodeTitle : "Tập 1";
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = episodeTitle;
    }

    public long getLastPositionMs() {
        return lastPositionMs;
    }

    public void setLastPositionMs(long lastPositionMs) {
        this.lastPositionMs = lastPositionMs;
    }

    public long getTotalDurationMs() {
        return totalDurationMs > 0 ? totalDurationMs : 1;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getProgressPercent() {
        if (totalDurationMs <= 0) return 0;
        int percent = (int) ((lastPositionMs * 100) / totalDurationMs);
        return Math.min(100, Math.max(0, percent));
    }

    public String getProgressTimeText() {
        return formatTime(lastPositionMs) + " / " + formatTime(totalDurationMs);
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}

