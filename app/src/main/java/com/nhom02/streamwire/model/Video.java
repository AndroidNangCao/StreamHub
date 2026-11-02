package com.nhom02.streamwire.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Video implements Serializable {
    private String id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String duration; // e.g., "45:20"
    private long durationMs;
    private String views; // e.g., "120K lượt xem"
    private double rating; // e.g., 9.8
    private int releaseYear; // e.g., 2025
    private String ageRating; // e.g., "16+"
    private String quality; // e.g., "4K HDR"
    private String category; // e.g., "Hành Động"
    private String status; // "ACTIVE", "PENDING", "REPORTED"
    private String publishedAt; // e.g., "2 ngày trước"
    private List<Episode> episodes;
    private List<CastMember> castList;

    public Video() {
        this.episodes = new ArrayList<>();
        this.castList = new ArrayList<>();
        this.status = "ACTIVE";
    }

    public Video(String id, String title, String description, String videoUrl, String thumbnailUrl,
                 String duration, long durationMs, String views, double rating, int releaseYear,
                 String ageRating, String quality, String category, String publishedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.durationMs = durationMs;
        this.views = views;
        this.rating = rating;
        this.releaseYear = releaseYear;
        this.ageRating = ageRating;
        this.quality = quality;
        this.category = category;
        this.publishedAt = publishedAt;
        this.status = "ACTIVE";
        this.episodes = new ArrayList<>();
        this.castList = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getViews() {
        return views;
    }

    public void setViews(String views) {
        this.views = views;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }

    public List<CastMember> getCastList() {
        return castList;
    }

    public void setCastList(List<CastMember> castList) {
        this.castList = castList;
    }
}

