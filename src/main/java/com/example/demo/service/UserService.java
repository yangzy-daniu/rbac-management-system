package com.example.demo.service;

import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @Resource
    private UserRepository userRepository;

    public UserInfoDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        return new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                user.getPassword(),
                user.getEmail(),
                user.getPhone(),
                user.getNickname(),
                user.getDepartment(),
                user.getPosition(),
                user.getEnabled(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }

    public User updateUserInfo(Long userId, User updatedUser) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        if (updatedUser.getName() != null) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(updatedUser.getPassword());
        }

        return userRepository.save(user);
    }

    // 分页查询用户
// 在 UserService 中添加方法
    public Page<User> getUsersByPage(int page, int size, String keyword, String role) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (StringUtils.hasText(keyword) || StringUtils.hasText(role)) {
            // 构建查询条件
            Specification<User> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(keyword)) {
                    Predicate usernamePredicate = cb.like(root.get("username"), "%" + keyword + "%");
                    Predicate namePredicate = cb.like(root.get("name"), "%" + keyword + "%");
                    predicates.add(cb.or(usernamePredicate, namePredicate));
                }

                if (StringUtils.hasText(role)) {
                    predicates.add(cb.equal(root.get("role"), role));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };
            return userRepository.findAll(spec, pageable);
        }

        return userRepository.findAll(pageable);
    }

    // 批量删除用户
    public void deleteUsers(List<Long> ids) {
        userRepository.deleteAllById(ids);
    }

    // 检查用户名是否存在
    public boolean isUsernameExists(String username) {
        return userRepository.findByUsername(username) != null;
    }

    // 检查用户名是否存在（排除指定ID）
    public boolean isUsernameExists(String username, Long excludeId) {
        User user = userRepository.findByUsername(username);
        return user != null && !user.getId().equals(excludeId);
    }
}