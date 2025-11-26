package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_access_log")
public class UserAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String accessPath;

    @Column(nullable = false)
    private String accessMethod;

    @Column(nullable = false)
    private LocalDateTime accessTime;

    private String ipAddress;
    private String userAgent;

    @Column(nullable = false)
    private Boolean success = true;

    @PrePersist
    public void prePersist() {
        if (accessTime == null) {
            accessTime = LocalDateTime.now();
        }
    }
}