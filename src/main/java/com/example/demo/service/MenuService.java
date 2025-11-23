package com.example.demo.service;

import com.example.demo.entity.Menu;
import com.example.demo.entity.Role;
import com.example.demo.entity.RoleMenu;
import com.example.demo.repository.MenuRepository;
import com.example.demo.repository.RoleMenuRepository;
import com.example.demo.repository.RoleRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    @Resource
    private MenuRepository menuRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private RoleMenuRepository roleMenuRepository;

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

    public List<Menu> getMenuTreeByRole(String role) {
        List<Menu> allMenus = menuRepository.findAllByOrderBySortAsc();

        if ("super".equals(role)) {
            // 超级管理员拥有所有菜单
            return buildMenuTree(allMenus);
        }

        // 查询该角色拥有的菜单权限
        List<Long> authorizedMenuIds = getAuthorizedMenuIdsByRole(role);

        // 获取需要显示的所有菜单ID（包括必要的父级菜单）
        List<Long> allMenuIdsToShow = getAllMenuIdsToShow(authorizedMenuIds, allMenus);

        // 过滤出需要显示的菜单
        List<Menu> menusToShow = allMenus.stream()
                .filter(menu -> allMenuIdsToShow.contains(menu.getId()))
                .collect(Collectors.toList());

        return buildMenuTree(menusToShow);
    }

    private List<Long> getAllMenuIdsToShow(List<Long> authorizedMenuIds, List<Menu> allMenus) {
        Set<Long> allIdsToShow = new HashSet<>();

        // 首先添加所有有权限的菜单
        allIdsToShow.addAll(authorizedMenuIds);

        // 为每个有权限的菜单，添加其所有父级菜单（用于构建菜单树）
        for (Long menuId : authorizedMenuIds) {
            addAllParentMenusForDisplay(menuId, allIdsToShow, allMenus);
        }

        return new ArrayList<>(allIdsToShow);
    }

    private void addAllParentMenusForDisplay(Long menuId, Set<Long> menuIds, List<Menu> allMenus) {
        Menu menu = allMenus.stream()
                .filter(m -> m.getId().equals(menuId))
                .findFirst()
                .orElse(null);

        if (menu == null || menu.getParentId() == null) {
            return;
        }

        // 添加父级菜单ID（仅用于显示）
        menuIds.add(menu.getParentId());

        // 递归添加父级菜单的父级
        addAllParentMenusForDisplay(menu.getParentId(), menuIds, allMenus);
    }

    // 修改获取授权菜单ID的方法，基于数据库查询
    private List<Long> getAuthorizedMenuIdsByRole(String roleCode) {
        try {
            // 根据角色代码找到角色
            Role role = roleRepository.findByCode(roleCode);
            if (role == null) {
                return new ArrayList<>();
            }

            // 查询该角色的菜单权限
            List<RoleMenu> roleMenus = roleMenuRepository.findByRoleId(role.getId());

            return roleMenus.stream()
                    .map(roleMenu -> {
                        try {
                            return Long.valueOf(roleMenu.getMenuCode());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // 如果查询失败，返回空列表
            return new ArrayList<>();
        }
    }
}