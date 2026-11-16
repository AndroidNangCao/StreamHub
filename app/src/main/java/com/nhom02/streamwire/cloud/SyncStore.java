package com.nhom02.streamwire.cloud;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.nhom02.streamwire.db.DatabaseHelper;
import com.nhom02.streamwire.model.User;
import java.util.*;

/** Durable outbox and cloud import boundary. All imports suppress triggers inside the same transaction. */
public final class SyncStore {
    private final DatabaseHelper helper;
    public SyncStore(Context context) { helper = DatabaseHelper.getInstance(context); }

    public void setOwner(String uid) {
        helper.getWritableDatabase().execSQL("UPDATE sync_context SET uid=? WHERE id=1", new Object[]{uid == null ? "guest" : uid});
    }

    public static final class Change {
        public String owner, entity, id, revision;
        public boolean deleted;
        public Map<String,Object> data;
    }

    private static String table(String entity) {
        switch (entity) {
            case "video": return "videos";
            case "profile": return "users";
            case "history": return "watch_history";
            case "favorite": return "favorites";
            case "setting": return "settings";
            default: throw new IllegalArgumentException("Unknown entity");
        }
    }

    private static String key(String entity) {
        return "favorite".equals(entity) ? "video_id" : "setting".equals(entity) ? "name" : "id";
    }

    private Map<String,Object> row(Cursor c) {
        Map<String,Object> data = new HashMap<>();
        for (String name : c.getColumnNames()) {
            if ("password".equals(name)) continue;
            int i = c.getColumnIndexOrThrow(name);
            if (c.isNull(i)) data.put(name, null);
            else if (c.getType(i) == Cursor.FIELD_TYPE_INTEGER) data.put(name, c.getLong(i));
            else if (c.getType(i) == Cursor.FIELD_TYPE_FLOAT) data.put(name, c.getDouble(i));
            else data.put(name, c.getString(i));
        }
        return data;
    }

