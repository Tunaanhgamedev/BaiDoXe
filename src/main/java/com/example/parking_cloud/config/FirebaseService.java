package com.example.parking_cloud.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.util.Map;

@Service
public class FirebaseService {

    private DatabaseReference dbReference;

    @PostConstruct
    public void initialize() {
        try {
            // 1. Thử tìm chìa khóa trong "Biến môi trường" (Dành cho Render)
            String firebaseConfig = System.getenv("FIREBASE_CONFIG_JSON");
            
            FirebaseOptions options;
            
            if (firebaseConfig != null && !firebaseConfig.isEmpty()) {
                // Nếu có biến môi trường, dùng nó để khởi tạo (Ưu tiên cho Render)
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(firebaseConfig.getBytes())))
                        .setDatabaseUrl("https://parksync-e31a0-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .build();
                System.out.println("✅ Firebase: Khởi tạo thành công bằng BIẾN MÔI TRƯỜNG trên Render!");
            } else {
                // Nếu không có, tìm file service-account.json ở Local (Dành cho máy Huy)
                org.springframework.core.io.ClassPathResource resource = 
                        new org.springframework.core.io.ClassPathResource("service-account.json");
                
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                        .setDatabaseUrl("https://parksync-e31a0-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .build();
                System.out.println("🏠 Firebase: Khởi tạo thành công bằng FILE LOCAL trong resources!");
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            // Trỏ đúng vào node cha "parking_status"
            dbReference = FirebaseDatabase.getInstance().getReference("parking_status");
            System.out.println("🚀 Firebase DA SAN SANG!");

        } catch (Exception e) {
            System.err.println("❌ LOI CAU HINH Firebase: " + e.getMessage());
            System.err.println("👉 HUY ƠI: Nếu chạy trên Render, hãy nhớ add biến FIREBASE_CONFIG_JSON vào mục Environment nhé!");
        }
    }

    // Hàm Transaction để cộng/trừ số lượng xe
    public void updateSpotsOnWeb(int change) { 
        if (dbReference != null) {
            DatabaseReference spotsRef = dbReference.child("available_spots");

            spotsRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    Integer current = mutableData.getValue(Integer.class);
                    if (current == null) {
                        mutableData.setValue(100 + change);
                    } else {
                        mutableData.setValue(current + change);
                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (committed) {
                        System.out.println("🔥 [Firebase] Số chỗ trống hiện tại: " + snapshot.getValue());
                    } else {
                        System.err.println("❌ Lỗi Firebase: " + error.getMessage());
                    }
                }
            });
        }
    }

    public void updateGateStatus(String gateName, Map<String, Object> data) {
        if (dbReference != null) {
            dbReference.child("gates").child(gateName).setValueAsync(data);
        }
    }
}