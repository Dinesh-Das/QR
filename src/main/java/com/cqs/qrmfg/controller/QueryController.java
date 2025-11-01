package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.annotation.RequireRole;
import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.dto.QueryCreateRequest;
import com.cqs.qrmfg.dto.QueryResolveRequest;
import com.cqs.qrmfg.dto.QuerySummaryDto;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.dto.DocumentUploadContext;
import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.exception.QueryException;
import com.cqs.qrmfg.exception.QueryNotFoundException;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Document;
import com.cqs.qrmfg.service.QueryService;
import com.cqs.qrmfg.service.DocumentService;
import com.cqs.qrmfg.util.QueryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/queries")
public class QueryController {

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    private QueryService queryService;

    @Autowired
    private QueryMapper queryMapper;

    // Basic CRUD operations
    @GetMapping
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.LIST)
    public ResponseEntity<List<QuerySummaryDto>> getAllQueries() {
        List<Query> queries = queryService.findAll();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/{id}")
    @RequireRole({RoleType.ADMIN, RoleType.JVC_ROLE, RoleType.CQS_ROLE, RoleType.TECH_ROLE, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.SINGLE)
    public ResponseEntity<QuerySummaryDto> getQueryById(@PathVariable Long id) {
        Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
        if (queryOpt.isPresent()) {
            QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryOpt.get());
            return ResponseEntity.ok(queryDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Query creation - Plant users only
    @PostMapping("/workflow/{workflowId}")
    @RequireRole({RoleType.ADMIN, RoleType.PLANT_ROLE})
    @PlantDataFilter(entityField = "plantCode", filterType = PlantDataFilter.FilterType.SINGLE)
    public ResponseEntity<QuerySummaryDto> createQueryForWorkflow(
            @PathVariable Long workflowId,
            @Valid @RequestBody QueryCreateRequest request,
            Authentication authentication) {
        
        try {
            logger.info("Creating query for workflow {} with request: {}", workflowId, request);
            
            String raisedBy = getCurrentUsername(authentication);
            logger.debug("Query raised by user: {}", raisedBy);
            
            // Validate request data
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                logger.error("Question is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            
            if (request.getAssignedTeam() == null) {
                logger.error("Assigned team is required but was null");
                return ResponseEntity.badRequest().build();
            }
            
            Query query = queryService.createQuery(
                workflowId,
                request.getQuestion(),
                request.getStepNumber(),
                request.getFieldName(),
                request.getAssignedTeam(),
                raisedBy
            );
            
            // Update additional properties if provided
            Query savedQuery = query;
            if (request.getPriorityLevel() != null) {
                savedQuery = queryService.updatePriority(query.getId(), request.getPriorityLevel(), raisedBy);
            }
            if (request.getQueryCategory() != null) {
                savedQuery = queryService.updateCategory(savedQuery.getId(), request.getQueryCategory(), raisedBy);
            }
            logger.info("Successfully created query with ID: {}", savedQuery.getId());
            
            // Reload the query with workflow to avoid lazy loading issues
            Query queryWithWorkflow = queryService.findByIdWithWorkflow(savedQuery.getId())
                .orElse(savedQuery);
            
            // Convert to DTO to avoid lazy loading issues
            QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryWithWorkflow);
            return ResponseEntity.status(HttpStatus.CREATED).body(queryDto);
        } catch (QueryException e) {
            logger.error("Query validation failed for workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to create query for workflow {}: {}", workflowId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/material/{materialCode}")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<QuerySummaryDto> createQueryForMaterial(
            @PathVariable String materialCode,
            @Valid @RequestBody QueryCreateRequest request,
            Authentication authentication) {
        
        String raisedBy = getCurrentUsername(authentication);
        
        Query query = queryService.createQuery(
            materialCode,
            request.getQuestion(),
            request.getStepNumber(),
            request.getFieldName(),
            request.getAssignedTeam(),
            raisedBy
        );
        
        if (request.getPriorityLevel() != null) {
            query.setPriorityLevel(request.getPriorityLevel());
        }
        if (request.getQueryCategory() != null) {
            query.setQueryCategory(request.getQueryCategory());
        }
        
        Query savedQuery = queryService.save(query);
        
        // Reload with workflow to avoid lazy loading issues
        Query queryWithWorkflow = queryService.findByIdWithWorkflow(savedQuery.getId())
            .orElse(savedQuery);
        
        QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryWithWorkflow);
        return ResponseEntity.status(HttpStatus.CREATED).body(queryDto);
    }

    // Query resolution - CQS/Tech/JVC users only
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<QuerySummaryDto> resolveQuery(
            @PathVariable Long id,
            @Valid @RequestBody QueryResolveRequest request,
            Authentication authentication) {
        
        String resolvedBy = getCurrentUsername(authentication);
        Query query = queryService.resolveQuery(id, request.getResponse(), resolvedBy, request.getPriorityLevel());
        
        // Reload with workflow to avoid lazy loading issues
        Query queryWithWorkflow = queryService.findByIdWithWorkflow(query.getId())
            .orElse(query);
        
        QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryWithWorkflow);
        return ResponseEntity.ok(queryDto);
    }

    @PutMapping("/bulk-resolve")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> bulkResolveQueries(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        @SuppressWarnings("unchecked")
        List<Long> queryIds = (List<Long>) request.get("queryIds");
        String response = (String) request.get("response");
        String resolvedBy = getCurrentUsername(authentication);
        
        queryService.bulkResolveQueries(queryIds, response, resolvedBy);
        return ResponseEntity.ok().build();
    }

    // Query management
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuerySummaryDto> assignToTeam(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String teamStr = request.get("team");
        if (teamStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            QueryTeam team = QueryTeam.valueOf(teamStr);
            String updatedBy = getCurrentUsername(authentication);
            Query query = queryService.assignToTeam(id, team, updatedBy);
            
            // Reload with workflow to avoid lazy loading issues
            Query queryWithWorkflow = queryService.findByIdWithWorkflow(query.getId())
                .orElse(query);
            
            QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryWithWorkflow);
            return ResponseEntity.ok(queryDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuerySummaryDto> updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String priorityLevel = request.get("priorityLevel");
        if (priorityLevel == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String updatedBy = getCurrentUsername(authentication);
        Query query = queryService.updatePriority(id, priorityLevel, updatedBy);
        
        // Reload with workflow to avoid lazy loading issues
        Query queryWithWorkflow = queryService.findByIdWithWorkflow(query.getId())
            .orElse(query);
        
        QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryWithWorkflow);
        return ResponseEntity.ok(queryDto);
    }

    @PutMapping("/{id}/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuerySummaryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String category = request.get("category");
        if (category == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String updatedBy = getCurrentUsername(authentication);
        Query query = queryService.updateCategory(id, category, updatedBy);
        
        // Reload with workflow to avoid lazy loading issues
        Query queryWithWorkflow = queryService.findByIdWithWorkflow(query.getId())
            .orElse(query);
        
        QuerySummaryDto queryDto = queryMapper.toSummaryDto(queryWithWorkflow);
        return ResponseEntity.ok(queryDto);
    }

    // Query search and filtering
    @GetMapping("/workflow/{workflowId}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByWorkflow(@PathVariable Long workflowId) {
        List<Query> queries = queryService.findByWorkflowId(workflowId);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/material/{materialCode}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByMaterial(@PathVariable String materialCode) {
        List<Query> queries = queryService.findByMaterialCode(materialCode);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByStatus(@PathVariable String status) {
        try {
            QueryStatus queryStatus = QueryStatus.valueOf(status);
            List<Query> queries = queryService.findByStatus(queryStatus);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/team/{team}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByTeam(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            List<Query> queries = queryService.findByAssignedTeam(queryTeam);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Team-specific query inboxes
    @GetMapping("/inbox/{team}")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getTeamInbox(@PathVariable String team) {
        try {
            logger.info("Loading inbox for team: {}", team);
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            
            List<Query> queries = queryService.findOpenQueriesForTeam(queryTeam);
            logger.debug("Found {} queries for team {}", queries.size(), team);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid team name: {}", team, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to load inbox for team {}: {}", team, e.getMessage(), e);
            throw e; // Re-throw to let the exception handler deal with it
        }
    }

    @GetMapping("/resolved/{team}")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getTeamResolvedQueries(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            List<Query> queries = queryService.findResolvedQueriesForTeam(queryTeam);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my-raised")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getMyRaisedQueries(Authentication authentication) {
        String username = getCurrentUsername(authentication);
        List<Query> queries = queryService.findMyRaisedQueries(username);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/my-resolved")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getMyResolvedQueries(Authentication authentication) {
        String username = getCurrentUsername(authentication);
        List<Query> queries = queryService.findMyResolvedQueries(username);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    // Role-based query history endpoints
    @GetMapping("/workflow/{workflowId}/history/{role}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getWorkflowQueryHistoryByRole(@PathVariable Long workflowId, @PathVariable String role) {
        try {
            List<Query> queries = queryService.findWorkflowQueriesResolvedByRole(workflowId, role);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (Exception e) {
            logger.error("Failed to get workflow query history for role {}: {}", role, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/material/{materialCode}/history/{role}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getMaterialQueryHistoryByRole(@PathVariable String materialCode, @PathVariable String role) {
        try {
            List<Query> queries = queryService.findMaterialQueriesResolvedByRole(materialCode, role);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (Exception e) {
            logger.error("Failed to get material query history for role {}: {}", role, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Dashboard and reporting endpoints
    @GetMapping("/pending")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getPendingQueries() {
        List<Query> queries = queryService.findPendingQueriesForDashboard();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/high-priority")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getHighPriorityQueries() {
        List<Query> queries = queryService.findHighPriorityQueries();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getOverdueQueries() {
        List<Query> queries = queryService.findOverdueQueries();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/needing-attention")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesNeedingAttention() {
        List<Query> queries = queryService.findQueriesNeedingAttention();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getRecentQueries(@RequestParam(defaultValue = "7") int days) {
        List<Query> queries = queryService.findRecentQueries(days);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/created-between")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesCreatedBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Query> queries = queryService.findQueriesCreatedBetween(start, end);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    // Enhanced search with material context
    @GetMapping("/search")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> searchQueries(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String plantCode,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false, defaultValue = "0") int minDaysOpen) {
        
        List<Query> queries = queryService.searchQueriesWithContext(
            materialCode, projectCode, plantCode, team, status, priority, minDaysOpen
        );
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    // Statistics endpoints
    @GetMapping("/stats/count-open/{team}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getOpenQueriesCount(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countOpenQueriesByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/count-resolved/{team}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getResolvedQueriesCount(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countResolvedQueriesByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/overdue-count")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getOverdueQueriesCount() {
        long count = queryService.countOverdueQueries();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/overdue-count/{team}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getOverdueQueriesCountByTeam(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countOverdueQueriesByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/created-today")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getQueriesCreatedToday() {
        long count = queryService.countQueriesCreatedToday();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/resolved-today")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getQueriesResolvedToday() {
        long count = queryService.countQueriesResolvedToday();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/resolved-today/{team}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getQueriesResolvedTodayByTeam(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countQueriesResolvedTodayByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/avg-resolution-time/{team}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Double> getAverageResolutionTime(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            double avgTime = queryService.getAverageResolutionTimeHours(queryTeam);
            return ResponseEntity.ok(avgTime);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Validation endpoints
    @GetMapping("/{id}/can-resolve")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> canUserResolveQuery(@PathVariable Long id, Authentication authentication) {
        Optional<Query> queryOpt = queryService.findById(id);
        if (!queryOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        String username = getCurrentUsername(authentication);
        boolean canResolve = queryService.canUserResolveQuery(queryOpt.get(), username);
        return ResponseEntity.ok(canResolve);
    }

    @GetMapping("/{id}/is-overdue")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isQueryOverdue(@PathVariable Long id) {
        Optional<Query> queryOpt = queryService.findById(id);
        if (!queryOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        boolean isOverdue = queryService.isQueryOverdue(queryOpt.get());
        return ResponseEntity.ok(isOverdue);
    }

    // Utility method to get current username
    private String getCurrentUsername(Authentication authentication) {
        try {
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return ((User) authentication.getPrincipal()).getUsername();
            }
            if (authentication != null && authentication.getName() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn("Failed to get username from authentication: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    // Exception handlers
    @ExceptionHandler(QueryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleQueryNotFound(QueryNotFoundException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Query not found");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(QueryException.class)
    public ResponseEntity<Map<String, String>> handleQueryException(QueryException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Query error");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid argument");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== QUERY DOCUMENT MANAGEMENT ENDPOINTS ==========

    /**
     * Get all documents associated with a query
     */
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> getQueryDocuments(@PathVariable Long id) {
        try {
            // Verify query exists
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Get documents using DocumentService
            List<DocumentSummary> documents = documentService.getQueryDocuments(id);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Failed to get documents for query {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload documents to a query
     */
    @PostMapping("/{id}/documents")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> uploadQueryDocuments(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication) {
        
        try {
            logger.info("Starting document upload for query {} with {} files", id, files != null ? files.length : 0);
            
            // Validate files parameter
            if (files == null || files.length == 0) {
                logger.error("No files provided for upload");
                return ResponseEntity.badRequest().build();
            }
            
            // Verify query exists and load with workflow
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                logger.error("Query not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            Query query = queryOpt.get();
            logger.info("Found query: {} with workflow: {}", query.getId(), 
                       query.getWorkflow() != null ? query.getWorkflow().getId() : "null");
            
            // Validate that query has workflow
            if (query.getWorkflow() == null) {
                logger.error("Query {} does not have an associated workflow", id);
                return ResponseEntity.badRequest().build();
            }
            
            String uploadedBy = getCurrentUsername(authentication);
            logger.info("Upload requested by user: {}", uploadedBy);

            // Create document upload context for query
            DocumentUploadContext context = DocumentUploadContext.forQuery(query);
            logger.info("Created document upload context: {}", context.getDescription());

            // Upload documents using DocumentService
            List<DocumentSummary> uploadedDocuments = 
                documentService.uploadDocuments(files, context, uploadedBy);

            logger.info("Successfully uploaded {} documents to query {}", uploadedDocuments.size(), id);
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);

        } catch (Exception e) {
            logger.error("Failed to upload documents to query {}: {}", id, e.getMessage(), e);
            e.printStackTrace(); // Add stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload documents to a query response
     */
    @PostMapping("/{id}/responses/{responseId}/documents")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> uploadQueryResponseDocuments(
            @PathVariable Long id,
            @PathVariable Long responseId,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication) {
        
        try {
            // Verify query exists and load with workflow
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Query query = queryOpt.get();
            String uploadedBy = getCurrentUsername(authentication);

            // Create document upload context for query response
            DocumentUploadContext context = DocumentUploadContext.forResponse(query, responseId);

            // Upload documents using DocumentService
            List<DocumentSummary> uploadedDocuments = 
                documentService.uploadDocuments(files, context, uploadedBy);

            logger.info("Successfully uploaded {} documents to query {} response {}", 
                       uploadedDocuments.size(), id, responseId);
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);

        } catch (Exception e) {
            logger.error("Failed to upload documents to query {} response {}: {}", 
                        id, responseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get documents for a specific query response
     */
    @GetMapping("/{id}/responses/{responseId}/documents")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<com.cqs.qrmfg.dto.DocumentSummary>> getQueryResponseDocuments(
            @PathVariable Long id,
            @PathVariable Long responseId) {
        
        try {
            // Verify query exists
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Get response documents using DocumentService
            List<DocumentSummary> documents = 
                documentService.getQueryResponseDocuments(id, responseId);
            return ResponseEntity.ok(documents);

        } catch (Exception e) {
            logger.error("Failed to get documents for query {} response {}: {}", 
                        id, responseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download a query document
     */
    @GetMapping("/{id}/documents/{documentId}/download")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadQueryDocument(
            @PathVariable Long id,
            @PathVariable Long documentId,
            Authentication authentication,
            javax.servlet.http.HttpServletRequest request) {
        
        try {
            // Verify query exists
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            String userId = getCurrentUsername(authentication);
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // Download document with security checks
            Resource resource = 
                documentService.downloadDocumentSecure(documentId, userId, ipAddress, userAgent, null);

            // Get document info for response headers
            Document document = documentService.getDocumentById(documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                       org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(resource);

        } catch (Exception e) {
            logger.error("Failed to download document {} for query {}: {}", 
                        documentId, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a query document
     */
    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteQueryDocument(
            @PathVariable Long id,
            @PathVariable Long documentId,
            Authentication authentication) {
        
        try {
            // Verify query exists
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            String deletedBy = getCurrentUsername(authentication);

            // Check if user has permission to delete the document
            if (!documentService.hasUnifiedDocumentAccess(documentId, deletedBy)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Delete document
            documentService.deleteDocument(documentId, deletedBy);
            
            logger.info("Successfully deleted document {} from query {} by user {}", 
                       documentId, id, deletedBy);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Failed to delete document {} from query {}: {}", 
                        documentId, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get document count for a query
     */
    @GetMapping("/{id}/documents/count")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getQueryDocumentCount(@PathVariable Long id) {
        try {
            // Verify query exists
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            if (!queryOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            long count = documentService.getQueryDocumentCount(id);
            return ResponseEntity.ok(count);

        } catch (Exception e) {
            logger.error("Failed to get document count for query {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Add DocumentService dependency
    @Autowired
    private DocumentService documentService;

    /**
     * Test endpoint to debug query document upload issues
     */
    @GetMapping("/{id}/test-context")
    @PreAuthorize("hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testQueryContext(@PathVariable Long id) {
        try {
            Map<String, Object> result = new java.util.HashMap<>();
            
            // Test query loading
            Optional<Query> queryOpt = queryService.findByIdWithWorkflow(id);
            result.put("queryExists", queryOpt.isPresent());
            
            if (queryOpt.isPresent()) {
                Query query = queryOpt.get();
                result.put("queryId", query.getId());
                result.put("hasWorkflow", query.getWorkflow() != null);
                
                if (query.getWorkflow() != null) {
                    result.put("workflowId", query.getWorkflow().getId());
                    result.put("projectCode", query.getWorkflow().getProjectCode());
                    result.put("materialCode", query.getWorkflow().getMaterialCode());
                    
                    // Test DocumentUploadContext creation
                    try {
                        DocumentUploadContext context = DocumentUploadContext.forQuery(query);
                        result.put("contextCreated", true);
                        result.put("contextType", context.getType().toString());
                        result.put("contextProjectCode", context.getProjectCode());
                        result.put("contextMaterialCode", context.getMaterialCode());
                        result.put("contextDescription", context.getDescription());
                    } catch (Exception e) {
                        result.put("contextCreated", false);
                        result.put("contextError", e.getMessage());
                    }
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Enhanced Query Status Management Endpoints
    

    
    @PostMapping("/{queryId}/close")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('ADMIN')")
    public ResponseEntity<QuerySummaryDto> closeQuery(
            @PathVariable Long queryId, 
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            String username = auth.getName();
            String reason = request.get("reason");
            
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Query query = queryService.closeQuery(queryId, reason, username);
            QuerySummaryDto queryDto = queryMapper.toSummaryDto(query);
            return ResponseEntity.ok(queryDto);
        } catch (QueryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error closing query {}: {}", queryId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/workflow/{workflowId}/can-edit")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> canPlantEditForm(@PathVariable Long workflowId) {
        try {
            boolean canEdit = queryService.canPlantEditForm(workflowId);
            Map<String, Object> response = new HashMap<>();
            response.put("canEdit", canEdit);
            response.put("workflowId", workflowId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking edit permissions for workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/workflow/{workflowId}/status-summary")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getQueryStatusSummary(@PathVariable Long workflowId) {
        try {
            Map<String, Object> summary = queryService.getQueryStatusSummary(workflowId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting query status summary for workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}