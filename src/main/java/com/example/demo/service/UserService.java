package com.example.demo.service;

import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.OperationLog;
import com.example.demo.entity.User;
import com.example.demo.repository.OperationLogRepository;
import com.example.demo.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OperationLogRepository operationLogRepository;

    public UserInfoDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        return new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRoleCode(),
                user.getPassword(),
                user.getEmail(),
                user.getPhone(),
                user.getNickname(),
                user.getDepartment(),
                user.getPosition(),
                user.getEnabled(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }

    public User updateUserInfo(Long userId, User updatedUser) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        if (updatedUser.getName() != null) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(updatedUser.getPassword());
        }

        return userRepository.save(user);
    }

    // 分页查询用户
    public Page<User> getUsersByPage(int page, int size, String keyword, String role) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (StringUtils.hasText(keyword) || StringUtils.hasText(role)) {
            // 构建查询条件
            Specification<User> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(keyword)) {
                    Predicate usernamePredicate = cb.like(root.get("username"), "%" + keyword + "%");
                    Predicate namePredicate = cb.like(root.get("name"), "%" + keyword + "%");
                    predicates.add(cb.or(usernamePredicate, namePredicate));
                }

                if (StringUtils.hasText(role)) {
                    predicates.add(cb.equal(root.get("role"), role));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };
            return userRepository.findAll(spec, pageable);
        }

        return userRepository.findAll(pageable);
    }

    // 批量删除用户
    public void deleteUsers(List<Long> ids) {
        userRepository.deleteAllById(ids);
    }

    // 检查用户名是否存在
    public boolean isUsernameExists(String username) {
        return userRepository.findByUsername(username) != null;
    }

    // 检查用户名是否存在（排除指定ID）
    public boolean isUsernameExists(String username, Long excludeId) {
        User user = userRepository.findByUsername(username);
        return user != null && !user.getId().equals(excludeId);
    }

    // 查询用户今日访问次数
    public Long getTodayAccessCount(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        return operationLogRepository.countByOperatorIdAndAccessTimeBetween(
                userId, startOfDay, endOfDay);
    }

    // 查询用户本月操作数量
    public Long getMonthOperationCount(Long userId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endOfMonth = LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        return operationLogRepository.countByOperatorIdAndCreateTimeBetween(
                userId, startOfMonth, endOfMonth);
    }

    // 查询用户操作完成率
    public Double getOperationSuccessRate(Long userId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endOfMonth = LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        Long totalOperations = operationLogRepository.countByOperatorIdAndCreateTimeBetween(
                userId, startOfMonth, endOfMonth);
        Long successOperations = operationLogRepository.countByOperatorIdAndResultAndCreateTimeBetween(
                userId, "SUCCESS", startOfMonth, endOfMonth);

        if (totalOperations == 0) {
            return 100.0; // 如果没有操作，默认100%
        }

        return (successOperations.doubleValue() / totalOperations.doubleValue()) * 100;
    }

    // 获取用户最近活动
    public List<Map<String, Object>> getRecentActivities(Long userId) {
        List<OperationLog> logs = operationLogRepository.findTop5ByOperatorIdOrderByCreateTimeDesc(userId);

        return logs.stream().map(log -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("description", log.getOperation());
            activity.put("time", formatActivityTime(log.getCreateTime()));
            return activity;
        }).collect(Collectors.toList());
    }

    private String formatActivityTime(LocalDateTime time) {
        Duration duration = Duration.between(time, LocalDateTime.now());

        if (duration.toMinutes() < 1) {
            return "刚刚";
        } else if (duration.toHours() < 1) {
            return duration.toMinutes() + "分钟前";
        } else if (duration.toDays() < 1) {
            return duration.toHours() + "小时前";
        } else if (duration.toDays() == 1) {
            return "昨天";
        } else {
            return duration.toDays() + "天前";
        }
    }
}