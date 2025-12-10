package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_behavior_statistics")
public class UserBehaviorStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_hour", nullable = false)
    private LocalDateTime statHour;

    @Column(name = "page_view")
    private Integer pageView = 0;

    @Column(name = "button_click")
    private Integer buttonClick = 0;

    @Column(name = "form_submit")
    private Integer formSubmit = 0;

    @Column(name = "file_download")
    private Integer fileDownload = 0;

    @Column(name = "other_actions")
    private Integer otherActions = 0;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
    }
}