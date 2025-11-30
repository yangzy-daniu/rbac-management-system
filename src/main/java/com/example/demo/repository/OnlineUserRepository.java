package com.example.demo.repository;

import com.example.demo.entity.OnlineUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineUserRepository extends JpaRepository<OnlineUser, String> {

    // 查询活跃的在线用户
    List<OnlineUser> findByIsActiveTrue();

    List<OnlineUser> findByUsername(String username);

    // 统计在线用户数量
    long countByIsActiveTrue();

    // 根据用户ID查找在线用户
    Optional<OnlineUser> findByUserId(Long userId);

    // 清理过期的会话（比如30分钟无活动）
    @Modifying
    @Query("DELETE FROM OnlineUser ou WHERE ou.lastAccessTime < :expireTime")
    int deleteExpiredSessions(@Param("expireTime") LocalDateTime expireTime);

    // 更新最后访问时间
    @Modifying
    @Query("UPDATE OnlineUser ou SET ou.lastAccessTime = :accessTime WHERE ou.sessionId = :sessionId")
    void updateLastAccessTime(@Param("sessionId") String sessionId, @Param("accessTime") LocalDateTime accessTime);
}