package com.example.demo.controller;

import com.example.demo.service.AuthService;
import com.example.demo.service.StatsService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    private final UserService userService;

    private final AuthService authService;

    @GetMapping("/dashboard-stats")
    public Map<String, Object> getDashboardStats(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);

        Long todayAccess = userService.getTodayAccessCount(userId);
        Long monthOperations = userService.getMonthOperationCount(userId);
        Double completionRate = userService.getOperationSuccessRate(userId); // 使用方案一
        List<Map<String, Object>> recentActivities = userService.getRecentActivities(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayAccess", todayAccess);
        stats.put("monthOperations", monthOperations);
        stats.put("completionRate", Math.round(completionRate * 100) / 100.0); // 保留两位小数
        stats.put("recentActivities", recentActivities);

        return stats;
    }

    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = statsService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
}