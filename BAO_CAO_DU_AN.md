# BÁO CÁO TỔNG QUAN DỰ ÁN STREAMWIRE

Ngày lập: 16/11/2026. Báo cáo dựa trên mã nguồn hiện tại trong thư mục `streamhub`, sau đợt chuyển các luồng dữ liệu sang SQLite.

## 1. Giới thiệu

StreamWire là ứng dụng Android phục vụ việc quản lý và xem video. Người dùng có thể duyệt thư viện, tìm kiếm, xem chi tiết, phát video, lưu yêu thích, tải video và theo dõi lịch sử xem. Quản trị viên có thể quản lý video và tài khoản ngay trên thiết bị.

Tên thư mục dự án là `streamhub`; tên hiển thị và package ứng dụng sử dụng StreamWire, `com.nhom02.streamwire`.

Ứng dụng hoạt động với cơ sở dữ liệu SQLite cục bộ. Tài khoản, thư viện video và dữ liệu cá nhân được lưu trên thiết bị cài đặt; hiện chưa có máy chủ để đồng bộ giữa nhiều thiết bị.

## 2. Công nghệ và cấu trúc

| Thành phần | Công nghệ / cách triển khai |
|---|---|
| Ngôn ngữ | Java; mức tương thích mã nguồn Java 17 |
| Giao diện | XML layout, Material Components, ViewBinding |
| Điều hướng | Activity, Fragment, thanh điều hướng dưới |
| Danh sách | RecyclerView và các Adapter |
| Phát video | AndroidX Media3 ExoPlayer |
| Hiển thị ảnh | Glide |
| Lưu dữ liệu | SQLiteOpenHelper và SQLiteDatabase |
| Phiên đăng nhập | SharedPreferences |
| Bảo vệ mật khẩu | PBKDF2-HMAC-SHA256, salt ngẫu nhiên |
| Tải video | Android DownloadManager |
| Xuất dữ liệu | CSV, trình chọn vị trí lưu của Android |
| Cảm biến | Ánh sáng và gia tốc kế |
| Hệ điều hành tối thiểu | Android 8.0, API 26 |
| SDK biên dịch / SDK mục tiêu | API 34 |
| Phiên bản trong cấu hình | 1.0.2 |

Các nhóm mã nguồn chính:

- `ui/auth`: đăng nhập, hộp thoại đăng ký.
- `ui/main`: khung điều hướng, trang chủ, lịch sử, quản trị, cài đặt.
- `ui/detail`: thông tin chi tiết video.
- `ui/player`: phát video và các điều khiển.
- `db`: tạo, nâng cấp, truy vấn và cập nhật SQLite.
- `model`: User, Video, Category, Episode, CastMember, WatchHistory.
- `adapter`: hiển thị danh sách video, thể loại, lịch sử, tập phim, diễn viên và dữ liệu quản trị.
- `utils`: phiên đăng nhập, băm mật khẩu và xử lý cảm biến.

Trong Gradle vẫn còn khai báo Retrofit/OkHttp/Gson. Luồng trang chủ hiện tại đọc SQLite, không còn sử dụng API đồng bộ JSON cố định trước đây.

## 3. Các màn hình hiện có

Ứng dụng có **7 màn hình nội dung chính**, cùng một khung điều hướng và các hộp thoại thao tác. Đăng ký được triển khai bằng hộp thoại, không phải một trang riêng.

| STT | Màn hình | Lớp triển khai | Mục đích |
|---|---|---|---|
| 1 | Đăng nhập | LoginActivity | Đăng nhập, mở đăng ký, vào chế độ khách |
| 2 | Trang chủ | HomeFragment | Duyệt, tìm kiếm, lọc và mở video |
| 3 | Chi tiết video | VideoDetailActivity | Xem thông tin, chọn tập, yêu thích, chia sẻ, tải video |
| 4 | Trình phát video | VideoPlayerActivity | Xem video và điều khiển phát |
| 5 | Lịch sử xem | HistoryFragment | Theo dõi và tiếp tục các nội dung đã xem |
| 6 | Quản trị | AdminFragment | Quản lý video, người dùng, xuất CSV |
| 7 | Cài đặt và cảm biến | SettingsFragment | Cấu hình ứng dụng, đổi mật khẩu, đăng xuất |

`MainActivity` là khung chứa bốn tab: **Trang chủ – Lịch sử – Quản trị – Cài đặt**. Tab Quản trị chỉ hiển thị cho ADMIN. Thanh trên có nút tìm kiếm và hộp thoại thông tin tài khoản.

### 3.1. Đăng nhập và đăng ký

