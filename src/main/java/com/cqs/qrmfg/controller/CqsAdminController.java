package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.QrmfgAutoCqs;
import com.cqs.qrmfg.model.PlantSpecificData;
import com.cqs.qrmfg.service.CqsIntegrationService;
import com.cqs.qrmfg.repository.QrmfgAutoCqsRepository;
import com.cqs.qrmfg.dto.CqsDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing CQS data
 * Provides endpoints for viewing and managing CQS auto-population data
 */
@RestController
@RequestMapping("/api/v1/admin/cqs")
public class CqsAdminController {
    
    @Autowired
    private CqsIntegrationService cqsIntegrationService;
    
    @Autowired
    private QrmfgAutoCqsRepository cqsRepository;
    
    /**
     * Get all CQS data records
     */
    @GetMapping("/data")
    public ResponseEntity<List<QrmfgAutoCqs>> getAllCqsData() {
        try {
            List<QrmfgAutoCqs> cqsData = cqsRepository.findAll();
            return ResponseEntity.ok(cqsData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get CQS data for a specific material
     */
    @GetMapping("/data/{materialCode}")
    public ResponseEntity<QrmfgAutoCqs> getCqsDataByMaterial(@PathVariable String materialCode) {
        try {
            return cqsRepository.findByMaterialCode(materialCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Create or update CQS data for a material
     */
    @PostMapping("/data/{materialCode}")
    public ResponseEntity<QrmfgAutoCqs> createOrUpdateCqsData(
            @PathVariable String materialCode,
            @RequestBody Map<String, String> cqsValues,
            @RequestParam(defaultValue = "ADMIN") String updatedBy) {
        try {
            QrmfgAutoCqs cqsData = cqsIntegrationService.createOrUpdateCqsData(
                materialCode, cqsValues, updatedBy);
            
            // Automatically sync CQS data to plant-specific records
            cqsIntegrationService.syncCqsDataToPlantSpecificRecords(materialCode, updatedBy);
            
            return ResponseEntity.ok(cqsData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete CQS data for a material
     */
    @DeleteMapping("/data/{materialCode}")
    public ResponseEntity<Void> deleteCqsData(@PathVariable String materialCode) {
        try {
            if (cqsRepository.existsByMaterialCode(materialCode)) {
                cqsRepository.deleteById(materialCode);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get CQS statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCqsStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            long totalRecords = cqsRepository.count();
            long activeRecords = cqsRepository.findBySyncStatus("ACTIVE").size();
            long incompleteRecords = cqsRepository.findIncompleteRecords().size();
            
            stats.put("totalRecords", totalRecords);
            stats.put("activeRecords", activeRecords);
            stats.put("incompleteRecords", incompleteRecords);
            stats.put("completionRate", totalRecords > 0 ? 
                (double) activeRecords / totalRecords * 100 : 0.0);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get CQS field mapping
     */
    @GetMapping("/field-mapping")
    public ResponseEntity<Map<String, String>> getCqsFieldMapping() {
        try {
            Map<String, String> mapping = cqsIntegrationService.getCqsFieldMapping();
            return ResponseEntity.ok(mapping);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Bulk update CQS sync status
     */
    @PostMapping("/bulk-update-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateSyncStatus(
            @RequestParam String fromStatus,
            @RequestParam String toStatus,
            @RequestParam(defaultValue = "ADMIN") String updatedBy) {
        try {
            List<QrmfgAutoCqs> records = cqsRepository.findBySyncStatus(fromStatus);
            
            for (QrmfgAutoCqs record : records) {
                record.setSyncStatus(toStatus);
                record.setUpdatedBy(updatedBy);
            }
            
            cqsRepository.saveAll(records);
            
            Map<String, Object> result = new HashMap<>();
            result.put("updatedCount", records.size());
            result.put("fromStatus", fromStatus);
            result.put("toStatus", toStatus);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Test CQS integration for a material
     */
    @GetMapping("/test/{materialCode}")
    public ResponseEntity<Map<String, Object>> testCqsIntegration(@PathVariable String materialCode) {
        try {
            Map<String, Object> testResult = new HashMap<>();
            
            // Check if CQS data exists
            boolean hasData = cqsIntegrationService.hasCqsData(materialCode);
            testResult.put("hasData", hasData);
            
            if (hasData) {
                // Get completion stats
                Map<String, Object> stats = cqsIntegrationService.getCqsCompletionStats(materialCode);
                testResult.put("completionStats", stats);
                
                // Get actual CQS data
                CqsDataDto cqsData = cqsIntegrationService.getCqsData(materialCode, "TEST_PLANT");
                testResult.put("cqsData", cqsData);
            }
            
            testResult.put("materialCode", materialCode);
            testResult.put("testTimestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Test failed");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Initialize sample CQS data manually (for testing)
     */
    @PostMapping("/init-sample-data")
    public ResponseEntity<Map<String, Object>> initializeSampleData() {
        try {
            // This will trigger the CqsDataInitializer logic manually
            Map<String, Object> result = new HashMap<>();
            
            long existingCount = cqsRepository.count();
            if (existingCount > 0) {
                result.put("message", "Sample data already exists");
                result.put("existingRecords", existingCount);
                return ResponseEntity.ok(result);
            }
            
            // Create sample data for testing
            String[] materials = {"MAT001", "MAT002", "MAT003", "MAT004", "MAT005"};
            int createdCount = 0;
            
            for (String materialCode : materials) {
                if (!cqsRepository.existsByMaterialCode(materialCode)) {
                    Map<String, String> sampleData = new HashMap<>();
                    sampleData.put("narcoticListed", "No");
                    sampleData.put("flashPoint65", materialCode.equals("MAT001") ? "No" : "Yes");
                    sampleData.put("isCorrosive", "No");
                    sampleData.put("highlyToxic", materialCode.equals("MAT001") || materialCode.equals("MAT002") ? "Yes" : "No");
                    sampleData.put("recommendedPpe", "Safety Glasses, Gloves");
                    sampleData.put("carcinogenic", "No");
                    sampleData.put("mutagenic", "No");
                    
                    cqsIntegrationService.createOrUpdateCqsData(materialCode, sampleData, "SYSTEM");
                    createdCount++;
                }
            }
            
            result.put("message", "Sample CQS data initialized");
            result.put("createdRecords", createdCount);
            result.put("totalRecords", cqsRepository.count());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to initialize sample data");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Sync CQS data to plant-specific records for a specific material
     */
    @PostMapping("/sync/{materialCode}")
    public ResponseEntity<Map<String, Object>> syncCqsDataToPlantRecords(
            @PathVariable String materialCode,
            @RequestParam(defaultValue = "ADMIN") String updatedBy) {
        try {
            cqsIntegrationService.syncCqsDataToPlantSpecificRecords(materialCode, updatedBy);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "CQS data synced to plant-specific records successfully");
            result.put("materialCode", materialCode);
            result.put("syncTimestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to sync CQS data");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Sync all CQS data to plant-specific records (bulk operation)
     */
    @PostMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllCqsDataToPlantRecords(
            @RequestParam(defaultValue = "ADMIN") String updatedBy) {
        try {
            cqsIntegrationService.syncAllCqsDataToPlantSpecificRecords(updatedBy);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "All CQS data synced to plant-specific records successfully");
            result.put("syncTimestamp", System.currentTimeMillis());
            
            // Get sync statistics
            Map<String, Object> stats = cqsIntegrationService.getCqsSyncStatistics();
            result.put("syncStatistics", stats);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to sync all CQS data");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Get CQS sync statistics for plant-specific data
     */
    @GetMapping("/sync-stats")
    public ResponseEntity<Map<String, Object>> getCqsSyncStatistics() {
        try {
            Map<String, Object> stats = cqsIntegrationService.getCqsSyncStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to get sync statistics");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Create or update plant-specific data with CQS sync
     */
    @PostMapping("/plant-data")
    public ResponseEntity<Map<String, Object>> createPlantSpecificDataWithCqs(
            @RequestParam String plantCode,
            @RequestParam String materialCode,
            @RequestParam Long workflowId,
            @RequestParam(defaultValue = "ADMIN") String updatedBy) {
        try {
            PlantSpecificData plantData = cqsIntegrationService.createOrUpdatePlantSpecificDataWithCqs(
                plantCode, materialCode, workflowId, updatedBy);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Plant-specific data created/updated with CQS sync");
            result.put("plantCode", plantData.getPlantCode());
            result.put("materialCode", plantData.getMaterialCode());
            result.put("cqsSyncStatus", plantData.getCqsSyncStatus());
            result.put("lastCqsSync", plantData.getLastCqsSync());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to create plant-specific data with CQS");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}