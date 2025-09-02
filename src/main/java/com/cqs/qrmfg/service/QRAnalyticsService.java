package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.QRAnalyticsDashboardDto;
import com.cqs.qrmfg.model.*;
import com.cqs.qrmfg.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QRAnalyticsService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;



    /**
     * Get workflow analytics dashboard data with role-based filtering
     */
    public Map<String, Object> getWorkflowAnalyticsDashboard(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Get user role and plant access
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> dashboard = new HashMap<>();
        
        // User context information
        Map<String, Object> userContext = new HashMap<>();
        userContext.put("username", username);
        userContext.put("role", userRole);
        userContext.put("plantCode", plantCode);
        userContext.put("isPlantUser", isPlantUser(userRole));
        userContext.put("hasPlantRestriction", isPlantUser(userRole) && userPlantCode != null);
        dashboard.put("userContext", userContext);
        
        // Main workflow statistics
        dashboard.put("totalWorkflows", getTotalWorkflows(startDate, endDate, plantCode));
        dashboard.put("activeWorkflows", getActiveWorkflows(startDate, endDate, plantCode));
        dashboard.put("completedWorkflows", getCompletedWorkflows(startDate, endDate, plantCode));
        dashboard.put("overdueWorkflows", getOverdueWorkflows(startDate, endDate, plantCode));
        
        // Query statistics
        dashboard.put("totalQueries", getTotalQueries(startDate, endDate, plantCode));
        dashboard.put("openQueries", getOpenQueries(startDate, endDate, plantCode));
        dashboard.put("overdueQueries", getOverdueQueries(startDate, endDate, plantCode));
        dashboard.put("averageCompletionTimeHours", getAverageCompletionTimeHours(startDate, endDate, plantCode));
        
        // Workflows by state
        dashboard.put("workflowsByState", getWorkflowsByState(startDate, endDate, plantCode));
        
        // Workflows by plant (only show if user has access to multiple plants)
        if (!isPlantUser(userRole) || userPlantCode == null) {
            dashboard.put("workflowsByPlant", getWorkflowsByPlant(startDate, endDate, plantCode));
        }
        
        // Recent activity (last 7 days)
        dashboard.put("recentActivity", getRecentActivity(startDate, endDate, plantCode));
        
        return dashboard;
    }

    /**
     * Get QR analytics dashboard data with role-based filtering (legacy method)
     */
    public QRAnalyticsDashboardDto getQRAnalyticsDashboard(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Get user role and plant access
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        QRAnalyticsDashboardDto dashboard = new QRAnalyticsDashboardDto();
        
        // Apply role-based filtering
        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        // Fetch live data based on filters
        dashboard.setTotalProduction(getTotalProduction(startDate, endDate, plantCode));
        dashboard.setQualityScore(getQualityScore(startDate, endDate, plantCode));
        dashboard.setActiveWorkflows(getActiveWorkflows(plantCode));
        dashboard.setCompletedToday(getCompletedToday(plantCode));
        
        // Set metadata
        dashboard.setPlantCode(plantCode);
        dashboard.setUserRole(userRole);
        dashboard.setLastUpdated(LocalDateTime.now());

        return dashboard;
    }

    /**
     * Get production metrics with role-based filtering
     */
    public Map<String, Object> getProductionMetrics(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> metrics = new HashMap<>();
        
        // Fetch live production data
        metrics.put("dailyProduction", getDailyProduction(startDate, endDate, plantCode));
        metrics.put("monthlyTotal", getMonthlyTotal(plantCode));
        metrics.put("dailyAverage", getDailyAverage(startDate, endDate, plantCode));
        metrics.put("targetAchievement", getTargetAchievement(plantCode));
        
        return metrics;
    }

    /**
     * Get quality metrics with role-based filtering
     */
    public Map<String, Object> getQualityMetrics(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> metrics = new HashMap<>();
        
        // Fetch live quality data
        metrics.put("qualityDistribution", getQualityDistribution(startDate, endDate, plantCode));
        metrics.put("qualityMetrics", getDetailedQualityMetrics(startDate, endDate, plantCode));
        
        return metrics;
    }

    /**
     * Get workflow efficiency with role-based filtering
     */
    public Map<String, Object> getWorkflowEfficiency(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> efficiency = new HashMap<>();
        
        // Fetch live workflow efficiency data
        efficiency.put("efficiencyByStage", getEfficiencyByStage(startDate, endDate, plantCode));
        efficiency.put("overallEfficiency", getOverallEfficiency(startDate, endDate, plantCode));
        efficiency.put("averageCycleTime", getAverageCycleTime(startDate, endDate, plantCode));
        efficiency.put("bottleneckStage", getBottleneckStage(startDate, endDate, plantCode));
        
        return efficiency;
    }

    // Database query methods for live data
    
    private Long getTotalProduction(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch total production", e);
        }
    }

    private Double getQualityScore(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            // Note: Workflow entity doesn't have quality_score field, so we'll return a default
            // This method should be updated when quality scoring is implemented
            return 95.0; // Default quality score
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch quality score", e);
        }
    }

    private Long getActiveWorkflows(String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getState() != WorkflowState.COMPLETED)
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch active workflows data", e);
        }
    }

    private Long getCompletedToday(String plantCode) {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .filter(w -> w.getCompletedAt() != null)
                .filter(w -> w.getCompletedAt().isAfter(startOfDay) && w.getCompletedAt().isBefore(endOfDay))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch completed today data", e);
        }
    }

    private Map<String, Object> getDailyProduction(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getCreatedAt().toLocalDate().toString(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch daily production", e);
        }
    }

    private Long getMonthlyTotal(String plantCode) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();
            
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getCreatedAt().getMonthValue() == currentMonth && w.getCreatedAt().getYear() == currentYear)
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch monthly total", e);
        }
    }

    private Double getDailyAverage(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            Map<String, Long> dailyCounts = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getCreatedAt().toLocalDate().toString(),
                    Collectors.counting()
                ));
            
            return dailyCounts.isEmpty() ? 0.0 : 
                dailyCounts.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch daily average", e);
        }
    }

    private Double getTargetAchievement(String plantCode) {
        // This would typically come from a targets table
        // For now, calculate based on actual vs expected production
        try {
            Long actualProduction = getMonthlyTotal(plantCode);
            Long targetProduction = 3400L; // This should come from database
            return (actualProduction.doubleValue() / targetProduction.doubleValue()) * 100.0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch target achievement", e);
        }
    }

    private Map<String, Object> getQualityDistribution(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            // Note: Workflow entity doesn't have quality_score field
            // This is a placeholder implementation until quality scoring is added
            Map<String, Object> distribution = new HashMap<>();
            distribution.put("Excellent", 0);
            distribution.put("Good", 0);
            distribution.put("Fair", 0);
            distribution.put("Poor", 0);
            return distribution;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch quality distribution", e);
        }
    }

    private Map<String, Object> getDetailedQualityMetrics(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Note: Workflow entity doesn't have quality_score or revision_count fields
            // These are placeholder implementations until quality metrics are added
            metrics.put("Defect Rate", 0.0);
            metrics.put("First Pass Yield", 0.0);
            
            return metrics;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch detailed quality metrics", e);
        }
    }

    private Map<String, Object> getEfficiencyByStage(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            Map<String, List<Double>> stageHours = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> mapStatusToStageName(w.getState().name()),
                    Collectors.mapping(w -> {
                        LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                        return (double) java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                    }, Collectors.toList())
                ));
            
            Map<String, Object> efficiency = new HashMap<>();
            stageHours.forEach((stage, hours) -> {
                double avgHours = hours.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                // Convert to efficiency percentage (assuming 24 hours is baseline)
                double efficiencyPercent = Math.max(0, 100 - (avgHours / 24.0 * 100));
                efficiency.put(stage, Math.min(100, efficiencyPercent));
            });
            
            return efficiency;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch efficiency by stage", e);
        }
    }

    private Double getOverallEfficiency(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            List<Double> efficiencies = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .map(w -> {
                    if (w.getState() == WorkflowState.COMPLETED && w.getCompletedAt() != null) {
                        double hours = java.time.Duration.between(w.getCreatedAt(), w.getCompletedAt()).toHours();
                        return Math.min(100, Math.max(0, 100 - (hours / 24.0 * 100)));
                    } else {
                        return 50.0; // Default for incomplete workflows
                    }
                })
                .collect(Collectors.toList());
            
            return efficiencies.isEmpty() ? 0.0 : 
                efficiencies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch overall efficiency", e);
        }
    }

    private Double getAverageCycleTime(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            List<Double> cycleTimes = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .map(w -> {
                    LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                    return (double) java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                })
                .collect(Collectors.toList());
            
            return cycleTimes.isEmpty() ? 0.0 : 
                cycleTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch average cycle time", e);
        }
    }

    private String getBottleneckStage(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            Map<String, Double> avgTimeByStage = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getState().name(),
                    Collectors.averagingDouble(w -> {
                        LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                        return java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                    })
                ));
            
            return avgTimeByStage.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> mapStatusToStageName(entry.getKey()))
                .orElse("Unknown");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch bottleneck stage", e);
        }
    }

    private String mapStatusToStageName(String status) {
        if (status == null) return "Unknown";
        
        switch (status.toUpperCase()) {
            case "JVC_PENDING":
            case "JVC_REVIEW":
                return "JVC Review";
            case "PLANT_PENDING":
            case "PLANT_PROCESSING":
                return "Plant Processing";
            case "CQS_PENDING":
            case "CQS_REVIEW":
                return "CQS Approval";
            case "TECH_PENDING":
            case "TECH_REVIEW":
                return "Tech Validation";
            default:
                return status.replace("_", " ");
        }
    }

    // Role and access helper methods
    
    private String getUserPrimaryRole(String username) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            if (user.getRoles().isEmpty()) {
                return "USER"; // Default role
            }
            
            // Return the first role name, prioritizing by hierarchy
            return user.getRoles().stream()
                .map(Role::getName)
                .sorted((r1, r2) -> {
                    // Define role hierarchy priority
                    int priority1 = getRolePriority(r1);
                    int priority2 = getRolePriority(r2);
                    return Integer.compare(priority1, priority2);
                })
                .findFirst()
                .orElse("USER");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user primary role", e);
        }
    }
    
    private int getRolePriority(String roleName) {
        switch (roleName) {
            case "ADMIN": return 1;
            case "TECH_USER": return 2;
            case "JVC_USER": return 3;
            case "CQS_USER": return 4;
            case "PLANT_USER": return 5;
            default: return 6;
        }
    }

    private String getUserPlantAccess(String username, String requestedPlantCode) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            // If user is not plant-specific, return requested plant
            String userRole = getUserPrimaryRole(username);
            if (!isPlantUser(userRole)) {
                return requestedPlantCode;
            }
            
            // Get user's primary plant
            String userPlant = user.getPrimaryPlant();
            
            // If user requests specific plant, validate they have access
            if (requestedPlantCode != null && !requestedPlantCode.trim().isEmpty()) {
                List<String> assignedPlants = user.getAssignedPlantsList();
                boolean hasAccess = assignedPlants.contains(requestedPlantCode) || 
                                  requestedPlantCode.equals(userPlant);
                return hasAccess ? requestedPlantCode : userPlant;
            }
            
            return userPlant;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user plant access", e);
        }
    }

    private boolean isPlantUser(String role) {
        return "PLANT_USER".equals(role);
    }

    /**
     * Get user information with role details for display
     */
    public Map<String, Object> getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("email", user.getEmail());
            userInfo.put("primaryRole", getUserPrimaryRole(username));
            userInfo.put("allRoles", user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList()));
            userInfo.put("primaryPlant", user.getPrimaryPlant());
            userInfo.put("assignedPlants", user.getAssignedPlantsList());
            userInfo.put("isPlantUser", isPlantUser(getUserPrimaryRole(username)));
            userInfo.put("status", user.getStatus());
            
            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current user info", e);
        }
    }

    /**
     * Get filtered user list with roles for admin users
     */
    public List<Map<String, Object>> getUsersWithRoles(String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        String currentUserRole = getUserPrimaryRole(currentUsername);
        
        // Only admin users can see all users
        if (!"ADMIN".equals(currentUserRole)) {
            throw new RuntimeException("Access denied: Only admin users can view user list");
        }
        
        try {
            List<User> users = userRepository.findAll();
            
            return users.stream()
                .filter(user -> plantCode == null || plantCode.trim().isEmpty() || 
                    user.getAssignedPlantsList().contains(plantCode) || 
                    plantCode.equals(user.getPrimaryPlant()))
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("primaryRole", user.getRoles().stream()
                        .map(Role::getName)
                        .sorted((r1, r2) -> Integer.compare(getRolePriority(r1), getRolePriority(r2)))
                        .findFirst()
                        .orElse("USER"));
                    userInfo.put("allRoles", user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()));
                    userInfo.put("primaryPlant", user.getPrimaryPlant());
                    userInfo.put("assignedPlants", user.getAssignedPlantsList());
                    userInfo.put("status", user.getStatus());
                    userInfo.put("isEnabled", user.isEnabled());
                    userInfo.put("createdAt", user.getCreatedAt());
                    return userInfo;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users with roles", e);
        }
    }

    /**
     * Export QR Analytics data as CSV with role-based filtering
     */
    public byte[] exportQRAnalyticsAsCsv(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        final String finalPlantCode = isPlantUser(userRole) && userPlantCode != null ? userPlantCode : plantCode;
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;

        // Generate CSV content based on filtered data
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Date,Plant,Workflow ID,Status,Quality Score,Created Date,Completed Date,Cycle Time (Hours)\n");
        
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            workflows.stream()
                .filter(w -> finalStartDate == null || w.getCreatedAt().isAfter(finalStartDate) || w.getCreatedAt().isEqual(finalStartDate))
                .filter(w -> finalEndDate == null || w.getCreatedAt().isBefore(finalEndDate) || w.getCreatedAt().isEqual(finalEndDate))
                .filter(w -> finalPlantCode == null || finalPlantCode.trim().isEmpty() || finalPlantCode.equals(w.getPlantCode()))
                .limit(1000) // Limit for performance
                .forEach(w -> {
                    LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                    double cycleTime = java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                    
                    csvContent.append(w.getCreatedAt().toLocalDate().toString()).append(",")
                              .append(w.getPlantCode()).append(",")
                              .append(w.getId()).append(",")
                              .append(w.getState().name()).append(",")
                              .append("N/A").append(",") // Quality score not available
                              .append(w.getCreatedAt()).append(",")
                              .append(w.getCompletedAt() != null ? w.getCompletedAt() : "In Progress").append(",")
                              .append(cycleTime).append("\n");
                });
        } catch (Exception e) {
            throw new RuntimeException("Failed to export QR analytics data", e);
        }
        
        return csvContent.toString().getBytes();
    }

    // New methods for workflow analytics dashboard

    /**
     * Get SLA metrics for queries and workflows
     */
    public Map<String, Object> getSlaMetrics(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> slaMetrics = new HashMap<>();
        
        // Overall SLA metrics
        slaMetrics.put("overallSlaCompliance", getOverallSlaCompliance(startDate, endDate, plantCode));
        slaMetrics.put("overallAverageResolutionTime", getAverageCompletionTimeHours(startDate, endDate, plantCode));
        slaMetrics.put("totalQueries", getTotalQueries(startDate, endDate, plantCode));
        slaMetrics.put("totalResolvedQueries", getResolvedQueries(startDate, endDate, plantCode));
        
        // SLA metrics by team
        slaMetrics.put("slaComplianceByTeam", getSlaComplianceByTeam(startDate, endDate, plantCode));
        slaMetrics.put("averageResolutionTimesByTeam", getAverageResolutionTimesByTeam(startDate, endDate, plantCode));
        slaMetrics.put("totalQueriesByTeam", getTotalQueriesByTeam(startDate, endDate, plantCode));
        slaMetrics.put("resolvedQueriesByTeam", getResolvedQueriesByTeam(startDate, endDate, plantCode));
        slaMetrics.put("overdueQueriesByTeam", getOverdueQueriesByTeam(startDate, endDate, plantCode));
        
        return slaMetrics;
    }

    /**
     * Get bottlenecks analysis
     */
    public Map<String, Object> getBottlenecksAnalysis(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> bottlenecks = new HashMap<>();
        
        bottlenecks.put("averageTimeInState", getAverageTimeInState(startDate, endDate, plantCode));
        bottlenecks.put("overdueByState", getOverdueByState(startDate, endDate, plantCode));
        bottlenecks.put("openQueriesByTeam", getOpenQueriesByTeam(startDate, endDate, plantCode));
        bottlenecks.put("delayedByPlant", getDelayedByPlant(startDate, endDate, plantCode));
        
        return bottlenecks;
    }

    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        Map<String, Object> performanceMetrics = new HashMap<>();
        
        performanceMetrics.put("completionRate", getCompletionRate(startDate, endDate, plantCode));
        performanceMetrics.put("averageCompletionTimeHours", getAverageCompletionTimeHours(startDate, endDate, plantCode));
        performanceMetrics.put("queriesPerWorkflow", getQueriesPerWorkflow(startDate, endDate, plantCode));
        performanceMetrics.put("throughputByMonth", getThroughputByMonth(startDate, endDate, plantCode));
        
        return performanceMetrics;
    }

    /**
     * Export workflow analytics as CSV
     */
    public byte[] exportWorkflowAnalyticsAsCsv(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        final String finalPlantCode = isPlantUser(userRole) && userPlantCode != null ? userPlantCode : plantCode;
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Date,Plant,Workflow ID,Status,Created Date,Completed Date,Cycle Time (Hours),Query Count\n");
        
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            workflows.stream()
                .filter(w -> finalStartDate == null || w.getCreatedAt().isAfter(finalStartDate) || w.getCreatedAt().isEqual(finalStartDate))
                .filter(w -> finalEndDate == null || w.getCreatedAt().isBefore(finalEndDate) || w.getCreatedAt().isEqual(finalEndDate))
                .filter(w -> finalPlantCode == null || finalPlantCode.trim().isEmpty() || finalPlantCode.equals(w.getPlantCode()))
                .sorted((w1, w2) -> w2.getCreatedAt().compareTo(w1.getCreatedAt())) // DESC order
                .limit(1000) // Limit for performance
                .forEach(w -> {
                    LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                    double cycleTime = java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                    
                    // Count queries for this workflow using the query repository
                    int queryCount = (int) queryRepository.findAllWithWorkflow().stream()
                        .filter(q -> q.getWorkflow() != null && q.getWorkflow().getId().equals(w.getId()))
                        .count();
                    
                    csvContent.append(w.getCreatedAt().toLocalDate().toString()).append(",")
                              .append(w.getPlantCode()).append(",")
                              .append(w.getId()).append(",")
                              .append(w.getState().name()).append(",")
                              .append(w.getCreatedAt()).append(",")
                              .append(w.getCompletedAt() != null ? w.getCompletedAt() : "In Progress").append(",")
                              .append(cycleTime).append(",")
                              .append(queryCount).append("\n");
                });
        } catch (Exception e) {
            throw new RuntimeException("Failed to export workflow report data", e);
        }
        
        return csvContent.toString().getBytes();
    }

    // Database query methods for workflow analytics

    private Long getTotalWorkflows(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch total workflows data", e);
        }
    }

    private Long getActiveWorkflows(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getState() != WorkflowState.COMPLETED)
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch active workflows data", e);
        }
    }

    private Long getCompletedWorkflows(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch completed workflows data", e);
        }
    }

    private Long getOverdueWorkflows(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3);
            
            return workflows.stream()
                .filter(w -> w.getState() != WorkflowState.COMPLETED)
                .filter(w -> w.getCreatedAt().isBefore(cutoffDate)) // Overdue if older than 3 days
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch overdue workflows data", e);
        }
    }

    private Long getTotalQueries(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            return queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch total queries data", e);
        }
    }

    private Long getOpenQueries(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            return queries.stream()
                .filter(q -> q.getStatus().isActive()) // Use new isActive() method
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch open queries data", e);
        }
    }

    private Long getOverdueQueries(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(2); // Overdue if older than 2 days
            
            return queries.stream()
                .filter(q -> q.getStatus().isActive()) // Use new isActive() method
                .filter(q -> q.getCreatedAt().isBefore(cutoffDate))
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch overdue queries data", e);
        }
    }

    private Map<String, Object> getWorkflowsByState(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getState().name(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch workflows by state", e);
        }
    }

    private Map<String, Object> getWorkflowsByPlant(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    Workflow::getPlantCode,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch workflows by plant", e);
        }
    }

    private Map<String, Object> getRecentActivity(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getCreatedAt().isAfter(sevenDaysAgo)) // Last 7 days
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getCreatedAt().toLocalDate().toString(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch recent activity", e);
        }
    }

    private Double getAverageCompletionTimeHours(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            List<Double> completionTimes = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .map(w -> {
                    LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                    return (double) java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                })
                .collect(Collectors.toList());
            
            return completionTimes.isEmpty() ? 0.0 : 
                completionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch average completion time", e);
        }
    }

    // Additional helper methods for SLA, bottlenecks, and performance metrics
    
    private Double getOverallSlaCompliance(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            List<Query> resolvedQueries = queries.stream()
                .filter(q -> q.getStatus() == QueryStatus.RESOLVED)
                .filter(q -> q.getResolvedAt() != null)
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .collect(Collectors.toList());
            
            if (resolvedQueries.isEmpty()) {
                return 0.0;
            }
            
            long slaCompliantCount = resolvedQueries.stream()
                .filter(q -> java.time.Duration.between(q.getCreatedAt(), q.getResolvedAt()).toDays() <= 2)
                .count();
            
            return (slaCompliantCount * 100.0) / resolvedQueries.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch SLA compliance", e);
        }
    }

    private Long getResolvedQueries(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            return queries.stream()
                .filter(q -> q.getStatus() == QueryStatus.RESOLVED)
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch total resolved queries", e);
        }
    }

    // Placeholder methods for complex analytics (implement based on your specific requirements)
    
    private Map<String, Object> getSlaComplianceByTeam(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .filter(q -> q.getStatus().isResolved()) // Only resolved queries for SLA calculation
                .collect(Collectors.toList());
            
            // Calculate SLA compliance by team (SLA = resolved within 3 days)
            Map<String, Double> slaComplianceByTeam = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam().name(),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        teamQueries -> {
                            if (teamQueries.isEmpty()) return 0.0;
                            
                            long withinSla = teamQueries.stream()
                                .filter(q -> q.getResolvedAt() != null)
                                .filter(q -> java.time.Duration.between(q.getCreatedAt(), q.getResolvedAt()).toDays() <= 3)
                                .count();
                            
                            return (double) withinSla / teamQueries.size() * 100.0;
                        }
                    )
                ));
            
            return new HashMap<>(slaComplianceByTeam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch SLA compliance by team", e);
        }
    }

    private Map<String, Object> getAverageResolutionTimesByTeam(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .filter(q -> q.getStatus().isResolved() && q.getResolvedAt() != null) // Only resolved queries with resolution time
                .collect(Collectors.toList());
            
            // Calculate average resolution times by team (in hours)
            Map<String, Double> avgResolutionTimesByTeam = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam().name(),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        teamQueries -> {
                            if (teamQueries.isEmpty()) return 0.0;
                            
                            double totalHours = teamQueries.stream()
                                .mapToDouble(q -> java.time.Duration.between(q.getCreatedAt(), q.getResolvedAt()).toHours())
                                .sum();
                            
                            return totalHours / teamQueries.size();
                        }
                    )
                ));
            
            return new HashMap<>(avgResolutionTimesByTeam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch average resolution times by team", e);
        }
    }

    private Map<String, Object> getTotalQueriesByTeam(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .collect(Collectors.toList());
            
            // Count total queries by team
            Map<String, Long> totalQueriesByTeam = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam().name(),
                    Collectors.counting()
                ));
            
            return new HashMap<>(totalQueriesByTeam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch total queries by team", e);
        }
    }

    private Map<String, Object> getResolvedQueriesByTeam(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .filter(q -> q.getStatus().isResolved()) // Only resolved queries
                .collect(Collectors.toList());
            
            // Count resolved queries by team
            Map<String, Long> resolvedQueriesByTeam = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam().name(),
                    Collectors.counting()
                ));
            
            return new HashMap<>(resolvedQueriesByTeam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch resolved queries by team", e);
        }
    }

    private Map<String, Object> getOverdueQueriesByTeam(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3); // Overdue if older than 3 days
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .filter(q -> q.getStatus().isActive()) // Only active queries can be overdue
                .filter(q -> q.getCreatedAt().isBefore(cutoffDate)) // Older than 3 days
                .collect(Collectors.toList());
            
            // Count overdue queries by team
            Map<String, Long> overdueQueriesByTeam = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam().name(),
                    Collectors.counting()
                ));
            
            return new HashMap<>(overdueQueriesByTeam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch overdue queries by team", e);
        }
    }

    private Map<String, Object> getAverageTimeInState(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            return workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getState().name(),
                    Collectors.averagingDouble(w -> {
                        LocalDateTime endTime = w.getCompletedAt() != null ? w.getCompletedAt() : now;
                        return java.time.Duration.between(w.getCreatedAt(), endTime).toHours();
                    })
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch average time in state", e);
        }
    }

    private Map<String, Object> getOverdueByState(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3); // Overdue if older than 3 days
            
            return workflows.stream()
                .filter(w -> w.getState() != WorkflowState.COMPLETED)
                .filter(w -> w.getCreatedAt().isBefore(cutoffDate))
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getState().name(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch overdue by state", e);
        }
    }

    private Map<String, Object> getOpenQueriesByTeam(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            Map<String, Long> openQueriesByTeam = queries.stream()
                .filter(q -> q.getStatus().isActive()) // Use the new isActive() method
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                    (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam() != null ? q.getAssignedTeam().name() : "Unassigned",
                    Collectors.counting()
                ));
            
            Map<String, Object> result = new HashMap<>();
            result.put("data", openQueriesByTeam);
            result.put("total", openQueriesByTeam.values().stream().mapToLong(Long::longValue).sum());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch open queries by team", e);
        }
    }

    private Map<String, Object> getDelayedByPlant(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            LocalDateTime now = LocalDateTime.now();
            
            Map<String, Long> delayedByPlant = workflows.stream()
                .filter(w -> w.getState() != WorkflowState.COMPLETED)
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .filter(w -> {
                    // Consider delayed if workflow is older than 24 hours and not completed
                    return java.time.Duration.between(w.getCreatedAt(), now).toHours() > 24;
                })
                .collect(Collectors.groupingBy(
                    w -> w.getPlantCode() != null ? w.getPlantCode() : "Unknown",
                    Collectors.counting()
                ));
            
            Map<String, Object> result = new HashMap<>();
            result.put("data", delayedByPlant);
            result.put("total", delayedByPlant.values().stream().mapToLong(Long::longValue).sum());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch delayed workflows by plant", e);
        }
    }

    private Double getCompletionRate(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            List<Workflow> filteredWorkflows = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.toList());
            
            if (filteredWorkflows.isEmpty()) {
                return 0.0;
            }
            
            long completedCount = filteredWorkflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .count();
            
            return (completedCount * 100.0) / filteredWorkflows.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch completion rate", e);
        }
    }

    private Double getQueriesPerWorkflow(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            List<Double> queryCounts = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .map(w -> {
                    // Count queries for this workflow using the query repository
                    List<Query> workflowQueries = queryRepository.findAllWithWorkflow().stream()
                        .filter(q -> q.getWorkflow() != null && q.getWorkflow().getId().equals(w.getId()))
                        .collect(Collectors.toList());
                    return (double) workflowQueries.size();
                })
                .collect(Collectors.toList());
            
            return queryCounts.isEmpty() ? 0.0 : 
                queryCounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch queries per workflow", e);
        }
    }

    private Map<String, Object> getThroughputByMonth(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
            List<Workflow> workflows = workflowRepository.findAll();
            
            return workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .filter(w -> w.getCompletedAt() != null && w.getCompletedAt().isAfter(oneYearAgo))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.groupingBy(
                    w -> w.getCompletedAt().getMonth().name() + " " + w.getCompletedAt().getYear(),
                    Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (Object) entry.getValue()
                ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch throughput by month", e);
        }
    }
    
    /**
     * Get query status breakdown for enhanced analytics
     */
    public Map<String, Object> getQueryStatusBreakdown(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        final String finalPlantCode = isPlantUser(userRole) && userPlantCode != null ? userPlantCode : plantCode;
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;

        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> finalStartDate == null || q.getCreatedAt().isAfter(finalStartDate) || q.getCreatedAt().isEqual(finalStartDate))
                .filter(q -> finalEndDate == null || q.getCreatedAt().isBefore(finalEndDate) || q.getCreatedAt().isEqual(finalEndDate))
                .filter(q -> finalPlantCode == null || finalPlantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && finalPlantCode.equals(q.getWorkflow().getPlantCode())))
                .collect(Collectors.toList());
            
            Map<String, Object> breakdown = new HashMap<>();
            
            // Count by status
            Map<String, Long> statusCounts = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getStatus().name(),
                    Collectors.counting()
                ));
            
            // Count by team and status
            Map<String, Map<String, Long>> teamStatusCounts = filteredQueries.stream()
                .collect(Collectors.groupingBy(
                    q -> q.getAssignedTeam().name(),
                    Collectors.groupingBy(
                        q -> q.getStatus().name(),
                        Collectors.counting()
                    )
                ));
            
            // Calculate totals
            long totalQueries = filteredQueries.size();
            long activeQueries = filteredQueries.stream()
                .filter(q -> q.getStatus().isActive())
                .count();
            long resolvedQueries = filteredQueries.stream()
                .filter(q -> q.getStatus().isResolved())
                .count();
            
            breakdown.put("statusCounts", statusCounts);
            breakdown.put("teamStatusCounts", teamStatusCounts);
            breakdown.put("totalQueries", totalQueries);
            breakdown.put("activeQueries", activeQueries);
            breakdown.put("resolvedQueries", resolvedQueries);
            
            return breakdown;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch query status breakdown", e);
        }
    }
    
    /**
     * Get performance rankings for users and teams
     */
    public Map<String, Object> getPerformanceRankings(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String userRole = getUserPrimaryRole(username);
        String userPlantCode = getUserPlantAccess(username, plantCode);

        if (isPlantUser(userRole) && userPlantCode != null) {
            plantCode = userPlantCode;
        }

        final String finalPlantCode = plantCode;
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;

        try {
            Map<String, Object> rankings = new HashMap<>();
            
            // Get top query resolvers
            rankings.put("topQueryResolvers", getTopQueryResolvers(finalStartDate, finalEndDate, finalPlantCode));
            
            // Get top workflow creators
            rankings.put("topWorkflowCreators", getTopWorkflowCreators(finalStartDate, finalEndDate, finalPlantCode));
            
            // Get top form completers
            rankings.put("topFormCompleters", getTopFormCompleters(finalStartDate, finalEndDate, finalPlantCode));
            
            // Get team performance comparison
            rankings.put("teamPerformance", getTeamPerformanceComparison(finalStartDate, finalEndDate, finalPlantCode));
            
            // Get plant performance comparison
            rankings.put("plantPerformance", getPlantPerformanceComparison(finalStartDate, finalEndDate, finalPlantCode));
            
            return rankings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch performance rankings", e);
        }
    }
    
    private List<Map<String, Object>> getTopQueryResolvers(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                    (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .filter(q -> q.getStatus().isResolved())
                .collect(Collectors.toList());
            
            // Group by resolver and count
            Map<String, List<Query>> resolverQueries = filteredQueries.stream()
                .filter(q -> q.getResolvedBy() != null)
                .collect(Collectors.groupingBy(Query::getResolvedBy));
            
            // Create ranking list
            List<Map<String, Object>> topResolvers = new ArrayList<>();
            resolverQueries.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .limit(10)
                .forEach(entry -> {
                    Map<String, Object> resolver = new HashMap<>();
                    resolver.put("name", entry.getKey());
                    resolver.put("count", entry.getValue().size());
                    
                    // Calculate average resolution time
                    double avgTime = entry.getValue().stream()
                        .filter(q -> q.getResolvedAt() != null)
                        .mapToDouble(q -> java.time.Duration.between(q.getCreatedAt(), q.getResolvedAt()).toHours())
                        .average()
                        .orElse(0.0);
                    resolver.put("avgResolutionTime", Math.round(avgTime * 100.0) / 100.0);
                    
                    topResolvers.add(resolver);
                });
            
            return topResolvers;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch top query resolvers", e);
        }
    }
    
    private List<Map<String, Object>> getTopWorkflowCreators(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            // Filter workflows by date range and plant
            List<Workflow> filteredWorkflows = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.toList());
            
            // Group by creator (user) and count - only show actual users, not plants
            Map<String, Long> creatorCounts = filteredWorkflows.stream()
                .filter(w -> w.getCreatedBy() != null && !w.getCreatedBy().trim().isEmpty())
                .collect(Collectors.groupingBy(Workflow::getCreatedBy, Collectors.counting()));
            
            // Convert to ranked list of users only
            return creatorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .map(entry -> {
                    Map<String, Object> creator = new HashMap<>();
                    creator.put("name", entry.getKey());
                    creator.put("count", entry.getValue());
                    creator.put("type", "user");
                    
                    // Calculate completion rate for this user
                    long completed = filteredWorkflows.stream()
                        .filter(w -> entry.getKey().equals(w.getCreatedBy()))
                        .filter(w -> w.getState() == WorkflowState.COMPLETED)
                        .count();
                    double completionRate = entry.getValue() > 0 ? (double) completed / entry.getValue() * 100.0 : 0.0;
                    creator.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
                    
                    return creator;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch top workflow creators", e);
        }
    }
    
    private List<Map<String, Object>> getTopFormCompleters(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            // Filter completed workflows by date range and plant
            List<Workflow> completedWorkflows = workflows.stream()
                .filter(w -> w.getState() == WorkflowState.COMPLETED)
                .filter(w -> startDate == null || w.getCompletedAt().isAfter(startDate) || w.getCompletedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCompletedAt().isBefore(endDate) || w.getCompletedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.toList());
            
            // Group by completer and count
            Map<String, Long> completerCounts = completedWorkflows.stream()
                .filter(w -> w.getUpdatedBy() != null)
                .collect(Collectors.groupingBy(Workflow::getUpdatedBy, Collectors.counting()));
            
            // Group by plant and count
            Map<String, Long> plantCounts = completedWorkflows.stream()
                .filter(w -> w.getPlantCode() != null)
                .collect(Collectors.groupingBy(Workflow::getPlantCode, Collectors.counting()));
            
            // Convert to ranked list
            List<Map<String, Object>> topCompleters = completerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> completer = new HashMap<>();
                    completer.put("name", entry.getKey());
                    completer.put("count", entry.getValue());
                    completer.put("type", "user");
                    
                    // Calculate average completion time for this user
                    double avgTime = completedWorkflows.stream()
                        .filter(w -> entry.getKey().equals(w.getUpdatedBy()))
                        .filter(w -> w.getCompletedAt() != null)
                        .mapToDouble(w -> java.time.Duration.between(w.getCreatedAt(), w.getCompletedAt()).toHours())
                        .average()
                        .orElse(0.0);
                    completer.put("avgCompletionTime", Math.round(avgTime * 100.0) / 100.0);
                    
                    return completer;
                })
                .collect(Collectors.toList());
            
            // Add plant rankings
            List<Map<String, Object>> topPlants = plantCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> plant = new HashMap<>();
                    plant.put("name", entry.getKey() + " Plant");
                    plant.put("count", entry.getValue());
                    plant.put("type", "plant");
                    
                    // Calculate average completion time for this plant
                    double avgTime = completedWorkflows.stream()
                        .filter(w -> entry.getKey().equals(w.getPlantCode()))
                        .filter(w -> w.getCompletedAt() != null)
                        .mapToDouble(w -> java.time.Duration.between(w.getCreatedAt(), w.getCompletedAt()).toHours())
                        .average()
                        .orElse(0.0);
                    plant.put("avgCompletionTime", Math.round(avgTime * 100.0) / 100.0);
                    
                    return plant;
                })
                .collect(Collectors.toList());
            
            // Combine and return top performers
            List<Map<String, Object>> combined = new ArrayList<>();
            combined.addAll(topCompleters);
            combined.addAll(topPlants);
            
            return combined.stream()
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .limit(15)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch top form completers", e);
        }
    }
    
    private Map<String, Object> getTeamPerformanceComparison(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Query> queries = queryRepository.findAllWithWorkflow();
            
            // Filter queries by date range and plant
            List<Query> filteredQueries = queries.stream()
                .filter(q -> startDate == null || q.getCreatedAt().isAfter(startDate) || q.getCreatedAt().isEqual(startDate))
                .filter(q -> endDate == null || q.getCreatedAt().isBefore(endDate) || q.getCreatedAt().isEqual(endDate))
                .filter(q -> plantCode == null || plantCode.trim().isEmpty() || 
                           (q.getWorkflow() != null && plantCode.equals(q.getWorkflow().getPlantCode())))
                .collect(Collectors.toList());
            
            Map<String, Object> teamPerformance = new HashMap<>();
            
            for (QueryTeam team : QueryTeam.values()) {
                String teamName = team.name();
                List<Query> teamQueries = filteredQueries.stream()
                    .filter(q -> q.getAssignedTeam() == team)
                    .collect(Collectors.toList());
                
                Map<String, Object> teamStats = new HashMap<>();
                teamStats.put("totalQueries", teamQueries.size());
                teamStats.put("resolvedQueries", teamQueries.stream().filter(q -> q.getStatus().isResolved()).count());
                teamStats.put("openQueries", teamQueries.stream().filter(q -> q.getStatus().isActive()).count());
                
                // Calculate resolution rate
                double resolutionRate = teamQueries.size() > 0 ? 
                    (double) teamQueries.stream().filter(q -> q.getStatus().isResolved()).count() / teamQueries.size() * 100.0 : 0.0;
                teamStats.put("resolutionRate", Math.round(resolutionRate * 100.0) / 100.0);
                
                // Calculate average resolution time
                double avgResolutionTime = teamQueries.stream()
                    .filter(q -> q.getStatus().isResolved() && q.getResolvedAt() != null)
                    .mapToDouble(q -> java.time.Duration.between(q.getCreatedAt(), q.getResolvedAt()).toHours())
                    .average()
                    .orElse(0.0);
                teamStats.put("avgResolutionTime", Math.round(avgResolutionTime * 100.0) / 100.0);
                
                teamPerformance.put(teamName, teamStats);
            }
            
            return teamPerformance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch team performance comparison", e);
        }
    }
    
    private Map<String, Object> getPlantPerformanceComparison(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            
            // Filter workflows by date range and plant
            List<Workflow> filteredWorkflows = workflows.stream()
                .filter(w -> startDate == null || w.getCreatedAt().isAfter(startDate) || w.getCreatedAt().isEqual(startDate))
                .filter(w -> endDate == null || w.getCreatedAt().isBefore(endDate) || w.getCreatedAt().isEqual(endDate))
                .filter(w -> plantCode == null || plantCode.trim().isEmpty() || plantCode.equals(w.getPlantCode()))
                .collect(Collectors.toList());
            
            // Group by plant
            Map<String, List<Workflow>> workflowsByPlant = filteredWorkflows.stream()
                .filter(w -> w.getPlantCode() != null)
                .collect(Collectors.groupingBy(Workflow::getPlantCode));
            
            Map<String, Object> plantPerformance = new HashMap<>();
            
            for (Map.Entry<String, List<Workflow>> entry : workflowsByPlant.entrySet()) {
                String plant = entry.getKey();
                List<Workflow> plantWorkflows = entry.getValue();
                
                Map<String, Object> plantStats = new HashMap<>();
                plantStats.put("totalWorkflows", plantWorkflows.size());
                plantStats.put("completedWorkflows", plantWorkflows.stream().filter(w -> w.getState() == WorkflowState.COMPLETED).count());
                plantStats.put("activeWorkflows", plantWorkflows.stream().filter(w -> w.getState() != WorkflowState.COMPLETED).count());
                
                // Calculate completion rate
                double completionRate = plantWorkflows.size() > 0 ? 
                    (double) plantWorkflows.stream().filter(w -> w.getState() == WorkflowState.COMPLETED).count() / plantWorkflows.size() * 100.0 : 0.0;
                plantStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
                
                // Calculate average completion time
                double avgCompletionTime = plantWorkflows.stream()
                    .filter(w -> w.getState() == WorkflowState.COMPLETED && w.getCompletedAt() != null)
                    .mapToDouble(w -> java.time.Duration.between(w.getCreatedAt(), w.getCompletedAt()).toHours())
                    .average()
                    .orElse(0.0);
                plantStats.put("avgCompletionTime", Math.round(avgCompletionTime * 100.0) / 100.0);
                
                plantPerformance.put(plant, plantStats);
            }
            
            return plantPerformance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch plant performance comparison", e);
        }
    }
}