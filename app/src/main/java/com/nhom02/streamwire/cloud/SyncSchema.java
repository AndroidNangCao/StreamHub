package com.nhom02.streamwire.cloud;

import android.database.sqlite.SQLiteDatabase;

/** SQLite triggers commit the data change and its outbox entry atomically, including deletions. */
public final class SyncSchema {
    private SyncSchema() {}
    public static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_context (id INTEGER PRIMARY KEY CHECK(id=1), uid TEXT NOT NULL, importing INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO sync_context(id,uid) VALUES(1,'guest')");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (owner_id TEXT NOT NULL, entity TEXT NOT NULL, record_id TEXT NOT NULL, deleted INTEGER NOT NULL, revision TEXT NOT NULL, PRIMARY KEY(owner_id,entity,record_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_state (name TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_known_videos (video_id TEXT PRIMARY KEY)");
        triggers(db, "watch_history", "history", "id", true, "");
        triggers(db, "favorites", "favorite", "video_id", true, "");
        triggers(db, "settings", "setting", "name", true, "");
        triggers(db, "videos", "video", "id", false,
                " OF title,description,video_url,thumbnail_url,duration,rating,release_year,age_rating,quality,category,status,published_at");
        // Do not queue account creation, cached profiles or password hashes.
        trigger(db, "users", "profile", "id", false, "UPDATE OF role,is_locked", "NEW", false);
    }

    private static void triggers(SQLiteDatabase db, String table, String entity, String key, boolean personal, String updateColumns) {
        trigger(db, table, entity, key, personal, "INSERT", "NEW", false);
        trigger(db, table, entity, key, personal, "UPDATE" + updateColumns, "NEW", false);
        trigger(db, table, entity, key, personal, "DELETE", "OLD", true);
    }

    private static void trigger(SQLiteDatabase db, String table, String entity, String key, boolean personal,
                                String event, String row, boolean deleted) {
        String owner = personal ? row + ".user_id" : "(SELECT uid FROM sync_context WHERE id=1)";
        String name = "sync_" + table + "_" + event.split(" ")[0].toLowerCase(java.util.Locale.ROOT);
        db.execSQL("CREATE TRIGGER IF NOT EXISTS " + name + " AFTER " + event + " ON " + table
                + " WHEN (SELECT importing FROM sync_context WHERE id=1)=0 AND " + owner + " NOT IN ('guest','legacy')"
                + " BEGIN INSERT OR REPLACE INTO sync_outbox(owner_id,entity,record_id,deleted,revision) VALUES("
                + owner + ",'" + entity + "'," + row + "." + key + "," + (deleted ? 1 : 0) + ",lower(hex(randomblob(16)))); END");
    }
}
