package com.example.demo.controller;

import com.example.demo.entity.Menu;
import com.example.demo.service.MenuService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MenuController {

    @Resource
    private MenuService menuService;

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
    public List<Menu> getMenuTree() {
        return menuService.getMenuTree();
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
}