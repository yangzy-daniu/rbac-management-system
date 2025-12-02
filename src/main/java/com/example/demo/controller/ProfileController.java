package com.example.demo.controller;

import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.dto.ProfileUpdateRequest;
import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    // 获取当前用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            return authService.getUserIdByToken(token);
        }
        return 1L; // 测试时使用默认值
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @GetMapping("/userInfo")
    public ResponseEntity<UserInfoDTO> getCurrentUserProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        log.info("获取用户信息, userId: {}", userId);

        UserInfoDTO user = userService.getUserInfo(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // 清除密码等敏感信息
        user.setPassword(null);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/userInfo")
    public ResponseEntity<Map<String, Object>> updateProfile(
            HttpServletRequest request,
            @RequestBody ProfileUpdateRequest profileRequest) {
        Long userId = getCurrentUserId(request);
        log.info("更新用户资料, userId: {}, data: {}", userId, profileRequest);

        // 创建User对象用于更新
        User user = new User();
        user.setNickname(profileRequest.getNickname());
        user.setName(profileRequest.getName());
        user.setEmail(profileRequest.getEmail());
        user.setPhone(profileRequest.getPhone());
        user.setDepartment(profileRequest.getDepartment());
        user.setPosition(profileRequest.getPosition());

        // 调用UserService更新用户信息
        User updatedUser = userService.updateUserInfo(userId, user);
        if (updatedUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 获取更新后的用户信息
        UserInfoDTO userInfo = userService.getUserInfo(userId);
        userInfo.setPassword(null); // 清除密码

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "资料更新成功");
        response.put("data", userInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            HttpServletRequest request,
            @RequestBody PasswordChangeRequest passwordRequest) {
        Long userId = getCurrentUserId(request);
        log.info("修改密码, userId: {}", userId);

        Map<String, Object> response = new HashMap<>();

        // 验证新密码和确认密码是否一致
        if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmPassword())) {
            response.put("success", false);
            response.put("message", "新密码和确认密码不一致");
            return ResponseEntity.badRequest().body(response);
        }

        // 验证新密码复杂度
        if (passwordRequest.getNewPassword().length() < 6) {
            response.put("success", false);
            response.put("message", "新密码长度不能少于6位");
            return ResponseEntity.badRequest().body(response);
        }

        // 获取当前用户
        User currentUser = userService.findById(userId);
        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(response);
        }

        // 验证旧密码 - 使用 PasswordEncoder 验证
        if (!passwordEncoder.matches(passwordRequest.getOldPassword(), currentUser.getPassword())) {
            response.put("success", false);
            response.put("message", "旧密码不正确");
            return ResponseEntity.badRequest().body(response);
        }

        // 检查新密码是否与旧密码相同
        if (passwordEncoder.matches(passwordRequest.getNewPassword(), currentUser.getPassword())) {
            response.put("success", false);
            response.put("message", "新密码不能与旧密码相同");
            return ResponseEntity.badRequest().body(response);
        }

        // 更新密码
        User user = new User();
        user.setPassword(passwordRequest.getNewPassword()); // 这里会被 UserService 加密
        User updatedUser = userService.updateUserInfo(userId, user);

        if (updatedUser == null) {
            response.put("success", false);
            response.put("message", "密码更新失败");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("message", "密码修改成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(
            HttpServletRequest request,
            @RequestBody Map<String, String> avatarRequest) {
        Long userId = getCurrentUserId(request);
        log.info("更新头像, userId: {}", userId);

        String avatarUrl = avatarRequest.get("avatarUrl");

        // 更新用户头像
        User user = new User();
        user.setAvatar(avatarUrl);
        User updatedUser = userService.updateUserInfo(userId, user);

        if (updatedUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 获取更新后的用户信息
        UserInfoDTO userInfo = userService.getUserInfo(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "头像更新成功");
        response.put("avatarUrl", avatarUrl);
        response.put("userInfo", userInfo);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        log.info("获取用户统计信息, userId: {}", userId);

        // 获取真实的统计信息
        Long todayAccessCount = userService.getTodayAccessCount(userId);
        Long monthOperationCount = userService.getMonthOperationCount(userId);
        Double operationSuccessRate = userService.getOperationSuccessRate(userId);
        List<Map<String, Object>> recentActivities = userService.getRecentActivities(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayAccessCount", todayAccessCount);
        stats.put("monthOperationCount", monthOperationCount);
        stats.put("operationSuccessRate", operationSuccessRate);
        stats.put("recentActivities", recentActivities);

        return ResponseEntity.ok(stats);
    }
}