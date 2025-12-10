package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TrendDataDTO {
    private String date; // 格式化的日期
    private LocalDate statDate;
    private Integer visitCount;
    private Integer userCount;
    private BigDecimal avgResponseTime;

    // 构造函数
    public TrendDataDTO(LocalDate statDate, Integer visitCount, Integer userCount, BigDecimal avgResponseTime) {
        this.statDate = statDate;
        this.date = formatDate(statDate);
        this.visitCount = visitCount != null ? visitCount : 0;
        this.userCount = userCount != null ? userCount : 0;
        this.avgResponseTime = avgResponseTime != null ? avgResponseTime : BigDecimal.ZERO;
    }

    private String formatDate(LocalDate date) {
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }
}