    private List<Map<String,Object>> rows(SQLiteDatabase db, String table, String filter, String... args) {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Cursor c = db.query(table, null, filter, args, null, null, null)) { while (c.moveToNext()) result.add(row(c)); }
        return result;
    }

    public List<Change> pending(String uid) {
        SQLiteDatabase db = helper.getReadableDatabase();
        // A read transaction gives each payload and revision one consistent snapshot.
        db.beginTransaction();
        try {
            List<Change> result = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT * FROM sync_outbox WHERE owner_id=? ORDER BY CASE entity WHEN 'video' THEN 0 WHEN 'profile' THEN 1 ELSE 2 END", new String[]{uid})) {
                while (c.moveToNext()) {
                    Change change = new Change(); change.owner=uid;
                    change.entity=c.getString(c.getColumnIndexOrThrow("entity")); change.id=c.getString(c.getColumnIndexOrThrow("record_id"));
                    change.revision=c.getString(c.getColumnIndexOrThrow("revision")); change.deleted=c.getInt(c.getColumnIndexOrThrow("deleted"))!=0;
                    change.data = new HashMap<>();
                    if (!change.deleted) {
                        boolean personal = !"video".equals(change.entity) && !"profile".equals(change.entity);
                        List<Map<String,Object>> values = rows(db, table(change.entity), key(change.entity)+"=?"+(personal?" AND user_id=?":""),
                                personal?new String[]{change.id,uid}:new String[]{change.id});
                        if (values.isEmpty()) change.deleted=true;
                        else change.data.putAll(values.get(0));
                        if ("video".equals(change.entity) && !change.deleted) {
                            change.data.put("episodes", rows(db,"episodes","video_id=?",change.id));
                            change.data.put("cast", rows(db,"cast_members","video_id=?",change.id));
                        }
                    }
                    change.data.put("_deleted",change.deleted);
                    result.add(change);
                }
            }
            db.setTransactionSuccessful(); return result;
        } finally { db.endTransaction(); }
    }

    public void acknowledge(Change change) {
        SQLiteDatabase db=helper.getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("sync_outbox","owner_id=? AND entity=? AND record_id=? AND revision=?",new String[]{change.owner,change.entity,change.id,change.revision});
            if ("video".equals(change.entity)) known(db,change.id);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public boolean hasPending(String uid,String entity,String id) {
        boolean shared="video".equals(entity)||"profile".equals(entity);
        try (Cursor c=helper.getReadableDatabase().rawQuery("SELECT 1 FROM sync_outbox WHERE entity=? AND record_id=?"+(shared?"":" AND owner_id=?"),
                shared?new String[]{entity,id}:new String[]{entity,id,uid})) { return c.moveToFirst(); }
    }

    public int pendingCount(String uid) {
        if (uid==null) return 0;
        try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sync_outbox WHERE owner_id=?",new String[]{uid})) { c.moveToFirst();return c.getInt(0); }
    }

    public void state(String key,String value) {
        ContentValues cv=new ContentValues();cv.put("name",key);cv.put("value",value);
        helper.getWritableDatabase().insertWithOnConflict("sync_state",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String state(String key) {
        try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT value FROM sync_state WHERE name=?",new String[]{key})) { return c.moveToFirst()?c.getString(0):""; }
    }

    private void enqueue(SQLiteDatabase db,String uid,String entity,String id,boolean deleted) {
        ContentValues cv=new ContentValues();cv.put("owner_id",uid);cv.put("entity",entity);cv.put("record_id",id);
        cv.put("deleted",deleted?1:0);cv.put("revision",UUID.randomUUID().toString());
        db.insertWithOnConflict("sync_outbox",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void enqueueUnpublishedVideos(String uid) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Only a server-verified ADMIN can adopt old local video edits; never adopt local roles.
            try (Cursor legacy = db.rawQuery("SELECT * FROM sync_outbox WHERE entity='video' AND owner_id IN (SELECT id FROM users WHERE password IS NOT NULL AND password<>'') AND owner_id<>?", new String[]{uid})) {
                while (legacy.moveToNext()) {
                    String id = legacy.getString(legacy.getColumnIndexOrThrow("record_id"));
                    boolean deleted = legacy.getInt(legacy.getColumnIndexOrThrow("deleted")) != 0;
                    try (Cursor own = db.rawQuery("SELECT 1 FROM sync_outbox WHERE owner_id=? AND entity='video' AND record_id=?", new String[]{uid, id})) {
                        if (!own.moveToFirst()) enqueue(db, uid, "video", id, deleted);
                    }
                }
            }
            db.delete("sync_outbox", "entity='video' AND owner_id IN (SELECT id FROM users WHERE password IS NOT NULL AND password<>'') AND owner_id<>?", new String[]{uid});
            try (Cursor videos = db.rawQuery("SELECT id FROM videos WHERE id NOT IN (SELECT video_id FROM sync_known_videos) AND id NOT IN (SELECT record_id FROM sync_outbox WHERE entity='video')", null)) {
                while (videos.moveToNext()) enqueue(db, uid, "video", videos.getString(0), false);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void cacheUser(User user, boolean migrateVerifiedAccount) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            db.execSQL("UPDATE sync_context SET importing=1 WHERE id=1");
            List<Map<String,Object>> conflicts=rows(db,"users","email=? COLLATE NOCASE AND id<>?",user.getEmail(),user.getId());
            if(!conflicts.isEmpty()) {
                String oldId=String.valueOf(conflicts.get(0).get("id"));
                ContentValues rename=new ContentValues();rename.put("email","legacy-"+oldId+"@local.invalid");db.update("users",rename,"id=?",new String[]{oldId});
                state("legacy:"+user.getEmail(),oldId);
            }
            if(migrateVerifiedAccount) {
                String oldId=state("legacy:"+user.getEmail());
                if(!oldId.isEmpty()) {
                    for(String name:new String[]{"watch_history","favorites","settings","downloads"})
                        db.execSQL("UPDATE OR IGNORE "+name+" SET user_id=? WHERE user_id=?",new Object[]{user.getId(),oldId});
                    for(String entity:new String[]{"history","favorite","setting"})
                        for(Map<String,Object> value:rows(db,table(entity),"user_id=?",user.getId())) enqueue(db,user.getId(),entity,String.valueOf(value.get(key(entity))),false);
                    state("legacy:"+user.getEmail(),"");
                }
            }
            ContentValues cv=new ContentValues();cv.put("id",user.getId());cv.put("email",user.getEmail());cv.put("username",user.getUsername());
            cv.put("full_name",user.getFullName());cv.put("role",user.getRole());cv.put("is_locked",user.isLocked()?1:0);cv.put("avatar_url",user.getAvatarUrl());
            if(db.update("users",cv,"id=?",new String[]{user.getId()})==0){cv.put("password","");db.insertOrThrow("users",null,cv);}
            db.execSQL("UPDATE sync_context SET importing=0 WHERE id=1");db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private ContentValues values(SQLiteDatabase db,String table,Map<String,Object> data) {
        ContentValues values=new ContentValues();
        try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)) {
            while(c.moveToNext()) {
                String name=c.getString(c.getColumnIndexOrThrow("name"));
                if("password".equals(name)||!data.containsKey(name))continue;
                Object value=data.get(name);
                if(value==null)values.putNull(name);
                else if(value instanceof Number)values.put(name,((Number)value).doubleValue());
                else if(value instanceof Boolean)values.put(name,(Boolean)value?1:0);
                else values.put(name,String.valueOf(value));
            }
        }
        return values;
    }

    public void importRecord(String uid,String entity,String id,Map<String,Object> source) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            if(hasPending(uid,entity,id))return;
            db.execSQL("UPDATE sync_context SET importing=1 WHERE id=1");
            Map<String,Object> data=new HashMap<>(source);
            boolean personal=!"video".equals(entity);
            data.put(key(entity),id);if(personal)data.put("user_id",uid);
            String filter=key(entity)+"=?"+(personal?" AND user_id=?":"");
            String[] args=personal?new String[]{id,uid}:new String[]{id};
            if(Boolean.TRUE.equals(data.get("_deleted")))db.delete(table(entity),filter,args);
            else {
                if(("history".equals(entity)||"favorite".equals(entity))&&rows(db,"videos","id=?",String.valueOf(data.get("video_id"))).isEmpty())return;
                ContentValues cv=values(db,table(entity),data);
                if("video".equals(entity)&&!rows(db,"videos","id=?",id).isEmpty()){cv.remove("views");cv.remove("duration_ms");}
                if(db.update(table(entity),cv,filter,args)==0)db.insertOrThrow(table(entity),null,cv);
                if("video".equals(entity)) {
                    children(db,"episodes",id,data.get("episodes"));children(db,"cast_members",id,data.get("cast"));
                    Object category=data.get("category");
                    if(category instanceof String&&!((String)category).isEmpty()){
                        ContentValues cat=new ContentValues();cat.put("id","cat_"+UUID.nameUUIDFromBytes(((String)category).getBytes(java.nio.charset.StandardCharsets.UTF_8)));cat.put("name",(String)category);
                        db.insertWithOnConflict("categories",null,cat,SQLiteDatabase.CONFLICT_IGNORE);
                    }
                }
            }
            if("video".equals(entity))known(db,id);
            db.execSQL("UPDATE sync_context SET importing=0 WHERE id=1");db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    @SuppressWarnings("unchecked")
    private void children(SQLiteDatabase db,String table,String parent,Object children) {
        if(!(children instanceof List))return;
        db.delete(table,"video_id=?",new String[]{parent});
        for(Object item:(List<?>)children) {
            if(!(item instanceof Map))continue;
            Map<String,Object> data=new HashMap<>((Map<String,Object>)item);data.put("video_id",parent);
            db.insertOrThrow(table,null,values(db,table,data));
        }
    }

    private void known(SQLiteDatabase db,String id){ContentValues cv=new ContentValues();cv.put("video_id",id);db.insertWithOnConflict("sync_known_videos",null,cv,SQLiteDatabase.CONFLICT_IGNORE);}

    public void hideMissingVideos(Set<String> visible) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try(Cursor c=db.rawQuery("SELECT video_id FROM sync_known_videos",null)) {
            db.execSQL("UPDATE sync_context SET importing=1 WHERE id=1");
            while(c.moveToNext())if(!visible.contains(c.getString(0))&&!hasPending("","video",c.getString(0))) {
                ContentValues cv=new ContentValues();cv.put("status","PENDING");db.update("videos",cv,"id=?",new String[]{c.getString(0)});
            }
            db.execSQL("UPDATE sync_context SET importing=0 WHERE id=1");db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
}
