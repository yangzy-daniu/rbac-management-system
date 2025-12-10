package com.example.demo.controller;

import com.example.demo.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system-monitor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemMonitorController {

    private final SystemMonitorService systemMonitorService;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = systemMonitorService.getSystemInfo();
        return ResponseEntity.ok(systemInfo);
    }

}