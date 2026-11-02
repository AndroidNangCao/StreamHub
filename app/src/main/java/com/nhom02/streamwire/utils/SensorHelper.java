package com.nhom02.streamwire.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorHelper implements SensorEventListener {

    // Thresholds & Cooldowns
    private static final float DARK_LUX_THRESHOLD = 15.0f; // Dưới 15 lux: chuyển Dark Mode
    private static final float LIGHT_LUX_THRESHOLD = 40.0f; // Trên 40 lux: chuyển Light Mode (Hysteresis chống nháy)
    private static final float SHAKE_THRESHOLD_G = 2.4f; // Ngưỡng G-force phát hiện rung lắc
    private static final long SHAKE_COOLDOWN_MS = 1500L; // Cooldown 1.5 giây chống nhận diện lặp
    private static final long LIGHT_DEBOUNCE_MS = 1000L; // Tránh đổi theme quá liên tục

    public interface OnLightChangeListener {
        void onLightModeChanged(boolean isDarkMode, float lux);
    }

    public interface OnShakeListener {
        void onShake();
    }

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final Sensor accelerometerSensor;

    private OnLightChangeListener lightListener;
    private OnShakeListener shakeListener;

    private Boolean currentDarkModeState = null;
    private long lastLightUpdateTime = 0;
    private long lastShakeTime = 0;

    public SensorHelper(Context context) {
        Context appContext = context.getApplicationContext();
        this.sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            this.lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            this.lightSensor = null;
            this.accelerometerSensor = null;
        }
    }

    public boolean hasLightSensor() {
        return lightSensor != null;
    }

    public boolean hasAccelerometerSensor() {
        return accelerometerSensor != null;
    }

    public void setOnLightChangeListener(OnLightChangeListener listener) {
        this.lightListener = listener;
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.shakeListener = listener;
    }

    public void registerLightSensor() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void unregisterLightSensor() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.unregisterListener(this, lightSensor);
        }
    }

    public void registerAccelerometer() {
        if (sensorManager != null && accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void unregisterAccelerometer() {
        if (sensorManager != null && accelerometerSensor != null) {
            sensorManager.unregisterListener(this, accelerometerSensor);
        }
    }

    public void unregisterAll() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) return;

        int sensorType = event.sensor.getType();
        long currentTime = System.currentTimeMillis();

        // 1. Xử lý Light Sensor
        if (sensorType == Sensor.TYPE_LIGHT && lightListener != null) {
            float lux = event.values[0];

            if (currentTime - lastLightUpdateTime >= LIGHT_DEBOUNCE_MS) {
                if (lux <= DARK_LUX_THRESHOLD) {
                    if (currentDarkModeState == null || !currentDarkModeState) {
                        currentDarkModeState = true;
                        lastLightUpdateTime = currentTime;
                        lightListener.onLightModeChanged(true, lux);
                    }
                } else if (lux >= LIGHT_LUX_THRESHOLD) {
                    if (currentDarkModeState == null || currentDarkModeState) {
                        currentDarkModeState = false;
                        lastLightUpdateTime = currentTime;
                        lightListener.onLightModeChanged(false, lux);
                    }
                }
            }
        }

        // 2. Xử lý Accelerometer (Phát hiện lắc máy)
        if (sensorType == Sensor.TYPE_ACCELEROMETER && shakeListener != null) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Chuẩn hóa vector gia tốc theo G
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce >= SHAKE_THRESHOLD_G) {
                if (currentTime - lastShakeTime >= SHAKE_COOLDOWN_MS) {
                    lastShakeTime = currentTime;
                    shakeListener.onShake();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không yêu cầu xử lý khi độ chính xác cảm biến thay đổi
    }
}

