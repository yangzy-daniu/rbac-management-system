package com.example.demo.controller;

import com.example.demo.annotation.OperationLog;
import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repo;

    private final AuthService authService;

    private final UserService userService;

    @GetMapping("/getAllUsers")
    public List<User> all() {
        List<User> users = repo.findAll();
        return users;
    }

    // 分页查询用户
    @OperationLog(module = "用户管理", type = "CREATE", value = "查询用户")
    @GetMapping("/getUsersPage")
    public Page<User> getUsersByPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return userService.getUsersByPage(page, size, keyword, role);
    }

    @PostMapping("/addUser")
    public User create(@RequestBody User u) {
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        return repo.save(u);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = repo.findById(id).orElseThrow();
        existingUser.setName(user.getName());
        existingUser.setRoleCode(user.getRoleCode());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(user.getPassword());
        }
        existingUser.setUpdateTime(LocalDateTime.now()); // 更新时间
        return repo.save(existingUser);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }

    // 批量删除用户
    @PostMapping("/batch-delete")
    public void batchDelete(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
    }

    // 检查用户名是否存在
    @GetMapping("/check-username")
    public Map<String, Boolean> checkUsername(
            @RequestParam String username,
            @RequestParam(required = false) Long excludeId) {
        boolean exists;
        if (excludeId != null) {
            exists = userService.isUsernameExists(username, excludeId);
        } else {
            exists = userService.isUsernameExists(username);
        }
        return Map.of("exists", exists);
    }

    @GetMapping("/info")
    public UserInfoDTO getUserInfo(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);
        return userService.getUserInfo(userId);
    }

    @PutMapping("/info")
    public UserInfoDTO updateUserInfo(@RequestHeader("Authorization") String token,
                                      @RequestBody User user) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);
        User updatedUser = userService.updateUserInfo(userId, user);
        return userService.getUserInfo(updatedUser.getId());
    }
}