- Đăng nhập bằng email hoặc tên đăng nhập, đối chiếu với dữ liệu SQLite.
- Kiểm tra thông tin còn thiếu và thông báo khi sai tài khoản hoặc mật khẩu.
- Từ chối đăng nhập đối với tài khoản bị khóa.
- Ghi nhớ phiên để quay lại ứng dụng.
- Đăng ký bằng họ tên, email và mật khẩu; kiểm tra định dạng email, email đã tồn tại và mật khẩu tối thiểu 6 ký tự.
- Tài khoản đăng ký đầu tiên trên cơ sở dữ liệu mới nhận quyền ADMIN; các tài khoản tiếp theo nhận quyền USER.
- Cho phép vào chế độ khách mà không cần đăng ký.
- Nút quên mật khẩu hiện mở hướng dẫn, chưa có quy trình gửi OTP/email hoặc đặt lại mật khẩu tự động.

### 3.2. Trang chủ

- Đọc video và danh mục từ SQLite.
- Hiển thị video tiêu điểm khi có video công khai.
- Hiển thị các khu vực danh sách video và đề xuất.
- Tìm kiếm theo tên video và thể loại.
- Lọc theo thể loại.
- Sắp xếp theo mới nhất, lượt xem hoặc điểm đánh giá đã lưu.
- Xem tất cả video công khai hoặc lọc danh sách yêu thích.
- Kéo để tải lại dữ liệu cục bộ.
- Nhấn video để mở trang chi tiết.
- Hiển thị hướng dẫn khi thư viện trống.

Video ở trạng thái PENDING hoặc REPORTED không xuất hiện trong truy vấn thư viện công khai. Khu vực đề xuất hiện dựa trên dữ liệu/truy vấn cục bộ, chưa có hệ thống gợi ý cá nhân hóa bằng học máy.

### 3.3. Chi tiết video

- Hiển thị tên, mô tả, ảnh bìa, thể loại, năm phát hành, thời lượng, chất lượng và điểm đánh giá nếu có.
- Mở rộng hoặc thu gọn mô tả.
- Đọc danh sách diễn viên và tập phim nếu có dữ liệu tương ứng.
- Phát video hoặc chọn tập để xem.
- Thêm/bỏ yêu thích, lưu theo người dùng trong SQLite.
- Chia sẻ tên và URL video bằng trình chia sẻ của Android.
- Gửi yêu cầu tải xuống qua DownloadManager, lưu mã tải trong SQLite.

Trang này không có giao diện chấm điểm, bình luận hoặc chọn mùa phim. Danh sách yêu thích được truy cập từ bộ lọc Trang chủ, chưa có tab riêng.

### 3.4. Trình phát video

- Phát nguồn video bằng Media3 ExoPlayer.
- Ưu tiên file đã tải thành công khi phát URL gốc của cùng video.
- Phát/tạm dừng, bật/tắt tiếng.
- Tua lùi hoặc tiến 10 giây; kéo thanh tiến trình để chọn thời điểm.
- Hiển thị thời gian hiện tại và tổng thời lượng.
- Đổi tốc độ: 0.75x, 1x, 1.25x, 1.5x, 2x.
- Đặt giới hạn chất lượng 1080p, 720p, 480p hoặc tự động; áp dụng lựa chọn mặc định đã lưu.
- Chuyển toàn màn hình ngang/dọc.
- Chạm để hiện/ẩn điều khiển; chạm hai lần hai bên để tua khi bật cử chỉ.
- Lắc thiết bị để tua tới 10 giây nếu có gia tốc kế và đã bật tùy chọn.
- Chọn tập khác từ danh sách tập.
- Lưu vị trí xem khi tạm rời màn hình và trước khi chuyển tập.
- Ghi nhận lượt xem khi nguồn sẵn sàng phát; báo lỗi nguồn khi không phát được.

Việc đổi chất lượng phụ thuộc các track mà nguồn cung cấp. File MP4 đơn không tự tạo thêm các phiên bản độ phân giải khác. Phát được còn phụ thuộc định dạng, codec, đường dẫn và khả năng truy cập nguồn.

### 3.5. Lịch sử xem

- Đọc lịch sử của tài khoản hiện tại; khách dùng vùng dữ liệu riêng.
- Hiển thị video, tên tập, vị trí đã xem và mức độ hoàn thành.
- Tiếp tục xem từ vị trí đã lưu, chọn lại tập theo tên tập trong dữ liệu lịch sử.
- Tìm kiếm theo tên video.
- Lọc tất cả, chưa xem xong và đã xem xong; ngưỡng phân loại hiện tại là 90%.
- Xóa từng mục hoặc toàn bộ lịch sử của người dùng hiện tại.
- Hiển thị số nội dung và trạng thái danh sách trống.

