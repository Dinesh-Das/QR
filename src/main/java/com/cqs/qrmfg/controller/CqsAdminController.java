package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.QrmfgAutoCqs;
import com.cqs.qrmfg.model.PlantSpecificData;
import com.cqs.qrmfg.service.CqsIntegrationService;
import com.cqs.qrmfg.repository.QrmfgAutoCqsRepository;
import com.cqs.qrmfg.dto.CqsDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     * Create CQS data for R123456 specifically
     */
    @PostMapping("/create-r123456-data")
    public ResponseEntity<Map<String, Object>> createR123456Data() {
        try {
            Map<String, Object> result = new HashMap<>();
            String materialCode = "R123456";
            
            // Create comprehensive CQS data for R123456
            Map<String, String> cqsData = new HashMap<>();
            cqsData.put("narcoticListed", "no");
            cqsData.put("flashPoint65", "yes");
            cqsData.put("petroleumClass", "class_b");
            cqsData.put("flashPoint21", "no");
            cqsData.put("isCorrosive", "yes");
            cqsData.put("highlyToxic", "no");
            cqsData.put("isExplosive", "no");
            cqsData.put("autoignitionTemp", "no");
            cqsData.put("dustExplosion", "no");
            cqsData.put("electrostaticCharge", "no");
            cqsData.put("ld50Oral", "yes");
            cqsData.put("ld50Dermal", "yes");
            cqsData.put("lc50Inhalation", "yes");
            cqsData.put("carcinogenic", "no");
            cqsData.put("mutagenic", "no");
            cqsData.put("reproductiveToxicants", "no");
            cqsData.put("endocrineDisruptor", "no");
            cqsData.put("recommendedPpe", "Safety Glasses, Chemical Resistant Gloves, Lab Coat");
            cqsData.put("isPoisonous", "no");
            cqsData.put("antidoteSpecified", "yes");
            cqsData.put("cmvrListed", "no");
            cqsData.put("msihcListed", "no");
            cqsData.put("factoriesActListed", "no");
            cqsData.put("spillMeasuresProvided", "yes");
            cqsData.put("silicaContent", "no");
            cqsData.put("swarfAnalysis", "na");
            cqsData.put("envToxic", "no");
            cqsData.put("hhrmCategory", "Category 2");
            cqsData.put("psmTier1Outdoor", "no");
            cqsData.put("psmTier1Indoor", "no");
            cqsData.put("psmTier2Outdoor", "no");
            cqsData.put("psmTier2Indoor", "no");
            cqsData.put("compatibilityClass", "Compatible");
            cqsData.put("sapCompatibility", "yes");
            
            // Create or update the CQS data
            cqsIntegrationService.createOrUpdateCqsData(materialCode, cqsData, "SYSTEM");
            
            result.put("message", "CQS data created/updated for " + materialCode);
            result.put("materialCode", materialCode);
            result.put("fieldsCreated", cqsData.size());
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to create CQS data for R123456");
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
            Map<String, Object> result = new HashMap<>();
            
            // Create sample data for testing including R123456
            String[] materials = {"MAT001", "MAT002", "MAT003", "MAT004", "MAT005", "R123456"};
            int createdCount = 0;
            int updatedCount = 0;
            
            for (String materialCode : materials) {
                Map<String, String> sampleData = new HashMap<>();
                
                // Create comprehensive CQS data based on material
                if (materialCode.equals("R123456")) {
                    // Hazardous material with comprehensive data
                    sampleData.put("narcoticListed", "no");
                    sampleData.put("flashPoint65", "yes");
                    sampleData.put("petroleumClass", "class_b");
                    sampleData.put("flashPoint21", "no");
                    sampleData.put("isCorrosive", "yes");
                    sampleData.put("highlyToxic", "no");
                    sampleData.put("isExplosive", "no");
                    sampleData.put("autoignitionTemp", "no");
                    sampleData.put("dustExplosion", "no");
                    sampleData.put("electrostaticCharge", "no");
                    sampleData.put("ld50Oral", "yes");
                    sampleData.put("ld50Dermal", "yes");
                    sampleData.put("lc50Inhalation", "yes");
                    sampleData.put("carcinogenic", "no");
                    sampleData.put("mutagenic", "no");
                    sampleData.put("reproductiveToxicants", "no");
                    sampleData.put("endocrineDisruptor", "no");
                    sampleData.put("recommendedPpe", "Safety Glasses, Chemical Resistant Gloves, Lab Coat");
                    sampleData.put("isPoisonous", "no");
                    sampleData.put("antidoteSpecified", "yes");
                    sampleData.put("cmvrListed", "no");
                    sampleData.put("msihcListed", "no");
                    sampleData.put("factoriesActListed", "no");
                    sampleData.put("spillMeasuresProvided", "yes");
                    sampleData.put("silicaContent", "no");
                    sampleData.put("swarfAnalysis", "na");
                    sampleData.put("envToxic", "no");
                    sampleData.put("hhrmCategory", "Category 2");
                    sampleData.put("psmTier1Outdoor", "no");
                    sampleData.put("psmTier1Indoor", "no");
                    sampleData.put("psmTier2Outdoor", "no");
                    sampleData.put("psmTier2Indoor", "no");
                    sampleData.put("compatibilityClass", "Compatible");
                    sampleData.put("sapCompatibility", "yes");
                } else {
                    // Default data for other materials
                    sampleData.put("narcoticListed", "no");
                    sampleData.put("flashPoint65", materialCode.equals("MAT001") ? "no" : "yes");
                    sampleData.put("isCorrosive", "no");
                    sampleData.put("highlyToxic", materialCode.equals("MAT001") || materialCode.equals("MAT002") ? "yes" : "no");
                    sampleData.put("recommendedPpe", "Safety Glasses, Gloves");
                    sampleData.put("carcinogenic", "no");
                    sampleData.put("mutagenic", "no");
                    sampleData.put("reproductiveToxicants", "no");
                }
                
                if (cqsRepository.existsByMaterialCode(materialCode)) {
                    // Update existing data
                    cqsIntegrationService.createOrUpdateCqsData(materialCode, sampleData, "SYSTEM");
                    updatedCount++;
                } else {
                    // Create new data
                    cqsIntegrationService.createOrUpdateCqsData(materialCode, sampleData, "SYSTEM");
                    createdCount++;
                }
            }
            
            result.put("message", "Sample CQS data initialized/updated");
            result.put("createdRecords", createdCount);
            result.put("updatedRecords", updatedCount);
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
    
    /**
     * Fix empty CQS inputs in plant-specific data table
     * This method finds all plant records that have empty CQS inputs but should have CQS data
     */
    @PostMapping("/fix-empty-cqs-inputs")
    public ResponseEntity<Map<String, Object>> fixEmptyCqsInputs() {
        try {
            Map<String, Object> result = cqsIntegrationService.fixEmptyCqsInputs();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to fix empty CQS inputs");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}