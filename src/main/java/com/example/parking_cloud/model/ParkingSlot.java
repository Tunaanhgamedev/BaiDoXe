package com.example.parking_cloud.model;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_slots")
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slotName;
    private boolean isOccupied;

    // 1. Constructor mặc định (Bắt buộc phải có để JPA hoạt động)
    public ParkingSlot() {
    }

    // 2. Các hàm Getter và Setter (Phải nằm NGOÀI constructor trên)
    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getSlotName() { 
        return slotName; 
    }
    public void setSlotName(String slotName) { 
        this.slotName = slotName; 
    }

    public boolean isOccupied() { 
        return isOccupied; 
    }
    public void setOccupied(boolean occupied) { 
        this.isOccupied = occupied; 
    }
}