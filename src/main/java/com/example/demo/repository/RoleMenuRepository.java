package com.example.demo.repository;

import com.example.demo.entity.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleMenuRepository extends JpaRepository<RoleMenu, Long> {

    // 根据角色ID查找菜单权限
    List<RoleMenu> findByRoleId(Long roleId);

    // 根据角色ID删除菜单权限
    @Modifying
    @Query("DELETE FROM RoleMenu rm WHERE rm.roleId = :roleId")
    void deleteByRoleId(Long roleId);

    // 根据角色ID列表批量删除菜单权限
    @Modifying
    @Query("DELETE FROM RoleMenu rm WHERE rm.roleId IN :roleIds")
    void deleteByRoleIdIn(List<Long> roleIds);

    // 根据角色ID查找菜单代码列表
    @Query("SELECT rm.menuCode FROM RoleMenu rm WHERE rm.roleId = :roleId")
    List<String> findMenuCodesByRoleId(Long roleId);

    // 根据角色ID列表查询所有角色菜单关联
    List<RoleMenu> findAllByRoleIdIn(List<Long> roleIds);

    // 可选：使用原生SQL进行批量插入（性能更好）
    @Modifying
    @Query(value = "INSERT INTO role_menus (role_id, menu_code) VALUES (:roleId, :menuCode)", nativeQuery = true)
    void saveRoleMenu(@Param("roleId") Long roleId, @Param("menuCode") String menuCode);
}