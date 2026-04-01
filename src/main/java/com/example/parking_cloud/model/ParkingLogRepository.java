package com.example.parking_cloud.model; // Hoặc .model tùy folder của Huy

// import com.example.parking_cloud.model.ParkingLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingLogRepository extends MongoRepository<ParkingLog, String> {
}