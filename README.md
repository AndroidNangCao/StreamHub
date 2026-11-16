# StreamWire — Android / SQLite

Ứng dụng Android Java, minSdk 26, dùng SQLite cục bộ (`streamwire.db`, schema 3).

## Chạy dự án

1. Mở bằng Android Studio, cấu hình Android SDK 34 và Gradle JDK 21 theo cấu hình wrapper của dự án.
2. Sync Gradle rồi chạy module `app` trên máy Android hoặc giả lập.
3. Đăng ký tài khoản đầu tiên: tài khoản này tự nhận quyền ADMIN. Những tài khoản sau nhận quyền USER. Bản cài mới không chứa phim, tài khoản hay lịch sử giả.
4. Vào **Quản trị → Thêm mới**, nhập tên và URL video HTTP/HTTPS thực tế. Nên dùng URL MP4 trực tiếp; URL trang web xem phim không phải nguồn phát video.
5. Trở về Trang chủ để xem, tìm kiếm, lọc, thêm yêu thích hoặc tải xuống. Danh sách yêu thích nằm trong nút bộ lọc Trang chủ.

## Dữ liệu

- SQLite lưu tài khoản, mật khẩu PBKDF2 có salt, danh mục, video, tập phim, diễn viên, lịch sử, yêu thích, cài đặt và mã tải xuống.
- Lịch sử, yêu thích, cài đặt và mã tải được tách theo tài khoản. Chế độ khách dùng dữ liệu riêng mang khóa `guest`.
- SharedPreferences giữ thông tin phiên đăng nhập trên thiết bị. Không có JWT hoặc máy chủ xác thực.
- Android DownloadManager tải file vào thư mục Movies riêng của ứng dụng; SQLite lưu metadata. Trình phát ưu tiên bản đã tải thành công cho cùng URL video. File video không được nhét vào SQLite.
- Xuất CSV mở trình chọn nơi lưu và xuất danh sách video hiện tại, không chứa mật khẩu.
- Nâng cấp schema 2 → 3 giữ nguyên tài khoản và video cũ. Dữ liệu mẫu đã tồn tại từ bản cài cũ không bị tự xóa. Lịch sử cũ không xác định chủ sở hữu được giữ với khóa `legacy`, không gán cho tài khoản bất kỳ.
- Không còn tự tải JSON cố định từ GitHub hoặc tự thay video lỗi bằng một đoạn phim mẫu khác. Khi nguồn không truy cập được, trình phát báo lỗi nguồn thực tế.
- Nguồn video/ảnh trực tuyến cần mạng. Giới hạn chất lượng chỉ có tác dụng khi nguồn có các track phù hợp; một file MP4 đơn không thể tự tạo các bản độ phân giải khác.

## Kiểm tra

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Kiểm thử Android cần thiết bị/giả lập đang kết nối. `SQLiteWorkflowTest` dùng database và preferences có tiền tố `test_`, kiểm tra tài khoản đầu tiên, lưu qua lần mở lại, tách dữ liệu tài khoản, video ẩn, xóa dữ liệu liên quan và migration giữ dữ liệu cũ.

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
