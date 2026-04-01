package com.example.parking_cloud.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;


@Service
public class FirebaseService {

    private DatabaseReference dbReference;

    @PostConstruct
    public void initialize() {
        try {
            org.springframework.core.io.ClassPathResource resource = 
            new org.springframework.core.io.ClassPathResource("service-account.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .setDatabaseUrl("https://parksync-e31a0-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    //link database URL của Firebase Realtime Database trên mây
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            dbReference = FirebaseDatabase.getInstance().getReference("parking_status");
            System.out.println("Firebase DA SAN SANG KET NOI Cloud!");
        } catch (Exception e) {
            System.err.println("LOI CAU HINH Firebase: " + e.getMessage());
        }
    }

    // Hàm để Coordinator gọi bắn số chỗ trống
    public void updateSpotsOnWeb(int availableSpots) {
        if (dbReference != null) {
            dbReference.child("available_spots").setValueAsync(availableSpots);
            System.out.println(" Cloud Firebase: DA CAP NHAT CON " + availableSpots + " CHO.");
        }
    }

    // Hàm để bắn trạng thái chi tiết của từng cổng lên Web
    // gateName: GATE_A, GATE_B... | data: Chứa biển số xe, thời gian...
    public void updateGateStatus(String gateName, java.util.Map<String, Object> data) {
        if (dbReference != null) {
            // Nó sẽ tạo cấu trúc: parking_status -> gates -> GATE_A -> {plate: "...", time: "..."}
            dbReference.child("gates").child(gateName).setValueAsync(data);
            System.out.println(" Firebase: DA CAP NHAT TRANG THAI CHO " + gateName);
        }
    }
}