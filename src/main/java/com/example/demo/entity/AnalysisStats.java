package com.example.demo.entity;

import lombok.Data;

@Data
public class AnalysisStats {
    private Long totalUsers;
    private Long totalRoles;
    private Long totalMenus;
    private Long todayLogins;
    private Long weekActiveUsers;
    private Double growthRate;

    public AnalysisStats() {
        this.totalUsers = 0L;
        this.totalRoles = 0L;
        this.totalMenus = 0L;
        this.todayLogins = 0L;
        this.weekActiveUsers = 0L;
        this.growthRate = 0.0;
    }
}