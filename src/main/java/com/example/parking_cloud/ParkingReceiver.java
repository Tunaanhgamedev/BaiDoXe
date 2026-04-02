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

    private int availableSpots = 100; // Giả sử bãi có 100 chỗ trống ban đầu

    private static final int BUSY_THRESHOLD = 2; // Cổng bận khi đang xử lý từ 2 xe
    private java.util.concurrent.atomic.AtomicInteger activeTasks = new java.util.concurrent.atomic.AtomicInteger(0);

    // ====================================================================
    // LUỒNG 1: ĐIỀU PHỐI PHÂN TÁN (Cổng nào rảnh nhất sẽ tự nhặt việc)
    // ====================================================================
    @RabbitListener(queues = RabbitMQConfig.DISTRIBUTED_QUEUE)
    public void handleDistributedWork(String thongTinXe) {
        // 1. Phát tín hiệu "Đang xử lý" để các cổng khác đều biết
        String startSignal = "START|" + thongTinXe + "|" + myGateName;
        rabbitTemplate.convertAndSend(RabbitMQConfig.SYNC_EXCHANGE, "", startSignal);

        // 2. Tiến hành xử lý nghiệp vụ
        processWork(thongTinXe);
    }

    // Logic xử lý chính (Hợp nhất từ processEntryRequest)
    private void processWork(String thongTinXe) {
        activeTasks.incrementAndGet();
        try {
            System.out.println("[" + myGateName + "] DANG XU LY XE: " + thongTinXe);

            // Tách chuỗi để lấy hành động (VAO/RA)
            String[] parts = thongTinXe.split("\\|");
            if (parts.length < 2)
                return;

            String action = parts[0];

            // Nếu xe muốn VÀO nhưng bãi đã hết chỗ -> Từ chối ngay
            if (action.equals("VAO") && availableSpots <= 0) {
                System.out.println("[" + myGateName + "] BAI DA DAY, TU CHOI XE VAO!");
                return;
            }

            // Phát loa đồng bộ cho 4 anh em còn lại
            String syncMessage = thongTinXe + "|" + myGateName;
            System.out.println("[" + myGateName + "] BAN LEN LOA DONG BO: " + syncMessage);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SYNC_EXCHANGE, "", syncMessage);
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    @RabbitListener(queues = "#{syncQueue.name}")
    public void syncDatabase(String syncMessage) {
        // Kiểm tra nếu là tín hiệu bắt đầu xử lý (chỉ để Log, không lưu DB)
        if (syncMessage.startsWith("START|")) {
            String[] signalParts = syncMessage.split("\\|");
            if (signalParts.length >= 3) {
                System.out.println(">>> [TÍN HIỆU HỆ THỐNG] " + signalParts[2] + " ĐANG TIẾP NHẬN XE: " + signalParts[1]);
            }
            return;
        }

        System.out.println("[" + myGateName + "] NHAN TIN HIEU DONG BO: " + syncMessage);
        try {
            String[] parts = syncMessage.split("\\|");
            if (parts.length < 3)
                return;

            String action = parts[0];
            String bienSoXe = parts[1];
            String nguoiDuyet = parts[2];

            ParkingSlot xeTrongBai = repository.findFirstBySlotNameContaining(bienSoXe);

            if (action.equals("VAO")) {
                if (xeTrongBai != null)
                    return;

                ParkingSlot slotMoi = new ParkingSlot();
                slotMoi.setSlotName("XE CUA: " + bienSoXe);
                slotMoi.setOccupied(true);
                repository.saveAndFlush(slotMoi);

                availableSpots--; // Cập nhật biến local để máy mình biết

                // CHỈ MÁY CHỦ DUYỆT MỚI GỌI FIREBASE ĐỂ TRỪ 1
                if (nguoiDuyet.equals(myGateName)) {
                    firebaseService.updateSpotsOnWeb(-1); // TRUYỀN -1 LÀ ĐÚNG
                }

            } else if (action.equals("RA")) {
                if (xeTrongBai == null)
                    return;

                repository.delete(xeTrongBai);
                repository.flush();

                availableSpots++; // Cập nhật biến local

                if (nguoiDuyet.equals(myGateName)) {
                    firebaseService.updateSpotsOnWeb(1); // TRUYỀN 1 LÀ ĐÚNG
                }
            }

            ParkingLog logEntry = new ParkingLog();
            logEntry.setBienSo(bienSoXe);
            logEntry.setHanhDong(action);
            logEntry.setCongXuly(nguoiDuyet);
            logEntry.setThoiGian(new java.util.Date().toString());
            parkingLogRepository.save(logEntry);

        } catch (Exception e) {
            System.err.println("LOI XU LY DONG BO: " + e.getMessage());
        }
    }
}