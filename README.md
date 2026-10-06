# phonecuibap (Android)
Launcher thật mô phỏng điện thoại cục gạch Nokia: thân máy + bàn phím cố định, chỉ màn hình LCD phía trên thay đổi.

- **Khóa màn hình:** mở app là thấy hình nền che khung hiển thị. Bấm **Menu** rồi bấm **\*** để mở khóa. Tắt màn hình máy sẽ khóa lại.
- **Menu:** Ứng dụng, Danh bạ, Tin nhắn, Đồng hồ (báo thức), Lịch (có âm lịch), Rắn săn mồi, Zalo, YouTube, Cài đặt
- **Cài đặt:** Chọn launcher · Cài đặt âm thanh (nhạc chuông, âm lượng, nhạc báo thức) · Wifi · SIM · Chế độ máy bay · Chia sẻ dữ liệu · Độ sáng · Pin · Bộ nhớ · Tài khoản · Lịch bật tắt nguồn · Bluetooth bật/tắt · Ứng dụng gọi · Khôi phục cài đặt gốc (chỉ xóa dữ liệu của app này)
  - **Wifi:** quét và chọn mạng; mạng có khóa thì hiện hộp nhập mật khẩu với bàn phím ảo của máy. Android 10+ không cho app tự bật/tắt wifi nên bấm dòng "Wifi" sẽ mở bảng wifi của hệ thống; Android 11+ lưu mạng qua hộp thoại của hệ thống.
  - **SIM:** mỗi SIM hiện nhà mạng, loại mạng (2G/3G/4G/5G), số điện thoại (nếu nhà mạng có ghi trên SIM). "Gọi bằng" / "Nhắn tin bằng": chọn SIM mà app này dùng khi gọi từ Danh bạ/màn hình chờ và khi gửi tin. SIM dữ liệu và công tắc dữ liệu di động: Android không cho app đổi nên bấm sẽ mở màn hình của hệ thống.
  - **Chế độ máy bay, Chia sẻ dữ liệu (phát wifi):** Android không cho app thường tự bật/tắt, bấm sẽ mở đúng màn hình cài đặt của hệ thống.
  - **Độ sáng:** ◀▶ chỉnh, OK tăng dần; cần cấp quyền "Sửa đổi cài đặt hệ thống". Có công tắc Tự động.
  - **Pin:** phần trăm pin, thời gian dùng app này trong ngày, nút mở trình tiết kiệm pin của hệ thống.
  - **Bộ nhớ:** tổng / trống / đã dùng của bộ nhớ trong. **Tài khoản:** hiện tài khoản Google (cần cấp quyền).
  - **Lịch bật tắt nguồn:** đặt giờ tắt / giờ bật lặp mỗi ngày. Android không cho app thường tắt/bật nguồn máy nên "tắt" = khóa và tắt màn hình (cần bật Trợ năng phonecuibap, Android 9+), "bật" = bật sáng màn hình.
- **Danh bạ:** đọc danh bạ trong máy; chọn một người (OK / chạm) là **gọi ngay**, không xác nhận. Bấm số 2–9 để nhảy tới tên theo chữ cái
- **Tin nhắn:** hộp thư theo hội thoại, đọc, trả lời, soạn mới (chọn người nhận bằng ▲▼ hoặc gõ số). Gõ chữ kiểu Nokia (bấm lặp phím số), `*` đổi dấu tiếng Việt (a → à á ả ã ạ ă…), `#` đổi kiểu abc/Abc/ABC/123
- **Zalo, YouTube:** nằm ngay trên Cài đặt. Zalo mở thẳng app. YouTube: máy có app "Tube for me" thì mở app đó (dò thấy lần đầu là ghi nhớ luôn), không có mới mở YouTube; chưa cài thì mở CH Play
- **Ghi âm:** nằm ngay dưới Lịch. OK để ghi/dừng và lưu, chọn bản ghi + OK để nghe lại, `#` để xóa
- **Máy tính:** + − × ÷ = , và ô phép tính lớn ở trên. Phím cứng: số, ▲ cộng, ▼ trừ, ◀ nhân, ▶ chia, OK bằng, `*` dấu phẩy, `#` xóa hết
- **Máy ảnh:** xem trước ngay trong ô LCD, OK để chụp (ảnh lưu vào Pictures/Nokia), ◀▶ đổi camera trước/sau
- **Thư viện:** xem ảnh trong máy, ◀▶ hoặc vuốt ngang để chuyển ảnh
- Màn hình chờ: bấm số + Gọi để gọi; nút Home về màn hình chờ, Back lùi một cấp
- Chia đôi màn hình + điều khiển app khác bằng Trợ năng (bật trong Cài đặt máy > Trợ năng > phonecuibap)

Build APK: push lên GitHub -> Actions -> "Build APK" -> tải artifact `phonecuibap-apk`.
Đặt làm launcher: Cài đặt máy -> Ứng dụng mặc định -> Ứng dụng màn hình chính -> phonecuibap.

## Gọi / nhận cuộc gọi kiểu Nokia

Cuộc gọi đi và đến hiển thị ngay trên màn hình LCD (tên + số, đồng hồ đếm giờ, phím mềm), điều khiển bằng phím Gọi (nghe), phím Tắt (từ chối / cúp), phím mềm, D-pad và phím số (gửi DTMF).

- Bật: Menu -> Cài đặt -> **Ứng dụng gọi** -> chọn phonecuibap làm ứng dụng gọi điện mặc định (bắt buộc để nhận cuộc gọi đến, Android không cho app thường tự vẽ giao diện cuộc gọi).
- Cuộc gọi đến: Gọi/Nghe = trả lời, Tắt/Từ chối = từ chối, OK = tắt chuông. Hiện cả trên màn hình khóa.
- Đang gọi: Menu = Loa ngoài, Micro, Giữ cuộc gọi, Ghi âm cuộc gọi, Kết thúc. Máy 2 SIM sẽ hỏi chọn SIM.
- Nếu chưa đặt làm mặc định, cuộc gọi vẫn dùng giao diện của hệ thống như cũ.

## App con Ghi âm

Menu -> Ghi âm (hoặc biểu tượng "Ghi âm Nokia" riêng): ghi, tạm dừng/tiếp tục, thanh mức âm, nghe lại có thanh tiến trình, xóa có xác nhận (#).
Bản ghi lưu trong bộ nhớ riêng của app (`files/recordings`), dùng chung với tính năng ghi âm cuộc gọi.
Ghi âm cuộc gọi thu bằng micro nên cần bật loa ngoài mới có tiếng bên kia; hãy báo cho người đối diện khi ghi âm.

