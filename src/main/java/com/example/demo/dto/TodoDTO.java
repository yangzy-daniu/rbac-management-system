package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TodoDTO {
    private Long id;
    private String title;
    private String description;
    private Boolean completed = false;
    private String priority; // HIGH, MEDIUM, LOW
    private String priorityLabel; // 高、中、低
    private LocalDateTime dueTime;
    private LocalDateTime remindTime;
    private String todoType; // PERSONAL, SYSTEM, ASSIGNED
    private String typeLabel; // 个人、系统、指派
    private String category;
    private String sourceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completeTime;
    private String timeLabel; // 今天 14:00、明天、日期格式
    private String assignUserName; // 被指派用户姓名（如果有）
}