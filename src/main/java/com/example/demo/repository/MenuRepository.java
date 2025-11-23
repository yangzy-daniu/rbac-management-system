package com.example.demo.repository;

import com.example.demo.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByParentIdOrderBySortAsc(Long parentId);

//    List<Menu> findByAvailableTrueOrderBySortAsc();
//    List<Menu> findByParentCodeIsNullOrderBySortOrder();

    @Query("SELECT m FROM Menu m WHERE m.parentId IS NULL ORDER BY m.sort ASC")
    List<Menu> findRootMenus();

//    List<Menu> findByTypeOrderBySortAsc(Integer type);

    // 新增查询方法
    List<Menu> findAllByOrderBySortAsc();

    List<Menu> findByNameContainingIgnoreCaseOrderBySortAsc(String name);

    boolean existsByPath(String path);

    boolean existsByPathAndIdNot(String path, Long id);

    // 查询指定父菜单下的最大排序值
    @Query("SELECT COALESCE(MAX(m.sort), 0) FROM Menu m WHERE m.parentId = :parentId")
    Integer findMaxSortByParentId(Long parentId);
}