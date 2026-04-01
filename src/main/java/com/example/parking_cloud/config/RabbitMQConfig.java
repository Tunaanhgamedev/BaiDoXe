package com.example.parking_cloud.config;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RabbitMQConfig {
   // Lấy tên cổng từ file properties (VD: GATE_HUY)
   @Value("${server.gate.name:DEFAULT_GATE}")
   private String gateName;
    
   // 1. TÊN CÁC ỐNG DẪN CHUNG CHO CẢ 5 MÁY
   public static final String WORK_QUEUE = "queue_xin_vao"; // Khay hồ sơ chung
   public static final String SYNC_EXCHANGE = "exchange_dong_bo"; // Loa phóng thanh tổng

    // 2. TẠO KHAY HỒ SƠ CHUNG (Để 5 máy tranh nhau xử lý)
    @Bean
    public Queue workQueue() {
        return new Queue(WORK_QUEUE, true);
    }

    // 3. TẠO LOA PHÓNG THANH (Dạng Fanout - Ai hét vào là dội đi muôn nơi)
    @Bean
    public FanoutExchange syncExchange() {
        return new FanoutExchange(SYNC_EXCHANGE);
    }

    // 4. TẠO ĐÔI TAI RIÊNG CHO MÁY NÀY (VD máy Vũ sẽ tạo ra: queue_sync_GATE_A)
    @Bean(name = "syncQueue")
    public Queue syncQueue() {
        return new Queue("queue_sync_" + gateName, true);
    }

    // 5. CẮM ĐÔI TAI RIÊNG VÀO CÁI LOA TỔNG
    @Bean
    public Binding bindingSync(Queue syncQueue, FanoutExchange syncExchange) {
        return BindingBuilder.bind(syncQueue).to(syncExchange);
    }
}