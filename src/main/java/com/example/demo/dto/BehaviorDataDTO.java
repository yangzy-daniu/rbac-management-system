package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BehaviorDataDTO {
    private String name;
    private Integer value;
    private String color;

    public BehaviorDataDTO(String name, Integer value) {
        this.name = name;
        this.value = value;
        this.color = getColorByName(name);
    }

    private String getColorByName(String name) {
        switch (name) {
            case "页面浏览": return "#5470c6";
            case "按钮点击": return "#91cc75";
            case "表单提交": return "#fac858";
            case "文件下载": return "#ee6666";
            case "其他操作": return "#73c0de";
            default: return "#999999";
        }
    }
}