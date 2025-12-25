package com.example.demo.controller;

import com.example.demo.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system-monitor")
@RequiredArgsConstructor
public class SystemMonitorController {

    private final SystemMonitorService systemMonitorService;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = systemMonitorService.getSystemInfo();
        return ResponseEntity.ok(systemInfo);
    }

    // 获取系统监控统计数据
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = systemMonitorService.getSystemStats();
        return ResponseEntity.ok(stats);
    }

    // 获取资源使用趋势数据
    @GetMapping("/resource-trend")
    public ResponseEntity<Map<String, Object>> getResourceTrend(
            @RequestParam String resourceType,
            @RequestParam String timeRange) {
        Map<String, Object> trendData = systemMonitorService.getResourceTrend(resourceType, timeRange);
        return ResponseEntity.ok(trendData);
    }
}