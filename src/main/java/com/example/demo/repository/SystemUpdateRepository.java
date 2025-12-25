package com.example.demo.repository;

import com.example.demo.entity.SystemUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemUpdateRepository extends JpaRepository<SystemUpdate, Long> {

    Optional<SystemUpdate> findByVersion(String version);

    List<SystemUpdate> findByStatusOrderByReleaseTimeDesc(String status);

//    List<SystemUpdate> findByUpdateTypeAndStatus(String updateType, String status);

//    @Query("SELECT su FROM SystemUpdate su WHERE su.status = 'RELEASED' " +
//            "AND (su.effectiveTime IS NULL OR su.effectiveTime <= :currentTime) " +
//            "ORDER BY su.releaseTime DESC")
//    List<SystemUpdate> findEffectiveUpdates(LocalDateTime currentTime);
//
//    @Query(value = "SELECT MAX(version) FROM system_updates WHERE status = 'RELEASED'",
//            nativeQuery = true)
//    String findLatestVersion();

    // 根据状态分页查询
    Page<SystemUpdate> findByStatus(String status, Pageable pageable);

    // 统计某个状态的更新数量
    Long countByStatus(String status);

}