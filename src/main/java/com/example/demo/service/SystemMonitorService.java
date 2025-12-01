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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMonitorService {

    private final SystemInfoRepository systemInfoRepository;
    private final OnlineUserRepository onlineUserRepository;
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
        int deletedCount = onlineUserRepository.deleteExpiredSessions(expireTime);
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

}