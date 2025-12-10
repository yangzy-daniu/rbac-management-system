package com.example.demo.repository;

import com.example.demo.entity.UserAccessStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserAccessStatisticsRepository extends JpaRepository<UserAccessStatistics, Long> {

    // 按日期范围查询
    List<UserAccessStatistics> findByStatDateBetweenOrderByStatDateAsc(LocalDate startDate, LocalDate endDate);

    // 获取最近N天的统计数据
    @Query("SELECT s FROM UserAccessStatistics s WHERE s.statDate >= :startDate ORDER BY s.statDate ASC")
    List<UserAccessStatistics> findRecentStats(LocalDate startDate);

    // 获取总用户数和活跃用户数
    @Query("SELECT COALESCE(SUM(s.userCount), 0), COALESCE(SUM(s.activeUsers), 0) FROM UserAccessStatistics s WHERE s.statDate >= :startDate")
    Object[] findUserStats(LocalDate startDate);

    // 获取最近一天的数据
    @Query("SELECT s FROM UserAccessStatistics s WHERE s.statDate = :date")
    UserAccessStatistics findByStatDate(LocalDate date);

    // 获取最大日期（最新）的数据
    @Query("SELECT s FROM UserAccessStatistics s ORDER BY s.statDate DESC LIMIT 1")
    UserAccessStatistics findLatestStats();
}