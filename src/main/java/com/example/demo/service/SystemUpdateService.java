//package com.example.demo.service;
//
//import com.example.demo.dto.CheckUpdateResponse;
//import com.example.demo.dto.SystemUpdateCreateDTO;
//import com.example.demo.dto.SystemUpdateDTO;
//import com.example.demo.entity.SystemUpdate;
//import com.example.demo.entity.User;
//import com.example.demo.repository.SystemUpdateRepository;
//import com.example.demo.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.ApplicationContext;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.DigestUtils;
//import org.springframework.util.StringUtils;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class SystemUpdateService {
//
//    private final SystemUpdateRepository systemUpdateRepository;
//    private final UserRepository userRepository;
//    private final SystemTodoService systemTodoService;
//    @Autowired
//    private ApplicationContext applicationContext;
//
//    @Value("${app.update.file-path:./updates}")
//    private String updateFilePath;
//
//    @Value("${app.version:1.0.0}")
//    private String currentSystemVersion;
//
//    /**
//     * 创建系统更新
//     */
//    @Transactional
//    public SystemUpdateDTO createSystemUpdate(SystemUpdateCreateDTO createDTO, Long userId) {
//        // 验证版本号格式
//        validateVersion(createDTO.getVersion());
//
//        // 检查版本是否已存在
//        if (systemUpdateRepository.findByVersion(createDTO.getVersion()).isPresent()) {
//            throw new RuntimeException("版本号 " + createDTO.getVersion() + " 已存在");
//        }
//
//        SystemUpdate systemUpdate = new SystemUpdate();
//        systemUpdate.setVersion(createDTO.getVersion());
//        systemUpdate.setTitle(createDTO.getTitle());
//        systemUpdate.setDescription(createDTO.getDescription());
//        systemUpdate.setReleaseNotes(createDTO.getReleaseNotes());
//        systemUpdate.setUpdateType(createDTO.getUpdateType());
//        systemUpdate.setForceUpdate(createDTO.getForceUpdate());
//        systemUpdate.setEffectiveTime(createDTO.getEffectiveTime());
//        systemUpdate.setStatus("DRAFT");
//        systemUpdate.setCreatedBy(userId);
//
//        // 处理上传的文件
//        if (createDTO.getUpdateFile() != null && !createDTO.getUpdateFile().isEmpty()) {
//            saveUpdateFile(systemUpdate, createDTO.getUpdateFile());
//        }
//
//        SystemUpdate savedUpdate = systemUpdateRepository.save(systemUpdate);
//
//        log.info("创建系统更新成功: {}", savedUpdate.getVersion());
//        return convertToDTO(savedUpdate);
//    }
//
//    /**
//     * 发布系统更新
//     */
//    @Transactional
//    public SystemUpdateDTO releaseSystemUpdate(Long updateId) {
//        SystemUpdate systemUpdate = systemUpdateRepository.findById(updateId)
//                .orElseThrow(() -> new RuntimeException("系统更新不存在"));
//
//        if ("RELEASED".equals(systemUpdate.getStatus())) {
//            throw new RuntimeException("该更新已发布");
//        }
//
//        systemUpdate.setStatus("RELEASED");
//        systemUpdate.setReleaseTime(LocalDateTime.now());
//
//        // 如果没有设置生效时间，默认为发布时间
//        if (systemUpdate.getEffectiveTime() == null) {
//            systemUpdate.setEffectiveTime(LocalDateTime.now());
//        }
//
//        SystemUpdate releasedUpdate = systemUpdateRepository.save(systemUpdate);
//
//        // 自动为管理员创建更新确认待办
//        try {
//            systemTodoService.createSystemUpdateTodo(
//                    releasedUpdate.getVersion(),
//                    releasedUpdate.getTitle()
//            );
//        } catch (Exception e) {
//            log.error("创建系统更新待办失败: {}", e.getMessage());
//        }
//
//        log.info("发布系统更新成功: {}", releasedUpdate.getVersion());
//        return convertToDTO(releasedUpdate);
//    }
//
//    /**
//     * 执行无痕更新
//     */
//    @Transactional
//    public SystemUpdateDTO performSeamlessUpdate(Long updateId) {
//        SystemUpdate systemUpdate = systemUpdateRepository.findById(updateId)
//                .orElseThrow(() -> new RuntimeException("系统更新不存在"));
//
//        if (!"RELEASED".equals(systemUpdate.getStatus())) {
//            throw new RuntimeException("只有已发布的更新才能执行");
//        }
//
//        if (systemUpdate.getEffectiveTime() != null &&
//                systemUpdate.getEffectiveTime().isAfter(LocalDateTime.now())) {
//            throw new RuntimeException("更新生效时间未到");
//        }
//
//        log.info("开始执行无痕更新: {}", systemUpdate.getVersion());
//
//        try {
//            // 1. 备份当前系统（在实际项目中这里会备份配置文件、数据库等）
//            backupCurrentSystem();
//
//            // 2. 应用更新文件（这里简化处理，实际项目需要根据文件类型处理）
//            if (StringUtils.hasText(systemUpdate.getFilePath())) {
//                applyUpdateFile(systemUpdate);
//            }
//
//            // 3. 更新系统配置
//            updateSystemConfiguration(systemUpdate);
//
//            // 4. 记录更新日志
//            logUpdateHistory(systemUpdate);
//
//            log.info("无痕更新执行完成: {}", systemUpdate.getVersion());
//
//            // 5. 发送更新完成通知
//            sendUpdateCompletionNotification(systemUpdate);
//
//            return convertToDTO(systemUpdate);
//
//        } catch (Exception e) {
//            log.error("执行无痕更新失败: {}", e.getMessage());
//
//            // 回滚更新
//            rollbackUpdate(systemUpdate);
//            throw new RuntimeException("更新执行失败，已回滚: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 检查更新
//     */
//    public CheckUpdateResponse checkForUpdates(String clientVersion) {
//        CheckUpdateResponse response = new CheckUpdateResponse();
//        response.setCurrentVersion(clientVersion);
//
//        try {
//            // 获取最新发布的更新
//            String latestVersion = systemUpdateRepository.findLatestVersion();
//
//            if (latestVersion == null) {
//                response.setHasUpdate(false);
//                response.setMessage("当前已是最新版本");
//                return response;
//            }
//
//            // 比较版本号
//            boolean hasUpdate = compareVersions(clientVersion, latestVersion) < 0;
//            response.setHasUpdate(hasUpdate);
//            response.setLatestVersion(latestVersion);
//
//            if (hasUpdate) {
//                // 获取最新更新的详细信息
//                SystemUpdate latestUpdate = systemUpdateRepository.findByVersion(latestVersion)
//                        .orElse(null);
//
//                if (latestUpdate != null && "RELEASED".equals(latestUpdate.getStatus())) {
//                    response.setLatestUpdate(convertToDTO(latestUpdate));
//                    response.setForceUpdate(latestUpdate.getForceUpdate());
//
//                    if (latestUpdate.getForceUpdate()) {
//                        response.setMessage("发现重要更新，请立即更新系统");
//                    } else {
//                        response.setMessage("发现新版本可用");
//                    }
//                }
//            } else {
//                response.setMessage("当前已是最新版本");
//            }
//
//        } catch (Exception e) {
//            log.error("检查更新失败: {}", e.getMessage());
//            response.setHasUpdate(false);
//            response.setMessage("检查更新失败: " + e.getMessage());
//        }
//
//        return response;
//    }
//
//    /**
//     * 获取更新历史
//     */
//    public List<SystemUpdateDTO> getUpdateHistory() {
//        List<SystemUpdate> updates = systemUpdateRepository.findByStatusOrderByReleaseTimeDesc("RELEASED");
//        return updates.stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * 获取待处理的更新
//     */
//    public List<SystemUpdateDTO> getPendingUpdates() {
//        LocalDateTime now = LocalDateTime.now();
//        List<SystemUpdate> updates = systemUpdateRepository.findEffectiveUpdates(now);
//        return updates.stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * 获取当前系统版本
//     */
//    public String getCurrentSystemVersion() {
//        // 从数据库获取最新已发布的版本，如果不存在则返回配置的版本
//        try {
//            String latestVersion = systemUpdateRepository.findLatestVersion();
//            if (latestVersion != null) {
//                return latestVersion;
//            }
//        } catch (Exception e) {
//            log.warn("获取最新版本失败，使用配置版本: {}", e.getMessage());
//        }
//        return currentSystemVersion;
//    }
//
//    /**
//     * 设置系统版本（更新配置）
//     */
//    public void setSystemVersion(String version) {
//        // 验证版本号格式
//        validateVersion(version);
//
//        // 更新内存中的版本
//        this.currentSystemVersion = version;
//
//        // 也可以更新配置文件或数据库中的版本记录
//        updateVersionInConfig(version);
//
//        log.info("系统版本已更新为: {}", version);
//    }
//
//    /**
//     * 获取完整的版本信息
//     */
//    public Map<String, Object> getVersionInfo() {
//        Map<String, Object> versionInfo = new HashMap<>();
//
//        // 基础版本信息
//        versionInfo.put("version", getCurrentSystemVersion());
//        versionInfo.put("buildTime", getBuildTime());
//        versionInfo.put("buildNumber", getBuildNumber());
//        versionInfo.put("environment", getEnvironment());
//
//        // 系统信息
//        versionInfo.put("javaVersion", System.getProperty("java.version"));
//        versionInfo.put("osName", System.getProperty("os.name"));
//        versionInfo.put("osVersion", System.getProperty("os.version"));
//
//        // 应用信息
//        versionInfo.put("appName", getApplicationName());
//        versionInfo.put("startupTime", getStartupTime());
//
//        // 更新信息
//        versionInfo.put("lastUpdateTime", getLastUpdateTime());
//        versionInfo.put("updateCount", getUpdateCount());
//
//        return versionInfo;
//    }
//
//    // ========== 私有方法 ==========
//
//    private String getBuildTime() {
//        try {
//            // 从META-INF读取构建时间
//            ClassLoader classLoader = getClass().getClassLoader();
//            try (InputStream inputStream = classLoader.getResourceAsStream("META-INF/build-info.properties")) {
//                if (inputStream != null) {
//                    Properties props = new Properties();
//                    props.load(inputStream);
//                    return props.getProperty("build.time", "未知");
//                }
//            }
//        } catch (Exception e) {
//            log.debug("无法读取构建信息: {}", e.getMessage());
//        }
//        return "未知";
//    }
//
//    private String getBuildNumber() {
//        try {
//            Package pkg = getClass().getPackage();
//            if (pkg != null) {
//                String implementationVersion = pkg.getImplementationVersion();
//                if (implementationVersion != null) {
//                    return implementationVersion;
//                }
//            }
//        } catch (Exception e) {
//            log.debug("无法获取构建版本号: {}", e.getMessage());
//        }
//        return "1.0.0";
//    }
//
//    private String getEnvironment() {
//        // 这里可以根据Spring Profile判断环境
//        return System.getProperty("spring.profiles.active", "dev");
//    }
//
//    private String getApplicationName() {
//        try {
//            // 从Spring环境获取应用名称
//            Environment env = applicationContext.getEnvironment();
//            return env.getProperty("spring.application.name", "管理系统");
//        } catch (Exception e) {
//            return "管理系统";
//        }
//    }
//
//    private LocalDateTime getStartupTime() {
//        // 记录应用启动时间
//        return LocalDateTime.now(); // 简化处理
//    }
//
//    private LocalDateTime getLastUpdateTime() {
//        try {
//            // 从更新记录表中获取最新更新时间
//            List<SystemUpdate> updates = systemUpdateRepository.findByStatusOrderByReleaseTimeDesc("RELEASED");
//            if (!updates.isEmpty()) {
//                return updates.get(0).getReleaseTime();
//            }
//        } catch (Exception e) {
//            log.debug("获取最后更新时间失败: {}", e.getMessage());
//        }
//        return null;
//    }
//
//    private Long getUpdateCount() {
//        try {
//            return systemUpdateRepository.countByStatus("RELEASED");
//        } catch (Exception e) {
//            log.debug("获取更新次数失败: {}", e.getMessage());
//        }
//        return 0L;
//    }
//
//    private void updateVersionInConfig(String version) {
//        // 在实际项目中，这里可以：
//        // 1. 更新 application.properties/yml 文件
//        // 2. 更新数据库配置表
//        // 3. 发送版本更新事件
//        log.info("更新系统版本配置为: {}", version);
//
//        // 示例：保存到临时文件或数据库
//        try {
//            // 保存到数据库配置表（如果有的话）
//            // 或者保存到文件
//            Path versionFile = Paths.get("version.info");
//            String content = "version=" + version + "\n" +
//                    "updateTime=" + LocalDateTime.now() + "\n";
//            Files.write(versionFile, content.getBytes(), StandardOpenOption.CREATE);
//        } catch (Exception e) {
//            log.error("保存版本信息失败: {}", e.getMessage());
//        }
//    }
//
//    private void validateVersion(String version) {
//        // 简单的版本号验证：v1.0.0 或 1.0.0 格式
//        if (!version.matches("^v?\\d+(\\.\\d+){0,2}$")) {
//            throw new RuntimeException("版本号格式不正确，应为 v1.0.0 或 1.0.0 格式");
//        }
//    }
//
//    private void saveUpdateFile(SystemUpdate systemUpdate, MultipartFile file) {
//        try {
//            // 创建更新文件目录
//            Path uploadPath = Paths.get(updateFilePath);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            // 生成文件名：版本号_时间戳.扩展名
//            String timestamp = LocalDateTime.now()
//                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//            String originalFilename = file.getOriginalFilename();
//            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
//            String filename = systemUpdate.getVersion() + "_" + timestamp + extension;
//
//            Path filePath = uploadPath.resolve(filename);
//
//            // 保存文件
//            Files.copy(file.getInputStream(), filePath);
//
//            // 计算MD5
//            String md5 = DigestUtils.md5DigestAsHex(file.getBytes());
//
//            // 更新实体
//            systemUpdate.setFilePath(filePath.toString());
//            systemUpdate.setFileSize(file.getSize());
//            systemUpdate.setMd5Hash(md5);
//
//        } catch (IOException e) {
//            throw new RuntimeException("保存更新文件失败: " + e.getMessage());
//        }
//    }
//
//    private void backupCurrentSystem() {
//        // 在实际项目中，这里会：
//        // 1. 备份数据库
//        // 2. 备份配置文件
//        // 3. 备份上传的文件等
//        log.info("备份当前系统...");
//        // 实现略
//    }
//
//    private void applyUpdateFile(SystemUpdate systemUpdate) {
//        // 根据文件类型执行不同的更新操作
//        log.info("应用更新文件: {}", systemUpdate.getFilePath());
//        // 实现略
//    }
//
//    private void updateSystemConfiguration(SystemUpdate systemUpdate) {
//        // 更新系统配置
//        log.info("更新系统配置...");
//        // 实现略
//    }
//
//    private void logUpdateHistory(SystemUpdate systemUpdate) {
//        // 记录更新日志
//        log.info("记录更新历史: {}", systemUpdate.getVersion());
//        // 实现略
//    }
//
//    private void sendUpdateCompletionNotification(SystemUpdate systemUpdate) {
//        // 发送更新完成通知
//        log.info("发送更新完成通知: {}", systemUpdate.getVersion());
//        // 实现略
//    }
//
//    private void rollbackUpdate(SystemUpdate systemUpdate) {
//        // 回滚更新
//        log.info("回滚更新: {}", systemUpdate.getVersion());
//        // 实现略
//    }
//
//    /**
//     * 比较版本号大小
//     * @return -1: v1 < v2, 0: v1 = v2, 1: v1 > v2
//     */
//    private int compareVersions(String v1, String v2) {
//        // 去除开头的v
//        v1 = v1.startsWith("v") ? v1.substring(1) : v1;
//        v2 = v2.startsWith("v") ? v2.substring(1) : v2;
//
//        String[] parts1 = v1.split("\\.");
//        String[] parts2 = v2.split("\\.");
//
//        int maxLength = Math.max(parts1.length, parts2.length);
//
//        for (int i = 0; i < maxLength; i++) {
//            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
//            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
//
//            if (num1 < num2) return -1;
//            if (num1 > num2) return 1;
//        }
//
//        return 0;
//    }
//
//    private SystemUpdateDTO convertToDTO(SystemUpdate systemUpdate) {
//        SystemUpdateDTO dto = new SystemUpdateDTO();
//        dto.setId(systemUpdate.getId());
//        dto.setVersion(systemUpdate.getVersion());
//        dto.setTitle(systemUpdate.getTitle());
//        dto.setDescription(systemUpdate.getDescription());
//        dto.setReleaseNotes(systemUpdate.getReleaseNotes());
//        dto.setUpdateType(systemUpdate.getUpdateType());
//        dto.setUpdateTypeLabel(getUpdateTypeLabel(systemUpdate.getUpdateType()));
//        dto.setForceUpdate(systemUpdate.getForceUpdate());
//        dto.setFilePath(systemUpdate.getFilePath());
//        dto.setFileSize(systemUpdate.getFileSize());
//        dto.setFileSizeFormat(formatFileSize(systemUpdate.getFileSize()));
//        dto.setMd5Hash(systemUpdate.getMd5Hash());
//        dto.setReleaseTime(systemUpdate.getReleaseTime());
//        dto.setEffectiveTime(systemUpdate.getEffectiveTime());
//        dto.setStatus(systemUpdate.getStatus());
//        dto.setStatusLabel(getStatusLabel(systemUpdate.getStatus()));
//        dto.setCreateTime(systemUpdate.getCreateTime());
//        dto.setUpdateTime(systemUpdate.getUpdateTime());
//
//        // 设置创建人姓名
//        if (systemUpdate.getCreatedBy() != null) {
//            userRepository.findById(systemUpdate.getCreatedBy())
//                    .ifPresent(user -> dto.setCreatedByName(user.getName()));
//        }
//
//        return dto;
//    }
//
//    private String getUpdateTypeLabel(String updateType) {
//        return switch (updateType) {
//            case "BUG_FIX" -> "Bug修复";
//            case "FEATURE" -> "功能新增";
//            case "SECURITY" -> "安全更新";
//            case "OPTIMIZATION" -> "性能优化";
//            default -> updateType;
//        };
//    }
//
//    private String getStatusLabel(String status) {
//        return switch (status) {
//            case "DRAFT" -> "草稿";
//            case "RELEASED" -> "已发布";
//            case "ROLLED_BACK" -> "已回滚";
//            default -> status;
//        };
//    }
//
//    private String formatFileSize(Long size) {
//        if (size == null) return "0 B";
//
//        if (size < 1024) {
//            return size + " B";
//        } else if (size < 1024 * 1024) {
//            return String.format("%.1f KB", size / 1024.0);
//        } else if (size < 1024 * 1024 * 1024) {
//            return String.format("%.1f MB", size / (1024.0 * 1024.0));
//        } else {
//            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
//        }
//    }
//}