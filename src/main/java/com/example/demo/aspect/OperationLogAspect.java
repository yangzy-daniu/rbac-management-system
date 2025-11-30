package com.example.demo.aspect;

import com.example.demo.entity.OperationLog;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.example.demo.common.context.SecurityContext.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Pointcut("@annotation(com.example.demo.annotation.OperationLog)")
    public void operationLogPointcut() {}

//    @Pointcut("execution(* com.example.demo.controller..*.*(..))")
    // 使用 !execution() 排除特定类
    @Pointcut("execution(* com.example.demo.controller..*.*(..)) && " +
            "!execution(* com.example.demo.controller.OperationLogController.*(..)) && " +
            "!execution(* com.example.demo.controller.OnlineUserController.*(..)) && " +
            "!execution(* com.example.demo.controller.SystemMonitorController.*(..)) && " +
            "!execution(* com.example.demo.controller.StatsController.*(..))")
    public void controllerPointcut() {}

    @Around("operationLogPointcut() || controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息进行判断
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 判断是否是不需要记录的请求
            if (isExcludeRequest(request, joinPoint)) {
                return joinPoint.proceed();
            }
        }
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            try {
                saveOperationLog(joinPoint, result, error, executionTime);
            } catch (Exception e) {
                log.error("保存操作日志异常", e);
            }
        }

        return result;
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint, Object result, Throwable error, long executionTime) {
        OperationLog operationLog = new OperationLog();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setOperatorIp(getClientIp(request));
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setUserAgent(request.getHeader("User-Agent"));
            operationLog.setAccessTime(LocalDateTime.now());

            // 记录请求参数（避免记录敏感信息）
            operationLog.setRequestParams(getRequestParams(request, joinPoint));
        }

        // 设置操作信息
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        operationLog.setModule(parseModule(className));
        operationLog.setType(parseOperationType(methodName));
        // 使用新的解析方法生成友好的操作描述
        operationLog.setOperation(parseOperation(className, methodName, joinPoint.getArgs()));

        // 设置操作者和租户信息
        operationLog.setOperator(getCurrentUsername());
        operationLog.setOperatorId(getCurrentUserId());
        operationLog.setTenantId(getCurrentTenantId());

        // 设置结果信息
        operationLog.setResult(error == null ? "SUCCESS" : "FAILURE");
        operationLog.setErrorMessage(error != null ? error.getMessage() : null);
        operationLog.setExecutionTime(executionTime);
        // 设置HTTP状态码（模拟，实际可以从响应中获取）
        operationLog.setStatusCode(error == null ? 200 : 500);
        operationLog.setResponseData(extractResponseData(result, error));

        // 异步保存日志
        operationLogService.saveLog(operationLog);
    }

    // 获取请求参数（过滤敏感信息）
    private String getRequestParams(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        try {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (parameterMap.isEmpty()) {
                return null;
            }

            Map<String, Object> filteredParams = new HashMap<>();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();

                // 过滤敏感字段
                if (isSensitiveField(key)) {
                    filteredParams.put(key, "***");
                } else {
                    filteredParams.put(key, values.length == 1 ? values[0] : Arrays.toString(values));
                }
            }

            return filteredParams.toString();
        } catch (Exception e) {
            log.warn("获取请求参数失败", e);
            return null;
        }
    }

    // 判断是否为敏感字段
    private boolean isSensitiveField(String fieldName) {
        String[] sensitiveFields = {"password", "pwd", "token", "authorization", "secret", "key"};
        for (String sensitive : sensitiveFields) {
            if (fieldName.toLowerCase().contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

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

    private String parseModule(String className) {
        if (className.contains("Order")) return "订单管理";
        if (className.contains("User")) return "用户管理";
        if (className.contains("Role")) return "角色管理";
        if (className.contains("Menu")) return "菜单管理";
        if (className.contains("System")) return "系统设置";
        if (className.contains("Profile")) return "个人中心";
        if (className.contains("Auth")) return "登录系统";
        return "其他";
    }

    private String parseOperationType(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("add")) return "CREATE";
        if (methodName.startsWith("update") || methodName.startsWith("edit")) return "UPDATE";
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) return "DELETE";
        if (methodName.startsWith("get") || methodName.startsWith("query")) return "QUERY";
        if (methodName.startsWith("login")) return "LOGIN";
        return "OTHER";
    }

    /**
     * 生成友好的操作描述
     * 根据类名、方法名和参数生成类似截图中的描述
     */
    private String parseOperation(String className, String methodName, Object[] args) {
        String module = parseModule(className);

        // 根据模块和方法名生成具体的操作描述
        switch (module) {
            case "用户管理":
                return parseUserOperation(methodName, args);
            case "角色管理":
                return parseRoleOperation(methodName, args);
            case "菜单管理":
                return parseMenuOperation(methodName, args);
            case "登录系统":
                return parseAuthOperation(methodName, args);
            case "订单管理":
                return parseOrderOperation(methodName, args);
            default:
                return parseDefaultOperation(methodName, args);
        }
    }

    /**
     * 用户管理相关操作描述
     */
    private String parseUserOperation(String methodName, Object[] args) {
        if (methodName.contains("update") || methodName.contains("edit")) {
            // 尝试从参数中提取用户名
            String userName = extractUserNameFromArgs(args);
            if (userName != null) {
                return "修改了用户\"" + userName + "\"的权限";
            }
            return "修改了用户权限";
        } else if (methodName.contains("create") || methodName.contains("add")) {
            String userName = extractUserNameFromArgs(args);
            if (userName != null) {
                return "创建了新用户\"" + userName + "\"";
            }
            return "创建了新用户";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "删除了用户";
        } else if (methodName.contains("get") || methodName.contains("query")) {
            return "查询了用户信息";
        }
        return "执行了用户管理操作";
    }

    /**
     * 角色管理相关操作描述
     */
    private String parseRoleOperation(String methodName, Object[] args) {
        if (methodName.contains("create") || methodName.contains("add")) {
            // 尝试从参数中提取角色名
            String roleName = extractRoleNameFromArgs(args);
            if (roleName != null) {
                return "创建了新角色\"" + roleName + "\"";
            }
            return "创建了新角色";
        } else if (methodName.contains("update") || methodName.contains("edit")) {
            return "更新了角色信息";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "删除了角色";
        } else if (methodName.contains("get") || methodName.contains("query")) {
            return "查询了角色信息";
        }
        return "执行了角色管理操作";
    }

    /**
     * 系统管理相关操作描述
     */
    private String parseSystemOperation(String methodName, Object[] args) {
        if (methodName.contains("update") || methodName.contains("edit")) {
            if (methodName.toLowerCase().contains("menu")) {
                return "更新了系统菜单结构";
            }
            return "更新了系统配置";
        } else if (methodName.contains("get") || methodName.contains("query")) {
            return "查询了系统信息";
        }
        return "执行了系统管理操作";
    }

    /**
     * 登录认证相关操作描述
     */
    private String parseAuthOperation(String methodName, Object[] args) {
        if (methodName.contains("login")) {
            return "登录系统";
        } else if (methodName.contains("logout")) {
            return "退出系统";
        } else if (methodName.contains("register")) {
            return "用户注册";
        }
        return "执行了认证操作";
    }

    /**
     * 订单管理相关操作描述
     */
    private String parseOrderOperation(String methodName, Object[] args) {
        if (methodName.contains("create") || methodName.contains("add")) {
            return "创建了新订单";
        } else if (methodName.contains("update") || methodName.contains("edit")) {
            return "更新了订单信息";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "删除了订单";
        } else if (methodName.contains("get") || methodName.contains("query")) {
            return "查询了订单信息";
        }
        return "执行了订单管理操作";
    }

    /**
     * 订单管理相关操作描述
     */
    private String parseMenuOperation(String methodName, Object[] args) {
        if (methodName.contains("create") || methodName.contains("add")) {
            return "创建了新菜单";
        } else if (methodName.contains("update") || methodName.contains("edit")) {
            return "更新了菜单信息";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "删除了菜单";
        } else if (methodName.contains("get") || methodName.contains("query")) {
            return "查询了菜单信息";
        }
        return "执行了菜单管理操作";
    }

    /**
     * 默认操作描述
     */
    private String parseDefaultOperation(String methodName, Object[] args) {
        if (methodName.contains("create") || methodName.contains("add")) {
            return "创建了新数据";
        } else if (methodName.contains("update") || methodName.contains("edit")) {
            return "更新了数据";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "删除了数据";
        } else if (methodName.contains("get") || methodName.contains("query")) {
            return "查询了数据";
        }
        return "执行了操作";
    }

    /**
     * 从参数中提取用户名
     */
    private String extractUserNameFromArgs(Object[] args) {
        if (args == null) return null;

        for (Object arg : args) {
            if (arg == null) continue;

            // 如果是字符串且看起来像用户名
            if (arg instanceof String) {
                String str = (String) arg;
                if (str.length() > 0 && str.length() <= 50 && !str.contains("@")) {
                    return str;
                }
            }

            // 如果是用户对象，尝试获取用户名
            try {
                if (arg.getClass().getSimpleName().toLowerCase().contains("user")) {
                    var nameField = arg.getClass().getDeclaredField("username");
                    nameField.setAccessible(true);
                    Object nameValue = nameField.get(arg);
                    if (nameValue instanceof String) {
                        return (String) nameValue;
                    }
                }
            } catch (Exception e) {
                // 忽略反射异常
            }
        }
        return null;
    }

    /**
     * 从参数中提取角色名
     */
    private String extractRoleNameFromArgs(Object[] args) {
        if (args == null) return null;

        for (Object arg : args) {
            if (arg == null) continue;

            // 如果是字符串且看起来像角色名
            if (arg instanceof String) {
                String str = (String) arg;
                if (str.length() > 0 && str.length() <= 50) {
                    return str;
                }
            }

            // 如果是角色对象，尝试获取角色名
            try {
                if (arg.getClass().getSimpleName().toLowerCase().contains("role")) {
                    var nameField = arg.getClass().getDeclaredField("name");
                    nameField.setAccessible(true);
                    Object nameValue = nameField.get(arg);
                    if (nameValue instanceof String) {
                        return (String) nameValue;
                    }
                }
            } catch (Exception e) {
                // 忽略反射异常
            }
        }
        return null;
    }

    /**
     * 提取响应数据
     */
    private String extractResponseData(Object result, Throwable error) {
        if (error != null) {
            // 记录错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", error.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return toJsonString(errorResponse);
        }

        if (result == null) {
            return null;
        }

        try {
            // 如果已经是字符串，直接返回
            if (result instanceof String) {
                String response = (String) result;
                // 避免记录过长的响应
                return response.length() > 1000 ? response.substring(0, 1000) + "..." : response;
            }

            // 对于对象，转换为JSON字符串
            String jsonResult = toJsonString(result);
            // 限制响应数据长度，避免数据库字段过长
            return jsonResult.length() > 2000 ? jsonResult.substring(0, 2000) + "..." : jsonResult;

        } catch (Exception e) {
            log.warn("序列化响应数据失败: {}", e.getMessage());
            return "Unable to serialize response: " + result.toString();
        }
    }

    /**
     * 对象转JSON字符串
     */
    private String toJsonString(Object obj) {
        try {
            // 使用Jackson或Gson，这里用简单实现
            if (obj == null) return null;
            return obj.toString(); // 实际项目中建议使用JSON序列化工具
        } catch (Exception e) {
            return "Serialization error: " + e.getMessage();
        }
    }

    /**
     * 判断是否是不需要记录的请求
     */
    private boolean isExcludeRequest(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        String requestURI = request.getRequestURI();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 1. Layout加载菜单的请求
        boolean isLayoutMenu = className.equals("MenuController") &&
                methodName.equals("getUserMenus") &&
                requestURI.equals("/api/menus/tree");

        return isLayoutMenu;
    }
}