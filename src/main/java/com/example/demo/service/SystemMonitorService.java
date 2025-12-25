package com.example.demo.service;

import com.example.demo.entity.OnlineUser;
import com.example.demo.entity.SystemInfo;
import com.example.demo.repository.OnlineUserRepository;
import com.example.demo.repository.SystemInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMonitorService {

    private final SystemInfoRepository systemInfoRepository;
    private final OnlineUserRepository onlineUserRepository;
    private final SystemHealthService systemHealthService;
    private final VersionService versionService;
    private final SystemLogService systemLogService;

    // 系统启动时初始化系统信息
    @PostConstruct
    @Transactional
    public void initSystemInfo() {
        Optional<SystemInfo> existingInfo = systemInfoRepository.findFirstByOrderByIdAsc();

        if (existingInfo.isEmpty()) {
            // 只有第一次部署时创建
            SystemInfo systemInfo = new SystemInfo();
            systemInfo.setSystemVersion(versionService.getVersion());
            systemInfo.setLastUpdateDate(versionService.getBuildDateTime());
            systemInfo.setUpdateTime(LocalDateTime.now());
            systemInfo.setSystemStatus("RUNNING");
            systemInfo.setDescription("RBAC权限管理系统");
            systemInfo.setUpdateUser("系统初始化");
            systemInfoRepository.save(systemInfo);
            log.info("系统信息初始化完成，版本: {}", systemInfo.getSystemVersion());
        } else {
            // 后续启动只更新版本信息（如果版本变化）
            SystemInfo systemInfo = existingInfo.get();
            String currentVersion = versionService.getVersion();

            if (!currentVersion.equals(systemInfo.getSystemVersion())) {
                systemInfo.setSystemVersion(currentVersion);
                systemInfo.setLastUpdateDate(versionService.getBuildDateTime());
                systemInfo.setUpdateUser("版本更新");
                systemInfoRepository.save(systemInfo);
                log.info("系统版本更新: {} -> {}", systemInfo.getSystemVersion(), currentVersion);
            }
        }
    }

    /**
     * 更新数据库中的系统状态（定时任务调用）
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void updateSystemStatus() {
        try {
            SystemInfo realTimeInfo = getRealTimeSystemInfo();

            Optional<SystemInfo> existingOpt = systemInfoRepository.findById(1L);
            if (existingOpt.isPresent()) {
                SystemInfo existing = existingOpt.get();
                // 只更新状态相关字段，不覆盖版本等基础信息
                existing.setSystemStatus(realTimeInfo.getSystemStatus());
                existing.setUpdateTime(LocalDateTime.now());
                existing.setDescription(realTimeInfo.getDescription());
                existing.setUpdateUser("系统监控");

                systemInfoRepository.save(existing);
                log.debug("系统状态已更新: {}", existing.getSystemStatus());
            }
        } catch (Exception e) {
            log.error("更新系统状态失败", e);
        }
    }

    /**
     * 获取实时系统信息（不保存到数据库）
     */
    public SystemInfo getRealTimeSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        // 基础信息（从 VersionService 获取）
        systemInfo.setSystemVersion(versionService.getVersion());
        systemInfo.setCreateTime(versionService.getBuildDateTime());

        // 实时状态检测
        SystemHealthService.SystemStatus status = systemHealthService.checkRealTimeStatus();
        systemInfo.setSystemStatus(status.name());

        // 动态描述信息
        String description = String.format("RBACK限管理系统 - 负载: %s, 内存: %s",
                systemHealthService.getSystemLoad(), systemHealthService.getMemoryInfo());
        systemInfo.setDescription(description);


        return systemInfo;
    }

    // 获取系统信息
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // 获取系统配置信息
        SystemInfo systemInfo = systemInfoRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("系统信息未初始化"));
