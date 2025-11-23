package com.example.demo.controller;

import com.example.demo.entity.Role;
import com.example.demo.service.RoleService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoleController {

    @Resource
    private RoleService roleService;

    // 分页查询角色
    @GetMapping("/page")
    public Page<Role> getRolesByPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return roleService.getRolesByPage(page, size, keyword);
    }

    // 创建角色
    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleService.createRole(role);
    }

    // 更新角色
    @PutMapping("/{id}")
    public Role updateRole(@PathVariable Long id, @RequestBody Role role) {
        return roleService.updateRole(id, role);
    }

    // 删除角色
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }

    // 批量删除角色
    @PostMapping("/batch-delete")
    public void batchDeleteRoles(@RequestBody List<Long> ids) {
        roleService.deleteRoles(ids);
    }

    // 检查角色代码是否存在
    @GetMapping("/check-code")
    public Map<String, Boolean> checkCode(
            @RequestParam String code,
            @RequestParam(required = false) Long excludeId) {
        boolean exists;
        if (excludeId != null) {
            exists = roleService.isCodeExists(code, excludeId);
        } else {
            exists = roleService.isCodeExists(code);
        }
        return Map.of("exists", exists);
    }

    // 根据ID获取角色详情
    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
    }
}