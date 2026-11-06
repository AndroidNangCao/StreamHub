package com.nhom02.streamwire.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.nhom02.streamwire.model.CastMember;
import com.nhom02.streamwire.model.Category;
import com.nhom02.streamwire.model.Episode;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.model.WatchHistory;
import com.nhom02.streamwire.utils.MockDataGenerator;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "streamwire.db";
    private static final int DATABASE_VERSION = 2;

    // Table: Users
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_USERNAME = "username";
    public static final String COL_USER_FULL_NAME = "full_name";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_ROLE = "role"; // ADMIN, USER, CREATOR, STANDARD
    public static final String COL_USER_IS_LOCKED = "is_locked";
    public static final String COL_USER_AVATAR = "avatar_url";

    // Table: Categories
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";

    // Table: Videos
    public static final String TABLE_VIDEOS = "videos";
    public static final String COL_VIDEO_ID = "id";
    public static final String COL_VIDEO_TITLE = "title";
    public static final String COL_VIDEO_DESC = "description";
    public static final String COL_VIDEO_URL = "video_url";
    public static final String COL_VIDEO_THUMB = "thumbnail_url";
    public static final String COL_VIDEO_DURATION = "duration";
    public static final String COL_VIDEO_DURATION_MS = "duration_ms";
    public static final String COL_VIDEO_VIEWS = "views";
    public static final String COL_VIDEO_RATING = "rating";
    public static final String COL_VIDEO_YEAR = "release_year";
    public static final String COL_VIDEO_AGE = "age_rating";
    public static final String COL_VIDEO_QUALITY = "quality";
    public static final String COL_VIDEO_CATEGORY = "category";
    public static final String COL_VIDEO_STATUS = "status"; // ACTIVE, PENDING, REPORTED
    public static final String COL_VIDEO_PUBLISHED = "published_at";

    // Table: Episodes
    public static final String TABLE_EPISODES = "episodes";
    public static final String COL_EP_ID = "id";
    public static final String COL_EP_VIDEO_ID = "video_id";
    public static final String COL_EP_NUMBER = "episode_number";
    public static final String COL_EP_TITLE = "title";
    public static final String COL_EP_DURATION = "duration";
    public static final String COL_EP_THUMB = "thumbnail_url";
    public static final String COL_EP_URL = "video_url";
    public static final String COL_EP_DESC = "description";

    // Table: Cast Members
    public static final String TABLE_CAST = "cast_members";
    public static final String COL_CAST_ID = "id";
    public static final String COL_CAST_VIDEO_ID = "video_id";
    public static final String COL_CAST_NAME = "name";
    public static final String COL_CAST_ROLE = "role";
    public static final String COL_CAST_AVATAR = "avatar_url";

    // Table: Watch History
    public static final String TABLE_HISTORY = "watch_history";
    public static final String COL_HIST_ID = "id";
    public static final String COL_HIST_VIDEO_ID = "video_id";
    public static final String COL_HIST_EP_TITLE = "episode_title";
    public static final String COL_HIST_LAST_POS = "last_position_ms";
    public static final String COL_HIST_TOTAL_DUR = "total_duration_ms";
    public static final String COL_HIST_UPDATED = "updated_at";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tạo bảng Users
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " TEXT PRIMARY KEY, "
                + COL_USER_EMAIL + " TEXT UNIQUE, "
                + COL_USER_USERNAME + " TEXT, "
                + COL_USER_FULL_NAME + " TEXT, "
                + COL_USER_PASSWORD + " TEXT, "
                + COL_USER_ROLE + " TEXT, "
                + COL_USER_IS_LOCKED + " INTEGER DEFAULT 0, "
                + COL_USER_AVATAR + " TEXT)";
        db.execSQL(createUsersTable);

        // 2. Tạo bảng Categories
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " ("
                + COL_CAT_ID + " TEXT PRIMARY KEY, "
                + COL_CAT_NAME + " TEXT UNIQUE)";
        db.execSQL(createCategoriesTable);

        // 3. Tạo bảng Videos
        String createVideosTable = "CREATE TABLE " + TABLE_VIDEOS + " ("
                + COL_VIDEO_ID + " TEXT PRIMARY KEY, "
                + COL_VIDEO_TITLE + " TEXT, "
                + COL_VIDEO_DESC + " TEXT, "
                + COL_VIDEO_URL + " TEXT, "
                + COL_VIDEO_THUMB + " TEXT, "
                + COL_VIDEO_DURATION + " TEXT, "
                + COL_VIDEO_DURATION_MS + " INTEGER, "
                + COL_VIDEO_VIEWS + " TEXT, "
                + COL_VIDEO_RATING + " REAL, "
                + COL_VIDEO_YEAR + " INTEGER, "
                + COL_VIDEO_AGE + " TEXT, "
                + COL_VIDEO_QUALITY + " TEXT, "
                + COL_VIDEO_CATEGORY + " TEXT, "
                + COL_VIDEO_STATUS + " TEXT, "
                + COL_VIDEO_PUBLISHED + " TEXT)";
        db.execSQL(createVideosTable);

        // 4. Tạo bảng Episodes
        String createEpisodesTable = "CREATE TABLE " + TABLE_EPISODES + " ("
                + COL_EP_ID + " TEXT PRIMARY KEY, "
                + COL_EP_VIDEO_ID + " TEXT, "
                + COL_EP_NUMBER + " INTEGER, "
                + COL_EP_TITLE + " TEXT, "
                + COL_EP_DURATION + " TEXT, "
                + COL_EP_THUMB + " TEXT, "
                + COL_EP_URL + " TEXT, "
                + COL_EP_DESC + " TEXT, "
                + "FOREIGN KEY(" + COL_EP_VIDEO_ID + ") REFERENCES " + TABLE_VIDEOS + "(" + COL_VIDEO_ID + ") ON DELETE CASCADE)";
        db.execSQL(createEpisodesTable);

        // 5. Tạo bảng Cast Members
        String createCastTable = "CREATE TABLE " + TABLE_CAST + " ("
                + COL_CAST_ID + " TEXT PRIMARY KEY, "
                + COL_CAST_VIDEO_ID + " TEXT, "
                + COL_CAST_NAME + " TEXT, "
                + COL_CAST_ROLE + " TEXT, "
                + COL_CAST_AVATAR + " TEXT, "
                + "FOREIGN KEY(" + COL_CAST_VIDEO_ID + ") REFERENCES " + TABLE_VIDEOS + "(" + COL_VIDEO_ID + ") ON DELETE CASCADE)";
        db.execSQL(createCastTable);

        // 6. Tạo bảng Watch History
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " ("
                + COL_HIST_ID + " TEXT PRIMARY KEY, "
                + COL_HIST_VIDEO_ID + " TEXT, "
                + COL_HIST_EP_TITLE + " TEXT, "
                + COL_HIST_LAST_POS + " INTEGER, "
                + COL_HIST_TOTAL_DUR + " INTEGER, "
                + COL_HIST_UPDATED + " TEXT, "
                + "FOREIGN KEY(" + COL_HIST_VIDEO_ID + ") REFERENCES " + TABLE_VIDEOS + "(" + COL_VIDEO_ID + ") ON DELETE CASCADE)";
        db.execSQL(createHistoryTable);

        // 7. Seed Initial Data
        seedInitialData(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        // Seed Users
        insertUserRaw(db, "u_admin", "admin@streamwire.com", "admin_boss", "Quản Trị Viên", "123456", "ADMIN", 0, "");
        insertUserRaw(db, "u1", "user@streamwire.com", "an_nguyen99", "Nguyễn Văn An", "123456", "USER", 0, "");
        insertUserRaw(db, "u2", "bich.tran@email.com", "bich_tt", "Trần Thị Bích", "123456", "STANDARD", 1, "");
        insertUserRaw(db, "u3", "nam.creator@email.com", "nam_creator", "Lê Hoàng Nam", "123456", "CREATOR", 0, "");

        // Seed Categories
        List<Category> categories = MockDataGenerator.getCategories();
        for (Category c : categories) {
            ContentValues cv = new ContentValues();
            cv.put(COL_CAT_ID, c.getId());
            cv.put(COL_CAT_NAME, c.getName());
            db.insert(TABLE_CATEGORIES, null, cv);
        }

        // Seed Hero Video
        Video hero = MockDataGenerator.getHeroVideo();
        insertVideoWithDetailsRaw(db, hero);

        // Seed Sample Detailed Video
        Video sampleDetail = MockDataGenerator.getDetailedSampleVideo();
        insertVideoWithDetailsRaw(db, sampleDetail);

        // Seed Trending Videos
        List<Video> trending = MockDataGenerator.getTrendingVideos();
        for (Video v : trending) {
            insertVideoWithDetailsRaw(db, v);
        }

        // Seed Recommended Videos
        List<Video> rec = MockDataGenerator.getRecommendedVideos();
        for (Video v : rec) {
            insertVideoWithDetailsRaw(db, v);
        }

        // Seed Admin Reported & Pending Videos
        List<Video> adminVids = MockDataGenerator.getAdminVideos();
        for (Video v : adminVids) {
            insertVideoWithDetailsRaw(db, v);
        }

        // Seed Watch History
        List<WatchHistory> histories = MockDataGenerator.getWatchHistories();
        for (WatchHistory h : histories) {
            if (h.getVideo() != null) {
                ContentValues cv = new ContentValues();
                cv.put(COL_HIST_ID, h.getId());
                cv.put(COL_HIST_VIDEO_ID, h.getVideo().getId());
                cv.put(COL_HIST_EP_TITLE, h.getEpisodeTitle());
                cv.put(COL_HIST_LAST_POS, h.getLastPositionMs());
                cv.put(COL_HIST_TOTAL_DUR, h.getTotalDurationMs());
                cv.put(COL_HIST_UPDATED, h.getUpdatedAt());
                db.insertWithOnConflict(TABLE_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    private void insertUserRaw(SQLiteDatabase db, String id, String email, String username,
                               String fullName, String password, String role, int isLocked, String avatar) {
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_ID, id);
        cv.put(COL_USER_EMAIL, email);
        cv.put(COL_USER_USERNAME, username);
        cv.put(COL_USER_FULL_NAME, fullName);
        cv.put(COL_USER_PASSWORD, password);
        cv.put(COL_USER_ROLE, role);
        cv.put(COL_USER_IS_LOCKED, isLocked);
        cv.put(COL_USER_AVATAR, avatar);
        db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void insertVideoWithDetailsRaw(SQLiteDatabase db, Video v) {
        ContentValues cv = new ContentValues();
        cv.put(COL_VIDEO_ID, v.getId());
        cv.put(COL_VIDEO_TITLE, v.getTitle());
        cv.put(COL_VIDEO_DESC, v.getDescription());
        cv.put(COL_VIDEO_URL, v.getVideoUrl());
        cv.put(COL_VIDEO_THUMB, v.getThumbnailUrl());
        cv.put(COL_VIDEO_DURATION, v.getDuration());
        cv.put(COL_VIDEO_DURATION_MS, v.getDurationMs());
        cv.put(COL_VIDEO_VIEWS, v.getViews());
        cv.put(COL_VIDEO_RATING, v.getRating());
        cv.put(COL_VIDEO_YEAR, v.getReleaseYear());
        cv.put(COL_VIDEO_AGE, v.getAgeRating());
        cv.put(COL_VIDEO_QUALITY, v.getQuality());
        cv.put(COL_VIDEO_CATEGORY, v.getCategory());
        cv.put(COL_VIDEO_STATUS, v.getStatus());
        cv.put(COL_VIDEO_PUBLISHED, v.getPublishedAt());
        db.insertWithOnConflict(TABLE_VIDEOS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Episodes
        if (v.getEpisodes() != null && !v.getEpisodes().isEmpty()) {
            for (Episode ep : v.getEpisodes()) {
                ContentValues epCv = new ContentValues();
                epCv.put(COL_EP_ID, ep.getId());
                epCv.put(COL_EP_VIDEO_ID, v.getId());
                epCv.put(COL_EP_NUMBER, ep.getEpisodeNumber());
                epCv.put(COL_EP_TITLE, ep.getTitle());
                epCv.put(COL_EP_DURATION, ep.getDuration());
                epCv.put(COL_EP_THUMB, ep.getThumbnailUrl());
                epCv.put(COL_EP_URL, ep.getVideoUrl());
                epCv.put(COL_EP_DESC, ep.getDescription());
                db.insertWithOnConflict(TABLE_EPISODES, null, epCv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        // Cast
        if (v.getCastList() != null && !v.getCastList().isEmpty()) {
            for (CastMember cm : v.getCastList()) {
                ContentValues castCv = new ContentValues();
                castCv.put(COL_CAST_ID, cm.getId());
                castCv.put(COL_CAST_VIDEO_ID, v.getId());
                castCv.put(COL_CAST_NAME, cm.getName());
                castCv.put(COL_CAST_ROLE, cm.getRole());
                castCv.put(COL_CAST_AVATAR, cm.getAvatarUrl());
                db.insertWithOnConflict(TABLE_CAST, null, castCv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EPISODES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VIDEOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}

