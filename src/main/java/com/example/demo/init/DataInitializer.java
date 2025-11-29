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
        homeMenu.setPath("/home");
        homeMenu.setIcon("House");
        homeMenu.setComponent("Home");
        homeMenu.setSort(1);
        homeMenu.setType(1); // 菜单
        homeMenu.setAvailable(true);
        menuRepository.save(homeMenu);

        // 一级菜单 - 工作台
        Menu workstationMenu = new Menu();
        workstationMenu.setName("工作台");
        workstationMenu.setPath("/workstation");
        workstationMenu.setIcon("Monitor");
        workstationMenu.setComponent("Workstation");
        workstationMenu.setSort(2);
        workstationMenu.setType(1); // 菜单
        workstationMenu.setAvailable(true);
        menuRepository.save(workstationMenu);

        // 一级菜单 - 个人中心
        Menu profileMenu = new Menu();
        profileMenu.setName("个人中心");
        profileMenu.setPath("/profile");
        profileMenu.setIcon("User");
        profileMenu.setComponent("Profile");
        profileMenu.setSort(3);
        profileMenu.setType(1); // 菜单
        profileMenu.setAvailable(true);
        menuRepository.save(profileMenu);

        // 一级菜单 - 数据分析
        Menu analysisMenu = new Menu();
        analysisMenu.setName("数据分析");
        analysisMenu.setPath("/analysis");
        analysisMenu.setIcon("Histogram");
        analysisMenu.setSort(4);
        analysisMenu.setType(0); // 目录
        analysisMenu.setAvailable(true);
        analysisMenu = menuRepository.save(analysisMenu);

        // 二级菜单 - 数据概览
        Menu dashboardMenu = new Menu();
        dashboardMenu.setName("数据概览");
        dashboardMenu.setPath("/dashboard");
        dashboardMenu.setIcon("DataAnalysis");
        dashboardMenu.setComponent("Dashboard");
        dashboardMenu.setSort(1);
        dashboardMenu.setType(1); // 菜单
        dashboardMenu.setParentId(analysisMenu.getId());
        dashboardMenu.setAvailable(true);
        menuRepository.save(dashboardMenu);

        // 二级菜单 - 系统分析
        Menu sysAMenu = new Menu();
        sysAMenu.setName("系统分析");
        sysAMenu.setPath("/sysA");
        sysAMenu.setIcon("Histogram");
        sysAMenu.setComponent("SystemAnalysis");
        sysAMenu.setSort(2);
        sysAMenu.setType(1); // 菜单
        sysAMenu.setParentId(analysisMenu.getId());
        sysAMenu.setAvailable(true);
        menuRepository.save(sysAMenu);

        // 二级菜单 - 用户分析
        Menu userAMenu = new Menu();
        userAMenu.setName("用户分析");
        userAMenu.setPath("/userA");
        userAMenu.setIcon("PieChart");
        userAMenu.setComponent("UserAnalysis");
        userAMenu.setSort(3);
        userAMenu.setType(1); // 菜单
        userAMenu.setParentId(analysisMenu.getId());
        userAMenu.setAvailable(true);
        menuRepository.save(userAMenu);

        // 一级菜单 - 订单管理
        Menu orderMenu = new Menu();
        orderMenu.setName("订单管理");
        orderMenu.setPath("/order");
        orderMenu.setIcon("ShoppingCart");
        orderMenu.setComponent("OrderManagement");
        orderMenu.setSort(5);
        orderMenu.setType(1); // 菜单
        orderMenu.setAvailable(true);
        menuRepository.save(orderMenu);

        // 一级菜单 - 系统管理
        Menu systemMenu = new Menu();
        systemMenu.setName("系统管理");
        systemMenu.setPath("/sysManagement");
        systemMenu.setIcon("Setting");
        systemMenu.setSort(6);
        systemMenu.setType(0); // 目录
        systemMenu.setAvailable(true);
        systemMenu = menuRepository.save(systemMenu);

        // 二级菜单 - 用户管理
        Menu userMenu = new Menu();
        userMenu.setName("用户管理");
        userMenu.setPath("/user");
        userMenu.setIcon("User");
        userMenu.setComponent("UserManagement");
        userMenu.setSort(1);
        userMenu.setType(1); // 菜单
        userMenu.setParentId(systemMenu.getId());
        userMenu.setAvailable(true);
        menuRepository.save(userMenu);

        // 二级菜单 - 角色管理
        Menu roleMenu = new Menu();
        roleMenu.setName("角色管理");
        roleMenu.setPath("/role");
        roleMenu.setIcon("Key");
        roleMenu.setComponent("RoleManagement");
        roleMenu.setSort(2);
        roleMenu.setType(1); // 菜单
        roleMenu.setParentId(systemMenu.getId());
        roleMenu.setAvailable(true);
        menuRepository.save(roleMenu);

        // 二级菜单 - 菜单管理
        Menu menuManager = new Menu();
        menuManager.setName("菜单管理");
        menuManager.setPath("/menu");
        menuManager.setIcon("Menu");
        menuManager.setComponent("MenuManagement");
        menuManager.setSort(3);
        menuManager.setType(1); // 菜单
        menuManager.setParentId(systemMenu.getId());
        menuManager.setAvailable(true);
        menuRepository.save(menuManager);

        // 二级菜单 - 系统设置
        Menu systemSetMenu = new Menu();
        systemSetMenu.setName("系统设置");
        systemSetMenu.setPath("/system");
        systemSetMenu.setIcon("Tools");
        systemSetMenu.setComponent("SystemSettings");
        systemSetMenu.setSort(4);
        systemSetMenu.setType(1); // 菜单
        systemSetMenu.setParentId(systemMenu.getId());
        systemSetMenu.setAvailable(true);
        menuRepository.save(systemSetMenu);

        // 一级菜单 - 日志管理
        Menu logMenu = new Menu();
        logMenu.setName("日志管理");
        logMenu.setPath("/logManagement");
        logMenu.setIcon("Notebook");
        logMenu.setSort(7);
        logMenu.setType(0); // 目录
        logMenu.setAvailable(true);
        logMenu = menuRepository.save(logMenu);

        // 二级菜单 - 操作日志
        Menu auditLogsMenu = new Menu();
        auditLogsMenu.setName("操作日志");
        auditLogsMenu.setPath("/auditLogs");
        auditLogsMenu.setIcon("Operation");
        auditLogsMenu.setComponent("OperationLog");
        auditLogsMenu.setSort(1);
        auditLogsMenu.setType(1); // 菜单
        auditLogsMenu.setParentId(logMenu.getId());
        auditLogsMenu.setAvailable(true);
        menuRepository.save(auditLogsMenu);

        // 二级菜单 - 系统日志
        Menu sysLogsMenu = new Menu();
        sysLogsMenu.setName("系统日志");
        sysLogsMenu.setPath("/sysLogs");
        sysLogsMenu.setIcon("DataBoard");
        sysLogsMenu.setComponent("SystemLogs");
        sysLogsMenu.setSort(1);
        sysLogsMenu.setType(1); // 菜单
        sysLogsMenu.setParentId(logMenu.getId());
        sysLogsMenu.setAvailable(true);
        menuRepository.save(sysLogsMenu);

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
        adminUser.setRoleCode("admin");
        adminUser.setCreateTime(LocalDateTime.now());
        adminUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(adminUser);

        User superUser = new User();
        superUser.setUsername("superAdmin");
        superUser.setPassword(passwordEncoder.encode("123456"));
        superUser.setName("超级管理员");
        superUser.setRoleCode("super");
        superUser.setCreateTime(LocalDateTime.now());
        superUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(superUser);

        User normalUser = new User();
        normalUser.setUsername("user");
        normalUser.setPassword(passwordEncoder.encode("123456"));
        normalUser.setName("张三");
        normalUser.setRoleCode("user");
        normalUser.setCreateTime(LocalDateTime.now());
        normalUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(normalUser);

        User guestUser = new User();
        guestUser.setUsername("guest");
        guestUser.setPassword(passwordEncoder.encode("123456"));
        guestUser.setName("李四");
        guestUser.setRoleCode("guest");
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

        // 普通用户拥有首页、工作台、个人中心、数据分析权限
        assignMenusToRole(userRole.getId(), "/home");
        assignMenusToRole(userRole.getId(), "/workstation");
        assignMenusToRole(userRole.getId(), "/profile");
        assignMenusToRole(userRole.getId(), "/analysis");
        assignMenusToRole(userRole.getId(), "/dashboard");
        assignMenusToRole(userRole.getId(), "/sysA");
        assignMenusToRole(userRole.getId(), "/userA");

        // 访客只有首页、工作台、个人中心查看权限
        assignMenusToRole(userRole.getId(), "/home");
        assignMenusToRole(guestRole.getId(), "/workstation");
        assignMenusToRole(guestRole.getId(), "/profile");

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