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
public class ParkingController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ParkingLogRepository parkingLogRepository;

    @GetMapping("/history")
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
        // Lệnh này bảo trình duyệt: "Này, đừng in chữ index.html nữa, hãy mở file /index.html ở thư mục static ra!"
        response.sendRedirect("/index.html");
    }
    
    // API dành cho máy quét thẻ (POST)
    @PostMapping("/quet-the")
    public String quetTheXe(@RequestParam String bienSo) {
        String thongTinXe = "VAO|" + bienSo;
        rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_QUEUE, thongTinXe);
        System.out.println("DA GUI LEN HANG DOI: " + thongTinXe);
        return "Da gui yeu cau xin vao bai cho xe [" + bienSo + "] len RabbitMQ.";
    }

    // API Test trên trình duyệt (GET)
    // Link ĐÚNG: http://localhost:8081/api/parking/vao-bai?bienSo=43A-99999
    @GetMapping("/vao-bai")  // Đảm bảo dòng này ĐÃ XUỐNG DÒNG, không nằm sau dấu //
    public String vaoBai(@RequestParam String bienSo) {
        String message = "VAO|" + bienSo;
        rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_QUEUE, message);
        System.out.println("DA GUI LEN HANG DOI: " + message);

        return "Da gui yeu cau VAO BAI cho xe [" + bienSo + "] len RabbitMQ.";
    }

    // Link test: http://localhost:8081/api/parking/ra-bai?bienSo=43A-99999
    @GetMapping("/ra-bai")
    public String raBai(@RequestParam String bienSo) {
        String message = "RA|" + bienSo;
        rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_QUEUE, message);
        System.out.println("DA GUI LEN HANG DOI: " + message);

        return "DA GUI YEU CAU ROI BAI CHO XE [" + bienSo + "]";
    }
}