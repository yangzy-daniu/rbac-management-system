package com.example.demo.service;

import com.example.demo.entity.Menu;
import com.example.demo.repository.MenuRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    @Resource
    private MenuRepository menuRepository;

    public List<Menu> getUserMenus() {
        // 构建树形菜单结构
        return buildMenuTree(menuRepository.findAllByOrderBySortAsc());
    }

    public List<Menu> getAllMenus() {
        return menuRepository.findAllByOrderBySortAsc();
    }

    public List<Menu> getMenuTree() {
        List<Menu> allMenus = menuRepository.findAllByOrderBySortAsc();
        return buildMenuTree(allMenus);
    }

    public List<Menu> searchMenus(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllMenus();
        }
        return menuRepository.findByNameContainingIgnoreCaseOrderBySortAsc(name);
    }

    public Menu createMenu(Menu menu) {
        // 设置默认值
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getType() == null) {
            menu.setType(0);
        }
        // 如果是根菜单，设置parentId为null
        if (menu.getParentId() != null && menu.getParentId() == 0) {
            menu.setParentId(null);
        }
        return menuRepository.save(menu);
    }

    public Menu updateMenu(Long id, Menu menu) {
        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("菜单不存在"));

        // 更新字段
        existingMenu.setName(menu.getName());
        existingMenu.setPath(menu.getPath());
        existingMenu.setIcon(menu.getIcon());
        existingMenu.setSort(menu.getSort());
        existingMenu.setParentId(menu.getParentId());
        existingMenu.setComponent(menu.getComponent());
        existingMenu.setType(menu.getType());
        existingMenu.setAvailable(menu.getAvailable()); // 添加这行

        // 处理parentId为0的情况（表示根菜单）
        if (existingMenu.getParentId() != null && existingMenu.getParentId() == 0) {
            existingMenu.setParentId(null);
        }

        return menuRepository.save(existingMenu);
    }

    @Transactional
    public boolean deleteMenu(Long id) {
        try {
            Menu menu = menuRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("菜单不存在"));

            // 检查是否有子菜单
            List<Menu> children = menuRepository.findByParentIdOrderBySortAsc(id);
            if (!children.isEmpty()) {
                throw new RuntimeException("该菜单存在子菜单，无法删除");
            }

            menuRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("删除菜单失败: " + e.getMessage());
        }
    }

    public Menu getMenuById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("菜单不存在"));
    }

    public boolean isPathExists(String path, Long excludeId) {
        if (excludeId != null) {
            return menuRepository.existsByPathAndIdNot(path, excludeId);
        }
        return menuRepository.existsByPath(path);
    }

    /**
     * 构建树形菜单结构
     */
    private List<Menu> buildMenuTree(List<Menu> menus) {
        // 按parentId分组
        Map<Long, List<Menu>> menuMap = menus.stream()
                .collect(Collectors.groupingBy(menu ->
                        menu.getParentId() == null ? 0L : menu.getParentId()));

        // 构建树形结构
        return buildTree(menuMap, 0L);
    }

    private List<Menu> buildTree(Map<Long, List<Menu>> menuMap, Long parentId) {
        List<Menu> children = menuMap.get(parentId);
        if (children == null) {
            return new ArrayList<>();
        }

        // 递归构建子树
        children.forEach(menu -> {
            List<Menu> grandChildren = buildTree(menuMap, menu.getId());
            menu.setChildren(grandChildren);
        });

        return children;
    }
}