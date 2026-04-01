package com.example.parking_cloud.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "history_logs") // Tên "bảng" trong MongoDB
public class ParkingLog {

    @Id
    private String id; // MongoDB tự sinh ID dạng chuỗi (ObjectId)
    private String bienSo;
    private String hanhDong; // VAO hoặc RA
    private String thoiGian;
    private String congXuly; // Ví dụ: GATE_HUY, GATE_VU...
public ParkingLog() {
    }
    // Constructor nhanh để dễ tạo đối tượng
    public ParkingLog(String bienSo, String hanhDong, String congXuly) {
        this.bienSo = bienSo;
        this.hanhDong = hanhDong;
        this.congXuly = congXuly;
        this.thoiGian = LocalDateTime.now().toString(); // Tự lấy giờ hiện tại
    }

    // --- GETTER / SETTER (Huy có thể dùng chuột phải -> Generate -> Getter and Setter) ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBienSo() { return bienSo; }
    public void setBienSo(String bienSo) { this.bienSo = bienSo; }
    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }
    public String getThoiGian() { return thoiGian; }
    public void setThoiGian(String thoiGian) { this.thoiGian = thoiGian; }
    public String getCongXuly() { return congXuly; }
    public void setCongXuly(String congXuly) { this.congXuly = congXuly; }
}