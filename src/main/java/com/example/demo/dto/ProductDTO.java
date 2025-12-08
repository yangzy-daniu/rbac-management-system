package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer salesCount; // 总销量
    private Integer monthlySales; // 月销量
    private BigDecimal growthRate; // 增长率
    private Integer rankPosition; // 排名
    private String status;
    private String imageUrl;
}