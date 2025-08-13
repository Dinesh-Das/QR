package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.dto.QRAnalyticsDashboardDto;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.service.QRAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test-analytics")
public class AdminMonitoringController {

    @Autowired
    private QRAnalyticsService qrAnalyticsService;

    /**
     * Simple test endpoint to verify controller is accessible
     */
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("QR Analytics Controller is working!");
    }

    /**
     * Get workflow analytics dashboard data from database
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getWorkflowAnalyticsDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        Map<String, Object> dashboard = qrAnalyticsService.getWorkflowAnalyticsDashboard(startDate, endDate, plantCode);
        return ResponseEntity.ok(dashboard);
    }



    /**
     * Get workflow SLA metrics from database
     */
    @GetMapping("/sla-metrics")
    public ResponseEntity<Map<String, Object>> getSlaMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        Map<String, Object> slaMetrics = qrAnalyticsService.getSlaMetrics(startDate, endDate, plantCode);
        return ResponseEntity.ok(slaMetrics);
    }

    /**
     * Get workflow bottlenecks analysis from database
     */
    @GetMapping("/bottlenecks")
    public ResponseEntity<Map<String, Object>> getBottlenecksAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        Map<String, Object> bottlenecks = qrAnalyticsService.getBottlenecksAnalysis(startDate, endDate, plantCode);
        return ResponseEntity.ok(bottlenecks);
    }

    /**
     * Get workflow performance metrics from database
     */
    @GetMapping("/performance-metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        Map<String, Object> performanceMetrics = qrAnalyticsService.getPerformanceMetrics(startDate, endDate, plantCode);
        return ResponseEntity.ok(performanceMetrics);
    }

    /**
     * Export workflow analytics report as CSV
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWorkflowAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        byte[] csvData = qrAnalyticsService.exportWorkflowAnalyticsAsCsv(startDate, endDate, plantCode);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "workflow-analytics-" + System.currentTimeMillis() + ".csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }


    
    /**
     * Get detailed query status breakdown for enhanced analytics
     */
    @GetMapping("/query-status-breakdown")
    public ResponseEntity<Map<String, Object>> getQueryStatusBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        Map<String, Object> breakdown = qrAnalyticsService.getQueryStatusBreakdown(startDate, endDate, plantCode);
        return ResponseEntity.ok(breakdown);
    }
    
    /**
     * Get performance rankings for users and teams
     */
    @GetMapping("/performance-rankings")
    public ResponseEntity<Map<String, Object>> getPerformanceRankings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String plantCode) {
        
        Map<String, Object> rankings = qrAnalyticsService.getPerformanceRankings(startDate, endDate, plantCode);
        return ResponseEntity.ok(rankings);
    }
}