package com.example.demo.repository;

import com.example.demo.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    List<OperationLog> findTop10ByOrderByCreateTimeDesc();

    @Query("SELECT ol FROM OperationLog ol WHERE " +
            "(:module IS NULL OR ol.module LIKE %:module%) AND " +
            "(:operator IS NULL OR ol.operator LIKE %:operator%) AND " +
            "(:type IS NULL OR ol.type = :type) AND " +
            "(:result IS NULL OR ol.result = :result) AND " +
            "(:requestMethod IS NULL OR ol.requestMethod = :requestMethod) AND " +
            "(:statusCode IS NULL OR ol.statusCode = :statusCode) AND " +
            "(:requestUrl IS NULL OR ol.requestUrl LIKE %:requestUrl%) AND " +
            "ol.accessTime BETWEEN :startTime AND :endTime")
    Page<OperationLog> findDetailedLogs(String module, String operator, String type,
                                        String result, String requestMethod, Integer statusCode,
                                        String requestUrl, LocalDateTime startTime,
                                        LocalDateTime endTime, Pageable pageable);
}