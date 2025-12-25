package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemUpdateDTO {
    private Long id;
    private String version;
    private String title;
    private String description;
    private String releaseNotes;
    private String updateType;
    private String updateTypeLabel;
    private Boolean forceUpdate = false;
    private String filePath;
    private Long fileSize;
    private String fileSizeFormat; // 格式化后的文件大小
    private String md5Hash;
    private LocalDateTime releaseTime;
    private LocalDateTime effectiveTime;
    private String status;
    private String statusLabel;
    private String createdByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 用于前端显示
    private Boolean isNewVersion = false;
    private Boolean requireUpdate = false;

}