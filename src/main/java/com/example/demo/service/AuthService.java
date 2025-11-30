package com.example.demo.service;

import com.example.demo.common.security.JwtTokenProvider;
import com.example.demo.common.security.TokenBlacklist;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final TokenBlacklist tokenBlacklist;

    private final SystemMonitorService systemMonitorService;

//    private final RedisTemplate<String, String> redisTemplate; // 需要添加Redis依赖和配置


    public LoginResponse login(LoginRequest request) {
        LoginResponse response = new LoginResponse();

        // 查找用户
        User user = userRepository.findByUsername(request.getUsername());
        // 先判断用户是否存在
        if (user == null) {
            response.setSuccess(false);
            response.setMessage("用户名不存在");
            return response;
        }
        // 查询用户角色
        Optional<Role> role = roleRepository.findByCode(user.getRoleCode());

        // 使用 passwordEncoder.matches 验证密码
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.setSuccess(false);
            response.setMessage("密码错误");
            return response;
        }

        // 记录在线用户
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        if (httpRequest != null) {
            String sessionId = httpRequest.getSession().getId();
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            systemMonitorService.userLogin(user.getId(), user.getUsername(),
                    sessionId, ipAddress, userAgent);

            log.info("用户 {} 登录成功，会话ID已存储: {}", user.getUsername(), sessionId);
        }

        // 使用 JWT 生成 token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), 1L);

        // 构建用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setName(user.getName());
        userInfo.setRoleName(role.orElseThrow().getCode());

        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setToken(token);
        response.setUser(userInfo);

        return response;
    }

    public void logout(String token) {
        if (token == null) return;

        // 处理Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 加入黑名单，有效期24小时
        tokenBlacklist.add(token, 24 * 60 * 60 * 1000L);

        // 移除在线用户记录
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        if (httpRequest != null) {
            String sessionId = httpRequest.getSession().getId();
            systemMonitorService.userLogout(sessionId);
        }
    }

    public boolean validateToken(String token) {
        if (token != null && tokenBlacklist.contains(token)) {
            return false;
        }
        return jwtTokenProvider.validateToken(token);
    }

    public Long getUserIdByToken(String token) {
        if (token != null && tokenBlacklist.contains(token)) {
            return null;
        }
        return jwtTokenProvider.getUserIdFromJWT(token);
    }



//    public void logout(String token) {
//        // 处理Bearer前缀
//        if (token != null && token.startsWith("Bearer ")) {
//            token = token.substring(7);
//        }
//
//        if (token != null && jwtTokenProvider.validateToken(token)) {
//            // 设置黑名单有效期为24小时（通常比JWT token的实际有效期短）
//            // 这样即使token本身还有更长的有效期，在黑名单中24小时后也会自动清理
//            long blacklistTtl = 24 * 60 * 60 * 1000L; // 24小时
//            tokenBlacklist.add(token, blacklistTtl);
//        }
//    }


//    public boolean validateToken(String token) {
//        // 使用 JWT 验证 token
//        return jwtTokenProvider.validateToken(token);
//    }

//    public boolean validateToken(String token) {
//        // 检查token是否在黑名单中
//        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
//            return false;
//        }
//        // 使用 JWT 验证 token
//        return jwtTokenProvider.validateToken(token);
//    }


//    public Long getUserIdByToken(String token) {
//        // 使用 JWT 解析用户ID
//        return jwtTokenProvider.getUserIdFromJWT(token);
//    }

//    public Long getUserIdByToken(String token) {
//        // 检查token是否在黑名单中
//        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
//            return null;
//        }
//        // 使用 JWT 解析用户ID
//        return jwtTokenProvider.getUserIdFromJWT(token);
//    }

//    public String getUserRoleByToken(String token) {
//        Long userId = getUserIdByToken(token);
//        if (userId == null) {
//            return null;
//        }
//
//        User user = userRepository.findById(userId).orElse(null);
//        return user != null ? user.getRoleCode() : null;
//    }
    public String getUserRoleByToken(String token) {
//        // 检查token是否在黑名单中
//        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
//            return null;
//        }
        if (token != null && tokenBlacklist.contains(token)) {
            return null;
        }

        Long userId = getUserIdByToken(token);
        if (userId == null) {
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getRoleCode() : null;
    }

    // 获取当前HTTP请求
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    // 获取客户端IP
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}