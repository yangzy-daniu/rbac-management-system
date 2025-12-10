package com.example.demo.repository;

import com.example.demo.entity.UserBehaviorStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorStatisticsRepository extends JpaRepository<UserBehaviorStatistics, Long> {

    // 按时间范围查询
    List<UserBehaviorStatistics> findByStatHourBetweenOrderByStatHourAsc(LocalDateTime start, LocalDateTime end);

    // 获取最近的行为分布数据
    @Query("SELECT " +
            "SUM(b.pageView) as pageView, " +
            "SUM(b.buttonClick) as buttonClick, " +
            "SUM(b.formSubmit) as formSubmit, " +
            "SUM(b.fileDownload) as fileDownload, " +
            "SUM(b.otherActions) as otherActions " +
            "FROM UserBehaviorStatistics b " +
            "WHERE b.statHour >= :startTime")
    Object[] findBehaviorSummary(LocalDateTime startTime);
}