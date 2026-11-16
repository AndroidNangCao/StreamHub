# StreamWire

Ứng dụng xem video streaming trên Android, viết bằng Java. Dữ liệu được lưu cục bộ bằng SQLite và đồng bộ lên Firebase khi có mạng.

## Tài khoản demo

| Email | Mật khẩu |
|---|---|
| `admin@gmail.com` | `123123` |

Tài khoản đã được tạo trên Firebase project của nhóm (`streamingvideo-9fbd3`). Dùng **Đăng nhập**, không đăng ký lại. **Để hiện tab Quản trị, tài khoản phải được cấp `role = ADMIN` theo mục 4.** Đây là tài khoản dùng chung cho bài tập lớn.

## Dành cho thành viên cùng dự án — cài và chạy nhanh

Thành viên dùng chung Firebase project **`streamingvideo-9fbd3`**. Nhận file **`google-services.json` do trưởng nhóm gửi riêng**; file này không có sẵn khi clone Git. Không cần tạo Firebase project mới hoặc làm lại mục 2.

1. Clone/pull mã nguồn rồi mở thư mục chứa `settings.gradle.kts` bằng **Android Studio → Open**.
2. Cài **Android SDK 34**, chọn **Gradle JDK 21** trong Settings → Build Tools → Gradle.
3. Sau khi nhận file, đặt đúng tên và vị trí:

   ```text
   streamhub/
   ├── app/
   │   ├── google-services.json   ← file trưởng nhóm gửi
   │   └── build.gradle.kts
   ├── scripts/
   └── settings.gradle.kts
   ```

4. Mở Terminal tại thư mục `streamhub`, chạy trên Windows:

   ```powershell
   powershell -NoProfile -File .\scripts\Configure-Firebase.ps1 -GoogleServicesJson .\app\google-services.json
   ```

   Đợi thông báo `Firebase Android configuration written to ...`. Trên macOS/Linux, điền cấu hình theo bảng ở mục 2.

5. Bấm **Sync Project with Gradle Files**, chọn điện thoại/giả lập **API 26 trở lên**, chọn module **app** rồi **Run ▶**.
6. Bật mạng và đăng nhập **`admin@gmail.com` / `123123`** để dùng tài khoản demo, hoặc đăng ký tài khoản riêng. Bấm **Đồng bộ** để tải dữ liệu chung của nhóm.

**Lưu ý khi làm việc nhóm:**

- Giữ nguyên package `com.nhom02.streamwire`. File JSON đã nằm trong `.gitignore`, không cần thêm vào Git.
- Chưa nhận file JSON: vẫn có thể cài SDK và mở dự án; hoàn thành bước cấu hình trước khi kiểm tra Firebase.
- Không thấy tab Quản trị: nhờ trưởng nhóm kiểm tra quyền ADMIN trên Firestore, sau đó bấm Đồng bộ.
- Nếu gặp lỗi xác thực hoặc Firestore, gửi trưởng nhóm mã lỗi trong hộp thoại; xem cách xử lý ở mục 5.

## 1. Chuẩn bị

- Android Studio hỗ trợ Android Gradle Plugin của dự án.
- JDK **21** để chạy Gradle; Android SDK **34**.
- Điện thoại hoặc giả lập Android **8.0 / API 26** trở lên.
- Internet để tải thư viện, đăng ký/đăng nhập và đồng bộ lần đầu.

## 2. Cấu hình Firebase

