package com.example.parking_cloud;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.parking_cloud.config.FirebaseService;
import com.example.parking_cloud.config.RabbitMQConfig;
import com.example.parking_cloud.model.ParkingLog;
import com.example.parking_cloud.model.ParkingLogRepository;
import com.example.parking_cloud.model.ParkingSlot;
import com.example.parking_cloud.model.ParkingSlotRepository;

@Component
public class ParkingReceiver {

    @Autowired
    private ParkingSlotRepository repository; // Dùng để lưu trạng thái chỗ đậu xe TRÊN MySQL
    @Autowired
    private ParkingLogRepository parkingLogRepository; // Dùng để lưu nhật ký TRÊN MONGODB ATLAS

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private RabbitTemplate rabbitTemplate; // Dùng để hét lên loa

    @Value("${server.gate.name:DEFAULT_GATE}")
    private String myGateName; // Tên cổng của máy này (VD: GATE_HUY)

    private int availableSpots = 100;  // Giả sử bãi có 100 chỗ trống ban đầu

    // ====================================================================
    // LUỒNG 1: TRANH VIỆC (Chỉ 1 máy giật được phiếu từ queue_xin_vao)
    // ====================================================================
    @RabbitListener(queues = RabbitMQConfig.WORK_QUEUE)
    public void processEntryRequest(String thongTinXe) {
        System.out.println("[" + myGateName + "] DA CHOP DUOC PHIEU CUA XE: " + thongTinXe);

        // Tách chuỗi để lấy hành động (VAO/RA)
        String[] parts = thongTinXe.split("\\|");
        
        // BẢO VỆ 1: Lọc tin nhắn rác hoặc định dạng sai
        if (parts.length < 2) {
            System.err.println("BO QUA TIN NHAN RAC: " + thongTinXe);
            return;
        }

        String action = parts[0];

        // Nếu xe muốn VÀO nhưng bãi đã hết chỗ -> Từ chối ngay
        if (action.equals("VAO") && availableSpots <= 0) {
            System.out.println("[" + myGateName + "] BAI DA DAY, TU CHOI XE VAO!");
            return;
        }

        // Đóng gói tin nhắn đồng bộ. Nối thêm tên Cổng bằng dấu "|"
        // Kết quả sẽ có dạng: "VAO|43A123|GATE_HUY" hoặc "RA|43A123|GATE_HUY"
        String syncMessage = thongTinXe + "|" + myGateName;
        
        // HÉT LÊN LOA ĐỂ BÁO CHO 4 ANH EM CÒN LẠI!
        System.out.println("[" + myGateName + "] HOP LE! BAN LEN LOA DONG BO: " + syncMessage);
        rabbitTemplate.convertAndSend(RabbitMQConfig.SYNC_EXCHANGE, "", syncMessage);
    }

    // ====================================================================
    // LUỒNG 2: NGHE ĐỒNG BỘ (Cả 5 máy cùng nghe loa để update MySQL)
    // ====================================================================
    // ====================================================================
    // LUỒNG 2: NGHE ĐỒNG BỘ (Cả 5 máy cùng nghe loa để update MySQL)
    // ====================================================================
    // ====================================================================
    // LUỒNG 2: NGHE ĐỒNG BỘ (Cả 5 máy cùng nghe loa để update MySQL)
    // ====================================================================
    @RabbitListener(queues = "#{syncQueue.name}")
    public void syncDatabase(String syncMessage) {
        System.out.println("[" + myGateName + "] NGHE TU LOA TONG: " + syncMessage);

        try {
            // Tách tin nhắn thành 3 phần chuẩn xác
            String[] parts = syncMessage.split("\\|");
            
            // BẢO VỆ 2: Loại bỏ các bản tin cũ bị lỗi còn kẹt trong RabbitMQ
            if (parts.length < 3) {
                System.err.println("BO QUA TIN DONG BO CU/LOI: " + syncMessage);
                return;
            }

            String action = parts[0];     // VAO hoặc RA
            String bienSoXe = parts[1];   // Biển số xe
            String nguoiDuyet = parts[2]; // Tên máy đã duyệt (VD: GATE_HUY)

            // ===============================================================
            // 1. MySQL GÁC CỔNG: Kiểm tra xem xe đang ở TRONG hay NGOÀI bãi
            // ===============================================================
            ParkingSlot xeTrongBai = repository.findFirstBySlotNameContaining(bienSoXe);

            if (action.equals("VAO")) {
                if (xeTrongBai != null) {
                    System.err.println("🚫 LỖI BẢO MẬT: Xe " + bienSoXe + " ĐÃ CÓ TRONG BÃI! Hủy lưu trữ.");
                    return; // Đuổi về, không làm gì tiếp
                }
                
                // Xe hợp lệ -> LƯU MỚI VÀO MySQL
                ParkingSlot slotMoi = new ParkingSlot(); 
                slotMoi.setSlotName("XE CUA: " + bienSoXe); 
                slotMoi.setOccupied(true);
                repository.saveAndFlush(slotMoi);
                System.out.println("DA LUU DONG MỚI VÀO MySQL CHO XE: " + bienSoXe);

                if (availableSpots > 0) {
                    availableSpots--; // Trừ chỗ
                } 

            } else if (action.equals("RA")) {
                if (xeTrongBai == null) {
                    System.err.println("⚠️ CẢNH BÁO: Xe " + bienSoXe + " KHÔNG CÓ TRONG BÃI! Hủy lưu trữ.");
                    return; // Đuổi về, không làm gì tiếp
                }
                
                // Xe hợp lệ -> XÓA LUÔN KHỎI MySQL CHO NHẸ DATABASE
                repository.delete(xeTrongBai);
                repository.flush(); // Ép xóa ngay lập tức
                System.out.println("ĐÃ XÓA XE KHỎI MySQL: " + bienSoXe);

                if (availableSpots < 100) { 
                    availableSpots++; // Cộng chỗ
                }
            }
            // ===============================================================

            // 2. MONGODB ATLAS GHI SỔ: Lưu nhật ký (Không bao giờ xóa)
            ParkingLog logEntry = new ParkingLog();
            logEntry.setBienSo(bienSoXe);
            logEntry.setHanhDong(action);
            logEntry.setCongXuly(nguoiDuyet); 
            logEntry.setThoiGian(new java.util.Date().toString()); 
            
            parkingLogRepository.save(logEntry);
            System.out.println("[MONGODB] Đã ghi nhận nhật ký xe " + bienSoXe + " lên Atlas thành công!");

            // 3. BẮN LÊN FIREBASE (Báo cáo số lượng chỗ trống mới nhất)
            firebaseService.updateSpotsOnWeb(availableSpots); 
            System.out.println("DA BAO CAO SO LUONG CHO TRONG: " + availableSpots);

        } catch (Exception e) {
            System.err.println("LOI XU LY DONG BO: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}