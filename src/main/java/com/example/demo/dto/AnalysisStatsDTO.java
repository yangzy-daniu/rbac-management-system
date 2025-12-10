package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AnalysisStatsDTO {
    // 关键指标
    private Integer totalUsers;
    private Integer activeUsers;
    private Integer sessionCount; // 会话次数（替换总订单数）
    private BigDecimal growthRate; // 增长率

    // 趋势数据
    private BigDecimal userGrowth;
    private BigDecimal activeGrowth;
    private BigDecimal sessionGrowth;
    private BigDecimal rateGrowth;

    // 活跃数据
    private Integer todayLogins;
    private Integer weekActiveUsers;
    private BigDecimal avgSessionTime;
}