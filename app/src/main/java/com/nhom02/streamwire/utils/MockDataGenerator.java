package com.nhom02.streamwire.utils;

import com.nhom02.streamwire.model.CastMember;
import com.nhom02.streamwire.model.Category;
import com.nhom02.streamwire.model.Episode;
import com.nhom02.streamwire.model.User;
import com.nhom02.streamwire.model.Video;
import com.nhom02.streamwire.model.WatchHistory;

import java.util.ArrayList;
import java.util.List;

public class MockDataGenerator {

    public static List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("1", "Tất cả", true));
        list.add(new Category("2", "Hành Động"));
        list.add(new Category("3", "Hoạt Hình"));
        list.add(new Category("4", "Kinh Dị"));
        list.add(new Category("5", "Viễn Tưởng"));
        list.add(new Category("6", "Hài Kịch"));
        list.add(new Category("7", "Tài Liệu"));
        return list;
    }

    public static Video getHeroVideo() {
        Video video = new Video(
                "hero_01",
                "Siêu Phẩm Hành Động 2026",
                "Biệt đội tinh nhuệ dấn thân vào vùng cấm địa để giải cứu con tin và ngăn chặn thảm họa diệt vong toàn cầu.",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
                "125 phút",
                125 * 60 * 1000L,
                "1.2M lượt xem",
                9.8,
                2026,
                "16+",
                "4K HDR",
                "Hành Động",
                "Hôm nay"
        );
        return video;
    }

    public static List<Video> getTrendingVideos() {
        List<Video> list = new ArrayList<>();

        Video v1 = new Video(
                "v_trend_01",
                "Tập 1: Hành Trình Bắt Đầu",
                "Khám phá các bí ẩn vùng đất cổ xưa và khởi đầu cuộc chiến giữa các vì sao.",
                "https://media.w3.org/2010/05/bunny/trailer.mp4",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
                "45:20",
                (45 * 60 + 20) * 1000L,
                "120K lượt xem",
                9.6,
                2025,
                "13+",
                "Full HD",
                "Hành Động",
                "2 ngày trước"
        );
        list.add(v1);

        Video v2 = new Video(
                "v_trend_02",
                "Review Phim Hài Chiếu Rạp Tuần Này",
                "Tổng hợp các pha hài hước cười ra nước mắt của các bom tấn màn ảnh rộng tháng này.",
                "https://media.w3.org/2010/05/video/movie_300.mp4",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
                "32:10",
                (32 * 60 + 10) * 1000L,
                "85K lượt xem",
                8.9,
                2025,
                "13+",
                "1080p",
                "Hài Kịch",
                "3 ngày trước"
        );
        list.add(v2);

        return list;
    }

    public static List<Video> getRecommendedVideos() {
        List<Video> list = new ArrayList<>();

        Video v1 = new Video(
                "v_rec_01",
                "Bí Ẩn Rừng Đen: Ký Sự",
                "Theo dấu các sinh vật huyền bí trong rừng nguyên sinh Amazon.",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=400&q=80",
                "40:00",
                40 * 60 * 1000L,
                "45K lượt xem",
                9.1,
                2024,
                "16+",
                "1080p",
                "Khoa học",
                "5 ngày trước"
        );
        list.add(v1);

        Video v2 = new Video(
                "v_rec_02",
                "Khám Phá Vũ Trụ & Đa Chiều",
                "Vũ trụ có bao nhiêu chiều không gian và hố đen đang che giấu điều gì?",
                "https://media.w3.org/2010/05/bunny/trailer.mp4",
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80",
                "45:00",
                45 * 60 * 1000L,
                "78K lượt xem",
                9.4,
                2025,
                "Tất cả",
                "4K HDR",
                "Khoa học",
                "1 tuần trước"
        );
        list.add(v2);

        return list;
    }

    public static Video getDetailedSampleVideo() {
        Video video = new Video(
                "detail_01",
                "Chiến Binh Không Gian: Khởi Nguyên",
                "Vào năm 2187, khi trạm quỹ đạo Titan mất liên lạc với Trái Đất, một đội đặc nhiệm gồm những chiến binh tinh nhuệ nhất được cử đi điều tra. Tại đây, họ phát hiện ra một cổng dịch chuyển không gian cổ xưa và chủng tộc người máy ngoài hành tinh đang chuẩn bị xâm lăng Thiên Hà. Đội trưởng Jax và đồng đội phải đưa ra quyết định sinh tử...",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
                "135 phút",
                135 * 60 * 1000L,
                "350K lượt xem",
                9.8,
                2025,
                "16+",
                "4K HDR",
                "Hành Động • Viễn Tưởng",
                "Mới phát hành"
        );

        List<CastMember> cast = new ArrayList<>();
        cast.add(new CastMember("c1", "Tuấn Anh", "Jax (Đội trưởng)", ""));
        cast.add(new CastMember("c2", "Minh Huy", "Alex (Kỹ sư)", ""));
        cast.add(new CastMember("c3", "Lan Anh", "Tiến sĩ Kira", ""));
        cast.add(new CastMember("c4", "Quốc Bảo", "Đạo diễn", ""));
        video.setCastList(cast);

        List<Episode> episodes = new ArrayList<>();
        Episode ep1 = new Episode("ep1", 1, "Tập 1: Điểm Khởi Đầu", "45:00",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&q=80",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "Khám phá trạm không gian Titan và tín hiệu cầu cứu bí ẩn.");
        ep1.setPlaying(true);
        episodes.add(ep1);

        episodes.add(new Episode("ep2", 2, "Tập 2: Bí Ẩn Vùng Tối Titan", "42:15",
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80",
                "https://media.w3.org/2010/05/bunny/trailer.mp4",
                "Sự xuất hiện 90% quái nhân phân chia vũ trụ."));

        episodes.add(new Episode("ep3", 3, "Tập 3: Thực Thể Trong Bóng Tối", "48:10",
                "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=400&q=80",
                "https://media.w3.org/2010/05/video/movie_300.mp4",
                "Cuộc chạm trán bất ngờ đe dọa sinh mạng cả đoàn."));

        episodes.add(new Episode("ep4", 4, "Tập 4: Thời Khắc Quyết Định", "50:00",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400&q=80",
                "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4",
                "Kích hoạt cỗ máy dịch chuyển và trở về Trái Đất."));

        video.setEpisodes(episodes);
        return video;
    }

    public static List<WatchHistory> getWatchHistories() {
        List<WatchHistory> list = new ArrayList<>();

        Video v1 = getDetailedSampleVideo();
        WatchHistory h1 = new WatchHistory("h1", v1, "Tập 1: Khởi đầu",
                (29 * 60 + 15) * 1000L, 45 * 60 * 1000L, "Xem 2 giờ trước");
        list.add(h1);

        Video v2 = new Video("v2", "Bí Ẩn Rừng Đen: Án Tích Đêm...", "",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=400&q=80",
                "40:00", 40 * 60 * 1000L, "45K", 9.1, 2024, "16+", "1080p", "Khoa học viễn tưởng", "Hôm qua");
        WatchHistory h2 = new WatchHistory("h2", v2, "Tập 3: Đêm Định Mệnh",
                34 * 60 * 1000L, 40 * 60 * 1000L, "Xem 1 ngày trước");
        list.add(h2);

        Video v3 = new Video("v3", "Review Phim Hài Chiếu Rạp Tuần Này", "",
                "https://media.w3.org/2010/05/video/movie_300.mp4",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
                "32:00", 32 * 60 * 1000L, "85K", 8.9, 2025, "13+", "1080p", "Hài Kịch", "3 ngày trước");
        WatchHistory h3 = new WatchHistory("h3", v3, "Bản đầy đủ",
                (30 * 60 + 24) * 1000L, 32 * 60 * 1000L, "Xem 3 ngày trước");
        list.add(h3);

        Video v4 = new Video("v4", "Khám Phá Vũ Trụ & Đa Chiều", "",
                "https://media.w3.org/2010/05/bunny/trailer.mp4",
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80",
                "45:00", 45 * 60 * 1000L, "78K", 9.4, 2025, "Tất cả", "4K HDR", "Khoa học", "1 tuần trước");
        WatchHistory h4 = new WatchHistory("h4", v4, "Tập 1",
                (6 * 60 + 45) * 1000L, 45 * 60 * 1000L, "Xem 5 ngày trước");
        list.add(h4);

        return list;
    }

    public static List<Video> getAdminVideos() {
        List<Video> list = new ArrayList<>();

        Video v1 = getDetailedSampleVideo();
        v1.setStatus("ACTIVE"); // Đang hiển thị
        list.add(v1);

        Video v2 = new Video("adm_v2", "Bí Ẩn Rừng Đen: Án Tích Đêm", "",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=400&q=80",
                "40:00", 40 * 60 * 1000L, "45K", 9.1, 2024, "16+", "1080p", "Kinh Dị", "Hôm qua");
        v2.setStatus("PENDING"); // Chờ duyệt
        list.add(v2);

        Video v3 = new Video("adm_v3", "Review Phim Hài Chiếu Rạp 2025", "",
                "https://media.w3.org/2010/05/video/movie_300.mp4",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
                "32:00", 32 * 60 * 1000L, "85K", 8.9, 2025, "13+", "1080p", "Hài Kịch", "3 ngày trước");
        v3.setStatus("REPORTED"); // Báo cáo vi phạm
        list.add(v3);

        return list;
    }

    public static List<User> getAdminUsers() {
        List<User> list = new ArrayList<>();

        User u1 = new User("u1", "an.nguyen@email.com", "an_nguyen99", "Nguyễn Văn An", "USER", false);
        list.add(u1);

        User u2 = new User("u2", "bich.tran@email.com", "bich_tt", "Trần Thị Bích", "STANDARD", true);
        list.add(u2);

        User u3 = new User("u3", "nam.creator@email.com", "nam_creator", "Lê Hoàng Nam", "CREATOR", false);
        list.add(u3);

        return list;
    }
}

