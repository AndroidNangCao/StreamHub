package com.nhom02.streamwire.cloud;

import java.util.Locale;

/** Converts provider diagnostics to actionable messages without displaying credentials or raw server responses. */
public final class AuthErrorMessage {
    private AuthErrorMessage() {}

    public static String describe(String code, String detail, String stage, boolean accountCreated) {
        String diagnostic = ((code == null ? "" : code) + " " + (detail == null ? "" : detail)).toUpperCase(Locale.ROOT);
        String message;
        if (diagnostic.contains("CONFIGURATION_NOT_FOUND") || diagnostic.contains("OPERATION_NOT_ALLOWED")) {
            message = "Firebase chưa cấu hình đăng nhập Email/Password. Mở Firebase Console đúng project → Authentication → Get started → Sign-in method → bật Email/Password và Save.";
        } else if (diagnostic.contains("API_KEY_INVALID") || diagnostic.contains("INVALID_API_KEY") || diagnostic.contains("API_KEY_SERVICE_BLOCKED")) {
            message = "API key Firebase không hợp lệ hoặc bị chặn. Kiểm tra google-services.json thuộc đúng project và cấu hình giới hạn API key, sau đó cập nhật cấu hình và build lại.";
        } else if (diagnostic.contains("EMAIL_ALREADY_IN_USE")) {
            message = "Email đã có tài khoản Firebase. Hãy dùng nút Đăng nhập hoặc Quên mật khẩu.";
        } else if (diagnostic.contains("INVALID_EMAIL")) {
            message = "Email không hợp lệ. Kiểm tra lại địa chỉ email.";
        } else if (diagnostic.contains("WEAK_PASSWORD")) {
            message = "Mật khẩu chưa đáp ứng chính sách Firebase. Dùng ít nhất 6 ký tự và các yêu cầu mật khẩu đã bật trong Console.";
        } else if (!"AUTH".equals(stage) && (diagnostic.contains("SERVICE_DISABLED") || diagnostic.contains("UNAVAILABLE") || diagnostic.contains("DEADLINE_EXCEEDED"))) {
            message = "Chưa truy cập được Cloud Firestore. Kiểm tra đã bật Cloud Firestore API và tạo Firestore database (default) trong đúng project, rồi kiểm tra kết nối Internet. Bật Authentication chưa tự bật Firestore.";
        } else if (diagnostic.contains("NETWORK_REQUEST_FAILED") || diagnostic.contains("UNAVAILABLE") || diagnostic.contains("DEADLINE_EXCEEDED")) {
            message = "Không kết nối được Firebase. Kiểm tra Internet trên thiết bị/giả lập rồi thử lại.";
        } else if (!"AUTH".equals(stage) && diagnostic.contains("PERMISSION_DENIED")) {
            message = "Firebase Authentication đã thành công nhưng Firestore từ chối truy cập hồ sơ. Tạo Cloud Firestore và Publish nội dung firestore.rules vào đúng project.";
        } else if (!"AUTH".equals(stage) && diagnostic.contains("NOT_FOUND")) {
            message = "Không tìm thấy cơ sở dữ liệu Firestore. Hãy tạo Cloud Firestore database (default) trong cùng Firebase project.";
        } else if (diagnostic.contains("TOO_MANY_REQUESTS")) {
            message = "Firebase đang giới hạn số lần thử. Chờ một lúc rồi thử lại.";
        } else if (diagnostic.contains("USER_DISABLED")) {
            message = "Tài khoản Firebase đã bị vô hiệu hóa. Hãy liên hệ quản trị viên.";
        } else if (diagnostic.contains("APP_NOT_AUTHORIZED")) {
            message = "Ứng dụng chưa được phép dùng Firebase Auth. Kiểm tra package com.nhom02.streamwire và giới hạn API key trong project.";
        } else if (diagnostic.contains("INVALID_CREDENTIAL") || diagnostic.contains("INVALID_LOGIN_CREDENTIALS") || diagnostic.contains("WRONG_PASSWORD") || diagnostic.contains("USER_NOT_FOUND")) {
            message = "Email hoặc mật khẩu không đúng. Kiểm tra lại hoặc dùng Quên mật khẩu.";
        } else if ("CACHE".equals(stage)) {
            message = "Đã xác thực Firebase nhưng chưa lưu được hồ sơ trên thiết bị. Hãy kiểm tra bộ nhớ và đăng nhập lại.";
        } else {
            message = "Không hoàn tất được thao tác Firebase. Kiểm tra cấu hình project và thử lại.";
        }
        if (accountCreated) message = "Tài khoản đã được tạo trên Firebase, nhưng chưa hoàn tất hồ sơ.\n\n" + message + "\n\nSau khi khắc phục, hãy Đăng nhập bằng email này; không cần đăng ký lại.";
        if (code != null && code.matches("[A-Za-z0-9_-]+")) message += "\n\nMã lỗi: " + code;
        return message;
    }
}
