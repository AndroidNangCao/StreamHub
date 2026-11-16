# Firebase + SQLite: cấu hình và demo

Ứng dụng lưu thao tác vào SQLite trước. Khi có mạng, WorkManager gửi các thay đổi tới Cloud Firestore rồi tải dữ liệu mới về SQLite. Không cần tự xây Backend Java.

## 1. Cấu hình project của bạn

1. Tạo/chọn Firebase project tại https://console.firebase.google.com/.
2. Thêm ứng dụng Android với package **com.nhom02.streamwire**, tải `google-services.json`.
3. Bật **Authentication → Sign-in method → Email/Password**.
4. Tạo **Cloud Firestore** ở chế độ production. Chọn location phù hợp trước khi tạo.
5. Mở tab **Rules**, dán nội dung `firestore.rules` trong repository và Publish. Không dùng quy tắc cho phép mọi người đọc/ghi.
6. Chạy từ thư mục dự án:

```powershell
powershell -NoProfile -File .\scripts\Configure-Firebase.ps1 -GoogleServicesJson "C:\duong-dan\google-services.json"
.\gradlew.bat :app:assembleDebug
```

Script kiểm tra package và chuyển ba giá trị cấu hình Android sang `app/src/main/res/values/firebase.xml`. SDK được khởi tạo bằng FirebaseOptions nên không cần Google Services Gradle plugin. Không đưa file service account/Admin SDK private key vào ứng dụng. Script này không tạo project, bật dịch vụ, deploy rules hay upload dữ liệu.

Nếu ba giá trị cấu hình để trống, ứng dụng vẫn chạy chế độ SQLite cũ, hiển thị **“Chưa cấu hình Firebase”** và vô hiệu hóa nút Đồng bộ. Đây không phải kết nối Cloud giả.

## 2. Tạo tài khoản và cấp quyền ADMIN đầu tiên

1. Cài APK đã build với cấu hình Firebase và đăng ký bằng email/mật khẩu khi có mạng.
2. Tài khoản Firebase mới luôn có quyền **USER**. Không tự cấp ADMIN từ Android.
3. Trong Firestore Console, mở document `users/{UID}` vừa được tạo và đổi `role` từ `USER` thành `ADMIN`; giữ `is_locked` là số **0**.
4. Bấm Đồng bộ trên Trang chủ; sau khi hoàn tất, tab Quản trị sẽ xuất hiện.
5. Thêm video bằng URL **HTTPS trực tiếp** có thể phát. Ảnh bìa cũng nên dùng HTTPS.

Sau đó ADMIN có thể cấp quyền/khóa các tài khoản Firebase khác đã được đồng bộ về máy. Không cho phép ADMIN tự sửa vai trò/trạng thái khóa của mình bằng ứng dụng. Rules phía Firebase vẫn là nơi quyết định quyền thật.

Nếu đăng ký báo `CONFIGURATION_NOT_FOUND`, mở đúng project có ID trong `firebase.xml`, vào Authentication, bấm Get started nếu chưa khởi tạo, bật Email/Password trong Sign-in method và Save. Chỉ điền API key không tự bật dịch vụ Authentication. `OPERATION_NOT_ALLOWED` cũng cần kiểm tra provider này.

Nếu Authentication đã tạo tài khoản nhưng Firestore báo `PERMISSION_DENIED`, hãy tạo database `(default)` và Publish `firestore.rules`. Sau khi sửa, dùng Đăng nhập bằng tài khoản vừa tạo; không cần đăng ký lại. Ứng dụng phân biệt lỗi xác thực, đọc/ghi hồ sơ và lưu cục bộ để hướng dẫn đúng bước.

Tài khoản SQLite cũ không tự trở thành tài khoản Firebase. Cần đăng ký/đăng nhập Firebase online. Khi Firebase Auth xác minh thành công email trùng tài khoản cũ, ứng dụng chuyển lịch sử, yêu thích, cài đặt và metadata tải xuống cục bộ sang UID mới. Mật khẩu/hash và vai trò ADMIN cũ không được tải lên hoặc tự kế thừa. Hồ sơ cũ được giữ với email `legacy-...@local.invalid`; hàng đợi cũ không thuộc UID hiện tại không được gửi tùy tiện.

Khi ADMIN đã được xác minh từ Firestore đồng bộ, video cục bộ chưa từng lên Cloud và các chỉnh sửa video của tài khoản SQLite cũ được đưa vào hàng đợi xuất bản. Các video đã có trong cache Cloud không bị xem là video mới.

## 3. Cấu trúc dữ liệu Cloud

```text
users/{firebaseUid}
  id, email, username, full_name, role, is_locked, avatar_url
  history/{historyId}
    user_id, video_id, episode_title, last_position_ms,
    total_duration_ms, updated_at, _deleted, _server_updated_at
  favorites/{videoId}
    user_id, video_id, _deleted, _server_updated_at
  settings/{settingName}
    user_id, name, value, _deleted, _server_updated_at

videos/{videoId}
  id, title, description, video_url, thumbnail_url,
  duration, duration_ms, views, rating, release_year,
  age_rating, quality, category, status, published_at,
  episodes: [...], cast: [...], _deleted, _server_updated_at
```

Các document video phải có `status: "ACTIVE"` và `_deleted: false` để người dùng thường/khách đọc được. Nên thêm video bằng giao diện Quản trị để có đủ cấu trúc; không chỉ tạo một document chứa URL.

Firestore lưu metadata và URL, không chứa file MP4. File video tiếp tục nằm ở nguồn HTTPS hiện có; chức năng tải xuống vẫn dùng Android DownloadManager và lưu metadata trên thiết bị.

