package com.example.demo.service;

import com.example.demo.entity.SystemLog;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final String[] HEADERS = {
            "序号", "日志时间", "日志级别", "服务名称", "模块名称",
            "操作用户", "操作IP", "操作内容", "操作结果"
    };

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出系统日志到Excel
     */
    public void exportSystemLogs(List<SystemLog> logs, OutputStream outputStream) {
        Workbook workbook = null;
        try {
            // 1. 创建工作簿（使用.xlsx格式）
            workbook = new XSSFWorkbook();

            // 2. 创建工作表
            Sheet sheet = workbook.createSheet("系统日志");

            // 3. 设置列宽
            setColumnWidth(sheet);

            // 4. 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 5. 创建数据行样式
            CellStyle dataStyle = createDataStyle(workbook);

            // 6. 创建表头
            createHeaderRow(sheet, headerStyle);

            // 7. 填充数据
            fillDataRows(sheet, logs, dataStyle);

            // 8. 写入输出流
            workbook.write(outputStream);

        } catch (Exception e) {
            throw new RuntimeException("生成Excel文件失败", e);
        } finally {
            // 9. 关闭工作簿
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 设置列宽
     */
    private void setColumnWidth(Sheet sheet) {
        // 设置各列宽度
        sheet.setColumnWidth(0, 8 * 256);      // 序号
        sheet.setColumnWidth(1, 20 * 256);     // 日志时间
        sheet.setColumnWidth(2, 10 * 256);     // 日志级别
        sheet.setColumnWidth(3, 15 * 256);     // 服务名称
        sheet.setColumnWidth(4, 15 * 256);     // 模块名称
        sheet.setColumnWidth(5, 12 * 256);     // 操作用户
        sheet.setColumnWidth(6, 15 * 256);     // 操作IP
        sheet.setColumnWidth(7, 40 * 256);     // 操作内容
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 设置背景色
        style.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置字体
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        // 设置居中对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 创建数据行样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 设置字体
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        // 设置自动换行
        style.setWrapText(true);

        return style;
    }

    /**
     * 创建表头行
     */
    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 填充数据行
     */
    private void fillDataRows(Sheet sheet, List<SystemLog> logs, CellStyle dataStyle) {
        if (logs == null || logs.isEmpty()) {
            createNoDataRow(sheet, dataStyle);
            return;
        }

        int rowNum = 1;
        for (SystemLog log : logs) {
            Row row = sheet.createRow(rowNum++);
            createDataRow(row, log, rowNum - 1, dataStyle);
        }
    }

    /**
     * 创建数据行
     */
    private void createDataRow(Row row, SystemLog log, int index, CellStyle dataStyle) {
        // 序号
        Cell cell0 = row.createCell(0);
        cell0.setCellValue(index);
        cell0.setCellStyle(dataStyle);

        // 日志时间
        Cell cell1 = row.createCell(1);
        if (log.getCreateTime() != null) {
            cell1.setCellValue(log.getCreateTime().format(DATE_FORMATTER));
        }
        cell1.setCellStyle(dataStyle);

        // 日志级别
        Cell cell2 = row.createCell(2);
        cell2.setCellValue(log.getLevel());
        // 可以根据级别设置不同的颜色
        setLevelColor(cell2, log.getLevel());
        cell2.setCellStyle(dataStyle);

        // 服务名称
        Cell cell3 = row.createCell(3);
        cell3.setCellValue(log.getService());
        cell3.setCellStyle(dataStyle);

        // 模块名称
        Cell cell4 = row.createCell(4);
        cell4.setCellValue(log.getModule());
        cell4.setCellStyle(dataStyle);

        // 操作用户
        Cell cell5 = row.createCell(5);
        cell5.setCellValue(log.getUsername());
        cell5.setCellStyle(dataStyle);

        // 操作IP
        Cell cell6 = row.createCell(6);
        cell6.setCellValue(log.getIpAddress());
        cell6.setCellStyle(dataStyle);

        // 操作内容
        Cell cell7 = row.createCell(7);
        cell7.setCellValue(log.getOperation());
        cell7.setCellStyle(dataStyle);

    }

    /**
     * 根据日志级别设置单元格颜色
     */
    private void setLevelColor(Cell cell, String level) {
        if (cell == null) return;

        Workbook workbook = cell.getSheet().getWorkbook();

        // 1. 创建一个全新的单元格样式
        CellStyle newStyle = workbook.createCellStyle();

        // 2. 设置字体
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 10);

        // 3. 根据日志级别设置字体颜色
        if (level != null && !level.trim().isEmpty()) {
            String levelUpper = level.trim().toUpperCase();

            if (levelUpper.contains("ERROR") || levelUpper.contains("FATAL")) {
                font.setColor(IndexedColors.RED.getIndex());
                font.setBold(true);
            } else if (levelUpper.contains("WARN")) {
                font.setColor(IndexedColors.ORANGE.getIndex());
            } else if (levelUpper.contains("INFO")) {
                font.setColor(IndexedColors.BLUE.getIndex());
            } else if (levelUpper.contains("DEBUG")) {
                font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            }
            // 其他级别保持默认颜色
        }

        newStyle.setFont(font);

        // 4. 设置单元格边框
        newStyle.setBorderTop(BorderStyle.THIN);
        newStyle.setBorderRight(BorderStyle.THIN);
        newStyle.setBorderBottom(BorderStyle.THIN);
        newStyle.setBorderLeft(BorderStyle.THIN);

        // 5. 设置自动换行
        newStyle.setWrapText(true);

        // 6. 应用到单元格
        cell.setCellStyle(newStyle);
    }


    /**
     * 创建无数据行
     */
    private void createNoDataRow(Sheet sheet, CellStyle dataStyle) {
        Row row = sheet.createRow(1);
        Cell cell = row.createCell(0);
        cell.setCellValue("没有找到符合条件的日志数据");
        cell.setCellStyle(dataStyle);
    }
}