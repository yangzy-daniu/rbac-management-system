package com.example.demo.common.context;

import com.example.demo.common.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

/**
 * 安全上下文工具类
 * 用于从Spring Security上下文中获取当前用户信息
 */
@Slf4j
public class SecurityContext {

    /**
     * 获取当前认证信息
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        if (principal instanceof String) {
            return (String) principal;
        }

        return null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 如果是自定义的UserDetails实现
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }

        // 如果有其他方式存储用户ID
        try {
            // 尝试从认证详情中获取
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = (Map<String, Object>) details;
                Object userId = detailsMap.get("userId");
                if (userId instanceof Long) return (Long) userId;
                if (userId instanceof Integer) return ((Integer) userId).longValue();
            }
        } catch (Exception e) {
            log.debug("从认证详情获取用户ID失败: {}", e.getMessage());
        }

        return null;
    }

//    /**
//     * 获取当前用户租户ID
//     */
//    public static Long getCurrentUserTenantId() {
//        Authentication authentication = getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return null;
//        }
//
//        Object principal = authentication.getPrincipal();
//
//        // 如果是自定义的UserDetails实现
//        if (principal instanceof CustomUserDetails) {
//            return ((CustomUserDetails) principal).getTenantId();
//        }
//
//        // 从JWT Claims中获取
//        try {
//            Object tenantId = authentication.getCredentials();
//            if (tenantId instanceof Long) return (Long) tenantId;
//
//            // 从认证详情获取
//            Object details = authentication.getDetails();
//            if (details instanceof Map) {
//                @SuppressWarnings("unchecked")
//                Map<String, Object> detailsMap = (Map<String, Object>) details;
//                Object tenant = detailsMap.get("tenantId");
//                if (tenant instanceof Long) return (Long) tenant;
//                if (tenant instanceof Integer) return ((Integer) tenant).longValue();
//                if (tenant instanceof String) return Long.valueOf((String) tenant);
//            }
//        } catch (Exception e) {
//            log.debug("获取用户租户ID失败: {}", e.getMessage());
//        }
//
//        return null;
//    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    /**
     * 检查用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 检查用户是否有特定权限
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = getAuthentication();
        if (authentication == null) return false;

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }
}