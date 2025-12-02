package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoDTO {
    private Long id;
    private String username;
    private String name;
    private String roleCode;
    private String roleName;
    private String avatar;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String department;
    private String position;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public UserInfoDTO(Long id, String username, String name, String roleCode,
                       String password, String email, String phone,
                       String nickname, String department, String position,
                       Boolean enabled, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.roleCode = roleCode;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.nickname = nickname;
        this.department = department;
        this.position = position;
        this.enabled = enabled;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.avatar = "/api/avatar/default-avatar.png";
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

}