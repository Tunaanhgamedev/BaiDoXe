package com.example.parking_cloud;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.parking_cloud.config.RabbitMQConfig;
import com.example.parking_cloud.model.ParkingLog;
import com.example.parking_cloud.model.ParkingLogRepository;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ParkingLogRepository parkingLogRepository;

    @org.springframework.beans.factory.annotation.Value("${server.gate.name:DEFAULT_GATE}")
    private String myGateName;

    @GetMapping("/history")
    // ... (rest of the code trimmed for brevity, but I'll provide the full block)
    public List<ParkingLog> getHistoryLogs() {
        // Lấy toàn bộ Log từ MongoDB Atlas
        List<ParkingLog> logs = parkingLogRepository.findAll();
        // Đảo ngược danh sách để xe nào vừa quẹt thẻ sẽ hiện lên đầu bảng
        Collections.reverse(logs);
        // Trả về tối đa 50 dòng mới nhất cho Web đỡ lag
        return logs.stream().limit(50).toList();
    }

    // Link: http://localhost:8081/api/parking/dashboard
    @GetMapping("/dashboard")
    public void showDashboard(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        // Lệnh này bảo trình duyệt: "Này, đừng in chữ index.html nữa, hãy mở file
        // /index.html ở thư mục static ra!"
        response.sendRedirect("/index.html");
    }

    // API dành cho máy quét thẻ (POST)
    @PostMapping("/quet-the")
    public String quetTheXe(@RequestParam String bienSo) {
        String thongTinXe = "VAO|" + bienSo;
        // Gửi vào hàng chờ chung của hệ thống phân tán
        rabbitTemplate.convertAndSend(RabbitMQConfig.DISTRIBUTED_QUEUE, thongTinXe);
        System.out.println("[" + myGateName + "] DA TIEP NHAN VA DAY VAO HANG CHO CHUNG: " + thongTinXe);
        return "Xe [" + bienSo + "] da duoc dua vao hang cho xu ly phan tan.";
    }

    // API Test trên trình duyệt (GET)
    @GetMapping("/vao-bai")
    public String vaoBai(@RequestParam String bienSo) {
        String message = "VAO|" + bienSo;
        rabbitTemplate.convertAndSend(RabbitMQConfig.DISTRIBUTED_QUEUE, message);
        System.out.println("[" + myGateName + "] DA TIEP NHAN (GET) VA DAY VAO HANG CHO CHUNG: " + message);
        return "Yeu cau VAO BAI cho xe [" + bienSo + "] da vao hang cho phan tan.";
    }

    @GetMapping("/ra-bai")
    public String raBai(@RequestParam String bienSo) {
        String message = "RA|" + bienSo;
        rabbitTemplate.convertAndSend(RabbitMQConfig.DISTRIBUTED_QUEUE, message);
        System.out.println("[" + myGateName + "] DA TIEP NHAN (GET) VA DAY VAO HANG CHO CHUNG: " + message);
        return "Yeu cau RA BAI cho xe [" + bienSo + "] da vao hang cho phan tan.";
    }
}