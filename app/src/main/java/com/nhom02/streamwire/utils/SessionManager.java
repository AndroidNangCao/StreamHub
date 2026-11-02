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

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
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
        return user;
    }

    public void setGuestMode(boolean isGuest) {
        editor.putBoolean(KEY_IS_GUEST, isGuest);
        if (isGuest) {
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
        String role = prefs.getString(KEY_USER_ROLE, "USER");
        return "ADMIN".equalsIgnoreCase(role);
    }

    // Sensor Toggles
    public boolean isLightSensorEnabled() {
        return prefs.getBoolean(KEY_SENSOR_LIGHT, true);
    }

    public void setLightSensorEnabled(boolean enabled) {
        editor.putBoolean(KEY_SENSOR_LIGHT, enabled).apply();
    }

    public boolean isShakeGestureEnabled() {
        return prefs.getBoolean(KEY_SENSOR_SHAKE, true);
    }

    public void setShakeGestureEnabled(boolean enabled) {
        editor.putBoolean(KEY_SENSOR_SHAKE, enabled).apply();
    }

    public boolean isSmartGesturesEnabled() {
        return prefs.getBoolean(KEY_SMART_GESTURES, true);
    }

    public void setSmartGesturesEnabled(boolean enabled) {
        editor.putBoolean(KEY_SMART_GESTURES, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public String getDefaultQuality() {
        return prefs.getString(KEY_DEFAULT_QUALITY, "1080p Full HD");
    }

    public void setDefaultQuality(String quality) {
        editor.putString(KEY_DEFAULT_QUALITY, quality).apply();
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

