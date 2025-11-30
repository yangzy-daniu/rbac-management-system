package com.example.demo.controller;

import com.example.demo.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemMonitorController {

    private final SystemMonitorService systemMonitorService;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = systemMonitorService.getSystemInfo();
        return ResponseEntity.ok(systemInfo);
    }

    @GetMapping("/online-users/count")
    public ResponseEntity<Map<String, Integer>> getOnlineUserCount() {
        Integer count = systemMonitorService.getOnlineUserCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/info")
    public ResponseEntity<Void> updateSystemInfo(
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String updateUser) {

        systemMonitorService.updateSystemInfo(version, status, description, updateUser);
        return ResponseEntity.ok().build();
    }
}