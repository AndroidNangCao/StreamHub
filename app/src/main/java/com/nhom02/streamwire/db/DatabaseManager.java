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

    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseManager(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    // ==========================================
    // 1. AUTH & USERS
    // ==========================================

    public User authenticate(String emailOrUsername, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_USERS
                + " WHERE (" + DatabaseHelper.COL_USER_EMAIL + " = ? OR "
                + DatabaseHelper.COL_USER_USERNAME + " = ?) AND "
                + DatabaseHelper.COL_USER_PASSWORD + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{emailOrUsername, emailOrUsername, password});
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public boolean registerUser(String fullName, String email, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if email already exists
        Cursor check = db.query(DatabaseHelper.TABLE_USERS, null,
                DatabaseHelper.COL_USER_EMAIL + " = ?", new String[]{email}, null, null, null);
        if (check != null && check.getCount() > 0) {
            check.close();
            return false;
        }
        if (check != null) check.close();

        String username = email.contains("@") ? email.split("@")[0] : email;
        String id = "u_" + System.currentTimeMillis();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_ID, id);
        cv.put(DatabaseHelper.COL_USER_EMAIL, email);
        cv.put(DatabaseHelper.COL_USER_USERNAME, username);
        cv.put(DatabaseHelper.COL_USER_FULL_NAME, fullName);
        cv.put(DatabaseHelper.COL_USER_PASSWORD, password);
        cv.put(DatabaseHelper.COL_USER_ROLE, "USER");
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
            cursor.close();
        }
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
        cv.put(DatabaseHelper.COL_USER_PASSWORD, newPassword);
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv, DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});
        return rows > 0;
    }

    public int getTotalUsersCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
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

    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, null, null, null, null, DatabaseHelper.COL_CAT_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            boolean first = true;
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
                list.add(new Category(id, name, first));
                first = false;
            } while (cursor.moveToNext());
            cursor.close();
        }
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
            cursor.close();
        }
        return list;
    }

    public Video getHeroVideo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                DatabaseHelper.COL_VIDEO_ID + " = ?", new String[]{"hero_01"}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Video v = cursorToVideo(cursor, true);
            cursor.close();
            return v;
        }
        if (cursor != null) cursor.close();

        // Fallback to first video
        List<Video> all = getAllVideos();
        return all.isEmpty() ? null : all.get(0);
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
            cursor.close();
        }
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
            cursor.close();
        }
        return list;
    }

    public Video getVideoById(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                DatabaseHelper.COL_VIDEO_ID + " = ?", new String[]{id}, null, null, null);
        Video video = null;
        if (cursor != null && cursor.moveToFirst()) {
            video = cursorToVideo(cursor, true);
            cursor.close();
        }
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
            cursor.close();
        }
        return list;
    }

    public List<Video> searchVideos(String query) {
        List<Video> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String wild = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_VIDEOS, null,
                "(" + DatabaseHelper.COL_VIDEO_TITLE + " LIKE ? OR " + DatabaseHelper.COL_VIDEO_CATEGORY + " LIKE ?)",
                new String[]{wild, wild}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToVideo(cursor, false));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean insertVideo(Video v) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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

        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_VIDEOS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Add default episode if none
        if (v.getEpisodes() == null || v.getEpisodes().isEmpty()) {
            Episode ep = new Episode("ep_" + v.getId() + "_1", 1, "Tập 1: Mở đầu",
                    v.getDuration() != null ? v.getDuration() : "45:00",
                    v.getThumbnailUrl(), v.getVideoUrl(), v.getDescription());
            insertEpisode(ep, v.getId());
        } else {
            for (Episode ep : v.getEpisodes()) {
                insertEpisode(ep, v.getId());
            }
        }

        return result != -1;
    }

    public void syncFromApi(List<Category> categories, List<Video> videos) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (categories != null && !categories.isEmpty()) {
                db.delete(DatabaseHelper.TABLE_CATEGORIES, null, null);
                for (Category c : categories) {
                    ContentValues cv = new ContentValues();
                    cv.put(DatabaseHelper.COL_CAT_ID, c.getId());
                    cv.put(DatabaseHelper.COL_CAT_NAME, c.getName());
                    db.insertWithOnConflict(DatabaseHelper.TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            if (videos != null && !videos.isEmpty()) {
                for (Video v : videos) {
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
                    cv.put(DatabaseHelper.COL_VIDEO_STATUS, v.getStatus() != null ? v.getStatus() : "ACTIVE");
                    cv.put(DatabaseHelper.COL_VIDEO_PUBLISHED, v.getPublishedAt());
                    db.insertWithOnConflict(DatabaseHelper.TABLE_VIDEOS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

                    if (v.getEpisodes() != null && !v.getEpisodes().isEmpty()) {
                        for (Episode ep : v.getEpisodes()) {
                            insertEpisode(ep, v.getId());
                        }
                    } else {
                        Episode ep = new Episode("ep_" + v.getId() + "_1", 1, "Tập 1: Mở đầu",
                                v.getDuration() != null ? v.getDuration() : "45:00",
                                v.getThumbnailUrl(), v.getVideoUrl(), v.getDescription());
                        insertEpisode(ep, v.getId());
                    }

                    if (v.getCastList() != null && !v.getCastList().isEmpty()) {
                        for (CastMember cm : v.getCastList()) {
                            ContentValues castCv = new ContentValues();
                            castCv.put(DatabaseHelper.COL_CAST_ID, cm.getId());
                            castCv.put(DatabaseHelper.COL_CAST_VIDEO_ID, v.getId());
                            castCv.put(DatabaseHelper.COL_CAST_NAME, cm.getName());
                            castCv.put(DatabaseHelper.COL_CAST_ROLE, cm.getRole());
                            castCv.put(DatabaseHelper.COL_CAST_AVATAR, cm.getAvatarUrl());
                            db.insertWithOnConflict(DatabaseHelper.TABLE_CAST, null, castCv, SQLiteDatabase.CONFLICT_REPLACE);
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
            cursor.close();
        }
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
            cursor.close();
        }
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
            cursor.close();
        }
        return list;
    }

    // ==========================================
    // 5. WATCH HISTORY
    // ==========================================

    public List<WatchHistory> getWatchHistories() {
        List<WatchHistory> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HISTORY, null, null, null, null, null, DatabaseHelper.COL_HIST_UPDATED + " DESC");

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
                list.add(h);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean saveOrUpdateHistory(String videoId, String episodeTitle, long lastPositionMs, long totalDurationMs) {
        if (videoId == null || totalDurationMs <= 0) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if history for this video exists
        String existingId = null;
        Cursor cursor = db.query(DatabaseHelper.TABLE_HISTORY, new String[]{DatabaseHelper.COL_HIST_ID},
                DatabaseHelper.COL_HIST_VIDEO_ID + " = ?", new String[]{videoId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            existingId = cursor.getString(0);
            cursor.close();
        }
        if (cursor != null) cursor.close();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_HIST_VIDEO_ID, videoId);
        cv.put(DatabaseHelper.COL_HIST_EP_TITLE, episodeTitle != null ? episodeTitle : "Tập 1");
        cv.put(DatabaseHelper.COL_HIST_LAST_POS, lastPositionMs);
        cv.put(DatabaseHelper.COL_HIST_TOTAL_DUR, totalDurationMs);
        cv.put(DatabaseHelper.COL_HIST_UPDATED, "Vừa xem");

        if (existingId != null) {
            int rows = db.update(DatabaseHelper.TABLE_HISTORY, cv, DatabaseHelper.COL_HIST_ID + " = ?", new String[]{existingId});
            return rows > 0;
        } else {
            cv.put(DatabaseHelper.COL_HIST_ID, "hist_" + System.currentTimeMillis());
            long r = db.insert(DatabaseHelper.TABLE_HISTORY, null, cv);
            return r != -1;
        }
    }

    public boolean deleteHistory(String historyId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_HISTORY, DatabaseHelper.COL_HIST_ID + " = ?", new String[]{historyId});
        return rows > 0;
    }

    public boolean clearAllHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_HISTORY, null, null);
        return rows >= 0;
    }
}

