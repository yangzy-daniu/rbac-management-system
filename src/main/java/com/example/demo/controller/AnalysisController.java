package com.example.demo.controller;

import com.example.demo.dto.AnalysisStatsDTO;
import com.example.demo.dto.BehaviorDataDTO;
import com.example.demo.dto.RetentionDataDTO;
import com.example.demo.dto.TrendDataDTO;
import com.example.demo.service.UserAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final UserAnalysisService userAnalysisService;

    @GetMapping("/stats")
    public ResponseEntity<AnalysisStatsDTO> getAnalysisStats(
            @RequestParam(defaultValue = "7d") String timeRange) {
        AnalysisStatsDTO stats = userAnalysisService.getAnalysisStats(timeRange);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendDataDTO>> getTrendData(
            @RequestParam(defaultValue = "7d") String timeRange) {
        List<TrendDataDTO> trendData = userAnalysisService.getTrendData(timeRange);
        return ResponseEntity.ok(trendData);
    }

    @GetMapping("/behavior")
    public ResponseEntity<List<BehaviorDataDTO>> getBehaviorData(
            @RequestParam(defaultValue = "7d") String timeRange) {
        List<BehaviorDataDTO> behaviorData = userAnalysisService.getBehaviorData(timeRange);
        return ResponseEntity.ok(behaviorData);
    }

    @GetMapping("/retention")
    public ResponseEntity<List<RetentionDataDTO>> getRetentionData(
            @RequestParam(defaultValue = "7d") String timeRange) {
        List<RetentionDataDTO> retentionData = userAnalysisService.getRetentionData(timeRange);
        return ResponseEntity.ok(retentionData);
    }
}