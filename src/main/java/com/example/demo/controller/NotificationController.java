package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.NotificationVO;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页查询通知列表
     */
    @GetMapping("/list")
    public Result<Page<NotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean important) {

        try {
            Page<NotificationVO> page = notificationService.getNotifications(pageNum, pageSize, status, type, important);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return Result.error("获取通知列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    public Result<NotificationVO> getNotificationDetail(@PathVariable Long id) {
        try {
            NotificationVO notification = notificationService.getNotificationDetail(id);
            return Result.success(notification);
        } catch (Exception e) {
            log.error("获取通知详情失败", e);
            return Result.error("获取通知详情失败: " + e.getMessage());
        }
    }

    /**
     * 标记为已读
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return Result.success();
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return Result.error("标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记为已读
     */
    @PutMapping("/batch-read")
    public Result<Void> batchMarkAsRead(@RequestBody List<Long> ids) {
        try {
            notificationService.batchMarkAsRead(ids);
            return Result.success();
        } catch (Exception e) {
            log.error("批量标记已读失败", e);
            return Result.error("批量标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 标记所有为已读
     */
    @PutMapping("/mark-all-read")
    public Result<Void> markAllAsRead() {
        try {
            notificationService.markAllAsRead();
            return Result.success();
        } catch (Exception e) {
            log.error("标记所有已读失败", e);
            return Result.error("标记所有已读失败: " + e.getMessage());
        }
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        try {
            Long count = notificationService.getUnreadCount();
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return Result.error("获取未读数量失败: " + e.getMessage());
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除通知失败", e);
            return Result.error("删除通知失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除通知
     */
    @DeleteMapping("/batch-delete")
    public Result<Void> batchDeleteNotifications(@RequestBody List<Long> ids) {
        try {
            notificationService.batchDeleteNotifications(ids);
            return Result.success();
        } catch (Exception e) {
            log.error("批量删除通知失败", e);
            return Result.error("批量删除通知失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作台通知（最近5条）
     */
    @GetMapping("/dashboard")
    public Result<List<NotificationVO>> getDashboardNotifications() {
        try {
            List<NotificationVO> notifications = notificationService.getDashboardNotifications();
            return Result.success(notifications);
        } catch (Exception e) {
            log.error("获取工作台通知失败", e);
            return Result.error("获取工作台通知失败: " + e.getMessage());
        }
    }

    /**
     * 创建系统通知（测试用）
     */
    @PostMapping("/test")
    public Result<Void> createTestNotification(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String content = (String) request.get("content");
            String type = (String) request.get("type");
            Map<String, Object> extraData = (Map<String, Object>) request.get("extraData");

            notificationService.createSystemNotification(title, content, type, extraData);
            return Result.success();
        } catch (Exception e) {
            log.error("创建测试通知失败", e);
            return Result.error("创建测试通知失败: " + e.getMessage());
        }
    }
}