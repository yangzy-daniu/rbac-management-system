package com.example.demo.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String position;
}