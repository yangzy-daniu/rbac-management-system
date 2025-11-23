package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    // 简单的token存储，生产环境请使用JWT
    private final Map<String, Long> tokenStore = new HashMap<>();

    public LoginResponse login(LoginRequest request) {
        LoginResponse response = new LoginResponse();

        // 查找用户
        User user = userRepository.findByUsername(request.getUsername());
        // 查询用户角色
        Optional<Role> role = roleRepository.findByCode(user.getRole());

//        if (user == null || !user.getPassword().equals(request.getPassword())) {
//            response.setSuccess(false);
//            response.setMessage("用户名或密码错误");
//            return response;
//        }
        // 使用 passwordEncoder.matches 验证密码
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.setSuccess(false);
            response.setMessage("用户名或密码错误");
            return response;
        }

        // 生成token
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, user.getId());

        // 构建用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setName(user.getName());
        userInfo.setRoleName(role.orElseThrow().getName());

        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setToken(token);
        response.setUser(userInfo);

        return response;
    }

    public boolean validateToken(String token) {
        return tokenStore.containsKey(token);
    }

    public Long getUserIdByToken(String token) {
        return tokenStore.get(token);
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }


    public String getUserRoleByToken(String token) {
        Long userId = tokenStore.get(token);
        if (userId == null) {
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getRole() : null;
    }
}