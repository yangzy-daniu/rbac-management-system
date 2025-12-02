package com.example.demo.service;

import com.example.demo.common.security.JwtTokenProvider;
import com.example.demo.common.security.TokenBlacklist;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    private final EmailService emailService;

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

        if (!user.getEnabled()) {
            response.setSuccess(false);
            response.setMessage("用户已被禁用");
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

    // 注册方法
    // 修改register方法中的这部分：
    public LoginResponse register(RegisterRequest request) {
        LoginResponse response = new LoginResponse();

        // 验证用户名是否已存在
        if (userRepository.findByUsername(request.getUsername()) != null) {
            response.setSuccess(false);
            response.setMessage("用户名已存在");
            return response;
        }

        // 验证邮箱是否已存在（如果需要）
        if (StringUtils.hasText(request.getEmail())) {
            User existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser != null) {
                response.setSuccess(false);
                response.setMessage("邮箱已被注册");
                return response;
            }
        }

        // 验证两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            response.setSuccess(false);
            response.setMessage("两次输入的密码不一致");
            return response;
        }

        // 创建新用户
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setName(request.getUsername()); // 默认用用户名作为姓名
        newUser.setNickname(request.getUsername()); // 默认用用户名作为昵称
        newUser.setRoleCode("user"); // 默认角色
        newUser.setEnabled(true);
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        userRepository.save(newUser);

        // 注册成功后自动生成token和用户信息，模拟登录
        Optional<Role> role = roleRepository.findByCode("user");

        // 生成JWT token
        String token = jwtTokenProvider.generateToken(newUser.getUsername(), newUser.getId(), 1L);

        // 构建用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(newUser.getId());
        userInfo.setUsername(newUser.getUsername());
        userInfo.setName(newUser.getName());
        userInfo.setRoleName(role.isPresent() ? role.get().getCode() : "user");

        response.setSuccess(true);
        response.setMessage("注册成功");
        response.setToken(token);
        response.setUser(userInfo);

        return response;
    }

    // 忘记密码方法
    public LoginResponse forgotPassword(ForgotPasswordRequest request) {
        LoginResponse response = new LoginResponse();
        // 根据邮箱查找用户
        // 这里简化处理，实际项目中需要验证邮箱是否存在
        if (!StringUtils.hasText(request.getEmail())) {
            response.setSuccess(false);
            response.setMessage("请输入邮箱");
            return response;
        }

        // 生成重置密码的token
        String resetToken = UUID.randomUUID().toString();

        // 实际项目中应该：
        // 1. 保存resetToken到数据库（设置过期时间）
        // 2. 发送重置密码邮件
        // 3. 返回成功信息

        // 这里模拟发送邮件
        if (emailService != null) {
            String resetLink = "http://your-domain.com/reset-password?token=" + resetToken;
            emailService.sendResetPasswordEmail(request.getEmail(), resetLink);
        }
        response.setSuccess(true);
        response.setMessage("重置密码邮件已发送，请查收邮箱");
        return response;
    }

    // 重置密码方法（可以另外添加）
    public LoginResponse resetPassword(String token, String newPassword) {
        LoginResponse response = new LoginResponse();
        // 验证token是否有效
        // 验证新密码强度
        // 更新用户密码
        response.setSuccess(true);
        response.setMessage("密码重置成功");
        return response;
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