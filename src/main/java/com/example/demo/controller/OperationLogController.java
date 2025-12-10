package com.example.demo.controller;

import com.example.demo.entity.OperationLog;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
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

    @GetMapping("/export")
    public void exportCurrentPageLogs(
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
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws IOException {

        // 获取当前页数据
        Page<OperationLog> logs = operationLogService.getLogsByPage(
                page, size, module, operator, type, result, requestMethod, statusCode,
                requestUrl, startTime, endTime);

        // 设置响应头
        String fileName = "操作日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        // 对文件名进行URL编码，解决浏览器下载文件名乱码
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8")
                .replaceAll("\\+", "%20");

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        // 只使用 OutputStream，不使用 Writer
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            // 写入UTF-8 BOM头，确保Excel正确识别编码
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);

            // 构建CSV内容
            StringBuilder csvContent = new StringBuilder();

            // 写入表头
            csvContent.append("ID,模块,操作类型,操作描述,操作者,请求方法,状态码,响应时间(ms),操作结果,访问时间,IP地址\n");

            // 写入数据
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (OperationLog log : logs.getContent()) {
                csvContent.append(log.getId() != null ? log.getId() : 0).append(",")
                        .append(escapeCsvField(log.getModule())).append(",")
                        .append(escapeCsvField(log.getType())).append(",")
                        .append(escapeCsvField(log.getOperation())).append(",")
                        .append(escapeCsvField(log.getOperator())).append(",")
                        .append(escapeCsvField(log.getRequestMethod())).append(",")
                        .append(log.getStatusCode() != null ? log.getStatusCode() : 0).append(",")
                        .append(log.getExecutionTime() != null ? log.getExecutionTime() : 0).append(",")
                        .append(escapeCsvField(log.getResult() != null ?
                                (log.getResult().equals("SUCCESS") ? "成功" : "失败") : "")).append(",")
                        .append(escapeCsvField(log.getAccessTime() != null ?
                                log.getAccessTime().format(formatter) : "")).append(",")
                        .append(escapeCsvField(log.getOperatorIp()))
                        .append("\n");
            }

            // 将内容写入输出流
            outputStream.write(csvContent.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    // CSV字段转义方法
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果字段包含逗号、换行或引号，需要用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}