package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TodoCreateDTO {
    private String title;
    private String description;
    private String priority = "MEDIUM";
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindTime;
    private String type = "PERSONAL";
    private String category;
    private String sourceId;
    private Long assignUserId; // 指派给其他人（可选）
}