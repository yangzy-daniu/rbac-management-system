package com.example.demo.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String name;
        private String roleName;
    }
}