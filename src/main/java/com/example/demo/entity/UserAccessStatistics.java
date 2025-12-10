package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "user_access_statistics")
public class UserAccessStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "visit_count")
    private Integer visitCount = 0;

    @Column(name = "user_count")
    private Integer userCount = 0;

    @Column(name = "avg_response_time", precision = 8, scale = 2)
    private BigDecimal avgResponseTime = BigDecimal.ZERO;

    @Column(name = "session_time_total")
    private Integer sessionTimeTotal = 0;

    @Column(name = "active_users")
    private Integer activeUsers = 0;

    @Column(name = "new_users")
    private Integer newUsers = 0;

    @Column(name = "retention_rate_1", precision = 5, scale = 2)
    private BigDecimal retentionRate1 = BigDecimal.ZERO;

    @Column(name = "retention_rate_7", precision = 5, scale = 2)
    private BigDecimal retentionRate7 = BigDecimal.ZERO;

    @Column(name = "retention_rate_30", precision = 5, scale = 2)
    private BigDecimal retentionRate30 = BigDecimal.ZERO;

    @Column(name = "created_time")
    private java.time.LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        if (createdTime == null) {
            createdTime = java.time.LocalDateTime.now();
        }
    }
}