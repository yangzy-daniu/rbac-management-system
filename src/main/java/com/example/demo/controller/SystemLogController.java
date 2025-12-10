package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.SystemLog;
import com.example.demo.service.ExportService;
import com.example.demo.service.SystemLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/system-logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    private final ExportService exportService;

    @GetMapping
    public ResponseEntity getSystemLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        return ResponseEntity.ok(systemLogService.getSystemLogsByPage(
                page, size, level, service, module, username, startTime, endTime));
    }

    @GetMapping("/recent")
    public Result getRecentLogs() {
        List<SystemLog> recentLogs = systemLogService.getRecentLogs();
        return Result.success(recentLogs);
    }

    @GetMapping("/stats")
    public Result getLogStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        Long infoCount = systemLogService.getLogCountByLevel("INFO", startTime, endTime);
        Long warningCount = systemLogService.getLogCountByLevel("WARNING", startTime, endTime);
        Long errorCount = systemLogService.getLogCountByLevel("ERROR", startTime, endTime);
        Long criticalCount = systemLogService.getLogCountByLevel("CRITICAL", startTime, endTime);

        return Result.success().data("infoCount", infoCount)
                .data("warningCount", warningCount)
                .data("errorCount", errorCount)
                .data("criticalCount", criticalCount);
    }

    @DeleteMapping("/clear")
    public Result clearSystemLogs() {
        try {
            // 这里可以实现清空日志的逻辑
            // systemLogService.clearLogs();
            return Result.success("日志清空成功");
        } catch (Exception e) {
            return Result.error("清空日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public void exportSystemLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) {

        try {
            // 设置响应头
//            response.setContentType("application/vnd.ms-excel");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("系统日志", "UTF-8") +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setHeader("Content-disposition", "attachment;filename=" + fileName);

            // 获取数据并导出
             List<SystemLog> logs = systemLogService.getLogsForExport(level, service, module, username, startTime, endTime);
             exportService.exportSystemLogs(logs, response.getOutputStream());

            // 刷新并关闭输出流
            response.getOutputStream().flush();

        } catch (Exception e) {
            throw new RuntimeException("导出日志失败", e);
        }
    }


//    @GetMapping("/export")
//    public void exportSystemLogs(
//            @RequestParam(required = false) String level,
//            @RequestParam(required = false) String service,
//            @RequestParam(required = false) String module,
//            @RequestParam(required = false) String username,
//            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
//            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
//            HttpServletResponse response) {
//
//        try {
//            // 设置响应头 - 使用正确的Content-Type
//            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//            response.setCharacterEncoding("utf-8");
//
//            String fileName = "系统日志_" +
//                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
//
//            // 使用RFC 5987编码处理中文文件名
//            String encodedFileName = URLEncoder.encode(fileName, "UTF-8")
//                    .replaceAll("\\+", "%20");
//            response.setHeader("Content-disposition",
//                    "attachment; filename*=UTF-8''" + encodedFileName);
//
//            // 使用Apache POI或其他Excel库生成Excel文件
//            Workbook workbook = generateExcelFile(); // 这里需要实现真正的Excel生成逻辑
//
//            // 将Excel写入输出流
//            workbook.write(response.getOutputStream());
//            workbook.close();
//
//        } catch (Exception e) {
//            throw new RuntimeException("导出日志失败", e);
//        }
//    }
//
//    // 示例：使用Apache POI生成Excel
//    private Workbook generateExcelFile() {
//        Workbook workbook = new XSSFWorkbook(); // XSSFWorkbook for .xlsx, HSSFWorkbook for .xls
//        Sheet sheet = workbook.createSheet("系统日志");
//
//        // 创建表头
//        Row headerRow = sheet.createRow(0);
//        String[] headers = {"时间", "级别", "服务", "模块", "用户", "操作内容"};
//        for (int i = 0; i < headers.length; i++) {
//            Cell cell = headerRow.createCell(i);
//            cell.setCellValue(headers[i]);
//        }
//
//        // 添加数据行（这里需要替换为你的实际数据）
//        // List<SystemLog> logs = systemLogService.getLogsForExport(...);
//        // for (SystemLog log : logs) { ... }
//
//        return workbook;
//    }
}