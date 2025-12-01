package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "system_log")
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 20)
    private String level; // INFO, WARNING, ERROR, CRITICAL

    @Column(nullable = false, length = 100)
    private String service; // 服务名称

    @Column(nullable = false, length = 500)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String details; // 详细错误信息或堆栈跟踪

    @Column(length = 50)
    private String module; // 模块名称

    @Column(length = 100)
    private String operation; // 操作类型

    @Column(length = 50)
    private String ipAddress; // 触发IP

    @Column(length = 100)
    private String username; // 触发用户

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        createTime = LocalDateTime.now();
    }
}