1. Vào [Firebase Console](https://console.firebase.google.com/), tạo hoặc chọn project.
2. Thêm ứng dụng Android với package **`com.nhom02.streamwire`**.
3. Tải `google-services.json`, đặt vào **`app/google-services.json`**.
4. Bật **Authentication → Sign-in method → Email/Password**, bấm **Save**.
5. Tạo **Cloud Firestore → Standard edition**, database **`(default)`**, chọn **Production mode**. Đảm bảo **Cloud Firestore API** đã bật trong Google Cloud Console của cùng project.
6. Trong **Firestore → Rules**, dán nội dung [firestore.rules](firestore.rules) rồi bấm **Publish**.
7. Mở Terminal tại thư mục gốc dự án, chạy trên Windows:

```powershell
powershell -NoProfile -File .\scripts\Configure-Firebase.ps1 -GoogleServicesJson .\app\google-services.json
```

Lệnh này điền cấu hình vào `app/src/main/res/values/firebase.xml`. **Chỉ đặt file JSON vào `app` chưa đủ, phải chạy lệnh trên.** Nếu dùng macOS/Linux, điền ba giá trị trong JSON vào file XML theo bảng sau:

| Trong `google-services.json` | Tên trong `firebase.xml` |
|---|---|
| `client[].api_key[].current_key` | `firebase_api_key` |
| `client[].client_info.mobilesdk_app_id` | `firebase_app_id` |
| `project_info.project_id` | `firebase_project_id` |

Chọn `client` có package `com.nhom02.streamwire`. Để dùng project riêng, thay đủ cả ba giá trị.

## 3. Chạy ứng dụng

1. Mở thư mục dự án bằng **Android Studio → Open**.
2. Trong **Settings → Build, Execution, Deployment → Build Tools → Gradle**, chọn **Gradle JDK 21**.
3. Bấm **Sync Project with Gradle Files**; cài SDK/thành phần còn thiếu nếu được yêu cầu.
4. Chọn thiết bị/giả lập, chọn module **app**, bấm **Run ▶**.
5. Đăng nhập bằng tài khoản demo ở trên, hoặc đăng ký tài khoản mới khi có mạng.

Build APK bằng Terminal trên Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK nằm tại **`app/build/outputs/apk/debug/app-debug.apk`**. Trên macOS/Linux dùng `./gradlew` thay cho `.\gradlew.bat`.

## 4. Tạo ADMIN và thêm video

1. Với tài khoản demo của nhóm, UID là **`MwooOMj6qqZmb7gdkb0L3eRkYXi2`**. Nếu dùng project Firebase riêng, đăng ký tài khoản mới hoặc chạy `python scripts/Init-DemoAdmin.py` để tạo cùng tài khoản demo (cần Python 3).
2. Chủ project mở **Firestore → users → document có UID của tài khoản**, đổi `role` thành **`ADMIN`**, giữ `is_locked` là số **`0`** rồi Save. Tài khoản mới mặc định là USER; script chỉ cấp quyền tự động nếu có phiên ADMIN hợp lệ trên giả lập debug.
3. Trong ứng dụng, bấm **Đồng bộ** ở Trang chủ.
4. Vào **Quản trị → Thêm mới**, nhập tên và URL video **HTTPS trực tiếp**, ưu tiên file MP4; không nhập URL trang web xem phim.
5. Về Trang chủ để xem video, lưu yêu thích và kiểm tra lịch sử.

Nếu project chưa có video, thư viện trống là bình thường.

## 5. Sử dụng offline và xử lý lỗi

- Mất mạng: vẫn xem được danh sách đã cache và lưu thay đổi vào SQLite. Có mạng trở lại: tự đồng bộ hoặc bấm **Đồng bộ**.
- Muốn phát video offline, cần tải file thành công trước đó. Đăng ký/đăng nhập mới cần Internet.
- Hai cảm biến: **ánh sáng** để đổi giao diện và **gia tốc kế** để lắc máy tua video; cần thiết bị hỗ trợ.

| Thông báo lỗi | Cách xử lý |
|---|---|
| `CONFIGURATION_NOT_FOUND` | Khởi tạo Authentication và bật Email/Password đúng project |
| `SERVICE_DISABLED`, Firestore `UNAVAILABLE` | Kiểm tra Cloud Firestore API, database `(default)` và Internet |
| `PERMISSION_DENIED` | Kiểm tra đã Publish `firestore.rules` vào đúng database/project |
| Tài khoản đã tạo nhưng chưa hoàn tất hồ sơ | Sửa cấu hình Firestore rồi **Đăng nhập**, không đăng ký lại |

Hướng dẫn chi tiết về đồng bộ và kiểm thử: [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