//                .orElseGet(this::createDefaultSystemInfo);

        info.put("systemVersion", systemInfo.getSystemVersion());
        info.put("lastUpdate", systemInfo.getLastUpdateDate().toLocalDate().toString());
        info.put("systemStatus", systemInfo.getSystemStatus());
        info.put("onlineUsers", getOnlineUserCount());
        info.put("serverTime", LocalDateTime.now());

        return info;
    }

    // 获取在线用户数量
    public Integer getOnlineUserCount() {
        return (int) onlineUserRepository.countByIsActiveTrue();
    }

    // 用户登录时记录在线用户
    @Transactional
    public void userLogin(Long userId, String username, String sessionId, String ipAddress, String userAgent) {
        try {
            // 先清理该用户可能存在的旧会话（避免重复登录）
            onlineUserRepository.findByUserId(userId).ifPresent(onlineUser -> {
                onlineUserRepository.delete(onlineUser);
                log.info("清理用户 {} 的旧会话", username);
            });

            OnlineUser onlineUser = new OnlineUser();
            onlineUser.setSessionId(sessionId);
            onlineUser.setUserId(userId);
            onlineUser.setUsername(username);
            onlineUser.setIpAddress(ipAddress);
            onlineUser.setUserAgent(userAgent);
            onlineUser.setIsActive(true);
            onlineUser.setLastAccessTime(LocalDateTime.now()); // 设置初始访问时间

            onlineUserRepository.save(onlineUser);
            log.info("用户 {} 登录系统，IP: {}", username, ipAddress);
            systemLogService.logInfo("用户服务",
                    String.format("用户 %s 登录系统", username));
        } catch (Exception e) {
            log.error("记录在线用户失败: {}", e.getMessage());
        }
    }

    // 用户退出时移除在线用户
    @Transactional
    public void userLogout(String sessionId) {
        try {
            // 通过sessionId查找
            Optional<OnlineUser> onlineUserOpt = onlineUserRepository.findById(sessionId);
            OnlineUser onlineUser = onlineUserOpt.get();
            if (onlineUserOpt.isPresent()) {
                onlineUserRepository.delete(onlineUser);
                log.info("用户 {} 通过会话退出系统，会话ID: {}", onlineUser.getUsername(), sessionId);
            } else {
                log.warn("未找到会话ID对应的在线用户: {}", sessionId);
                // 通过当前请求的用户信息来清理
                cleanupByCurrentUser();
            }
            systemLogService.logInfo("用户服务",
                    String.format("用户 %s 退出系统", onlineUser.getUsername()));
        } catch (Exception e) {
            log.error("移除在线用户失败: {}", e.getMessage());
        }
    }

    // 通过当前认证用户清理在线记录
    private void cleanupByCurrentUser() {
        try {
            // 获取当前认证的用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();

                // 通过用户名查找并清理
                List<OnlineUser> userSessions = onlineUserRepository.findByUsername(username);
                if (!userSessions.isEmpty()) {
                    onlineUserRepository.deleteAll(userSessions);
                    log.info("通过用户认证信息清理了用户 {} 的 {} 个会话", username, userSessions.size());
                }
            }
        } catch (Exception e) {
            log.error("通过当前用户信息清理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据会话ID获取在线用户
     */
    public Optional<OnlineUser> getOnlineUserBySessionId(String sessionId) {
        return onlineUserRepository.findById(sessionId);
    }

    /**
     * 获取所有在线用户列表
     */
    public List<OnlineUser> getAllOnlineUsers() {
        return onlineUserRepository.findByIsActiveTrue();
    }

    // 更新用户最后访问时间
    @Transactional
    public void updateUserAccessTime(String sessionId) {
        onlineUserRepository.updateLastAccessTime(sessionId, LocalDateTime.now());
    }

    // 定时清理过期会话（每5分钟执行一次）
    @Scheduled(fixedRate = 300000) // 5分钟
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(30); // 30分钟无活动视为过期
        int deletedCount = onlineUserRepository.deleteByLastAccessTimeBefore(expireTime);
        if (deletedCount > 0) {
            systemLogService.logInfo("会话管理",
                    String.format("清理了 %d 个过期会话", deletedCount));
            log.info("清理了 {} 个过期会话", deletedCount);
        }
    }

    // 更新系统信息
    @Transactional
    public void updateSystemInfo(String version, String status, String description, String updateUser) {
        SystemInfo systemInfo = systemInfoRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("系统信息未初始化"));

        if (version != null) systemInfo.setSystemVersion(version);
        if (status != null) systemInfo.setSystemStatus(status);
        if (description != null) systemInfo.setDescription(description);
        if (updateUser != null) systemInfo.setUpdateUser(updateUser);

        systemInfo.setLastUpdateDate(LocalDateTime.now());
        systemInfoRepository.save(systemInfo);
    }

    /**
     * 获取系统监控统计数据
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 获取CPU使用率（模拟真实数据）
            double cpuUsage = getCpuUsage();
            stats.put("cpuUsage", Math.round(cpuUsage * 10.0) / 10.0); // 保留一位小数

            // 获取内存使用率
            double memoryUsage = getMemoryUsage();
            stats.put("memoryUsage", Math.round(memoryUsage * 10.0) / 10.0);

            // 获取磁盘使用率
            double diskUsage = getDiskUsage();
            stats.put("diskUsage", Math.round(diskUsage * 10.0) / 10.0);

            // 获取网络流量
            double networkTraffic = getNetworkTraffic();
            stats.put("networkTraffic", Math.round(networkTraffic));

            // 获取响应时间
            double responseTime = getResponseTime();
            stats.put("responseTime", Math.round(responseTime));

            // 获取吞吐量
            double throughput = getThroughput();
            stats.put("throughput", Math.round(throughput));

            // 获取成功率
            double successRate = getSuccessRate();
            stats.put("successRate", Math.round(successRate * 10.0) / 10.0);

            // 获取在线用户数
            stats.put("onlineUsers", getOnlineUserCount());

            // 获取系统状态
            SystemHealthService.SystemStatus status = systemHealthService.checkRealTimeStatus();
            stats.put("systemStatus", status.name());

            // 获取系统负载
            stats.put("systemLoad", systemHealthService.getSystemLoad());

        } catch (Exception e) {
            log.error("获取系统统计信息失败", e);
            // 返回默认值
            stats.put("cpuUsage", 0);
            stats.put("memoryUsage", 0);
            stats.put("diskUsage", 0);
            stats.put("networkTraffic", 0);
            stats.put("responseTime", 0);
            stats.put("throughput", 0);
            stats.put("successRate", 100);
            stats.put("systemStatus", "ERROR");
        }

        return stats;
    }

    /**
     * 获取资源使用趋势数据
     */
    public Map<String, Object> getResourceTrend(String resourceType, String timeRange) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 根据时间范围确定数据点数
            int dataPoints = getDataPointsByTimeRange(timeRange);
            List<String> timestamps = generateTimestamps(timeRange, dataPoints);
            List<Double> data = new ArrayList<>();

            // 根据资源类型和时间范围生成数据
            switch (resourceType.toLowerCase()) {
                case "cpu":
                    data = generateCPUData(timeRange, dataPoints);
                    break;
                case "memory":
                    data = generateMemoryData(timeRange, dataPoints);
                    break;
                case "disk":
                    data = generateDiskData(timeRange, dataPoints);
                    break;
                default:
                    data = generateCPUData(timeRange, dataPoints);
            }

            result.put("timestamps", timestamps);
            result.put("data", data);
            result.put("resourceType", resourceType);
            result.put("unit", "%");

        } catch (Exception e) {
            log.error("获取资源趋势数据失败", e);
            result.put("timestamps", new ArrayList<>());
            result.put("data", new ArrayList<>());
        }

        return result;
    }

    // 辅助方法 - 获取CPU使用率（模拟真实数据）
    private double getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osBean.getCpuLoad() * 100;
        } catch (Exception e) {
            // 如果无法获取真实数据，返回模拟数据（在20-80之间波动）
            return 20 + Math.random() * 60;
        }
    }

    // 辅助方法 - 获取内存使用率
    private double getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            return (double) usedMemory / maxMemory * 100;
        } catch (Exception e) {
            // 模拟数据（在40-90之间波动）
            return 40 + Math.random() * 50;
        }
    }

    // 辅助方法 - 获取磁盘使用率
    private double getDiskUsage() {
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            return (double) usedSpace / totalSpace * 100;
        } catch (Exception e) {
            // 模拟数据（在50-95之间波动）
            return 50 + Math.random() * 45;
        }
    }

    // 辅助方法 - 获取网络流量
    private double getNetworkTraffic() {
        // 这里可以集成实际的网络监控工具
        // 暂时返回模拟数据（在100-2000之间波动）
        return 100 + Math.random() * 1900;
    }

    // 辅助方法 - 获取响应时间
    private double getResponseTime() {
        // 模拟响应时间（在50-300ms之间波动）
        return 50 + Math.random() * 250;
    }

    // 辅助方法 - 获取吞吐量
    private double getThroughput() {
        // 模拟吞吐量（在500-2000之间波动）
        return 500 + Math.random() * 1500;
    }

    // 辅助方法 - 获取成功率
    private double getSuccessRate() {
        // 模拟成功率（在98-100之间波动）
        return 98 + Math.random() * 2;
    }

    // 辅助方法 - 根据时间范围确定数据点数
    private int getDataPointsByTimeRange(String timeRange) {
        switch (timeRange) {
            case "1h": return 12;  // 5分钟一个点
            case "6h": return 12;  // 30分钟一个点
            case "24h": return 24; // 1小时一个点
            default: return 12;
        }
    }

    // 辅助方法 - 生成时间戳
    private List<String> generateTimestamps(String timeRange, int dataPoints) {
        List<String> timestamps = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = dataPoints - 1; i >= 0; i--) {
            LocalDateTime timePoint = now;

            switch (timeRange) {
                case "1h":
                    timePoint = now.minusMinutes(i * 5);
                    timestamps.add(timePoint.format(DateTimeFormatter.ofPattern("HH:mm")));
                    break;
                case "6h":
                    timePoint = now.minusMinutes(i * 30);
                    timestamps.add(timePoint.format(DateTimeFormatter.ofPattern("HH:mm")));
                    break;
                case "24h":
                    timePoint = now.minusHours(i);
                    timestamps.add(timePoint.format(DateTimeFormatter.ofPattern("HH:00")));
                    break;
            }
        }

        return timestamps;
    }

    // 辅助方法 - 生成CPU数据
    private List<Double> generateCPUData(String timeRange, int dataPoints) {
        List<Double> data = new ArrayList<>();
        double baseValue = 20 + Math.random() * 40; // 基础值在20-60之间

        for (int i = 0; i < dataPoints; i++) {
            // 根据时间范围调整波动幅度
            double fluctuation = 0;
            switch (timeRange) {
                case "1h": fluctuation = Math.random() * 15 - 7.5; break; // 波动范围±7.5
                case "6h": fluctuation = Math.random() * 20 - 10; break;  // 波动范围±10
                case "24h": fluctuation = Math.random() * 25 - 12.5; break; // 波动范围±12.5
            }

            double value = baseValue + fluctuation;
            // 确保值在合理范围内
            value = Math.max(0, Math.min(100, value));
            data.add(Math.round(value * 10.0) / 10.0);
        }

        return data;
    }

    // 辅助方法 - 生成内存数据
    private List<Double> generateMemoryData(String timeRange, int dataPoints) {
        List<Double> data = new ArrayList<>();
        double baseValue = 40 + Math.random() * 30; // 基础值在40-70之间

        for (int i = 0; i < dataPoints; i++) {
            double fluctuation = 0;
            switch (timeRange) {
                case "1h": fluctuation = Math.random() * 10 - 5; break;
                case "6h": fluctuation = Math.random() * 15 - 7.5; break;
                case "24h": fluctuation = Math.random() * 20 - 10; break;
            }

            double value = baseValue + fluctuation;
            value = Math.max(0, Math.min(100, value));
            data.add(Math.round(value * 10.0) / 10.0);
        }

        return data;
    }

    // 辅助方法 - 生成磁盘数据
    private List<Double> generateDiskData(String timeRange, int dataPoints) {
        List<Double> data = new ArrayList<>();
        double baseValue = 50 + Math.random() * 30; // 基础值在50-80之间

        for (int i = 0; i < dataPoints; i++) {
            double fluctuation = 0;
            switch (timeRange) {
                case "1h": fluctuation = Math.random() * 5 - 2.5; break;  // 磁盘使用率变化较慢
                case "6h": fluctuation = Math.random() * 8 - 4; break;
                case "24h": fluctuation = Math.random() * 10 - 5; break;
            }

            double value = baseValue + fluctuation;
            value = Math.max(0, Math.min(100, value));
            data.add(Math.round(value * 10.0) / 10.0);
        }

        return data;
    }
}