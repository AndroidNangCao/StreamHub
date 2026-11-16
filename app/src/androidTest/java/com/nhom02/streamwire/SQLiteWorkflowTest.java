package com.nhom02.streamwire;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.nhom02.streamwire.db.DatabaseHelper;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.utils.SessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SQLiteWorkflowTest {
    private Context context;
    private DatabaseManager manager;
    private DatabaseHelper helper;
    private SessionManager session;

    @Before public void setup() {
        context = new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory,
                    android.database.DatabaseErrorHandler handler) {
                return super.openOrCreateDatabase("test_" + name, mode, factory, handler);
            }
            @Override public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("test_" + name, mode);
            }
        };
        manager = DatabaseManager.getInstance(context);
        helper = DatabaseHelper.getInstance(context);
        session = new SessionManager(context);
        session.clearSession();
        SQLiteDatabase db = helper.getWritableDatabase();
        for (String table : new String[]{"favorites", "downloads", "watch_history", "cast_members", "episodes", "videos", "categories", "settings", "users"}) db.delete(table, null, null);
    }

    private Video video(String id, String status) {
        Video v = new Video(); v.setId(id); v.setTitle("Test video"); v.setVideoUrl("https://example.com/video.mp4");
        v.setCategory("Test"); v.setStatus(status); v.setPublishedAt("2026-11-16"); return v;
    }

    private User login(String email) {
        assertTrue(manager.registerUser("Tester", email, "test-password"));
        User user = manager.authenticate(email, "test-password"); assertNotNull(user);
        session.saveAuthToken("local-session"); session.saveUser(user); return user;
    }

    @Test public void firstAccountIsAdminAndDataSurvivesReopen() {
        assertEquals(0, manager.getTotalUsersCount()); assertTrue(manager.getWatchHistories().isEmpty());
        User admin = login("admin@test.local"); assertEquals("ADMIN", admin.getRole());
        assertNull(manager.authenticate("admin@test.local", "wrong"));
        assertTrue(manager.insertVideo(video("saved", "ACTIVE")));
        session.setDefaultQuality("720p HD");
        helper.close();
        assertEquals("Test video", manager.getVideoById("saved").getTitle());
        assertEquals("720p HD", new SessionManager(context).getDefaultQuality());
        assertEquals("USER", login("user@test.local").getRole());
    }

    @Test public void historyFavoritesAndSettingsAreScopedToAccount() {
        User first = login("first@test.local");
        assertTrue(manager.insertVideo(video("video", "ACTIVE")));
        assertTrue(manager.saveOrUpdateHistory("video", "Episode 1", 1000, 5000));
        assertTrue(manager.saveOrUpdateHistory("video", "Episode 2", 2000, 5000));
        assertTrue(manager.setFavorite("video", true)); session.setDefaultQuality("480p SD");
        User second = login("second@test.local");
        assertTrue(manager.getWatchHistories().isEmpty()); assertFalse(manager.isFavorite("video"));
        assertEquals("Tự động (Auto)", session.getDefaultQuality());
        assertTrue(manager.saveOrUpdateHistory("video", "Episode 1", 3000, 5000));
        session.saveUser(first);
        assertEquals(2, manager.getWatchHistories().size()); assertTrue(manager.isFavorite("video"));
        manager.clearAllHistory(); session.saveUser(second);
        assertEquals(1, manager.getWatchHistories().size());
    }

    @Test public void hiddenVideosAreNotPublicAndDeleteCascades() {
        login("admin@test.local");
        assertTrue(manager.insertVideo(video("active", "ACTIVE")));
        assertTrue(manager.insertVideo(video("pending", "PENDING")));
        assertEquals(1, manager.searchVideos("Test").size());
        assertEquals(1, manager.getPublicVideos(0).size());
        manager.setFavorite("active", true); manager.saveDownload(123, "active");
        manager.saveOrUpdateHistory("active", "Episode 1", 100, 200);
        assertTrue(manager.deleteVideo("active"));
        assertTrue(manager.getWatchHistories().isEmpty()); assertTrue(manager.getFavoriteVideos().isEmpty());
        assertTrue(manager.getEpisodesByVideoId("active").isEmpty());
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM downloads", null)) {
            assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0));
        }
        assertNull(manager.getHeroVideo());
    }

    @Test public void upgradePreservesExistingRows() {
        try (SQLiteDatabase old = SQLiteDatabase.create(null)) {
            old.execSQL("CREATE TABLE videos (id TEXT PRIMARY KEY)");
            old.execSQL("CREATE TABLE users (id TEXT PRIMARY KEY)");
            old.execSQL("CREATE TABLE watch_history (id TEXT PRIMARY KEY, video_id TEXT)");
            old.execSQL("INSERT INTO users VALUES ('existing')");
            old.execSQL("INSERT INTO watch_history VALUES ('history', NULL)");
            helper.onUpgrade(old, 2, 3);
            try (Cursor c = old.rawQuery("SELECT id FROM users", null)) { assertTrue(c.moveToFirst()); assertEquals("existing", c.getString(0)); }
            try (Cursor c = old.rawQuery("SELECT user_id FROM watch_history", null)) { assertTrue(c.moveToFirst()); assertEquals("legacy", c.getString(0)); }
        }
    }

    @Test public void emptyHomeAndAdminScreensOpen() {
        User user = login("screen@test.local");
        SessionManager runtimeSession = new SessionManager(InstrumentationRegistry.getInstrumentation().getTargetContext());
        runtimeSession.saveUser(user);
        runtimeSession.saveAuthToken("test-session");
        runtimeSession.setLightSensorEnabled(false);
        android.content.Intent intent = new android.content.Intent(context, com.nhom02.streamwire.ui.main.MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try (androidx.test.core.app.ActivityScenario<com.nhom02.streamwire.ui.main.MainActivity> screen =
                     androidx.test.core.app.ActivityScenario.launch(intent)) {
            screen.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.tvEmptyLibrary));
                assertEquals(android.view.View.VISIBLE, activity.findViewById(R.id.tvEmptyLibrary).getVisibility());
                activity.loadFragment(new com.nhom02.streamwire.ui.main.AdminFragment(), "Quản trị");
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> assertNotNull(activity.findViewById(R.id.etSearchAdminVideo)));
        } finally { runtimeSession.clearSession(); }
    }
}
