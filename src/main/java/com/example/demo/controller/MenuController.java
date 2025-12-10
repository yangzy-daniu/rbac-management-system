package com.example.demo.controller;

import com.example.demo.entity.Menu;
import com.example.demo.service.AuthService;
import com.example.demo.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    private final AuthService authService;

    @GetMapping
    public List<Menu> getUserMenus() {
        return menuService.getUserMenus();
    }

    @GetMapping("/all")
    public List<Menu> getAllMenus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        validateToken(authHeader);
        return menuService.getAllMenus();
    }

    @GetMapping("/tree")
    public List<Menu> getMenuTree(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 验证token并获取用户角色
        validateTokenAndRole(authHeader);

        String userRole = getUserRoleFromToken(authHeader);

        // 根据用户角色返回对应的菜单树
        return menuService.getMenuTreeByRole(userRole);
    }

    // 添加辅助方法
    private void validateTokenAndRole(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("未授权访问");
        }

        String token = authHeader.replace("Bearer ", "");
        if (token.isEmpty() || "undefined".equals(token) || "null".equals(token)) {
            throw new AccessDeniedException("Token无效");
        }
    }

    private String getUserRoleFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        if (!authService.validateToken(token)) {
            throw new AccessDeniedException("Token无效或已过期");
        }

        String userRole = authService.getUserRoleByToken(token);
        if (userRole == null) {
            throw new AccessDeniedException("用户角色信息不存在");
        }

        return userRole;
    }

    @GetMapping("/search")
    public List<Menu> searchMenus(@RequestParam(required = false) String name) {
        return menuService.searchMenus(name);
    }

    @PostMapping
    public Menu createMenu(@RequestBody Menu menu) {
        return menuService.createMenu(menu);
    }

    @PutMapping("/{id}")
    public Menu updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        return menuService.updateMenu(id, menu);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> deleteMenu(@PathVariable Long id) {
        boolean success = menuService.deleteMenu(id);
        return Map.of("success", success);
    }

    @GetMapping("/{id}")
    public Menu getMenuById(@PathVariable Long id) {
        return menuService.getMenuById(id);
    }

    @GetMapping("/check-path")
    public Map<String, Boolean> checkPathExists(
            @RequestParam String path,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = menuService.isPathExists(path, excludeId);
        return Map.of("exists", exists);
    }

    // 统一的token验证方法
    private void validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("未授权访问");
        }

        String token = authHeader.replace("Bearer ", "");
        if (token.isEmpty() || "undefined".equals(token) || "null".equals(token)) {
            throw new AccessDeniedException("Token无效");
        }
    }

    /**
     * 获取前端路由配置
     */
    @GetMapping("/config")
    public Map<String, Map<String, Object>> getMenuConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validateToken(authHeader);

        List<Menu> menus = menuService.getAllMenus();
        Map<String, Map<String, Object>> config = new HashMap<>();

        // 只获取类型为菜单的项（type=1）
        for (Menu menu : menus) {
            if (menu.getType() != null && menu.getType() == 1 && menu.getPath() != null && !menu.getPath().isEmpty()) {
                config.put(menu.getPath(), menu.getFrontendConfig());
            }
        }

        return config;
    }
}