### 3.6. Quản trị

Quản lý nội dung:

- Xem tổng số người dùng và số video đang công khai.
- Xem, tìm kiếm danh sách video quản trị.
- Thêm video bằng tên, thể loại, mô tả, URL nguồn, URL ảnh bìa và thời lượng.
- Tạo một tập mặc định gắn với nguồn khi thêm video không có danh sách tập.
- Chỉnh sửa tên, thể loại, mô tả, nguồn video, ảnh bìa và thời lượng.
- Công khai/ẩn video bằng trạng thái ACTIVE/PENDING.
- Xóa video và các bản ghi liên quan.
- Hiển thị video có trạng thái REPORTED nếu dữ liệu đó tồn tại; chuyển video được chọn về PENDING để xem xét.

Quản lý tài khoản:

- Xem danh sách người dùng.
- Đổi vai trò giữa USER, STANDARD, CREATOR, ADMIN.
- Khóa hoặc mở khóa tài khoản.
- Cấp quyền ADMIN cho một tài khoản đã tồn tại thông qua email hoặc tên đăng nhập.

Xuất dữ liệu:

- Chọn vị trí lưu và xuất danh sách video ra CSV.
- Các cột hiện có: `id`, `title`, `category`, `status`.
- Đây là báo cáo thư viện video, chưa phải nhật ký đầy đủ về mọi thao tác quản trị.

### 3.7. Cài đặt và cảm biến

- Bật/tắt tự đổi giao diện sáng/tối theo cảm biến ánh sáng.
- Bật/tắt lắc thiết bị để tua video.
- Bật/tắt cử chỉ chạm thông minh.
- Phát hiện thiết bị không hỗ trợ cảm biến và vô hiệu hóa tùy chọn tương ứng.
- Bật/tắt thông báo tải xuống theo tùy chọn ứng dụng.
- Chọn giới hạn chất lượng phát mặc định.
- Đổi mật khẩu cho tài khoản đang đăng nhập; khách không được đổi mật khẩu.
- Đăng xuất và quay lại màn hình đăng nhập.
- Lưu các tùy chọn vào SQLite theo tài khoản.

## 4. Người dùng và quyền truy cập

| Nhóm | Khả năng hiện tại |
|---|---|
| Khách | Duyệt/xem video công khai, tìm kiếm, dùng yêu thích/lịch sử/cài đặt dưới khóa guest; không có trang quản trị và không đổi mật khẩu |
| USER | Chức năng xem video và dữ liệu cá nhân theo tài khoản, đổi mật khẩu |
| STANDARD | Hiện sử dụng các chức năng thông thường như USER |
| CREATOR | Hiện sử dụng các chức năng thông thường như USER; chưa có trang đăng tải dành riêng cho nhà sáng tạo |
| ADMIN | Các chức năng thông thường và trang quản trị video, tài khoản, CSV |

STANDARD và CREATOR hiện là các giá trị vai trò có thể gán, chưa có bộ quyền nghiệp vụ riêng. Đăng ký đầu tiên thành ADMIN chỉ áp dụng khi bảng người dùng chưa có tài khoản.

## 5. Cơ sở dữ liệu và lưu trữ

Tên database: **streamwire.db**. Phiên bản schema hiện tại: **3**. Có **9 bảng**:

| Bảng | Nội dung lưu trữ |
|---|---|
| users | Tài khoản, thông tin người dùng, mật khẩu đã băm, vai trò, trạng thái khóa |
| categories | Danh mục/thể loại video |
| videos | Thông tin, đường dẫn, trạng thái, lượt xem và metadata của video |
| episodes | Các tập thuộc video |
| cast_members | Diễn viên thuộc video |
| watch_history | Video, tên tập, vị trí xem, thời lượng, thời gian cập nhật và chủ sở hữu |
| favorites | Liên kết người dùng với video yêu thích |
| settings | Cặp tên/giá trị cài đặt theo người dùng |
| downloads | Mã yêu cầu DownloadManager, người dùng và video tương ứng |

Quan hệ tập phim, diễn viên, lịch sử, yêu thích và metadata tải xuống được gắn với video. Các khóa ngoại tương ứng dùng xóa liên quan khi video bị xóa; phần quản lý dữ liệu cũng xử lý xóa các bản ghi con.

SQLite lưu thông tin và đường dẫn, **không lưu file MP4 dưới dạng dữ liệu nhị phân**. File tải nằm trong thư mục Movies riêng của ứng dụng, do DownloadManager quản lý. SharedPreferences giữ thông tin phiên đăng nhập cục bộ.

