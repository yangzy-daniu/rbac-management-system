package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByUsername(String username);

    // 根据多个角色代码查找用户
    List<User> findByRoleCodeIn(List<String> roleCodes);

    // 根据部门和职位查找用户
    List<User> findByDepartmentAndPosition(String department, String position);
}