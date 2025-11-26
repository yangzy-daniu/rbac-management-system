package com.example.demo.controller;


import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.dto.ProfileUpdateRequest;
import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 先全放行，生产再收窄
public class ProfileController {

    private final UserService userService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    // 模拟当前用户数据 - 实际项目中应该从Security Context获取
    private UserInfoDTO getCurrentUser(Long currentUserId) {
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("admin");
//        user.setNickname("系统管理员");
//        user.setEmail("admin@example.com");
//        user.setPhone("13800138000");
//        user.setDepartment("技术部");
//        user.setPosition("系统管理员");
//        user.setAvatar("/avatars/default-avatar.png");
//        user.setCreateTime(LocalDateTime.now().minusDays(30));
//        user.setUpdateTime(LocalDateTime.now());
        return userService.getUserInfo(currentUserId);
    }

    @GetMapping("/userInfo")
    public ResponseEntity<UserInfoDTO> getCurrentUserProfile() {
        log.info("获取当前用户信息");
        UserInfoDTO user = getCurrentUser(1L);

        // 清除密码等敏感信息
        user.setPassword(null);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/userInfo")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        log.info("更新用户资料: {}", request);

        // 模拟更新操作
        UserInfoDTO user = getCurrentUser(1L);
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setUpdateTime(LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "资料更新成功");
        response.put("data", user);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody PasswordChangeRequest request) {
        log.info("修改密码");

        Map<String, Object> response = new HashMap<>();

        // 验证旧密码 - 实际项目中应该验证数据库中的密码
        if (!"password".equals(request.getOldPassword())) {
            response.put("success", false);
            response.put("message", "旧密码不正确");
            return ResponseEntity.badRequest().body(response);
        }

        // 验证新密码和确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            response.put("success", false);
            response.put("message", "新密码和确认密码不一致");
            return ResponseEntity.badRequest().body(response);
        }

        // 验证新密码复杂度
        if (request.getNewPassword().length() < 6) {
            response.put("success", false);
            response.put("message", "新密码长度不能少于6位");
            return ResponseEntity.badRequest().body(response);
        }

        // 模拟密码更新
        response.put("success", true);
        response.put("message", "密码修改成功");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(@RequestBody Map<String, String> avatarRequest) {
        log.info("更新头像");

        String avatarUrl = avatarRequest.get("avatarUrl");

        // 模拟更新头像
        UserInfoDTO user = getCurrentUser(1L);
        user.setAvatar(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "头像更新成功");
        response.put("avatarUrl", avatarUrl);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("获取用户统计信息");

        Map<String, Object> stats = new HashMap<>();
        stats.put("loginDays", 156);
        stats.put("lastLogin", LocalDateTime.now().minusHours(2));
        stats.put("totalOperations", 2845);
        stats.put("successRate", 99.8);
        stats.put("favoriteModules", new String[]{"系统管理", "用户管理", "角色管理"});

        return ResponseEntity.ok(stats);
    }
}