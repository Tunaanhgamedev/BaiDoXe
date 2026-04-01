package com.example.parking_cloud.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        // Chứng minh: Khởi tạo kết nối trực tiếp đến Atlas URI
        System.out.println("DANG THIET LAP KET NOI DEN MONGODB ATLAS...");
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        // Chứng minh: Tạo đối tượng MongoTemplate để thao tác với dữ liệu trên mây
        return new MongoTemplate(mongoClient(), "parking_db");
    }
}