package com.example.demo.filter;

import com.example.demo.common.context.TenantContext;
import com.example.demo.common.security.CustomUserDetails;
import com.example.demo.common.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // 从JWT中解析用户信息
                String username = jwtTokenProvider.getUsernameFromJWT(jwt);
                Long userId = jwtTokenProvider.getUserIdFromJWT(jwt);
                Long tenantId = jwtTokenProvider.getTenantIdFromJWT(jwt);

                // 设置租户上下文
                TenantContext.setTenantId(tenantId);

                // 从JWT中获取权限信息
                List<String> roles = jwtTokenProvider.getRolesFromJWT(jwt);
                List<GrantedAuthority> authorities = convertToAuthorities(roles);

                // 创建认证信息
                CustomUserDetails userDetails = new CustomUserDetails(userId, username, tenantId,  null);

                // 创建认证令牌
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT认证成功: username={}, userId={}", username, userId);
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    /**
     * 将角色字符串列表转换为GrantedAuthority列表
     */
    private List<GrantedAuthority> convertToAuthorities(List<String> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                // 确保角色名称以ROLE_开头（Spring Security规范）
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(roleName));
            }
        }
        return authorities;
    }
}