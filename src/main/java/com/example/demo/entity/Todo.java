package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "todos")
@Data
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(nullable = false)
    private String priority; // 优先级 HIGH, MEDIUM, LOW

    @Column(name = "due_time")
    private LocalDateTime dueTime; // 截止时间

    @Column(name = "remind_time")
    private LocalDateTime remindTime;//

    @Column(name = "todo_type")
    private String todoType; // PERSONAL, SYSTEM, ASSIGNED

    @Column(name = "source_id")
    private String sourceId; // 关联业务ID，如订单ID、申请ID等

    private String category; // 分类：工作、学习、生活等

    @Column(name = "assign_user_id")
    private Long assignUserId; // 被指派用户ID

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
        if (completed && completeTime == null) {
            completeTime = LocalDateTime.now();
        }
    }
}