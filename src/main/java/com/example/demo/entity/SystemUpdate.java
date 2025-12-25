package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_updates")
@Data
public class SystemUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "update_type", nullable = false, length = 20)
    private String updateType; // BUG_FIX, FEATURE, SECURITY, OPTIMIZATION

    @Column(name = "force_update")
    private Boolean forceUpdate = false;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "md5_hash", length = 64)
    private String md5Hash;

    @Column(name = "release_time", nullable = false)
    private LocalDateTime releaseTime;

    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, RELEASED, ROLLED_BACK

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (releaseTime == null) {
            releaseTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}