## 4. Cơ chế đồng bộ

- SQLite schema 4 bổ sung `sync_context`, `sync_outbox`, `sync_state`, `sync_known_videos`; migration từ schema 3 không xóa bảng dữ liệu cũ.
- SQLite trigger ghi dữ liệu và entry hàng đợi trong cùng giao dịch, bao gồm thêm/sửa/xóa lịch sử, yêu thích, cài đặt và thay đổi quản trị video/tài khoản.
- Nhiều sửa đổi cùng một bản ghi được gộp thành phiên bản mới nhất. Chỉ xóa entry khi Firebase xác nhận đúng revision đã gửi; phản hồi cũ không xóa một chỉnh sửa mới hơn.
- Thao tác xóa gửi document `_deleted: true` thay vì xóa vật lý, để thiết bị đang offline biết cần xóa cache khi quay lại. Không xóa tombstone tùy tiện trong Console.
- Khách chỉ dùng lịch sử/yêu thích/cài đặt cục bộ. Không tải dữ liệu riêng của khách lên Firebase.
- Mỗi lần đồng bộ kiểm tra lại profile/quyền trên server; chỉ gửi entry của đúng Firebase UID đang đăng nhập.
- Trước khi tải dữ liệu, ứng dụng gửi các thay đổi đang chờ. Khi import, không ghi đè bản ghi vẫn còn thay đổi cục bộ chưa gửi; import cũng không tạo entry gửi ngược vô hạn.
- Khi hai thiết bị cùng sửa một bản ghi, phiên bản được ghi lên server sau cùng thắng. Đây là chính sách đơn giản cho bài tập, không phải cơ chế hợp nhất từng trường.
- Lịch sử mới dùng ID ổn định theo tài khoản/video/tên tập. Các bản ghi lịch sử cũ được giữ nguyên ID; tên tập trùng nhau trong cùng video vẫn là giới hạn của mô hình hiện tại.
- Đồng bộ được yêu cầu khi có thay đổi, mở Trang chủ, kéo làm mới, bấm Đồng bộ hoặc mạng trở lại; có kiểm tra nền định kỳ 15 phút. Android có thể trì hoãn tác vụ nền để tiết kiệm pin.
- Lỗi mạng giữ entry và dùng backoff để thử lại. Lỗi quyền/cấu hình giữ entry, báo chưa đồng bộ và cần sửa nguyên nhân rồi bấm Đồng bộ lại.
- Lượt xem và thời lượng đo lúc phát hiện vẫn được cập nhật cục bộ; chưa có dịch vụ thống kê lượt xem toàn hệ thống. Mật khẩu đổi qua Firebase Auth, không qua hàng đợi SQLite.

## 5. Demo online/offline

1. Đăng nhập online, tải danh sách và đợi dòng “Đã đồng bộ Firebase”.
2. Bật chế độ máy bay. Mở lại ứng dụng với phiên đã đăng nhập: thư viện đã cache vẫn xuất hiện.
3. Thêm/bỏ yêu thích hoặc đổi cài đặt. Nếu có file video đã tải, phát và thoát để lưu tiến trình.
4. Trang chủ hiển thị trạng thái ngoại tuyến và số thay đổi chờ gửi.
5. Bật mạng; chờ tác vụ hoặc bấm Đồng bộ. Kiểm tra các document dưới UID tương ứng trên Firestore Console.
6. Đăng nhập cùng tài khoản trên thiết bị khác, bấm Đồng bộ để tải lại lịch sử/yêu thích/cài đặt.
7. Dùng tài khoản khác để xác nhận dữ liệu riêng không bị trộn.
8. Thử xóa yêu thích khi offline, bật mạng rồi đồng bộ thiết bị thứ hai để kiểm tra tombstone.

Đăng ký, đăng nhập mới, đặt lại/đổi mật khẩu cần mạng. Offline dùng phiên Firebase đã lưu từ lần đăng nhập trước; không có cơ chế xác thực mật khẩu Firebase bằng SQLite. Việc khóa từ xa có hiệu lực khi thiết bị kết nối và cập nhật lại; không thể nhận lệnh khóa mới khi thiết bị hoàn toàn mất mạng.

Cache danh sách không đồng nghĩa đã tải file video. Chỉ video đã tải thành công mới phát được khi không có mạng. URL streaming/playlist không nhất thiết tải được bằng DownloadManager như một MP4 đơn.

## 6. Kiểm thử

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Kiểm thử Android dùng database/preferences có tiền tố `test_`. Các bài mới kiểm tra hàng đợi qua lần mở lại, revision chống mất sửa đổi, tombstone, tránh ghi đè dữ liệu chờ gửi, tránh gửi ngược sau import, tách tài khoản và migration schema 3 → 4.

Kiểm thử Firestore Rules cục bộ (cần Node.js và Java tương thích Firebase CLI):

```powershell
cd firebase-tests
npm install
npm test
```

Emulator dùng project giả `demo-streamwire`, không deploy hay ghi dữ liệu lên project thật. Kiểm tra quyền chủ sở hữu, cấm tự cấp ADMIN, tài khoản khóa, quyền quản trị video, catalog công khai và tombstone.

Việc build/test cục bộ không thay thế kiểm tra Firebase project thật. Sau khi cung cấp cấu hình và publish Rules, cần thực hiện kịch bản demo ở mục 5 để xác nhận quyền, kết nối và dữ liệu trên project của bạn.

Tham khảo: [Firebase Auth Android](https://firebase.google.com/docs/auth/android/password-auth), [Firestore offline](https://firebase.google.com/docs/firestore/manage-data/enable-offline), [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/rules-conditions).
