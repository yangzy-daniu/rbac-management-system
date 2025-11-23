package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

//    @ElementCollection
//    @CollectionTable(name = "role_menus", joinColumns = @JoinColumn(name = "role_id"))
//    @Column(name = "menu_code")
    @Transient
    private List<String> menuPermissions;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}