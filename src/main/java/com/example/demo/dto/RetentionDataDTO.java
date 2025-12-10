package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RetentionDataDTO {
    private String period; // 时间段，如 "1月"
    private LocalDate startDate;
    private BigDecimal retention1Day;
    private BigDecimal retention7Day;
    private BigDecimal retention30Day;

    // 构造函数
    public RetentionDataDTO(String period, LocalDate startDate,
                            BigDecimal retention1Day, BigDecimal retention7Day, BigDecimal retention30Day) {
        this.period = period;
        this.startDate = startDate;
        this.retention1Day = retention1Day != null ? retention1Day : BigDecimal.ZERO;
        this.retention7Day = retention7Day != null ? retention7Day : BigDecimal.ZERO;
        this.retention30Day = retention30Day != null ? retention30Day : BigDecimal.ZERO;
    }
}