Nâng cấp từ schema 2 lên 3 giữ dữ liệu cũ. Lịch sử trước đây chưa xác định chủ sở hữu được giữ dưới khóa `legacy`; dữ liệu mẫu từ các bản cài cũ không bị tự xóa. Bản cài mới không tự sinh tài khoản, phim hoặc lịch sử giả.

## 6. Luồng sử dụng để trình bày/demo

1. Mở ứng dụng và đăng ký tài khoản đầu tiên để nhận quyền ADMIN.
2. Vào Quản trị, thêm một video bằng URL MP4 HTTP/HTTPS trực tiếp có thể truy cập.
3. Về Trang chủ, tìm kiếm hoặc lọc thể loại và mở chi tiết video.
4. Thêm yêu thích; quay về Trang chủ, chọn bộ lọc Yêu thích để kiểm tra.
5. Phát video, tua, đổi tốc độ và thử toàn màn hình.
6. Thoát trình phát, vào Lịch sử và tiếp tục xem từ vị trí đã lưu.
7. Tải video, chờ tải thành công và mở lại cùng video để thử bản cục bộ.
8. Vào Cài đặt, thay đổi tùy chọn chất lượng/cử chỉ và kiểm tra khi mở lại.
9. Vào Quản trị để sửa thông tin, ẩn/công khai video và xuất CSV.
10. Đăng ký thêm tài khoản để kiểm tra lịch sử và yêu thích được tách riêng.

## 7. Những phần chưa có hoặc còn giới hạn

| Hạng mục | Hiện trạng |
|---|---|
| Backend và đồng bộ đa thiết bị | Chưa có; dữ liệu hiện nằm trên từng thiết bị |
| Khôi phục mật khẩu bằng OTP/email | Chưa có; nút hiện chỉ cung cấp hướng dẫn |
| Đăng nhập Google/Facebook | Chưa có |
| Upload file lên máy chủ | Chưa có; quản trị nhập URL nguồn video |
| Quản lý nhiều tập/mùa bằng giao diện | Đã có model, bảng và giao diện xem tập; chưa có màn hình CRUD nhiều tập/mùa đầy đủ |
| Quản lý diễn viên bằng giao diện | Đã có bảng và giao diện hiển thị; chưa có màn hình nhập/sửa diễn viên |
| Bình luận và chấm sao | Chưa có luồng nhập của người xem; điểm rating hiện là dữ liệu đọc từ video |
| Gửi báo cáo vi phạm | Có xử lý trạng thái REPORTED phía quản trị; chưa có luồng gửi báo cáo của người xem và bảng báo cáo riêng |
| Trang tải xuống | Chưa có trang riêng để xem tiến độ, hủy hoặc xóa các file đã tải |
| Nhật ký quản trị | Chưa có bảng audit ghi toàn bộ thao tác; CSV hiện chỉ xuất thư viện video |
| Gợi ý cá nhân hóa | Chưa có thuật toán đề xuất dựa trên hành vi người dùng |
| Bảo vệ quản trị viên cuối cùng | Chưa có quy tắc ngăn hạ quyền/khóa tài khoản ADMIN cuối cùng |

Các mục này được ghi riêng để tránh nhầm giữa thành phần đã có trong dữ liệu/giao diện và chức năng nghiệp vụ đã triển khai đầy đủ.

## 8. Kết quả kiểm tra gần nhất

Trong đợt sửa trước khi lập báo cáo:

- APK debug đã được đóng gói thành công.
- Unit test, bao gồm kiểm tra băm/xác minh mật khẩu, đã đạt.
- Android lint đã chạy thành công; vẫn có cảnh báo cần cải thiện dần, không đồng nghĩa dự án không có cảnh báo.
- **6/6 kiểm thử Android trên giả lập Pixel 8 đã đạt**, gồm kiểm tra cơ bản package và các trường hợp: tài khoản đầu tiên/lưu qua lần mở lại database, tách dữ liệu tài khoản, video ẩn/xóa liên quan, migration giữ dữ liệu, mở Trang chủ trống và Quản trị.

Phạm vi kiểm thử này chưa chứng minh mọi URL video ngoài mạng, codec, tình huống tải xuống hoặc mọi thiết bị/cảm biến đều hoạt động. Các chức năng đó còn phụ thuộc nguồn và môi trường chạy.

Tệp liên quan:

- Hướng dẫn chạy: `README.md`.
- APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Kiểm thử SQLite/giao diện: `app/src/androidTest/java/com/nhom02/streamwire/SQLiteWorkflowTest.java`.
- Kiểm thử mật khẩu: `app/src/test/java/com/nhom02/streamwire/PasswordHasherTest.java`.
