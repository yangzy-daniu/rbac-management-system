package com.example.demo.service;

import com.example.demo.entity.OperationLog;
import com.example.demo.repository.OperationLogRepository;
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
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Async
    public void saveLog(OperationLog operationLog) {
        try {
            operationLog.setCreateTime(LocalDateTime.now());
            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    // 分页查询操作日志（支持多条件筛选）
    public Page<OperationLog> getLogsByPage(int page, int size, String module, String operator, String type,
                                            String result, String requestMethod, Integer statusCode,
                                            String requestUrl, LocalDateTime startTime, LocalDateTime endTime) {
        Specification<OperationLog> spec = buildLogSpecification(module, operator, type, result,
                requestMethod, statusCode, requestUrl, startTime, endTime);
        Pageable pageable = PageRequest.of(page - 1, size);
        return operationLogRepository.findAll(spec, pageable);
    }

    // 构建日志查询条件
    private Specification<OperationLog> buildLogSpecification(String module, String operator, String type,
                                                              String result, String requestMethod, Integer statusCode,
                                                              String requestUrl, LocalDateTime startTime,
                                                              LocalDateTime endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(module)) {
                predicates.add(cb.like(root.get("module"), "%" + module + "%"));
            }

            if (StringUtils.hasText(operator)) {
                predicates.add(cb.like(root.get("operator"), "%" + operator + "%"));
            }

            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (StringUtils.hasText(result)) {
                predicates.add(cb.equal(root.get("result"), result));
            }

            if (StringUtils.hasText(requestMethod)) {
                predicates.add(cb.equal(root.get("requestMethod"), requestMethod));
            }

            if (statusCode != null) {
                predicates.add(cb.equal(root.get("statusCode"), statusCode));
            }

            if (StringUtils.hasText(requestUrl)) {
                predicates.add(cb.like(root.get("requestUrl"), "%" + requestUrl + "%"));
            }

            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }

            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }

            query.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<OperationLog> getRecentLogs() {
        return operationLogRepository.findTop10ByOrderByCreateTimeDesc();
    }
}