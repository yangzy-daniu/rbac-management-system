package com.example.demo.init;

import com.example.demo.entity.Menu;
import com.example.demo.entity.Role;
import com.example.demo.entity.RoleMenu;
import com.example.demo.entity.User;
import com.example.demo.repository.MenuRepository;
import com.example.demo.repository.RoleMenuRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData() {
        // 检查是否已有数据
        boolean shouldInitialize = menuRepository.count() == 0
                && roleRepository.count() == 0
                && roleMenuRepository.count() == 0
                && userRepository.count() == 0;

        if (shouldInitialize) {
            log.info("开始初始化RBAC系统演示数据...");

            initMenus();
            initRoles();
            initUsers();
            initRoleMenus();

            log.info("RBAC系统演示数据初始化完成！");
        } else {
            log.info("数据库中已有数据，跳过初始化");
        }
    }

    private void initMenus() {
        log.info("初始化菜单数据...");

        // 一级菜单 - 首页
        Menu homeMenu = new Menu();
        homeMenu.setName("首页");
        homeMenu.setIcon("House");
        homeMenu.setPath("/home");
        homeMenu.setComponent("Home");
        homeMenu.setSort(1);
        homeMenu.setType(1); // 菜单
        homeMenu.setAvailable(true);
        menuRepository.save(homeMenu);

        // 一级菜单 - 工作台
        Menu dashboardMenu = new Menu();
        dashboardMenu.setName("工作台");
        dashboardMenu.setPath("/dashboard");
        dashboardMenu.setIcon("Monitor");
        dashboardMenu.setSort(2);
        dashboardMenu.setType(0); // 目录
        dashboardMenu.setAvailable(true);
        dashboardMenu = menuRepository.save(dashboardMenu);

        // 二级菜单 - 分析页
        Menu analysisMenu = new Menu();
        analysisMenu.setName("分析页");
        analysisMenu.setPath("/analysis");
        analysisMenu.setIcon("PieChart");
        analysisMenu.setSort(1);
        analysisMenu.setParentId(dashboardMenu.getId());
        analysisMenu.setComponent("Analysis");
        analysisMenu.setType(1); // 菜单
        analysisMenu.setAvailable(true);
        menuRepository.save(analysisMenu);

        // 一级菜单 - 个人中心
        Menu profileMenu = new Menu();
        profileMenu.setName("个人中心");
        profileMenu.setPath("/profile");
        profileMenu.setIcon("User");
        profileMenu.setSort(3);
        profileMenu.setType(1); // 菜单
        profileMenu.setComponent("Index");
        profileMenu.setAvailable(true);
        menuRepository.save(profileMenu);

        // 一级菜单 - 系统管理
        Menu systemMenu = new Menu();
        systemMenu.setName("系统管理");
        systemMenu.setIcon("Setting");
        systemMenu.setPath("/system");
        systemMenu.setSort(4);
        systemMenu.setType(0); // 目录
        systemMenu.setAvailable(true);
        systemMenu = menuRepository.save(systemMenu);

        // 二级菜单 - 用户管理
        Menu userMenu = new Menu();
        userMenu.setName("用户管理");
        userMenu.setPath("/user");
        userMenu.setIcon("User");
        userMenu.setSort(1);
        userMenu.setParentId(systemMenu.getId());
        userMenu.setComponent("UserManagement");
        userMenu.setType(1); // 菜单
        userMenu.setAvailable(true);
        menuRepository.save(userMenu);

        // 二级菜单 - 角色管理
        Menu roleMenu = new Menu();
        roleMenu.setName("角色管理");
        roleMenu.setPath("/role");
        roleMenu.setIcon("Lock");
        roleMenu.setSort(2);
        roleMenu.setParentId(systemMenu.getId());
        roleMenu.setComponent("RoleManagement");
        roleMenu.setType(1); // 菜单
        roleMenu.setAvailable(true);
        menuRepository.save(roleMenu);

        // 二级菜单 - 菜单管理
        Menu menuManager = new Menu();
        menuManager.setName("菜单管理");
        menuManager.setPath("/menu");
        menuManager.setIcon("Menu");
        menuManager.setSort(3);
        menuManager.setParentId(systemMenu.getId());
        menuManager.setComponent("MenuManagement");
        menuManager.setType(1); // 菜单
        menuManager.setAvailable(true);
        menuRepository.save(menuManager);

        log.info("菜单数据初始化完成，共创建 {} 个菜单", menuRepository.count());
    }

    private void initRoles() {
        log.info("初始化角色数据...");

        Role adminRole = new Role();
        adminRole.setCode("admin");
        adminRole.setName("系统管理员");
        adminRole.setDescription("拥有系统权限");
        adminRole.setCreateTime(LocalDateTime.now());
        adminRole.setUpdateTime(LocalDateTime.now());
        roleRepository.save(adminRole);

        Role superRole = new Role();
        superRole.setCode("super");
        superRole.setName("超级管理员");
        superRole.setDescription("拥有所有权限");
        superRole.setCreateTime(LocalDateTime.now());
        superRole.setUpdateTime(LocalDateTime.now());
        roleRepository.save(superRole);

        Role userRole = new Role();
        userRole.setCode("user");
        userRole.setName("普通用户");
        userRole.setDescription("拥有基本权限");
        userRole.setCreateTime(LocalDateTime.now());
        userRole.setUpdateTime(LocalDateTime.now());
        roleRepository.save(userRole);

        Role guestRole = new Role();
        guestRole.setCode("guest");
        guestRole.setName("访客");
        guestRole.setDescription("只读权限");
        guestRole.setCreateTime(LocalDateTime.now());
        guestRole.setUpdateTime(LocalDateTime.now());
        roleRepository.save(guestRole);

        log.info("角色数据初始化完成，共创建 {} 个角色", roleRepository.count());
    }

    private void initUsers() {
        log.info("初始化用户数据...");

        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("123456"));
        adminUser.setName("系统管理员");
        adminUser.setRole("admin");
        adminUser.setCreateTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(adminUser);

        User superUser = new User();
        superUser.setUsername("superAdmin");
        superUser.setPassword(passwordEncoder.encode("123456"));
        superUser.setName("超级管理员");
        superUser.setRole("super");
        superUser.setCreateTime(LocalDateTime.now());
        superUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(superUser);

        User normalUser = new User();
        normalUser.setUsername("user");
        normalUser.setPassword(passwordEncoder.encode("123456"));
        normalUser.setName("张三");
        normalUser.setRole("user");
        normalUser.setCreateTime(LocalDateTime.now());
        normalUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(normalUser);

        User guestUser = new User();
        guestUser.setUsername("guest");
        guestUser.setPassword(passwordEncoder.encode("123456"));
        guestUser.setName("李四");
        guestUser.setRole("guest");
        guestUser.setCreateTime(LocalDateTime.now());
        guestUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(guestUser);

        log.info("用户数据初始化完成，共创建 {} 个用户", userRepository.count());
    }

    private void initRoleMenus() {
        log.info("初始化角色菜单权限...");

        // 获取所有菜单
        List<Menu> allMenus = menuRepository.findAll();

        // 获取角色
        Role superRole = getRoleByCode("super", "超级管理员");
        Role adminRole = getRoleByCode("admin", "管理员");
        Role userRole = getRoleByCode("user", "普通用户");
        Role guestRole = getRoleByCode("guest", "访客");

        // 管理员拥有所有菜单权限
        assignAllMenusToRole(superRole.getId(), allMenus);
        assignAllMenusToRole(adminRole.getId(), allMenus);

        // 普通用户拥有首页、工作台和个人中心权限
        assignMenusToRole(userRole.getId(), "/home");
        assignMenusToRole(userRole.getId(), "/analysis");
        assignMenusToRole(userRole.getId(), "/profile");

        // 访客只有首页、工作台查看权限
        assignMenusToRole(userRole.getId(), "/home");
        assignMenusToRole(guestRole.getId(), "/analysis");

        log.info("角色菜单权限初始化完成");
    }

    private Role getRoleByCode(String roleCode, String roleName) {
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new RuntimeException(roleName + "角色不存在"));
    }

    private void assignAllMenusToRole(Long roleId, List<Menu> menus) {
        for (Menu menu : menus) {
            createRoleMenu(roleId, String.valueOf(menu.getId()));
        }
    }

    private void assignMenusToRole(Long roleId, String menuPath) {
        Menu menu = menuRepository.findByPath(menuPath);
        if (menu != null) {
            createRoleMenu(roleId, String.valueOf(menu.getId()));
        }
    }

    private void createRoleMenu(Long roleId, String menuCode) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuCode(menuCode);
        roleMenuRepository.save(roleMenu);
    }

}