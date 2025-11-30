package com.example.demo.service;

import com.example.demo.repository.SystemInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@Service
@Slf4j
public class SystemHealthService {

    private final VersionService versionService;
    private final SystemInfoRepository systemInfoRepository;
    private final ApplicationContext applicationContext;

    // 状态枚举
    public enum SystemStatus {
        STARTING, RUNNING, DEGRADED, STOPPING, ERROR
    }

    public SystemHealthService(VersionService versionService,
                               SystemInfoRepository systemInfoRepository,
                               ApplicationContext applicationContext) {
        this.versionService = versionService;
        this.systemInfoRepository = systemInfoRepository;
        this.applicationContext = applicationContext;
    }

    /**
     * 实时检测系统状态
     */
    public SystemStatus checkRealTimeStatus() {
        try {
            // 1. 检查数据库连接
            if (!checkDatabaseHealth()) {
                return SystemStatus.DEGRADED;
            }

            // 2. 检查内存使用率
            if (!checkMemoryHealth()) {
                return SystemStatus.DEGRADED;
            }

            // 3. 检查关键服务是否可用
            if (!checkCriticalServices()) {
                return SystemStatus.ERROR;
            }

            return SystemStatus.RUNNING;

        } catch (Exception e) {
            log.error("系统状态检测异常", e);
            return SystemStatus.ERROR;
        }
    }

    /**
     * 检查数据库连接
     */
    private boolean checkDatabaseHealth() {
        try {
            // 尝试执行一个简单的查询
            systemInfoRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("数据库连接异常");
            return false;
        }
    }

    /**
     * 检查内存健康度
     */
    private boolean checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        // 如果内存使用率超过 90%，认为不健康
        double memoryUsage = (double) usedMemory / maxMemory;
        return memoryUsage < 0.9;
    }

    /**
     * 检查关键服务
     */
    private boolean checkCriticalServices() {
        try {
            // 检查必要的 Bean 是否正常
            String[] beanNames = applicationContext.getBeanDefinitionNames();
            return beanNames.length > 0; // 简单检查

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取系统负载信息
     */
    public String getSystemLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);
        double load = osBean.getSystemLoadAverage();
        return String.format("%.2f", load);
    }

    /**
     * 获取内存使用信息
     */
    public String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        return String.format("%dMB/%dMB", usedMemory, maxMemory);
    }
}