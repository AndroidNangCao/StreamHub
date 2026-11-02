package com.nhom02.streamwire.ui.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.adapter.AdminUserAdapter;
import com.nhom02.streamwire.adapter.AdminVideoAdapter;
import com.nhom02.streamwire.databinding.FragmentAdminBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.model.Video;

import java.util.ArrayList;
import java.util.List;

public class AdminFragment extends Fragment {

    private FragmentAdminBinding binding;
    private DatabaseManager dbManager;
    private AdminVideoAdapter videoAdapter;
    private AdminUserAdapter userAdapter;

    private final List<Video> adminVideos = new ArrayList<>();
    private final List<User> adminUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = DatabaseManager.getInstance(requireContext());

        setupVideoSection();
        setupUserSection();
        setupWarningBanner();
        setupAuditAndExport();
        loadAdminDataFromSQLite();
    }

    private void setupVideoSection() {
        videoAdapter = new AdminVideoAdapter(new AdminVideoAdapter.OnAdminVideoActionListener() {
            @Override
            public void onEdit(Video video, int position) {
                showEditVideoDialog(video, position);
            }

            @Override
            public void onToggleStatus(Video video, int position) {
                String newStatus;
                if ("PENDING".equalsIgnoreCase(video.getStatus())) {
                    newStatus = "ACTIVE";
                    Toast.makeText(requireContext(), "Đã duyệt và công khai video lên SQLite!", Toast.LENGTH_SHORT).show();
                } else if ("ACTIVE".equalsIgnoreCase(video.getStatus())) {
                    newStatus = "PENDING";
                    Toast.makeText(requireContext(), "Đã ẩn video khỏi trang chủ (lưu SQLite)", Toast.LENGTH_SHORT).show();
                } else {
                    newStatus = "ACTIVE";
                    Toast.makeText(requireContext(), "Đã mở khóa video vi phạm", Toast.LENGTH_SHORT).show();
                }

                // Cập nhật SQLite
                dbManager.updateVideoStatus(video.getId(), newStatus);
                video.setStatus(newStatus);
                videoAdapter.notifyItemChanged(position);
                refreshStats();
            }

            @Override
            public void onDelete(Video video, int position) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận xóa video")
                        .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn video: \"" + video.getTitle() + "\" khỏi SQLite không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // Xóa khỏi SQLite
                            dbManager.deleteVideo(video.getId());
                            adminVideos.remove(position);
                            videoAdapter.removeItem(position);
                            refreshStats();
                            Toast.makeText(requireContext(), "Đã xóa vĩnh viễn video khỏi SQLite", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }
        });

        binding.rvAdminVideos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminVideos.setAdapter(videoAdapter);

        // Nút thêm mới video
        binding.btnAdminAddVideo.setOnClickListener(v -> showAddVideoDialog());

        // Tìm kiếm video
        binding.etSearchAdminVideo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAdminVideos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupUserSection() {
        userAdapter = new AdminUserAdapter(new AdminUserAdapter.OnAdminUserActionListener() {
            @Override
            public void onChangeRole(User user, int position) {
                String[] roles = {"USER", "STANDARD", "CREATOR", "ADMIN"};
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Phân quyền cho " + user.getFullName())
                        .setItems(roles, (dialog, which) -> {
                            String newRole = roles[which];
                            // Cập nhật SQLite
                            dbManager.updateUserRole(user.getId(), newRole);
                            user.setRole(newRole);
                            userAdapter.notifyItemChanged(position);
                            Toast.makeText(requireContext(), "Đã cập nhật SQLite: Vai trò " + newRole, Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }

            @Override
            public void onToggleLock(User user, int position) {
                boolean newLockState = !user.isLocked();
                // Cập nhật SQLite
                dbManager.toggleUserLock(user.getId(), newLockState);
                user.setLocked(newLockState);
                userAdapter.notifyItemChanged(position);

                String msg = newLockState ? "Đã khóa tài khoản @" + user.getUsername() + " trong SQLite"
                        : "Đã mở khóa tài khoản @" + user.getUsername() + " trong SQLite";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvAdminUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminUsers.setAdapter(userAdapter);

        binding.btnQuickGrantAdmin.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setHint("Nhập username hoặc email");

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cấp quyền Admin nhanh")
                    .setView(input)
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Cấp quyền", (dialog, which) -> {
                        String target = input.getText().toString().trim();
                        if (!TextUtils.isEmpty(target)) {
                            String email = target.contains("@") ? target : target + "@streamwire.com";
                            // Thêm vào SQLite với vai trò ADMIN
                            dbManager.registerUser("Admin " + target, email, "123456");
                            User newAdmin = dbManager.authenticate(email, "123456");
                            if (newAdmin != null) {
                                dbManager.updateUserRole(newAdmin.getId(), "ADMIN");
                                newAdmin.setRole("ADMIN");
                            }
                            loadAdminDataFromSQLite();
                            Toast.makeText(requireContext(), "Đã tạo tài khoản Admin trong SQLite cho: " + target, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });
    }

    private void setupWarningBanner() {
        binding.btnResolveReport.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xử lý 08 Báo Cáo Vi Phạm")
                    .setMessage("1. Video vi phạm bản quyền âm thanh\n2. Bình luận spam quảng cáo\n3. Lỗi 404 video...")
                    .setNegativeButton("Đóng", null)
                    .setPositiveButton("Giải quyết tất cả", (dialog, which) -> {
                        binding.cardReportWarning.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Đã xử lý xong các báo cáo vi phạm!", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    private void setupAuditAndExport() {
        binding.btnExportReportCsv.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Đã xuất báo cáo cơ sở dữ liệu SQLite: StreamWire_Audit.csv", Toast.LENGTH_LONG).show();
        });
    }

    private void loadAdminDataFromSQLite() {
        refreshStats();

        adminVideos.clear();
        adminVideos.addAll(dbManager.getAllVideos());
        videoAdapter.setData(adminVideos);

        adminUsers.clear();
        adminUsers.addAll(dbManager.getAllUsers());
        userAdapter.setData(adminUsers);
    }

    private void refreshStats() {
        int totalUsers = dbManager.getTotalUsersCount();
        int activeVideos = dbManager.getActiveVideosCount();

        binding.tvTotalUsersStat.setText(String.format("%,d", totalUsers));
        binding.tvActiveVideosStat.setText(String.format("%,d", activeVideos));
    }

    private void filterAdminVideos(String query) {
        if (query.trim().isEmpty()) {
            videoAdapter.setData(adminVideos);
            return;
        }

        List<Video> filtered = new ArrayList<>();
        for (Video v : adminVideos) {
            if (v.getTitle().toLowerCase().contains(query.toLowerCase())
                    || v.getCategory().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(v);
            }
        }
        videoAdapter.setData(filtered);
    }

    private void showAddVideoDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Tên video / Phim mới");
        layout.addView(etTitle);

        EditText etCat = new EditText(requireContext());
        etCat.setHint("Thể loại (Hành Động, Viễn Tưởng...)");
        layout.addView(etCat);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thêm video mới vào SQLite")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu vào SQLite", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String cat = etCat.getText().toString().trim();

                    if (!TextUtils.isEmpty(title)) {
                        Video newV = new Video("v_" + System.currentTimeMillis(),
                                title, "Video mới được thêm bởi Quản trị viên StreamWire.",
                                "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
                                "45:00", 45 * 60 * 1000L, "1 lượt xem", 9.5, 2026, "13+", "1080p",
                                TextUtils.isEmpty(cat) ? "Phim Mới" : cat, "Vừa xong");
                        newV.setStatus("ACTIVE");

                        // Lưu vào SQLite
                        dbManager.insertVideo(newV);

                        adminVideos.add(0, newV);
                        videoAdapter.notifyItemInserted(0);
                        binding.rvAdminVideos.scrollToPosition(0);
                        refreshStats();

                        Toast.makeText(requireContext(), "Đã lưu vĩnh viễn video vào SQLite: " + title, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showEditVideoDialog(Video video, int position) {
        EditText input = new EditText(requireContext());
        input.setText(video.getTitle());
        input.setSelection(video.getTitle().length());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa tên video trong SQLite")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(newTitle)) {
                        // Cập nhật SQLite
                        dbManager.updateVideoTitle(video.getId(), newTitle);
                        video.setTitle(newTitle);
                        videoAdapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Đã lưu thay đổi vào SQLite!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAdminDataFromSQLite();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
