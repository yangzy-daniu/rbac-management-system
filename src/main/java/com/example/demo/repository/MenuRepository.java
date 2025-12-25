package com.example.demo.repository;

import com.example.demo.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByParentIdOrderBySortAsc(Long parentId);

    List<Menu> findByIdIn(List<Long> menuIds);

    List<Menu> findAllByOrderBySortAsc();

    List<Menu> findByNameContainingIgnoreCaseOrderBySortAsc(String name);

    boolean existsByPath(String path);

    boolean existsByPathAndIdNot(String path, Long id);

    Menu findByPath(String path);

}