package com.nhom02.streamwire.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.nhom02.streamwire.R;
import com.nhom02.streamwire.model.CastMember;
import com.nhom02.streamwire.model.Category;
import com.nhom02.streamwire.model.Episode;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.model.WatchHistory;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static DatabaseManager instance;
    private final DatabaseHelper dbHelper;
    private final Context context;

    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseManager(Context context) {
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    // ==========================================
    // 1. AUTH & USERS
    // ==========================================

    public User authenticate(String emailOrUsername, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email=? COLLATE NOCASE OR username=?", new String[]{emailOrUsername, emailOrUsername})) {
            if (!cursor.moveToFirst()) return null;
            String stored = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            if (!com.nhom02.streamwire.utils.PasswordHasher.verify(password, stored)) return null;
            User user = cursorToUser(cursor);
            if (!stored.startsWith("pbkdf2$")) updatePassword(user.getId(), password);
            return user;
        }
    }

    public synchronized boolean registerUser(String fullName, String email, String password) {
        if (fullName == null || fullName.trim().isEmpty() || email == null || password == null || password.length() < 6) return false;
        email = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if email already exists
        Cursor check = db.query(DatabaseHelper.TABLE_USERS, null,
                DatabaseHelper.COL_USER_EMAIL + " = ? COLLATE NOCASE", new String[]{email}, null, null, null);
        if (check != null && check.getCount() > 0) {
            check.close();
            return false;
        }
        if (check != null) check.close();

        String username = email.contains("@") ? email.split("@")[0] : email;
        if (findUser(username) != null) username += "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String id = "u_" + java.util.UUID.randomUUID();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_ID, id);
        cv.put(DatabaseHelper.COL_USER_EMAIL, email);
        cv.put(DatabaseHelper.COL_USER_USERNAME, username);
        cv.put(DatabaseHelper.COL_USER_FULL_NAME, fullName);
        cv.put(DatabaseHelper.COL_USER_PASSWORD, com.nhom02.streamwire.utils.PasswordHasher.hash(password));
        cv.put(DatabaseHelper.COL_USER_ROLE, getTotalUsersCount() == 0 ? "ADMIN" : "USER");
        cv.put(DatabaseHelper.COL_USER_IS_LOCKED, 0);
        cv.put(DatabaseHelper.COL_USER_AVATAR, "");

        long result = db.insert(DatabaseHelper.TABLE_USERS, null, cv);
        return result != -1;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, null, null, null, null, DatabaseHelper.COL_USER_FULL_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToUser(cursor));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public boolean updateUserRole(String userId, String newRole) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_ROLE, newRole);
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv, DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});
        return rows > 0;
    }

    public boolean toggleUserLock(String userId, boolean isLocked) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_IS_LOCKED, isLocked ? 1 : 0);
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv, DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});
        return rows > 0;
    }

    public boolean updatePassword(String userId, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_PASSWORD, com.nhom02.streamwire.utils.PasswordHasher.hash(newPassword));
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv, DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});
        return rows > 0;
    }

    public int getTotalUsersCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        return count;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_USERNAME)));
        user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FULL_NAME)));
        user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ROLE)));
        user.setLocked(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_IS_LOCKED)) == 1);
        user.setAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_AVATAR)));
        return user;
    }

    // ==========================================
    // 2. CATEGORIES
    // ==========================================

    public void addCategory(String name) {
        if (name == null || name.trim().isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_CAT_ID, "cat_" + Math.abs(name.trim().hashCode()));
        cv.put(DatabaseHelper.COL_CAT_NAME, name.trim());
        db.insertWithOnConflict(DatabaseHelper.TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("all", "Tất cả", true));
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, null, null, null, null, DatabaseHelper.COL_CAT_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            boolean first = false;
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
                list.add(new Category(id, name, first));
                first = false;
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    // ==========================================
    // 3. VIDEOS
    // ==========================================

    public List<Video> getAllVideos() {
        List<Video> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToVideo(cursor, false));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public Video getHeroVideo() {
        List<Video> videos = getPublicVideos(0);
        return videos.isEmpty() ? null : getVideoById(videos.get(0).getId());
    }

    public List<Video> getPublicVideos(int sort) {
        String order = sort == 2 ? "rating DESC" : sort == 1
                ? "CAST(views AS INTEGER) DESC" : "published_at DESC, rowid DESC";
        List<Video> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().query("videos", null, "status = ?",
                new String[]{"ACTIVE"}, null, null, order)) {
            while (c.moveToNext()) result.add(cursorToVideo(c, false));
        }
        return result;
    }

    public List<Video> getTrendingVideos() {
        List<Video> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                DatabaseHelper.COL_VIDEO_STATUS + " = 'ACTIVE' AND " + DatabaseHelper.COL_VIDEO_ID + " != 'hero_01'",
                null, null, null, DatabaseHelper.COL_VIDEO_RATING + " DESC", "4");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToVideo(cursor, false));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public List<Video> getRecommendedVideos() {
        List<Video> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                DatabaseHelper.COL_VIDEO_STATUS + " = 'ACTIVE'",
                null, null, null, DatabaseHelper.COL_VIDEO_YEAR + " DESC", "4");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToVideo(cursor, false));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public Video getVideoById(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                DatabaseHelper.COL_VIDEO_ID + " = ?", new String[]{id}, null, null, null);
        Video video = null;
        if (cursor != null && cursor.moveToFirst()) {
            video = cursorToVideo(cursor, true);
        }
        if (cursor != null) cursor.close();
        return video;
    }

    public List<Video> getVideosByCategory(String categoryName) {
        if ("Tất cả".equalsIgnoreCase(categoryName)) {
            return getTrendingVideos();
        }
        List<Video> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                DatabaseHelper.COL_VIDEO_CATEGORY + " LIKE ? AND " + DatabaseHelper.COL_VIDEO_STATUS + " = 'ACTIVE'",
                new String[]{"%" + categoryName + "%"}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToVideo(cursor, false));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public List<Video> searchVideos(String query) {
        List<Video> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String wild = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                "status = 'ACTIVE' AND (" + DatabaseHelper.COL_VIDEO_TITLE + " LIKE ? OR " + DatabaseHelper.COL_VIDEO_CATEGORY + " LIKE ?)",
                new String[]{wild, wild}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToVideo(cursor, false));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public boolean insertVideo(Video v) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_VIDEO_ID, v.getId());
        cv.put(DatabaseHelper.COL_VIDEO_TITLE, v.getTitle());
        cv.put(DatabaseHelper.COL_VIDEO_DESC, v.getDescription());
        cv.put(DatabaseHelper.COL_VIDEO_URL, v.getVideoUrl());
        cv.put(DatabaseHelper.COL_VIDEO_THUMB, v.getThumbnailUrl());
        cv.put(DatabaseHelper.COL_VIDEO_DURATION, v.getDuration());
        cv.put(DatabaseHelper.COL_VIDEO_DURATION_MS, v.getDurationMs());
        cv.put(DatabaseHelper.COL_VIDEO_VIEWS, v.getViews());
        cv.put(DatabaseHelper.COL_VIDEO_RATING, v.getRating());
        cv.put(DatabaseHelper.COL_VIDEO_YEAR, v.getReleaseYear());
        cv.put(DatabaseHelper.COL_VIDEO_AGE, v.getAgeRating());
        cv.put(DatabaseHelper.COL_VIDEO_QUALITY, v.getQuality());
        cv.put(DatabaseHelper.COL_VIDEO_CATEGORY, v.getCategory());
        cv.put(DatabaseHelper.COL_VIDEO_STATUS, v.getStatus());
        cv.put(DatabaseHelper.COL_VIDEO_PUBLISHED, v.getPublishedAt());

        long result = db.insert(DatabaseHelper.TABLE_VIDEOS, null, cv);
        if (result == -1) return false;

        if (v.getCategory() != null && !v.getCategory().trim().isEmpty()) {
            addCategory(v.getCategory().trim());
        }

        // Add default episode if none
        if (v.getEpisodes() == null || v.getEpisodes().isEmpty()) {
            Episode ep = new Episode("ep_" + v.getId() + "_1", 1, "Tập 1: Mở đầu",
                    v.getDuration() != null ? v.getDuration() : "45:00",
                    v.getThumbnailUrl(), v.getVideoUrl(), v.getDescription());
            if (!insertEpisode(ep, v.getId())) throw new android.database.SQLException("Cannot save episode");
        } else {
            for (Episode ep : v.getEpisodes()) {
                if (!insertEpisode(ep, v.getId())) throw new android.database.SQLException("Cannot save episode");
            }
        }

        db.setTransactionSuccessful();
        return true;
        } finally { db.endTransaction(); }
    }

    public boolean updateVideoDetails(Video video) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", video.getTitle()); cv.put("description", video.getDescription());
        cv.put("video_url", video.getVideoUrl()); cv.put("thumbnail_url", video.getThumbnailUrl());
        cv.put("category", video.getCategory()); cv.put("duration", video.getDuration());
        db.beginTransaction();
        try {
            if (db.update("videos", cv, "id=?", new String[]{video.getId()}) == 0) return false;
            // The automatically generated single episode follows the video's source.
            ContentValues ep = new ContentValues(); ep.put("video_url", video.getVideoUrl());
            ep.put("thumbnail_url", video.getThumbnailUrl()); ep.put("description", video.getDescription());
            ep.put("duration", video.getDuration());
            db.update("episodes", ep, "id=? AND video_id=?", new String[]{"ep_" + video.getId() + "_1", video.getId()});
            addCategory(video.getCategory());
            db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }

    public boolean updateVideoTitle(String videoId, String newTitle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_VIDEO_TITLE, newTitle);
        int rows = db.update(DatabaseHelper.TABLE_VIDEOS, cv, DatabaseHelper.COL_VIDEO_ID + " = ?", new String[]{videoId});
        return rows > 0;
    }

    public boolean updateVideoStatus(String videoId, String newStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_VIDEO_STATUS, newStatus);
        int rows = db.update(DatabaseHelper.TABLE_VIDEOS, cv, DatabaseHelper.COL_VIDEO_ID + " = ?", new String[]{videoId});
        return rows > 0;
    }

    public boolean deleteVideo(String videoId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_EPISODES, DatabaseHelper.COL_EP_VIDEO_ID + " = ?", new String[]{videoId});
        db.delete(DatabaseHelper.TABLE_CAST, DatabaseHelper.COL_CAST_VIDEO_ID + " = ?", new String[]{videoId});
        db.delete(DatabaseHelper.TABLE_HISTORY, DatabaseHelper.COL_HIST_VIDEO_ID + " = ?", new String[]{videoId});
        int rows = db.delete(DatabaseHelper.TABLE_VIDEOS, DatabaseHelper.COL_VIDEO_ID + " = ?", new String[]{videoId});
        return rows > 0;
    }

    public int getActiveVideosCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_VIDEOS + " WHERE " + DatabaseHelper.COL_VIDEO_STATUS + " = 'ACTIVE'", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        return count;
    }

    private Video cursorToVideo(Cursor cursor, boolean loadDetails) {
        Video v = new Video();
        v.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_ID)));
        v.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_TITLE)));
        v.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_DESC)));
        v.setVideoUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_URL)));
        v.setThumbnailUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_THUMB)));
        v.setDuration(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_DURATION)));
        v.setDurationMs(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_DURATION_MS)));
        v.setViews(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_VIEWS)));
        v.setRating(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_RATING)));
        v.setReleaseYear(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_YEAR)));
        v.setAgeRating(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_AGE)));
        v.setQuality(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_QUALITY)));
        v.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_CATEGORY)));
        v.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_STATUS)));
        v.setPublishedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_VIDEO_PUBLISHED)));

        if (loadDetails) {
            v.setEpisodes(getEpisodesByVideoId(v.getId()));
            v.setCastList(getCastByVideoId(v.getId()));
        }

        return v;
    }

    // ==========================================
    // 4. EPISODES & CAST
    // ==========================================

    public List<Episode> getEpisodesByVideoId(String videoId) {
        List<Episode> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_EPISODES, null,
                DatabaseHelper.COL_EP_VIDEO_ID + " = ?", new String[]{videoId},
                null, null, DatabaseHelper.COL_EP_NUMBER + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Episode ep = new Episode();
                ep.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_ID)));
                ep.setEpisodeNumber(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_NUMBER)));
                ep.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_TITLE)));
                ep.setDuration(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_DURATION)));
                ep.setThumbnailUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_THUMB)));
                ep.setVideoUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_URL)));
                ep.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EP_DESC)));
                list.add(ep);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public boolean insertEpisode(Episode ep, String videoId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EP_ID, ep.getId());
        cv.put(DatabaseHelper.COL_EP_VIDEO_ID, videoId);
        cv.put(DatabaseHelper.COL_EP_NUMBER, ep.getEpisodeNumber());
        cv.put(DatabaseHelper.COL_EP_TITLE, ep.getTitle());
        cv.put(DatabaseHelper.COL_EP_DURATION, ep.getDuration());
        cv.put(DatabaseHelper.COL_EP_THUMB, ep.getThumbnailUrl());
        cv.put(DatabaseHelper.COL_EP_URL, ep.getVideoUrl());
        cv.put(DatabaseHelper.COL_EP_DESC, ep.getDescription());
        long r = db.insertWithOnConflict(DatabaseHelper.TABLE_EPISODES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return r != -1;
    }

    public List<CastMember> getCastByVideoId(String videoId) {
        List<CastMember> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CAST, null,
                DatabaseHelper.COL_CAST_VIDEO_ID + " = ?", new String[]{videoId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                CastMember cm = new CastMember();
                cm.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAST_ID)));
                cm.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAST_NAME)));
                cm.setRole(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAST_ROLE)));
                cm.setAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAST_AVATAR)));
                list.add(cm);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    // ==========================================
    // 5. WATCH HISTORY
    // ==========================================

    public List<WatchHistory> getWatchHistories() {
        List<WatchHistory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HISTORY, null, "user_id = ?", new String[]{owner()}, null, null, DatabaseHelper.COL_HIST_UPDATED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                WatchHistory h = new WatchHistory();
                h.setId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_ID)));
                String videoId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_VIDEO_ID));
                h.setVideo(getVideoById(videoId));
                h.setEpisodeTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_EP_TITLE)));
                h.setLastPositionMs(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_LAST_POS)));
                h.setTotalDurationMs(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_TOTAL_DUR)));
                h.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_UPDATED)));
                if (h.getVideo() != null) list.add(h);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public boolean saveOrUpdateHistory(String videoId, String episodeTitle, long lastPositionMs, long totalDurationMs) {
        if (videoId == null || totalDurationMs <= 0 || getVideoById(videoId) == null) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if history for this video exists
        String existingId = null;
        Cursor cursor = db.query(DatabaseHelper.TABLE_HISTORY, new String[]{DatabaseHelper.COL_HIST_ID},
                DatabaseHelper.COL_HIST_VIDEO_ID + " = ? AND user_id = ? AND episode_title = ?", new String[]{videoId, owner(), episodeTitle != null ? episodeTitle : "Tập 1"}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            existingId = cursor.getString(0);
        }
        if (cursor != null) cursor.close();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_HIST_VIDEO_ID, videoId);
        cv.put(DatabaseHelper.COL_HIST_EP_TITLE, episodeTitle != null ? episodeTitle : "Tập 1");
        cv.put(DatabaseHelper.COL_HIST_LAST_POS, Math.max(0, Math.min(lastPositionMs, totalDurationMs)));
        cv.put(DatabaseHelper.COL_HIST_TOTAL_DUR, totalDurationMs);
        cv.put(DatabaseHelper.COL_HIST_UPDATED, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(new java.util.Date()));
        cv.put("user_id", owner());

        if (existingId != null) {
            int rows = db.update(DatabaseHelper.TABLE_HISTORY, cv, DatabaseHelper.COL_HIST_ID + " = ?", new String[]{existingId});
            return rows > 0;
        } else {
            cv.put(DatabaseHelper.COL_HIST_ID, "hist_" + java.util.UUID.randomUUID());
            long r = db.insert(DatabaseHelper.TABLE_HISTORY, null, cv);
            return r != -1;
        }
    }

    public boolean deleteHistory(String historyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_HISTORY, DatabaseHelper.COL_HIST_ID + " = ? AND user_id = ?", new String[]{historyId, owner()});
        return rows > 0;
    }

    public boolean clearAllHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_HISTORY, "user_id = ?", new String[]{owner()});
        return rows >= 0;
    }

    private String owner() {
        com.nhom02.streamwire.utils.SessionManager session = new com.nhom02.streamwire.utils.SessionManager(context);
        User user = session.getUser();
        return session.isGuest() || user == null ? "guest" : user.getId();
    }

    public boolean isFavorite(String videoId) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM favorites WHERE user_id=? AND video_id=?", new String[]{owner(), videoId})) {
            return c.moveToFirst();
        }
    }

    public boolean setFavorite(String videoId, boolean favorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (!favorite) return db.delete("favorites", "user_id=? AND video_id=?", new String[]{owner(), videoId}) >= 0;
        ContentValues cv = new ContentValues();
        cv.put("user_id", owner()); cv.put("video_id", videoId);
        return db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    public List<Video> getFavoriteVideos() {
        List<Video> list = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT v.* FROM videos v JOIN favorites f ON f.video_id=v.id WHERE f.user_id=? AND v.status='ACTIVE'", new String[]{owner()})) {
            while (c.moveToNext()) list.add(cursorToVideo(c, false));
        }
        return list;
    }

    public android.net.Uri getDownloadedUri(String videoId) {
        android.app.DownloadManager downloads = (android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id FROM downloads WHERE user_id=? AND video_id=? ORDER BY id DESC", new String[]{owner(), videoId})) {
            while (c.moveToNext()) {
                android.net.Uri uri = downloads.getUriForDownloadedFile(c.getLong(0));
                if (uri != null) return uri;
            }
        }
        return null;
    }

    public void recordView(String videoId, long durationMs) {
        dbHelper.getWritableDatabase().execSQL("UPDATE videos SET views=CAST(COALESCE(views,'0') AS INTEGER)+1, duration_ms=? WHERE id=?", new Object[]{durationMs, videoId});
    }

    public void saveDownload(long id, String videoId) {
        ContentValues cv = new ContentValues();
        cv.put("id", id); cv.put("user_id", owner()); cv.put("video_id", videoId);
        dbHelper.getWritableDatabase().insertOrThrow("downloads", null, cv);
    }

    public String getSetting(String userId, String name, String fallback) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT value FROM settings WHERE user_id=? AND name=?", new String[]{userId, name})) {
            return c.moveToFirst() ? c.getString(0) : fallback;
        }
    }

    public void setSetting(String userId, String name, String value) {
        ContentValues cv = new ContentValues(); cv.put("user_id", userId); cv.put("name", name); cv.put("value", value);
        dbHelper.getWritableDatabase().insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User findUser(String login) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT * FROM users WHERE email=? COLLATE NOCASE OR username=? OR id=?", new String[]{login, login, login})) {
            return c.moveToFirst() ? cursorToUser(c) : null;
        }
    }
}
