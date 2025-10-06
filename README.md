# Ứng dụng Mẫu UiccDeploy

## Tổng quan

UiccDeploySample là một ứng dụng mẫu Android minh họa việc tích hợp UICC (Universal Integrated Circuit Card) SDK để ký số tài liệu PDF trên thiết bị di động chạy Android OS. Project này giới thiệu cách thực hiện chữ ký số bằng cách sử dụng các khóa mật mã được lưu trữ trên thẻ SIM PKI (UICC), kết hợp với khả năng thao tác PDF.

Dự án bao gồm hai thành phần chính:
- **app**: Ứng dụng Android mẫu minh họa việc ký PDF dựa trên UICC
- **SimplePdfSigner**: Thư viện Android cung cấp chức năng ký PDF với hỗ trợ chữ ký bên ngoài

## Tính năng

- **Ký số dựa trên UICC**: Sử dụng khả năng mật mã của thẻ SIM để tạo chữ ký an toàn
- **Ký tài liệu PDF**: Tích hợp với iText7 để thao tác và nhúng chữ ký PDF
- **Tích hợp Cơ quan đóng dấu thời gian (TSA)**: Bao gồm gắn dấu thời gian vào chữ ký số

## Kiến trúc

### Mô-đun Ứng dụng
Ứng dụng chính minh họa quy trình ký hoàn chỉnh:
1. Tải tài liệu PDF mẫu và chứng thư từ assets
2. Nhúng hình ảnh chữ ký vào PDF
3. Chuẩn bị PDF để ký bằng SimplePdfSigner
4. Tạo hash tài liệu để ký
5. Sử dụng UiccSdk để ký hash trên UICC (thẻ SIM PKI)
6. Tùy chọn thêm dấu thời gian từ TSA

### Thư viện SimplePdfSigner
Thư viện Android nhẹ cung cấp:
- Chuẩn bị chữ ký PDF và tính toán hash
- Container chữ ký bên ngoài cho việc ký hoãn lại
- Đóng gói chữ ký PKCS#7
- Tích hợp với xử lý PDF iText7

## Điều kiện tiên quyết

- **Android Studio**: Arctic Fox hoặc phiên bản mới hơn (khuyến nghị)
- **Android SDK**: API level 28 (Android 9.0) hoặc cao hơn
- **Java**: JDK 8 hoặc cao hơn
- **Thiết bị hỗ trợ UICC**: Thiết bị Android với thẻ SIM 
- **Thư viện UiccSdk**: Được nhúng trong `app/libs/uiccsdk.aar`

## Xây dựng Dự án

1. Sao chép hoặc tải dự án về máy cục bộ
2. Mở dự án trong Android Studio
3. Đảm bảo tất cả các phụ thuộc được giải quyết (Gradle sync)
4. Cấu hình ký trong `app/build.gradle` (cập nhật đường dẫn keystore và mật khẩu)
5. Xây dựng dự án:
   ```bash
   ./gradlew build
   ```

## Chạy Ứng dụng

1. Kết nối thiết bị Android có khả năng UICC hoặc sử dụng trình giả lập
2. Cài đặt ứng dụng:
   ```bash
   ./gradlew installDebug
   ```
3. Khởi chạy ứng dụng trên thiết bị của bạn
4. Nhập PIN để truy cập UICC
5. Nhấn "Run" để bắt đầu quá trình ký PDF
6. PDF đã ký sẽ được lưu vào bộ nhớ trong của ứng dụng

## Cách sử dụng

### Quy trình ký cơ bản

1. **Nhập PIN**: Nhập PIN UICC vào trường được cung cấp
2. **Tải Assets**: Ứng dụng tự động tải các assets mẫu:
   - `vanbanmau.pdf`: Tài liệu PDF mẫu
   - `hoan.cer`: Chứng thư người ký
   - `thao.png`: Hình ảnh chữ ký
