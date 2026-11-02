package com.nhom02.streamwire.model;

import java.io.Serializable;

public class Episode implements Serializable {
    private String id;
    private int episodeNumber;
    private String title;
    private String duration;
    private String thumbnailUrl;
    private String videoUrl;
    private String description;
    private boolean isPlaying;

    public Episode() {
    }

    public Episode(String id, int episodeNumber, String title, String duration, String thumbnailUrl, String videoUrl, String description) {
        this.id = id;
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.duration = duration;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.description = description;
        this.isPlaying = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}

