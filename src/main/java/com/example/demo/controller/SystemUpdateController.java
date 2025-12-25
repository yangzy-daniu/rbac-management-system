//package com.example.demo.controller;
//
//import com.example.demo.dto.CheckUpdateResponse;
//import com.example.demo.dto.SystemUpdateCreateDTO;
//import com.example.demo.dto.SystemUpdateDTO;
//import com.example.demo.entity.SystemUpdate;
//import com.example.demo.entity.User;
//import com.example.demo.repository.SystemUpdateRepository;
//import com.example.demo.repository.UserRepository;
//import com.example.demo.service.AuthService;
//import com.example.demo.service.SystemUpdateService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/system/updates")
//@RequiredArgsConstructor
//@Slf4j
//public class SystemUpdateController {
//
//    private final SystemUpdateService systemUpdateService;
//    private final AuthService authService;
//    private final SystemUpdateRepository systemUpdateRepository;
//    private final UserRepository userRepository;
//
//    /**
//     * 检查更新
//     */
//    @GetMapping("/check")
//    public ResponseEntity<CheckUpdateResponse> checkForUpdates(
//            @RequestParam(defaultValue = "1.0.0") String version) {
//        CheckUpdateResponse response = systemUpdateService.checkForUpdates(version);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 获取更新历史
//     */
//    @GetMapping("/history")
//    public ResponseEntity<List<SystemUpdateDTO>> getUpdateHistory() {
//        List<SystemUpdateDTO> history = systemUpdateService.getUpdateHistory();
//        return ResponseEntity.ok(history);
//    }
//
//    /**
//     * 获取待处理的更新
//     */
//    @GetMapping("/pending")
//    public ResponseEntity<List<SystemUpdateDTO>> getPendingUpdates() {
//        List<SystemUpdateDTO> updates = systemUpdateService.getPendingUpdates();
//        return ResponseEntity.ok(updates);
//    }
//
//    /**
//     * 创建系统更新（管理员）
//     */
//    @PostMapping
//    public ResponseEntity<?> createSystemUpdate(
//            @ModelAttribute SystemUpdateCreateDTO createDTO,
//            @RequestHeader("Authorization") String token) {
//        try {
//            String actualToken = token.replace("Bearer ", "");
//            Long userId = authService.getUserIdByToken(actualToken);
//
//            SystemUpdateDTO result = systemUpdateService.createSystemUpdate(createDTO, userId);
//
//            return ResponseEntity.ok()
//                    .body(Map.of(
//                            "success", true,
//                            "message", "创建系统更新成功",
//                            "data", result
//                    ));
//        } catch (Exception e) {
//            log.error("创建系统更新失败", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "success", false,
//                            "message", "创建失败: " + e.getMessage()
//                    ));
//        }
//    }
//
//    /**
//     * 发布更新（管理员）
//     */
//    @PutMapping("/{id}/release")
//    public ResponseEntity<?> releaseSystemUpdate(
//            @PathVariable Long id,
//            @RequestHeader("Authorization") String token) {
//        try {
//            SystemUpdateDTO result = systemUpdateService.releaseSystemUpdate(id);
//
//            return ResponseEntity.ok()
//                    .body(Map.of(
//                            "success", true,
//                            "message", "发布更新成功",
//                            "data", result
//                    ));
//        } catch (Exception e) {
//            log.error("发布更新失败", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "success", false,
//                            "message", "发布失败: " + e.getMessage()
//                    ));
//        }
//    }
//
//    /**
//     * 执行无痕更新（管理员）
//     */
//    @PutMapping("/{id}/apply")
//    public ResponseEntity<?> applySystemUpdate(
//            @PathVariable Long id,
//            @RequestHeader("Authorization") String token) {
//        try {
//            SystemUpdateDTO result = systemUpdateService.performSeamlessUpdate(id);
//
//            return ResponseEntity.ok()
//                    .body(Map.of(
//                            "success", true,
//                            "message", "更新执行成功",
//                            "data", result
//                    ));
//        } catch (Exception e) {
//            log.error("执行更新失败", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "success", false,
//                            "message", "执行失败: " + e.getMessage()
//                    ));
//        }
//    }
//
//    /**
//     * 获取系统当前版本
//     */
//    @GetMapping("/current-version")
//    public ResponseEntity<?> getCurrentVersion() {
//        String version = systemUpdateService.getCurrentSystemVersion();
//        return ResponseEntity.ok()
//                .body(Map.of(
//                        "success", true,
//                        "version", version,
//                        "timestamp", LocalDateTime.now()
//                ));
//    }
//
//    /**
//     * 获取详细的版本信息（包含构建信息、环境信息等）
//     */
//    @GetMapping("/version-info")
//    public ResponseEntity<?> getVersionInfo() {
//        try {
//            Map<String, Object> versionInfo = systemUpdateService.getVersionInfo();
//            return ResponseEntity.ok()
//                    .body(Map.of(
//                            "success", true,
//                            "data", versionInfo
//                    ));
//        } catch (Exception e) {
//            log.error("获取版本信息失败", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "success", false,
//                            "message", "获取版本信息失败: " + e.getMessage()
//                    ));
//        }
//    }
//
//    /**
//     * 更新系统版本（管理员权限）
//     */
//    @PutMapping("/version")
//    public ResponseEntity<?> updateVersion(
//            @RequestBody Map<String, String> request,
//            @RequestHeader("Authorization") String token) {
//        try {
//            String version = request.get("version");
//            if (!StringUtils.hasText(version)) {
//                return ResponseEntity.badRequest()
//                        .body(Map.of(
//                                "success", false,
//                                "message", "版本号不能为空"
//                        ));
//            }
//
//            // 验证权限（这里简化为管理员权限）
//            String actualToken = token.replace("Bearer ", "");
//            Long userId = authService.getUserIdByToken(actualToken);
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("用户不存在"));
//
//            if (!"admin".equals(user.getRoleCode()) && !"super".equals(user.getRoleCode())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of(
//                                "success", false,
//                                "message", "需要管理员权限"
//                        ));
//            }
//
//            systemUpdateService.setSystemVersion(version);
//
//            return ResponseEntity.ok()
//                    .body(Map.of(
//                            "success", true,
//                            "message", "系统版本更新成功",
//                            "version", version
//                    ));
//        } catch (Exception e) {
//            log.error("更新系统版本失败", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "success", false,
//                            "message", "更新失败: " + e.getMessage()
//                    ));
//        }
//    }
//
//    /**
//     * 获取版本更新历史（按时间排序）
//     */
//    @GetMapping("/version-history")
//    public ResponseEntity<?> getVersionHistory(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        try {
//            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "releaseTime"));
//            Page<SystemUpdate> updatePage = systemUpdateRepository.findByStatus("RELEASED", pageable);
//
//            List<SystemUpdateDTO> history = updatePage.getContent().stream()
//                    .map(this::convertToDTO)
//                    .collect(Collectors.toList());
//
//            return ResponseEntity.ok()
//                    .body(Map.of(
//                            "success", true,
//                            "data", history,
//                            "total", updatePage.getTotalElements(),
//                            "page", page,
//                            "size", size
//                    ));
//        } catch (Exception e) {
//            log.error("获取版本历史失败", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "success", false,
//                            "message", "获取失败: " + e.getMessage()
//                    ));
//        }
//    }
//
//    private SystemUpdateDTO convertToDTO(SystemUpdate entity) {
//        if (entity == null) {
//            return null;
//        }
//
//        SystemUpdateDTO dto = new SystemUpdateDTO();
//
//        // 基本字段映射
//        dto.setId(entity.getId());
//        dto.setVersion(entity.getVersion());
//        dto.setTitle(entity.getTitle());
//        dto.setDescription(entity.getDescription());
//        dto.setReleaseNotes(entity.getReleaseNotes());
//        dto.setUpdateType(entity.getUpdateType());
//        dto.setForceUpdate(entity.getForceUpdate());
//        dto.setFilePath(entity.getFilePath());
//        dto.setFileSize(entity.getFileSize());
//        dto.setMd5Hash(entity.getMd5Hash());
//        dto.setReleaseTime(entity.getReleaseTime());
//        dto.setEffectiveTime(entity.getEffectiveTime());
//        dto.setStatus(entity.getStatus());
//        dto.setCreateTime(entity.getCreateTime());
//        dto.setUpdateTime(entity.getUpdateTime());
//
//        // 处理关联的用户信息（创建人）
//        if (entity.getCreatedBy() != null) {
//            dto.setCreatedByName(String.valueOf(entity.getCreatedBy())); // 或其他名称字段
//        }
//
//        // 格式化文件大小
//        if (entity.getFileSize() != null) {
//            dto.setFileSizeFormat(formatFileSize(entity.getFileSize()));
//        }
//
//        // 处理枚举标签（如果需要）
//        dto.setUpdateTypeLabel(getUpdateTypeLabel(entity.getUpdateType()));
//        dto.setStatusLabel(getStatusLabel(entity.getStatus()));
//
//        // 计算是否为新版本（根据当前系统版本判断）
//        dto.setIsNewVersion(checkIfNewVersion(entity.getVersion()));
//
//        // 计算是否需要更新（业务逻辑）
//        dto.setRequireUpdate(checkIfRequireUpdate(entity));
//
//        return dto;
//    }
//
//    // 辅助方法：格式化文件大小
//    private String formatFileSize(long size) {
//        if (size < 1024) {
//            return size + " B";
//        } else if (size < 1024 * 1024) {
//            return String.format("%.2f KB", size / 1024.0);
//        } else if (size < 1024 * 1024 * 1024) {
//            return String.format("%.2f MB", size / (1024.0 * 1024.0));
//        } else {
//            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
//        }
//    }
//
//    // 获取更新类型标签
//    private String getUpdateTypeLabel(String updateType) {
//        if (updateType == null) return null;
//        switch (updateType) {
//            case "MAJOR": return "重大更新";
//            case "MINOR": return "次要更新";
//            case "PATCH": return "补丁更新";
//            case "SECURITY": return "安全更新";
//            default: return updateType;
//        }
//    }
//
//    // 获取状态标签
//    private String getStatusLabel(String status) {
//        if (status == null) return null;
//        switch (status) {
//            case "DRAFT": return "草稿";
//            case "PUBLISHED": return "已发布";
//            case "ARCHIVED": return "已归档";
//            case "DEPRECATED": return "已废弃";
//            default: return status;
//        }
//    }
//
//    // 检查是否为新版本（需要当前系统版本信息）
//    private Boolean checkIfNewVersion(String version) {
//        // 这里需要获取当前系统版本，假设从配置或数据库获取
//        String currentVersion = getCurrentSystemVersion();
//        return compareVersions(version, currentVersion) > 0;
//    }
//
//    // 检查是否需要更新（根据强制更新、有效时间等）
//    private Boolean checkIfRequireUpdate(SystemUpdate entity) {
//        if (Boolean.TRUE.equals(entity.getForceUpdate())) {
//            return true;
//        }
//
//        // 如果有效时间已过，可能需要更新
//        if (entity.getEffectiveTime() != null &&
//                entity.getEffectiveTime().isBefore(LocalDateTime.now())) {
//            return true;
//        }
//
//        // 其他业务逻辑...
//        return false;
//    }
//
//    // 版本比较方法
//    private int compareVersions(String v1, String v2) {
//        // 简单版本比较，可以根据实际情况实现更复杂的比较逻辑
//        if (v1 == null || v2 == null) return 0;
//        return v1.compareTo(v2);
//    }
//
//    // 获取当前系统版本（示例）
//    private String getCurrentSystemVersion() {
//        // 这里可以从配置文件、数据库或缓存中获取当前系统版本
//        return "1.0.0"; // 示例值
//    }
//}