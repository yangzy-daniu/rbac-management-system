package com.example.demo.repository;

import com.example.demo.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {

    List<SystemLog> findTop10ByOrderByTimestampDesc();

    Page<SystemLog> findByLevelOrderByTimestampDesc(String level, Pageable pageable);

    List<SystemLog> findByServiceAndTimestampBetween(String service, LocalDateTime start, LocalDateTime end);

    @Query("SELECT sl FROM SystemLog sl WHERE " +
            "(:level IS NULL OR sl.level = :level) AND " +
            "(:service IS NULL OR sl.service LIKE %:service%) AND " +
            "(:module IS NULL OR sl.module LIKE %:module%) AND " +
            "(:username IS NULL OR sl.username LIKE %:username%) AND " +
            "sl.timestamp BETWEEN :startTime AND :endTime")
    Page<SystemLog> findSystemLogs(String level, String service, String module,
                                   String username, LocalDateTime startTime,
                                   LocalDateTime endTime, Pageable pageable);

    Long countByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end);

    @Query("SELECT sl.service, COUNT(sl) FROM SystemLog sl " +
            "WHERE sl.timestamp BETWEEN :start AND :end " +
            "GROUP BY sl.service")
    List<Object[]> countLogsByService(LocalDateTime start, LocalDateTime end);
}