package com.example.demo.service;

import com.example.demo.entity.SystemLog;
import com.example.demo.repository.SystemLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    @Async
    public void saveSystemLog(SystemLog systemLog) {
        try {
            systemLogRepository.save(systemLog);
            log.debug("系统日志保存成功: {} - {}", systemLog.getLevel(), systemLog.getMessage());
        } catch (Exception e) {
            log.error("保存系统日志失败", e);
        }
    }

    // 快速记录方法
    @Async
    public void logInfo(String service, String message) {
        SystemLog log = new SystemLog();
        log.setLevel("INFO");
        log.setService(service);
        log.setMessage(message);
        saveSystemLog(log);
    }

    @Async
    public void logWarning(String service, String message, String details) {
        SystemLog log = new SystemLog();
        log.setLevel("WARNING");
        log.setService(service);
        log.setMessage(message);
        log.setDetails(details);
        saveSystemLog(log);
    }

    @Async
    public void logError(String service, String message, String details) {
        SystemLog log = new SystemLog();
        log.setLevel("ERROR");
        log.setService(service);
        log.setMessage(message);
        log.setDetails(details);
        saveSystemLog(log);
    }

    // 分页查询系统日志
    public Page<SystemLog> getSystemLogsByPage(int page, int size, String level, String service,
                                               String module, String username,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        Specification<SystemLog> spec = buildSystemLogSpecification(level, service, module,
                username, startTime, endTime);
        Pageable pageable = PageRequest.of(page - 1, size);
        return systemLogRepository.findAll(spec, pageable);
    }

    // 构建查询条件
    private Specification<SystemLog> buildSystemLogSpecification(String level, String service,
                                                                 String module, String username,
                                                                 LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(level)) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (StringUtils.hasText(service)) {
                predicates.add(cb.like(root.get("service"), "%" + service + "%"));
            }

            if (StringUtils.hasText(module)) {
                predicates.add(cb.like(root.get("module"), "%" + module + "%"));
            }

            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }

            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
            }

            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
            }

            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 获取最近日志
    public List<SystemLog> getRecentLogs() {
        return systemLogRepository.findTop10ByOrderByTimestampDesc();
    }

    // 统计日志数量
    public Long getLogCountByLevel(String level, LocalDateTime start, LocalDateTime end) {
        return systemLogRepository.countByLevelAndTimestampBetween(level, start, end);
    }

    // 获取服务统计
    public List<Object[]> getServiceStats(LocalDateTime start, LocalDateTime end) {
        return systemLogRepository.countLogsByService(start, end);
    }

    public void clearLogs() {
        systemLogRepository.deleteAll();
    }

    public List<SystemLog> getLogsForExport(String level, String service, String module,
                                            String username, LocalDateTime startTime,
                                            LocalDateTime endTime) {
        Specification<SystemLog> spec = buildSystemLogSpecification(level, service, module,
                username, startTime, endTime);
        return systemLogRepository.findAll(spec);
    }
}