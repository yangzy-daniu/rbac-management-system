package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sales_count")
    private Integer salesCount = 0; // 总销量

    @Column(name = "monthly_sales")
    private Integer monthlySales = 0; // 月销量

    @Column(name = "growth_rate", precision = 5, scale = 2)
    private BigDecimal growthRate = BigDecimal.ZERO; // 增长率

    @Column(name = "rank_position")
    private Integer rankPosition; // 排名

    @Column(name = "status", length = 20)
    private String status = "active"; // active, inactive

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}