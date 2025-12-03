package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true：在该字段上创建唯一约束，确保数据库中没有重复值
    // nullable = false：该字段不允许为 NULL，必须提供值
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String name;

    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private String department;
    private String position;
    private Boolean enabled = true;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 角色字段，可以扩展为角色表
    private String roleCode = "user";

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}