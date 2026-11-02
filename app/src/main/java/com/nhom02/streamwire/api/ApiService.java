package com.nhom02.streamwire.api;

import com.nhom02.streamwire.model.Category;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.model.WatchHistory;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- Authentication ---
    class LoginRequest {
        public String usernameOrEmail;
        public String password;
        public LoginRequest(String usernameOrEmail, String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }
    }

    class RegisterRequest {
        public String fullName;
        public String email;
        public String password;
        public RegisterRequest(String fullName, String email, String password) {
            this.fullName = fullName;
            this.email = email;
            this.password = password;
        }
    }

    class AuthResponse {
        public boolean success;
        public String message;
        public String token;
        public User user;
    }

    class SaveHistoryRequest {
        public String videoId;
        public String episodeTitle;
        public long lastPositionMs;
        public long totalDurationMs;
        public SaveHistoryRequest(String videoId, String episodeTitle, long lastPositionMs, long totalDurationMs) {
            this.videoId = videoId;
            this.episodeTitle = episodeTitle;
            this.lastPositionMs = lastPositionMs;
            this.totalDurationMs = totalDurationMs;
        }
    }

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    // --- Video Catalog ---
    @GET("api/categories")
    Call<List<Category>> getCategories();

    @GET("api/videos")
    Call<List<Video>> getVideos(@Query("categoryId") String categoryId, @Query("query") String query);

    @GET("api/videos/trending")
    Call<List<Video>> getTrendingVideos();

    @GET("api/videos/recommended")
    Call<List<Video>> getRecommendedVideos();

    @GET("api/videos/{id}")
    Call<Video> getVideoDetail(@Path("id") String videoId);

    // --- Watch History ---
    @GET("api/history")
    Call<List<WatchHistory>> getWatchHistory();

    @POST("api/history/save")
    Call<WatchHistory> saveWatchHistory(@Body SaveHistoryRequest request);

    @DELETE("api/history/{id}")
    Call<Map<String, Object>> deleteHistoryItem(@Path("id") String historyId);

    @DELETE("api/history/clear")
    Call<Map<String, Object>> clearAllHistory();

    // --- Admin Endpoints ---
    @GET("api/admin/videos")
    Call<List<Video>> getAdminVideos();

    @PUT("api/admin/videos/{id}/status")
    Call<Video> updateVideoStatus(@Path("id") String videoId, @Query("status") String status);

    @DELETE("api/admin/videos/{id}")
    Call<Map<String, Object>> deleteVideo(@Path("id") String videoId);

    @GET("api/admin/users")
    Call<List<User>> getAdminUsers();

    @PUT("api/admin/users/{id}/lock")
    Call<User> toggleUserLock(@Path("id") String userId, @Query("locked") boolean locked);

    @PUT("api/admin/users/{id}/role")
    Call<User> updateUserRole(@Path("id") String userId, @Query("role") String role);
}

