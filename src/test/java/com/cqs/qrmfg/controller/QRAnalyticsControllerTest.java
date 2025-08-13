package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.QRAnalyticsDashboardDto;
import com.cqs.qrmfg.service.QRAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
public class QRAnalyticsControllerTest {

    @Mock
    private QRAnalyticsService qrAnalyticsService;

    @InjectMocks
    private AdminMonitoringController adminMonitoringController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock security context
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetWorkflowAnalyticsDashboard() {
        // Act
        ResponseEntity<Map<String, Object>> response = adminMonitoringController.getWorkflowAnalyticsDashboard(null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("totalWorkflows"));
        assertTrue(response.getBody().containsKey("activeWorkflows"));
        assertTrue(response.getBody().containsKey("completedWorkflows"));
        assertTrue(response.getBody().containsKey("overdueWorkflows"));
        assertTrue(response.getBody().containsKey("workflowsByState"));
        assertTrue(response.getBody().containsKey("workflowsByPlant"));
    }

    @Test
    void testGetSlaMetrics() {
        // Act
        ResponseEntity<Map<String, Object>> response = adminMonitoringController.getSlaMetrics(null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("overallSlaCompliance"));
        assertTrue(response.getBody().containsKey("overallAverageResolutionTime"));
        assertTrue(response.getBody().containsKey("slaComplianceByTeam"));
        assertTrue(response.getBody().containsKey("averageResolutionTimesByTeam"));
    }

    @Test
    void testGetBottlenecksAnalysis() {
        // Act
        ResponseEntity<Map<String, Object>> response = adminMonitoringController.getBottlenecksAnalysis(null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("averageTimeInState"));
        assertTrue(response.getBody().containsKey("overdueByState"));
        assertTrue(response.getBody().containsKey("openQueriesByTeam"));
        assertTrue(response.getBody().containsKey("delayedByPlant"));
    }

    @Test
    void testGetPerformanceMetrics() {
        // Act
        ResponseEntity<Map<String, Object>> response = adminMonitoringController.getPerformanceMetrics(null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("completionRate"));
        assertTrue(response.getBody().containsKey("averageCompletionTimeHours"));
        assertTrue(response.getBody().containsKey("queriesPerWorkflow"));
        assertTrue(response.getBody().containsKey("throughputByMonth"));
    }

    @Test
    void testTestEndpoint() {
        // Act
        ResponseEntity<String> response = adminMonitoringController.testEndpoint();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("QR Analytics Controller is working!", response.getBody());
    }

    @Test
    void testAllEndpointsReturnValidData() {
        // Test that all endpoints return valid responses
        
        // Test dashboard
        ResponseEntity<Map<String, Object>> dashboardResponse = adminMonitoringController.getWorkflowAnalyticsDashboard(null, null, null);
        assertNotNull(dashboardResponse);
        assertEquals(200, dashboardResponse.getStatusCodeValue());
        
        // Test SLA metrics
        ResponseEntity<Map<String, Object>> slaResponse = adminMonitoringController.getSlaMetrics(null, null, null);
        assertNotNull(slaResponse);
        assertEquals(200, slaResponse.getStatusCodeValue());
        
        // Test bottlenecks analysis
        ResponseEntity<Map<String, Object>> bottlenecksResponse = adminMonitoringController.getBottlenecksAnalysis(null, null, null);
        assertNotNull(bottlenecksResponse);
        assertEquals(200, bottlenecksResponse.getStatusCodeValue());
        
        // Test performance metrics
        ResponseEntity<Map<String, Object>> performanceResponse = adminMonitoringController.getPerformanceMetrics(null, null, null);
        assertNotNull(performanceResponse);
        assertEquals(200, performanceResponse.getStatusCodeValue());
        
        // Test endpoint
        ResponseEntity<String> testResponse = adminMonitoringController.testEndpoint();
        assertNotNull(testResponse);
        assertEquals(200, testResponse.getStatusCodeValue());
    }
}