package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_info")
@Data
public class SystemInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_version", nullable = false, length = 50)
    private String systemVersion;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @Column(name = "system_status", length = 20)
    private String systemStatus = "RUNNING"; // RUNNING, STOPPED, MAINTENANCE

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "update_user", length = 100)
    private String updateUser;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (lastUpdateDate == null) {
            lastUpdateDate = LocalDateTime.of(2023, 10, 15, 0, 0);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}