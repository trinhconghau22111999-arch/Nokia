# Nokia Phone Launcher (Android)
Launcher thật mô phỏng điện thoại cục gạch Nokia: thân máy + bàn phím cố định, chỉ màn hình LCD phía trên thay đổi.

- **Khóa màn hình:** mở app là thấy hình nền che khung hiển thị. Bấm **Menu** rồi bấm **\*** để mở khóa. Tắt màn hình máy sẽ khóa lại.
- **Menu:** Ứng dụng, Danh bạ, Tin nhắn, Đồng hồ (báo thức), Lịch (có âm lịch), Rắn săn mồi, Cài đặt
- **Cài đặt:** Chọn launcher · Cài đặt âm thanh (nhạc chuông, âm lượng, nhạc báo thức) · Bluetooth bật/tắt · Khôi phục cài đặt gốc (chỉ xóa dữ liệu của app này)
- **Danh bạ:** đọc danh bạ trong máy; chọn một người (OK / chạm) là **gọi ngay**, không xác nhận. Bấm số 2–9 để nhảy tới tên theo chữ cái
- **Tin nhắn:** hộp thư theo hội thoại, đọc, trả lời, soạn mới (chọn người nhận bằng ▲▼ hoặc gõ số). Gõ chữ kiểu Nokia (bấm lặp phím số), `*` đổi dấu tiếng Việt (a → à á ả ã ạ ă…), `#` đổi kiểu abc/Abc/ABC/123
- **Zalo, YouTube:** mở thẳng app tương ứng (chưa cài thì mở CH Play)
- **Ghi âm:** nằm ngay dưới Lịch. OK để ghi/dừng và lưu, chọn bản ghi + OK để nghe lại, `#` để xóa
- **Máy tính:** + − × ÷ = , và ô phép tính lớn ở trên. Phím cứng: số, ▲ cộng, ▼ trừ, ◀ nhân, ▶ chia, OK bằng, `*` dấu phẩy, `#` xóa hết
- **Máy ảnh:** xem trước ngay trong ô LCD, OK để chụp (ảnh lưu vào Pictures/Nokia), ◀▶ đổi camera trước/sau
- **Thư viện:** xem ảnh trong máy, ◀▶ hoặc vuốt ngang để chuyển ảnh
- Màn hình chờ: bấm số + Gọi để gọi; nút Home về màn hình chờ, Back lùi một cấp
- Chia đôi màn hình + điều khiển app khác bằng Trợ năng (bật trong Cài đặt máy > Trợ năng > Nokia Phone)

Build APK: push lên GitHub -> Actions -> "Build APK" -> tải artifact `nokia-phone-apk`.
Đặt làm launcher: Cài đặt máy -> Ứng dụng mặc định -> Ứng dụng màn hình chính -> Nokia Phone.

## Gọi / nhận cuộc gọi kiểu Nokia

Cuộc gọi đi và đến hiển thị ngay trên màn hình LCD (tên + số, đồng hồ đếm giờ, phím mềm), điều khiển bằng phím Gọi (nghe), phím Tắt (từ chối / cúp), phím mềm, D-pad và phím số (gửi DTMF).

- Bật: Menu -> Cài đặt -> **Ứng dụng gọi** -> chọn Nokia Phone làm ứng dụng gọi điện mặc định (bắt buộc để nhận cuộc gọi đến, Android không cho app thường tự vẽ giao diện cuộc gọi).
- Cuộc gọi đến: Gọi/Nghe = trả lời, Tắt/Từ chối = từ chối, OK = tắt chuông. Hiện cả trên màn hình khóa.
- Đang gọi: Menu = Loa ngoài, Micro, Giữ cuộc gọi, Ghi âm cuộc gọi, Kết thúc. Máy 2 SIM sẽ hỏi chọn SIM.
- Nếu chưa đặt làm mặc định, cuộc gọi vẫn dùng giao diện của hệ thống như cũ.

## App con Ghi âm

Menu -> Ghi âm (hoặc biểu tượng "Ghi âm Nokia" riêng): ghi, tạm dừng/tiếp tục, thanh mức âm, nghe lại có thanh tiến trình, xóa có xác nhận (#).
Bản ghi lưu trong bộ nhớ riêng của app (`files/recordings`), dùng chung với tính năng ghi âm cuộc gọi.
Ghi âm cuộc gọi thu bằng micro nên cần bật loa ngoài mới có tiếng bên kia; hãy báo cho người đối diện khi ghi âm.

