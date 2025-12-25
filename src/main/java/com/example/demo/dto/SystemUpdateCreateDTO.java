package com.example.demo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
public class SystemUpdateCreateDTO {
    private String version;
    private String title;
    private String description;
    private String releaseNotes;
    private String updateType;
    private Boolean forceUpdate = false;
    private LocalDateTime effectiveTime;
    private MultipartFile updateFile;
}

