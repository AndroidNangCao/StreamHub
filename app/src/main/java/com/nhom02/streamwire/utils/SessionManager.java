package com.nhom02.streamwire.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.nhom02.streamwire.model.User;

public class SessionManager {
    private static final String PREF_NAME = "StreamWireSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_FULLNAME = "user_fullname";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_GUEST = "is_guest";

    // Sensor Settings
    private static final String KEY_SENSOR_LIGHT = "sensor_light_enabled";
    private static final String KEY_SENSOR_SHAKE = "sensor_shake_enabled";
    private static final String KEY_SMART_GESTURES = "smart_gestures_enabled";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DEFAULT_QUALITY = "default_quality";

    private final com.nhom02.streamwire.db.DatabaseManager db;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.db = com.nhom02.streamwire.db.DatabaseManager.getInstance(context);
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = prefs.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_TOKEN, token).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUser(User user) {
        if (user == null) return;
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_NAME, user.getUsername());
        editor.putString(KEY_USER_FULLNAME, user.getFullName());
        editor.putString(KEY_USER_ROLE, user.getRole());
        editor.putBoolean(KEY_IS_GUEST, false);
        editor.apply();
    }

    public User getUser() {
        if (!isLoggedIn() && !isGuest()) return null;
        User user = new User();
        user.setId(prefs.getString(KEY_USER_ID, "0"));
        user.setEmail(prefs.getString(KEY_USER_EMAIL, "guest@streamwire.com"));
        user.setUsername(prefs.getString(KEY_USER_NAME, "Guest"));
        user.setFullName(prefs.getString(KEY_USER_FULLNAME, "Khách trải nghiệm"));
        user.setRole(prefs.getString(KEY_USER_ROLE, "USER"));
        if (!isGuest()) {
            User stored = db.findUser(user.getId());
            return stored == null || stored.isLocked() ? null : stored;
        }
        return user;
    }

    public void setGuestMode(boolean isGuest) {
        editor.putBoolean(KEY_IS_GUEST, isGuest);
        if (isGuest) {
            editor.remove(KEY_TOKEN);
            editor.remove(KEY_USER_ID);
            editor.remove(KEY_USER_EMAIL);
            editor.remove(KEY_USER_NAME);
            editor.putString(KEY_USER_ROLE, "USER");
            editor.putString(KEY_USER_FULLNAME, "Khách trải nghiệm");
        }
        editor.apply();
    }

    public boolean isGuest() {
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    public boolean isLoggedIn() {
        return prefs.getString(KEY_TOKEN, null) != null;
    }

    public boolean isAdmin() {
        User user = getUser();
        return !isGuest() && user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private String settingOwner() { return isGuest() ? "guest" : prefs.getString(KEY_USER_ID, "guest"); }

    // Sensor Toggles
    public boolean isLightSensorEnabled() {
        return Boolean.parseBoolean(db.getSetting(settingOwner(), KEY_SENSOR_LIGHT, "true"));
    }

    public void setLightSensorEnabled(boolean enabled) {
        db.setSetting(settingOwner(), KEY_SENSOR_LIGHT, String.valueOf(enabled));
    }

    public boolean isShakeGestureEnabled() {
        return Boolean.parseBoolean(db.getSetting(settingOwner(), KEY_SENSOR_SHAKE, "true"));
    }

    public void setShakeGestureEnabled(boolean enabled) {
        db.setSetting(settingOwner(), KEY_SENSOR_SHAKE, String.valueOf(enabled));
    }

    public boolean isSmartGesturesEnabled() {
        return Boolean.parseBoolean(db.getSetting(settingOwner(), KEY_SMART_GESTURES, "true"));
    }

    public void setSmartGesturesEnabled(boolean enabled) {
        db.setSetting(settingOwner(), KEY_SMART_GESTURES, String.valueOf(enabled));
    }

    public boolean isNotificationsEnabled() {
        return Boolean.parseBoolean(db.getSetting(settingOwner(), KEY_NOTIFICATIONS, "true"));
    }

    public void setNotificationsEnabled(boolean enabled) {
        db.setSetting(settingOwner(), KEY_NOTIFICATIONS, String.valueOf(enabled));
    }

    public String getDefaultQuality() {
        return db.getSetting(settingOwner(), KEY_DEFAULT_QUALITY, "Tự động (Auto)");
    }

    public void setDefaultQuality(String quality) {
        db.setSetting(settingOwner(), KEY_DEFAULT_QUALITY, quality);
    }

    public void clearSession() {
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_FULLNAME);
        editor.remove(KEY_USER_ROLE);
        editor.putBoolean(KEY_IS_GUEST, false);
        editor.apply();
    }
}

