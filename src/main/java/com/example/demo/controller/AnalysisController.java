package com.example.demo.controller;

import com.example.demo.entity.AnalysisStats;
import com.example.demo.entity.UserAccessLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    @GetMapping("/stats")
    public ResponseEntity<AnalysisStats> getSystemStats() {
        log.info("获取系统统计信息");

        AnalysisStats stats = new AnalysisStats();
        // 模拟数据 - 实际项目中应该从数据库查询
        stats.setTotalUsers(1568L);
        stats.setTotalRoles(24L);
        stats.setTotalMenus(48L);
        stats.setTodayLogins(342L);
        stats.setWeekActiveUsers(892L);
        stats.setGrowthRate(12.5);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/access-trend")
    public ResponseEntity<Map<String, Object>> getAccessTrend(
            @RequestParam(defaultValue = "7") int days) {
        log.info("获取访问趋势数据，天数: {}", days);

        Map<String, Object> result = new HashMap<>();

        // 生成日期标签
        List<String> dates = new ArrayList<>();
        List<Long> accessCounts = new ArrayList<>();
        List<Long> userCounts = new ArrayList<>();

        Random random = new Random();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.toString());
            accessCounts.add(200L + random.nextLong(100));
            userCounts.add(150L + random.nextLong(50));
        }

        result.put("dates", dates);
        result.put("accessCounts", accessCounts);
        result.put("userCounts", userCounts);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/role-distribution")
    public ResponseEntity<List<Map<String, Object>>> getRoleDistribution() {
        log.info("获取角色分布数据");

        List<Map<String, Object>> distribution = Arrays.asList(
                createRoleData("系统管理员", 45, "#FF6B6B"),
                createRoleData("普通管理员", 120, "#4ECDC4"),
                createRoleData("操作员", 356, "#45B7D1"),
                createRoleData("观察员", 234, "#96CEB4"),
                createRoleData("审计员", 89, "#FECA57")
        );

        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/recent-access")
    public ResponseEntity<List<UserAccessLog>> getRecentAccessLogs() {
        log.info("获取最近访问记录");

        List<UserAccessLog> logs = Arrays.asList(
                createAccessLog("admin", "/api/users", "GET", LocalDateTime.now().minusMinutes(5), true),
                createAccessLog("user1", "/api/profile", "PUT", LocalDateTime.now().minusMinutes(12), true),
                createAccessLog("admin", "/api/roles", "POST", LocalDateTime.now().minusMinutes(23), true),
                createAccessLog("auditor", "/api/logs", "GET", LocalDateTime.now().minusMinutes(45), true),
                createAccessLog("user2", "/api/dashboard", "GET", LocalDateTime.now().minusMinutes(67), true)
        );

        return ResponseEntity.ok(logs);
    }

    private Map<String, Object> createRoleData(String name, int count, String color) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("value", count);
        data.put("color", color);
        return data;
    }

    private UserAccessLog createAccessLog(String username, String path, String method,
                                          LocalDateTime time, boolean success) {
        UserAccessLog log = new UserAccessLog();
        log.setUsername(username);
        log.setAccessPath(path);
        log.setAccessMethod(method);
        log.setAccessTime(time);
        log.setIpAddress("192.168.1." + new Random().nextInt(255));
        log.setUserAgent("Mozilla/5.0...");
        log.setSuccess(success);
        return log;
    }
}