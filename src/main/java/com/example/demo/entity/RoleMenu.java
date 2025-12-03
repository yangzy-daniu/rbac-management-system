package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "role_menus")
@Data
public class RoleMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "menu_code", nullable = false)
    private Long menuCode;
}