package com.example.demo.interceptor;

import com.example.demo.service.SystemMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserActivityInterceptor implements HandlerInterceptor {

    private final SystemMonitorService systemMonitorService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 排除静态资源和API文档等
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/css/") || requestURI.startsWith("/js/") ||
                requestURI.startsWith("/api-docs") || requestURI.startsWith("/swagger")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            // 更新用户最后访问时间
            systemMonitorService.updateUserAccessTime(sessionId);
        }

        return true;
    }
}