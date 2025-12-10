package com.example.demo.controller;

import com.example.demo.dto.AnalysisStatsDTO;
import com.example.demo.dto.BehaviorDataDTO;
import com.example.demo.dto.RetentionDataDTO;
import com.example.demo.dto.TrendDataDTO;
import com.example.demo.service.AnalysisService;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserAnalysisService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final UserAnalysisService userAnalysisService;

    private final AnalysisService analysisService;

    private final UserService userService;

    private final AuthService authService;

    /**
     * 近期操作
     * @param token
     * @return
     */
    @GetMapping("/recent-actions")
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

    /**
     * 快速统计
     * @return
     */
    @GetMapping("/quick-stats")
    public ResponseEntity<Map<String, Object>> getQuickStats() {
        Map<String, Object> stats = analysisService.getQuickStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats")
    public ResponseEntity<AnalysisStatsDTO> getAnalysisStats(
            @RequestParam(defaultValue = "7d") String timeRange) {
        AnalysisStatsDTO stats = userAnalysisService.getAnalysisStats(timeRange);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendDataDTO>> getTrendData(
            @RequestParam(defaultValue = "7d") String timeRange) {
        List<TrendDataDTO> trendData = userAnalysisService.getTrendData(timeRange);
        return ResponseEntity.ok(trendData);
    }

    @GetMapping("/behavior")
    public ResponseEntity<List<BehaviorDataDTO>> getBehaviorData(
            @RequestParam(defaultValue = "7d") String timeRange) {
        List<BehaviorDataDTO> behaviorData = userAnalysisService.getBehaviorData(timeRange);
        return ResponseEntity.ok(behaviorData);
    }

    @GetMapping("/retention")
    public ResponseEntity<List<RetentionDataDTO>> getRetentionData(
            @RequestParam(defaultValue = "7d") String timeRange) {
        List<RetentionDataDTO> retentionData = userAnalysisService.getRetentionData(timeRange);
        return ResponseEntity.ok(retentionData);
    }
}