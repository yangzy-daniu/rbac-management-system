package com.example.demo.service;

import com.example.demo.entity.SystemInfo;
import com.example.demo.repository.SystemInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class DynamicSystemInfoService {

    private final VersionService versionService;
    private final SystemHealthService healthService;
    private final SystemInfoRepository systemInfoRepository;

    public DynamicSystemInfoService(VersionService versionService,
                                    SystemHealthService healthService,
                                    SystemInfoRepository systemInfoRepository) {
        this.versionService = versionService;
        this.healthService = healthService;
        this.systemInfoRepository = systemInfoRepository;
    }

    /**
     * 获取实时系统信息（不保存到数据库）
     */
    public SystemInfo getRealTimeSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        // 基础信息（从 VersionService 获取）
        systemInfo.setSystemVersion(versionService.getVersion());
        systemInfo.setCreateTime(versionService.getBuildDateTime());

        // 实时信息
        systemInfo.setLastUpdateDate(LocalDateTime.now());
        systemInfo.setUpdateTime(LocalDateTime.now());

        // 实时状态检测
        SystemHealthService.SystemStatus status = healthService.checkRealTimeStatus();
        systemInfo.setSystemStatus(status.name());

//        // 动态描述信息
//        String description = String.format("RBACK限管理系统 - 负载: %s, 内存: %s",
//                healthService.getSystemLoad(), healthService.getMemoryInfo());
//        systemInfo.setDescription(description);

        systemInfo.setUpdateUser("系统监控");

        return systemInfo;
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
                existing.setLastUpdateDate(realTimeInfo.getLastUpdateDate());
                existing.setUpdateTime(realTimeInfo.getUpdateTime());
//                existing.setDescription(realTimeInfo.getDescription());
                existing.setUpdateUser(realTimeInfo.getUpdateUser());

                systemInfoRepository.save(existing);
                log.debug("系统状态已更新: {}", existing.getSystemStatus());
            }
        } catch (Exception e) {
            log.error("更新系统状态失败", e);
        }
    }
}