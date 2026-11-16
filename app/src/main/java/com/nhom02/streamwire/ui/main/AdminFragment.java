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

        if (!new com.nhom02.streamwire.utils.SessionManager(requireContext()).isAdmin()) {
            binding.getRoot().setVisibility(View.GONE); return;
        }
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
                            loadAdminDataFromSQLite();
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
                            User user = dbManager.findUser(target);
                            if (user != null && dbManager.updateUserRole(user.getId(), "ADMIN")) {
                                loadAdminDataFromSQLite();
                                Toast.makeText(requireContext(), "Đã cấp quyền quản trị", Toast.LENGTH_SHORT).show();
                            } else Toast.makeText(requireContext(), "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });
    }

    private void setupWarningBanner() {
        binding.btnResolveReport.setOnClickListener(v -> {
            List<Video> reported = new ArrayList<>();
            for (Video video : dbManager.getAllVideos()) if ("REPORTED".equals(video.getStatus())) reported.add(video);
            String[] titles = new String[reported.size()];
            for (int i = 0; i < titles.length; i++) titles[i] = reported.get(i).getTitle();
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Video bị báo cáo")
                    .setItems(titles, (dialog, which) -> {
                        dbManager.updateVideoStatus(reported.get(which).getId(), "PENDING");
                        loadAdminDataFromSQLite();
                    }).setNegativeButton("Đóng", null).show();
        });
    }

    private final androidx.activity.result.ActivityResultLauncher<String> exportCsv = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri == null) return;
                try (java.io.OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
                     java.io.Writer writer = new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8)) {
                    writer.write("\ufeffid,title,category,status\r\n");
                    for (Video video : dbManager.getAllVideos()) writer.write(csv(video.getId()) + "," + csv(video.getTitle()) + "," + csv(video.getCategory()) + "," + csv(video.getStatus()) + "\r\n");
                    writer.flush();
                    Toast.makeText(requireContext(), "Đã xuất CSV", Toast.LENGTH_SHORT).show();
                } catch (java.io.IOException | RuntimeException e) {
                    Toast.makeText(requireContext(), "Không thể xuất CSV", Toast.LENGTH_SHORT).show();
                }
            });

    private static String csv(String value) {
        String text = value == null ? "" : value;
        if (text.matches("^[=+@-].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void setupAuditAndExport() {
        binding.btnExportReportCsv.setOnClickListener(v -> exportCsv.launch("StreamWire_Videos.csv"));
    }

    private void loadAdminDataFromSQLite() {
        if (binding == null || videoAdapter == null) return;
        refreshStats();

        adminVideos.clear();
        adminVideos.addAll(dbManager.getAllVideos());
        videoAdapter.setData(adminVideos);

        adminUsers.clear();
        adminUsers.addAll(dbManager.getAllUsers());
        userAdapter.setData(adminUsers);
    }

    private void refreshStats() {
        boolean hasReports = false;
        for (Video video : dbManager.getAllVideos()) if ("REPORTED".equals(video.getStatus())) hasReports = true;
        binding.cardReportWarning.setVisibility(hasReports ? View.VISIBLE : View.GONE);
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
                    || (v.getCategory() != null && v.getCategory().toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(v);
            }
        }
        videoAdapter.setData(filtered);
    }

    private View wrapEditor(View editor) {
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext()); scroll.addView(editor); return scroll;
    }

    private void showAddVideoDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Tên video / Phim mới (*)");
        layout.addView(etTitle);

        EditText etCat = new EditText(requireContext());
        etCat.setHint("Thể loại (VD: Hành Động, Hoạt Hình, Hài Kịch)");
        layout.addView(etCat);

        EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Mô tả tóm tắt nội dung");
        layout.addView(etDesc);

        EditText etVideoUrl = new EditText(requireContext());
        etVideoUrl.setHint("Link video HTTP/HTTPS (*)");

        layout.addView(etVideoUrl);

        EditText etThumbUrl = new EditText(requireContext());
        etThumbUrl.setHint("Link ảnh bìa Thumbnail");

        layout.addView(etThumbUrl);

        EditText etDuration = new EditText(requireContext());
        etDuration.setHint("Thời lượng (VD: 45:00)");

        layout.addView(etDuration);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("➕ Thêm video mới vào Database SQLite")
                .setView(wrapEditor(layout))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu vào Database", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String cat = etCat.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    String url = etVideoUrl.getText().toString().trim();
                    String thumb = etThumbUrl.getText().toString().trim();
                    String duration = etDuration.getText().toString().trim();

                    if (!TextUtils.isEmpty(title)) {
                        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                            Toast.makeText(requireContext(), "Nhập đường dẫn video HTTP/HTTPS", Toast.LENGTH_SHORT).show(); return;
                        }

                        if (TextUtils.isEmpty(duration)) {
                            duration = "00:00";
                        }
                        if (TextUtils.isEmpty(cat)) {
                            cat = "Hành Động";
                        }
                        if (TextUtils.isEmpty(desc)) {
                            desc = "Video mới được thêm bởi Quản trị viên StreamWire.";
                        }

                        Video newV = new Video("v_" + System.currentTimeMillis(),
                                title, desc, url, thumb,
                                duration, 0L, "0", 0, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), "", "Nguồn gốc",
                                cat, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(new java.util.Date()));
                        newV.setStatus("ACTIVE");

                        // Lưu vĩnh viễn vào SQLite
                        if (!dbManager.insertVideo(newV)) {
                            Toast.makeText(requireContext(), "Không thể lưu video", Toast.LENGTH_SHORT).show(); return;
                        }

                        loadAdminDataFromSQLite();
                        binding.rvAdminVideos.scrollToPosition(0);
                        refreshStats();

                        Toast.makeText(requireContext(), "✓ Đã thêm video vào SQLite thành công: " + title, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Vui lòng nhập tên video", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showEditVideoDialog(Video video, int position) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 20, 40, 20);
        String[] labels = {"Tên video", "Thể loại", "Mô tả", "URL video", "URL ảnh bìa", "Thời lượng"};
        String[] values = {video.getTitle(), video.getCategory(), video.getDescription(), video.getVideoUrl(), video.getThumbnailUrl(), video.getDuration()};
        EditText[] fields = new EditText[values.length];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new EditText(requireContext()); fields[i].setHint(labels[i]); fields[i].setText(values[i]); layout.addView(fields[i]);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext()); scroll.addView(layout);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa video").setView(scroll).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(view -> {
            String title = fields[0].getText().toString().trim();
            String url = fields[3].getText().toString().trim();
            if (title.isEmpty()) { fields[0].setError("Nhập tên video"); return; }
            if (!(url.startsWith("https://") || url.startsWith("http://"))) { fields[3].setError("Nhập URL HTTP/HTTPS"); return; }
            video.setTitle(title); video.setCategory(fields[1].getText().toString().trim());
            video.setDescription(fields[2].getText().toString().trim()); video.setVideoUrl(url);
            video.setThumbnailUrl(fields[4].getText().toString().trim()); video.setDuration(fields[5].getText().toString().trim());
            if (dbManager.updateVideoDetails(video)) { loadAdminDataFromSQLite(); dialog.dismiss(); }
            else Toast.makeText(requireContext(), "Không thể lưu thay đổi", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
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
