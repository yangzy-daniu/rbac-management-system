package com.example.demo.service;

import com.example.demo.repository.MenuRepository;
import com.example.demo.repository.OperationLogRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final OperationLogRepository operationLogRepository;

    // 获取总用户数
    public Long getTotalUsers() {
        return userRepository.count();
    }

    // 获取角色数量
    public Long getTotalRoles() {
        return roleRepository.count();
    }

    // 获取菜单项数量
    public Long getTotalMenus() {
        return menuRepository.count();
    }

    // 获取操作日志总数
    public Long getTotalOperationLogs() {
        return operationLogRepository.count();
    }

    // 获取所有统计信息
    public Map<String, Object> getQuickStats() {
        return Map.of(
                "totalUsers", getTotalUsers(),
                "totalRoles", getTotalRoles(),
                "totalMenus", getTotalMenus(),
                "totalLogs", getTotalOperationLogs()
        );
    }
}