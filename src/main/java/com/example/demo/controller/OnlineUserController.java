package com.example.demo.controller;

import com.example.demo.entity.OnlineUser;
import com.example.demo.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final SystemMonitorService systemMonitorService;

    @GetMapping
    public ResponseEntity<List<OnlineUser>> getOnlineUsers() {
        List<OnlineUser> onlineUsers = systemMonitorService.getAllOnlineUsers();
        return ResponseEntity.ok(onlineUsers);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getOnlineUserCount() {
        Integer count = systemMonitorService.getOnlineUserCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> forceLogout(@PathVariable String sessionId) {
        systemMonitorService.userLogout(sessionId);
        return ResponseEntity.ok().build();
    }
}