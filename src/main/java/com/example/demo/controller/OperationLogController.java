package com.example.demo.controller;

import com.example.demo.entity.OperationLog;
import com.example.demo.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/detailed")
    public ResponseEntity<Page<OperationLog>> getDetailedLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String requestMethod,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String requestUrl,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        Page<OperationLog> logs = operationLogService.getLogsByPage(
                page, size, module, operator, type, result, requestMethod, statusCode,
                requestUrl, startTime, endTime);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<OperationLog>> getRecentLogs() {
        List<OperationLog> logs = operationLogService.getRecentLogs();
        return ResponseEntity.ok(logs);
    }
}