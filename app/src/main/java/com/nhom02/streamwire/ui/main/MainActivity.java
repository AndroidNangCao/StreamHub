package com.nhom02.streamwire.ui.main;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.R;
import com.nhom02.streamwire.databinding.ActivityMainBinding;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.utils.SensorHelper;
import com.nhom02.streamwire.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements SensorHelper.OnLightChangeListener {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private SensorHelper sensorHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        sensorHelper = new SensorHelper(this);
        sensorHelper.setOnLightChangeListener(this);

        setupBottomNavigation();
        setupTopBarActions();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Trang chủ");
        }
    }

    private void setupBottomNavigation() {
        // Chỉ hiển thị tab Quản trị nếu user có vai trò ADMIN
        boolean isAdmin = sessionManager.isAdmin();
        binding.bottomNavigation.getMenu().findItem(R.id.nav_admin).setVisible(isAdmin);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment(), "Trang chủ");
                return true;
            } else if (id == R.id.nav_history) {
                loadFragment(new HistoryFragment(), "Lịch sử xem");
                return true;
            } else if (id == R.id.nav_admin) {
                loadFragment(new AdminFragment(), "Bảng Quản trị");
                return true;
            } else if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment(), "Cài đặt & Cảm biến");
                return true;
            }
            return false;
        });
    }

    private void setupTopBarActions() {
        binding.btnTopSearch.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), "Tìm kiếm video");
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        });

        binding.ivTopProfile.setOnClickListener(v -> {
            User user = sessionManager.getUser();
            String userInfo = user != null ?
                    "Họ tên: " + user.getFullName() + "\nEmail: " + user.getEmail() + "\nVai trò: " + user.getRole()
                    : "Đang ở chế độ Khách";

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Thông tin người dùng")
                    .setMessage(userInfo)
                    .setPositiveButton("Đóng", null)
                    .show();
        });
    }

    public void loadFragment(Fragment fragment, String screenTitle) {
        binding.tvCurrentScreen.setText(screenTitle);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public void onLightModeChanged(boolean isDarkMode, float lux) {
        if (!sessionManager.isLightSensorEnabled()) {
            return;
        }

        runOnUiThread(() -> {
            int targetMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
                String msg = isDarkMode ? "Ánh sáng yếu (" + (int)lux + " lux) ➔ Bật Chế độ Tối"
                        : "Ánh sáng đủ (" + (int)lux + " lux) ➔ Bật Chế độ Sáng";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLightSensorEnabled() && sensorHelper.hasLightSensor()) {
            sensorHelper.registerLightSensor();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.unregisterLightSensor();
    }
}

