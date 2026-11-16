package com.nhom02.streamwire.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom02.streamwire.databinding.ActivityLoginBinding;
import com.nhom02.streamwire.databinding.DialogRegisterBinding;
import com.nhom02.streamwire.db.DatabaseManager;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.ui.main.MainActivity;
import com.nhom02.streamwire.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private DatabaseManager dbManager;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbManager = DatabaseManager.getInstance(this);

        // Auto login check
        if (sessionManager.isLoggedIn() || sessionManager.isGuest()) {
            navigateToMain();
            return;
        }

        setupActions();
    }

    private void setupActions() {
        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.btnRegister.setOnClickListener(v -> showRegisterDialog());

        binding.btnGuestMode.setOnClickListener(v -> {
            sessionManager.setGuestMode(true);
            Toast.makeText(this, "Đang vào chế độ xem thử (Khách)", Toast.LENGTH_SHORT).show();
            navigateToMain();
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            if (com.nhom02.streamwire.cloud.FirebaseRuntime.configured(this)) {
                String email = binding.etEmail.getText().toString().trim();
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilEmail.setError("Nhập email để nhận thư đặt lại mật khẩu"); return; }
                com.nhom02.streamwire.cloud.FirebaseRuntime.auth().sendPasswordResetEmail(email)
                        .addOnSuccessListener(this, ignored -> Toast.makeText(this, "Đã gửi yêu cầu đặt lại mật khẩu. Hãy kiểm tra email.", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(this, error -> Toast.makeText(this, "Không gửi được yêu cầu. Kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show());
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Khôi phục mật khẩu")
                    .setMessage("Dữ liệu được lưu trên thiết bị. Tài khoản đăng ký đầu tiên là quản trị viên. Nếu quên mật khẩu, hãy nhờ quản trị viên của thiết bị hỗ trợ.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
        });
    }

    private void performLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Vui lòng nhập Email hoặc tên đăng nhập");
            return;
        } else {
            binding.tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Vui lòng nhập mật khẩu");
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        if (com.nhom02.streamwire.cloud.FirebaseRuntime.configured(this)) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilEmail.setError("Đăng nhập Firebase bằng email"); return; }
            cloudLogin(email, password, null, null);
            return;
        }

        // Xác thực qua SQLite Database
        User user = dbManager.authenticate(email, password);

        if (user != null) {
            if (user.isLocked()) {
                Toast.makeText(this, "Tài khoản của bạn đã bị khóa bởi Quản trị viên!", Toast.LENGTH_LONG).show();
                return;
            }

            sessionManager.saveAuthToken("token_sqlite_" + System.currentTimeMillis());
            sessionManager.saveUser(user);

            Toast.makeText(this, "Đăng nhập thành công! Vai trò: " + user.getRole(), Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRegisterDialog() {
        DialogRegisterBinding dialogBinding = DialogRegisterBinding.inflate(LayoutInflater.from(this));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        dialogBinding.btnSubmitRegister.setOnClickListener(v -> {
            String fullName = dialogBinding.etRegFullName.getText() != null ? dialogBinding.etRegFullName.getText().toString().trim() : "";
            String email = dialogBinding.etRegEmail.getText() != null ? dialogBinding.etRegEmail.getText().toString().trim() : "";
            String password = dialogBinding.etRegPassword.getText() != null ? dialogBinding.etRegPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                dialogBinding.etRegEmail.setError("Email không hợp lệ"); return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
                return;
            }

            if (com.nhom02.streamwire.cloud.FirebaseRuntime.configured(this)) {
                if (busy) return;
                cloudLogin(email, password, fullName, () -> { dialogBinding.btnSubmitRegister.setEnabled(true); dialog.dismiss(); });
                return;
            }
            // Ghi vào SQLite
            boolean success = dbManager.registerUser(fullName, email, password);
            if (success) {
                User newUser = dbManager.authenticate(email, password);
                if (newUser != null) {
                    sessionManager.saveAuthToken("token_reg_" + System.currentTimeMillis());
                    sessionManager.saveUser(newUser);
                }
                dialog.dismiss();
                Toast.makeText(this, "Đăng ký thành công và đã lưu vào SQLite!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else {
                Toast.makeText(this, "Email này đã tồn tại trong hệ thống!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void cloudLogin(String email, String password, String name, Runnable registered) {
        if (busy) return;
        busy = true; binding.btnLogin.setEnabled(false); binding.btnRegister.setEnabled(false); binding.btnGuestMode.setEnabled(false);
        com.nhom02.streamwire.cloud.CloudAuth.authenticate(getApplicationContext(), email, password, name)
                .addOnCompleteListener(this, task -> {
                    busy = false; binding.btnLogin.setEnabled(true); binding.btnRegister.setEnabled(true); binding.btnGuestMode.setEnabled(true);
                    if (task.isSuccessful()) {
                        sessionManager.saveUser(task.getResult());
                        com.nhom02.streamwire.cloud.SyncScheduler.periodic(this);
                        com.nhom02.streamwire.cloud.SyncScheduler.request(this);
                        if (registered != null) registered.run();
                        navigateToMain();
                    } else {
                        Exception failure = task.getException();
                        String stage = "AUTH";
                        boolean accountCreated = false;
                        if (failure instanceof com.nhom02.streamwire.cloud.CloudAuth.Failure) {
                            com.nhom02.streamwire.cloud.CloudAuth.Failure wrapped = (com.nhom02.streamwire.cloud.CloudAuth.Failure) failure;
                            stage = wrapped.stage; accountCreated = wrapped.accountCreated;
                            failure = (Exception) wrapped.getCause();
                        }
                        String code = null;
                        if (failure instanceof com.google.firebase.auth.FirebaseAuthException)
                            code = ((com.google.firebase.auth.FirebaseAuthException) failure).getErrorCode();
                        else if (failure instanceof com.google.firebase.firestore.FirebaseFirestoreException)
                            code = ((com.google.firebase.firestore.FirebaseFirestoreException) failure).getCode().name();
                        else if (failure instanceof com.google.firebase.FirebaseNetworkException) code = "NETWORK_REQUEST_FAILED";
                        String message = com.nhom02.streamwire.cloud.AuthErrorMessage.describe(code,
                                failure == null ? "" : failure.getMessage(), stage, accountCreated);
                        if (failure instanceof IllegalStateException && !accountCreated) message = failure.getMessage();
                        android.util.Log.w("CloudAuth", "Failure stage=" + stage + ", code=" + code);
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(name == null ? "Không đăng nhập được" : "Chưa hoàn tất đăng ký")
                                .setMessage(message).setPositiveButton("Đã hiểu", null).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
