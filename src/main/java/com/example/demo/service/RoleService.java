package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.entity.RoleMenu;
import com.example.demo.repository.MenuRepository;
import com.example.demo.repository.RoleMenuRepository;
import com.example.demo.repository.RoleRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.example.demo.entity.Menu;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private RoleMenuRepository roleMenuRepository;

    @Resource
    private MenuRepository menuRepository;

    // 分页查询角色
    public Page<Role> getRolesByPage(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Role> rolePage;
        if (StringUtils.hasText(keyword)) {
            Specification<Role> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
                predicates.add(cb.like(root.get("code"), "%" + keyword + "%"));
                predicates.add(cb.like(root.get("description"), "%" + keyword + "%"));
                return cb.or(predicates.toArray(new Predicate[0]));
            };
            rolePage = roleRepository.findAll(spec, pageable);
        } else {
            rolePage = roleRepository.findAll(pageable);
        }

        // 为分页结果设置菜单权限
        setMenuPermissionsForRoles(rolePage.getContent());

        return rolePage;
    }

    // 如果菜单权限存储的是菜单ID而不是代码
    private void setMenuPermissionsForRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        // 批量查询所有角色的菜单权限
        List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());

        // 查询这些角色的菜单权限
        List<RoleMenu> allRoleMenus = roleMenuRepository.findAllByRoleIdIn(roleIds);

        // 获取所有涉及的菜单ID
        List<Long> menuIds = allRoleMenus.stream()
                .map(RoleMenu::getMenuCode) // 假设这里存储的是菜单ID
                .distinct()
                .collect(Collectors.toList());

        // 批量查询菜单名称
        Map<Long, String> menuNameMap = menuRepository.findByIdIn(menuIds)
                .stream()
                .collect(Collectors.toMap(
                        Menu::getId,
                        Menu::getName
                ));

        // 按角色ID分组菜单代码
        Map<Long, List<Long>> menuPermissionsMap = allRoleMenus.stream()
                .collect(Collectors.groupingBy(
                        RoleMenu::getRoleId,
                        Collectors.mapping(
                                RoleMenu::getMenuCode,  // 直接获取菜单代码
                                Collectors.toList()
                        )
                ));
        // 为每个角色设置菜单权限（现在包含的是菜单名称）
        for (Role role : roles) {
            List<Long> permissions = menuPermissionsMap.get(role.getId());
            role.setMenuPermissions(permissions != null ? permissions : new ArrayList<>());
        }
    }

    private void saveRoleMenus(Long roleId, List<Long> menuPermissions) {
        if (menuPermissions == null || menuPermissions.isEmpty()) {
            return;
        }

        // 先删除该角色原有的菜单权限
        roleMenuRepository.deleteByRoleId(roleId);

        // 保存新的菜单权限
        List<RoleMenu> roleMenus = menuPermissions.stream()
                .map(menuCode -> {
                    RoleMenu roleMenu = new RoleMenu();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setMenuCode(menuCode);
                    return roleMenu;
                })
                .collect(Collectors.toList());

        roleMenuRepository.saveAll(roleMenus);
    }

    // 创建角色
    @Transactional
    public Role createRole(Role role) {
        if (roleRepository.existsByCode(role.getCode())) {
            throw new RuntimeException("角色代码已存在");
        }

        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());

        // 先保存角色基本信息
        Role savedRole = roleRepository.save(role);

        // 保存菜单权限
        saveRoleMenus(savedRole.getId(), role.getMenuPermissions());

        return savedRole;
    }

    // 更新角色
    @Transactional
    public Role updateRole(Long id, Role role) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        if (roleRepository.existsByCodeAndIdNot(role.getCode(), id)) {
            throw new RuntimeException("角色代码已存在");
        }

        existingRole.setName(role.getName());
        existingRole.setCode(role.getCode());
        existingRole.setDescription(role.getDescription());
        existingRole.setUpdateTime(LocalDateTime.now());

        // 先更新角色基本信息
        Role updatedRole = roleRepository.save(existingRole);

        // 更新菜单权限（先删除旧的，再保存新的）
        saveRoleMenus(id, role.getMenuPermissions());

        return updatedRole;
    }

    // 删除角色
    @Transactional
    public void deleteRole(Long id) {
        // 先删除菜单权限关联
        roleMenuRepository.deleteByRoleId(id);
        // 再删除角色
        roleRepository.deleteById(id);
    }

    // 批量删除角色
    @Transactional
    public void deleteRoles(List<Long> ids) {
        // 先删除菜单权限关联
        roleMenuRepository.deleteByRoleIdIn(ids);
        // 再批量删除角色
        roleRepository.deleteAllById(ids);
    }

    // 检查角色代码是否存在
    public boolean isCodeExists(String code) {
        return roleRepository.existsByCode(code);
    }

    // 检查角色代码是否存在（排除指定ID）
    public boolean isCodeExists(String code, Long excludeId) {
        return roleRepository.existsByCodeAndIdNot(code, excludeId);
    }

    // 根据角色ID获取角色
    public Optional<Role> getRoleById(Long id) {
        Optional<Role> roleOptional = roleRepository.findById(id);

        if (roleOptional.isPresent()) {
            Role role = roleOptional.get();
            // 查询并设置菜单权限
            List<Long> menuPermissions = roleMenuRepository.findMenuCodesByRoleId(id);
            role.setMenuPermissions(menuPermissions);
        }

        return roleOptional;
    }

    // 在 RoleService.java 中添加方法
    private List<Long> getAuthorizedMenuIdsByRole(String roleCode) {
        // 根据角色代码找到角色ID
        Optional<Role> role = roleRepository.findByCode(roleCode);
        if (role == null) {
            return new ArrayList<>();
        }

        // 根据角色ID查询菜单权限
        List<RoleMenu> roleMenus = roleMenuRepository.findByRoleId(role.orElseThrow().getId());

        return roleMenus.stream()
                .map(roleMenu -> Long.valueOf(roleMenu.getMenuCode()))
                .collect(Collectors.toList());
    }

}