3. **Chuẩn bị chữ ký**: Ứng dụng chuẩn bị PDF và tính toán hash
4. **Ký UICC**: Hash được gửi đến UICC để ký mật mã
5. **Nhúng chữ ký**: Chữ ký được nhúng vào PDF
6. **Đóng dấu thời gian TSA**: Dấu thời gian được thêm từ máy chủ TSA đã cấu hình

### Cấu hình

- **URL TSA**: Sử dụng đường dẫn`http://tsa.ca.gov.vn` (có thể sửa đổi trong MainActivity.java)
- **Thuật toán Digest**: SHA-256 (có thể cấu hình)
- **Hiển thị chữ ký**: Hình chữ nhật 180x70 pixel tại vị trí (0,0) trên trang 1

## Phụ thuộc

### Mô-đun Ứng dụng
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.8.0`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `io.netty:netty-all:4.1.15.Final`
- `com.madgag.spongycastle:core:1.58.0.0`
- `com.madgag.spongycastle:bcpkix-jdk15on:1.58.0.0`
- `androidx.security:security-crypto:1.0.0-rc02`
- `com.google.android.gms:play-services-auth:17.0.0`
- `com.google.android.gms:play-services-base:18.0.1`
- `com.itextpdf.android:bouncy-castle-connector-android:8.0.5`
- `com.itextpdf.android:itext7-core-android:7.2.3`
- `uiccsdk.aar` (thư viện cục bộ)

### Thư viện SimplePdfSigner
- `androidx.appcompat:appcompat:1.7.0`
- `com.google.android.material:material:1.12.0`
- `com.itextpdf.android:bouncy-castle-connector-android:8.0.5`
- `com.itextpdf.android:itext7-core-android:7.2.3`

## Cân nhắc về Bảo mật

- **Bảo vệ PIN**: Các thao tác UICC yêu cầu xác thực PIN
- **Kiểm tra chứng thư**: Luôn kiểm tra chứng thư trước khi sử dụng
- **Xác minh TSA**: Đảm bảo chứng thư máy chủ TSA được tin cậy

## Khắc phục sự cố

### Các vấn đề phổ biến

1. **Truy cập UICC bị từ chối**: Xác minh PIN đúng và UICC hỗ trợ các thao tác mật mã, cập nhật hệ điều hành Android mới nhất
2. **Lỗi xây dựng**: Đảm bảo tất cả phụ thuộc được cấu hình đúng và Android SDK được cập nhật
3. **Lỗi ký PDF**: Kiểm tra tính hợp lệ của chứng thư và tính khả dụng của máy chủ TSA
4. **Vấn đề quyền**: Cấp các quyền lưu trữ cần thiết cho các thao tác tệp PDF

### Nhật ký
Kiểm tra logcat Android để biết thông báo lỗi chi tiết với thẻ "uiccdeploy".

## Đóng góp

Đây là ứng dụng mẫu cho mục đích minh họa. Để đóng góp hoặc sửa đổi:
1. Fork kho lưu trữ
2. Tạo nhánh tính năng
3. Thực hiện thay đổi của bạn
4. Kiểm tra kỹ lưỡng trên thiết bị hỗ trợ UICC
5. Gửi pull request

## Giấy phép

[Thêm thông tin giấy phép phù hợp tại đây]

## Tuyên bố miễn trừ trách nhiệm

Ứng dụng mẫu này được cung cấp cho mục đích tập huấn và minh họa. Đảm bảo tuân thủ các quy định liên quan đến chữ ký số và các thao tác mật mã trước khi triển khai trong môi trường sản xuất.


Nếu repo này hữu ích với bạn, hay ủng hộ chúng tôi nhé:

<a href="https://buymeacoffee.com/txaopc" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="170px"></a>

or TPBank: 0292 1484 501 - NGUYEN THI HANG 
(<a href="https://github.com/user-attachments/assets/8ae98397-f1bc-4f49-b266-9d098e797bfd" target="_blank">QR Code</a>)
