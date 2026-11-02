package com.nhom02.streamwire.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.databinding.FragmentSettingsBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.ui.auth.LoginActivity;
import com.nhom02.streamwire.utils.SensorHelper;
import com.nhom02.streamwire.utils.SessionManager;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;
    private DatabaseManager dbManager;
    private SensorHelper sensorHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        dbManager = DatabaseManager.getInstance(requireContext());
        sensorHelper = new SensorHelper(requireContext());

        setupSensorSwitches();
        setupAccountSettings();
        setupLogout();
    }

    private void setupSensorSwitches() {
        // 1. Light Sensor
        boolean hasLight = sensorHelper.hasLightSensor();
        if (!hasLight) {
            binding.switchLightSensor.setEnabled(false);
            binding.switchLightSensor.setChecked(false);
            binding.tvLightSensorDesc.setText("(Thiết bị không hỗ trợ cảm biến ánh sáng môi trường)");
        } else {
            binding.switchLightSensor.setChecked(sessionManager.isLightSensorEnabled());
            binding.switchLightSensor.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setLightSensorEnabled(isChecked);
                Toast.makeText(requireContext(), isChecked ?
                        "Đã bật tự đổi giao diện bằng cảm biến ánh sáng" : "Đã tắt cảm biến ánh sáng", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Shake Accelerometer
        boolean hasAccel = sensorHelper.hasAccelerometerSensor();
        if (!hasAccel) {
            binding.switchShakeGesture.setEnabled(false);
            binding.switchShakeGesture.setChecked(false);
            binding.tvShakeSensorDesc.setText("(Thiết bị không hỗ trợ cảm biến gia tốc)");
        } else {
            binding.switchShakeGesture.setChecked(sessionManager.isShakeGestureEnabled());
            binding.switchShakeGesture.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setShakeGestureEnabled(isChecked);
                Toast.makeText(requireContext(), isChecked ?
                        "Đã kích hoạt cử chỉ lắc máy để tua" : "Đã tắt tính năng lắc máy", Toast.LENGTH_SHORT).show();
            });
        }

        // 3. Smart Touch Gestures
        binding.switchSmartGestures.setChecked(sessionManager.isSmartGesturesEnabled());
        binding.switchSmartGestures.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setSmartGesturesEnabled(isChecked);
            Toast.makeText(requireContext(), isChecked ?
                    "Đã kích hoạt cử chỉ chạm thông minh" : "Đã tắt cử chỉ chạm", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAccountSettings() {
        // Đổi mật khẩu trong SQLite
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Thông báo
        binding.switchNotifications.setChecked(sessionManager.isNotificationsEnabled());
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setNotificationsEnabled(isChecked);
        });

        // Chất lượng phát mặc định
        binding.tvCurrentQualitySetting.setText(sessionManager.getDefaultQuality());
        binding.btnDefaultQuality.setOnClickListener(v -> showQualityPickerDialog());
    }

    private void showChangePasswordDialog() {
        User currentUser = sessionManager.getUser();
        if (currentUser == null || sessionManager.isGuest()) {
            Toast.makeText(requireContext(), "Bạn đang ở chế độ khách, vui lòng đăng nhập để đổi mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(requireContext());
        input.setHint("Nhập mật khẩu mới");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi mật khẩu tài khoản")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu mật khẩu", (dialog, which) -> {
                    String newPass = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
                        Toast.makeText(requireContext(), "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
                    } else {
                        // Cập nhật mật khẩu vào SQLite
                        boolean ok = dbManager.updatePassword(currentUser.getId(), newPass);
                        if (ok) {
                            Toast.makeText(requireContext(), "Đã cập nhật mật khẩu mới vào SQLite thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Không thể cập nhật mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void showQualityPickerDialog() {
        String[] options = {"1080p Full HD", "720p HD", "480p SD", "Tự động (Auto)"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chất lượng phát mặc định")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    sessionManager.setDefaultQuality(selected);
                    binding.tvCurrentQualitySetting.setText(selected);
                    Toast.makeText(requireContext(), "Đã lưu cài đặt: " + selected, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setupLogout() {
        binding.btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng StreamWire không?")
                    .setNegativeButton("Ở lại", null)
                    .setPositiveButton("Đăng xuất ngay", (dialog, which) -> {
                        sessionManager.clearSession();
                        Toast.makeText(requireContext(), "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
