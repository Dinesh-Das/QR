package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.QrmfgAutoCqs;
import com.cqs.qrmfg.model.PlantSpecificData;
import com.cqs.qrmfg.repository.QrmfgAutoCqsRepository;
import com.cqs.qrmfg.repository.PlantSpecificDataRepository;
import com.cqs.qrmfg.dto.CqsDataDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CqsIntegrationService {
    
    @Autowired
    private QrmfgAutoCqsRepository cqsRepository;
    
    @Autowired
    private PlantSpecificDataRepository plantSpecificDataRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Get CQS data for a material code
     */
    @Transactional(readOnly = true)
    public CqsDataDto getCqsData(String materialCode, String plantCode) {
        try {
            Optional<QrmfgAutoCqs> cqsDataOpt = cqsRepository.findByMaterialCode(materialCode);
            
            if (cqsDataOpt.isPresent()) {
                QrmfgAutoCqs cqsData = cqsDataOpt.get();
                return convertToDto(cqsData, plantCode);
            } else {
                // Return empty CQS data structure
                return createEmptyCqsData(materialCode, plantCode);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CQS data for material: " + materialCode, e);
        }
    }
    
    /**
     * Convert CQS entity to DTO with field mapping
     */
    private CqsDataDto convertToDto(QrmfgAutoCqs cqsEntity, String plantCode) {
        Map<String, Object> cqsData = new HashMap<>();
        
        // Map CQS fields to exact questionnaire field names
        mapCqsField(cqsData, "is_corrosive", cqsEntity.getIsCorrosive());
        mapCqsField(cqsData, "highly_toxic", cqsEntity.getHighlyToxic());
        mapCqsField(cqsData, "flash_point_65", cqsEntity.getFlashPoint65());
        mapCqsField(cqsData, "petroleum_class", cqsEntity.getPetroleumClass());
        mapCqsField(cqsData, "flash_point_21", cqsEntity.getFlashPoint21());
        mapCqsField(cqsData, "ld50_oral", cqsEntity.getLd50Oral());
        mapCqsField(cqsData, "ld50_dermal", cqsEntity.getLd50Dermal());
        mapCqsField(cqsData, "lc50_inhalation", cqsEntity.getLc50Inhalation());
        mapCqsField(cqsData, "carcinogenic", cqsEntity.getCarcinogenic());
        mapCqsField(cqsData, "recommended_ppe", cqsEntity.getRecommendedPpe());
        mapCqsField(cqsData, "is_poisonous", cqsEntity.getIsPoisonous());
        mapCqsField(cqsData, "antidote_specified", cqsEntity.getAntidoteSpecified());
        mapCqsField(cqsData, "cmvr_listed", cqsEntity.getCmvrListed());
        mapCqsField(cqsData, "msihc_listed", cqsEntity.getMsihcListed());
        mapCqsField(cqsData, "factories_act_listed", cqsEntity.getFactoriesActListed());
        
        // Additional CQS fields for comprehensive coverage
        mapCqsField(cqsData, "narcotic_listed", cqsEntity.getNarcoticListed());
        mapCqsField(cqsData, "spill_measures_provided", cqsEntity.getSpillMeasuresProvided());
        mapCqsField(cqsData, "reproductive_toxicants", cqsEntity.getReproductiveToxicants());
        mapCqsField(cqsData, "silica_content", cqsEntity.getSilicaContent());
        mapCqsField(cqsData, "swarf_analysis", cqsEntity.getSwarfAnalysis());
        mapCqsField(cqsData, "env_toxic", cqsEntity.getEnvToxic());
        mapCqsField(cqsData, "hhrm_category", cqsEntity.getHhrmCategory());
        mapCqsField(cqsData, "psm_tier1_outdoor", cqsEntity.getPsmTier1Outdoor());
        mapCqsField(cqsData, "psm_tier1_indoor", cqsEntity.getPsmTier1Indoor());
        mapCqsField(cqsData, "psm_tier2_outdoor", cqsEntity.getPsmTier2Outdoor());
        mapCqsField(cqsData, "psm_tier2_indoor", cqsEntity.getPsmTier2Indoor());
        mapCqsField(cqsData, "compatibility_class", cqsEntity.getCompatibilityClass());
        mapCqsField(cqsData, "sap_compatibility", cqsEntity.getSapCompatibility());
        mapCqsField(cqsData, "is_explosive", cqsEntity.getIsExplosive());
        mapCqsField(cqsData, "autoignition_temp", cqsEntity.getAutoignitionTemp());
        mapCqsField(cqsData, "dust_explosion", cqsEntity.getDustExplosion());
        mapCqsField(cqsData, "electrostatic_charge", cqsEntity.getElectrostaticCharge());
        mapCqsField(cqsData, "mutagenic", cqsEntity.getMutagenic());
        mapCqsField(cqsData, "endocrine_disruptor", cqsEntity.getEndocrineDisruptor());
        
        // Create CQS DTO
        CqsDataDto cqsDto = new CqsDataDto(cqsEntity.getMaterialCode(), plantCode, cqsData);
        cqsDto.setSyncStatus(cqsEntity.getSyncStatus());
        cqsDto.setSyncMessage("CQS data loaded successfully");
        cqsDto.setLastSyncTime(cqsEntity.getLastSyncDate());
        
        // Calculate completion statistics
        Integer populatedFields = cqsRepository.getPopulatedFieldsCount(cqsEntity.getMaterialCode());
        Integer totalFields = cqsRepository.getTotalFieldsCount();
        
        cqsDto.setTotalFields(totalFields);
        cqsDto.setPopulatedFields(populatedFields != null ? populatedFields : 0);
        cqsDto.setCompletionPercentage(totalFields > 0 ? 
            (double) cqsDto.getPopulatedFields() / totalFields * 100 : 0.0);
        
        return cqsDto;
    }
    
    /**
     * Helper method to map CQS field values
     */
    private void mapCqsField(Map<String, Object> cqsData, String fieldName, String value) {
        if (value != null && !value.trim().isEmpty()) {
            cqsData.put(fieldName, value);
        } else {
            cqsData.put(fieldName, null); // Explicitly set null for empty fields
        }
    }
    
    /**
     * Create empty CQS data structure when no data exists
     */
    private CqsDataDto createEmptyCqsData(String materialCode, String plantCode) {
        Map<String, Object> emptyCqsData = new HashMap<>();
        
        // Initialize all CQS fields as null
        String[] cqsFields = {
            "narcotic_listed", "flash_point_65", "petroleum_class", "flash_point_21",
            "is_corrosive", "highly_toxic", "spill_measures_provided", "is_poisonous",
            "antidote_specified", "cmvr_listed", "msihc_listed", "factories_act_listed",
            "recommended_ppe", "reproductive_toxicants", "silica_content", "swarf_analysis",
            "env_toxic", "hhrm_category", "psm_tier1_outdoor", "psm_tier1_indoor",
            "psm_tier2_outdoor", "psm_tier2_indoor", "compatibility_class", "sap_compatibility",
            "is_explosive", "autoignition_temp", "dust_explosion", "electrostatic_charge",
            "ld50_oral", "ld50_dermal", "lc50_inhalation", "carcinogenic", "mutagenic",
            "endocrine_disruptor"
        };
        
        for (String field : cqsFields) {
            emptyCqsData.put(field, null);
        }
        
        CqsDataDto cqsDto = new CqsDataDto(materialCode, plantCode, emptyCqsData);
        cqsDto.setSyncStatus("NO_DATA");
        cqsDto.setSyncMessage("No CQS data found for this material");
        cqsDto.setTotalFields(cqsFields.length);
        cqsDto.setPopulatedFields(0);
        cqsDto.setCompletionPercentage(0.0);
        
        return cqsDto;
    }
    
    /**
     * Check if CQS data exists for a material
     */
    public boolean hasCqsData(String materialCode) {
        return cqsRepository.existsByMaterialCode(materialCode);
    }
    
    /**
     * Get CQS completion statistics
     */
    public Map<String, Object> getCqsCompletionStats(String materialCode) {
        Map<String, Object> stats = new HashMap<>();
        
        if (cqsRepository.existsByMaterialCode(materialCode)) {
            Integer populatedFields = cqsRepository.getPopulatedFieldsCount(materialCode);
            Integer totalFields = cqsRepository.getTotalFieldsCount();
            
            stats.put("totalFields", totalFields);
            stats.put("populatedFields", populatedFields != null ? populatedFields : 0);
            stats.put("completionPercentage", totalFields > 0 ? 
                (double) (populatedFields != null ? populatedFields : 0) / totalFields * 100 : 0.0);
            stats.put("hasData", true);
        } else {
            stats.put("totalFields", cqsRepository.getTotalFieldsCount());
            stats.put("populatedFields", 0);
            stats.put("completionPercentage", 0.0);
            stats.put("hasData", false);
        }
        
        return stats;
    }
    
    /**
     * Create or update CQS data (for testing/admin purposes)
     */
    @Transactional
    public QrmfgAutoCqs createOrUpdateCqsData(String materialCode, Map<String, String> cqsValues, String updatedBy) {
        try {
            QrmfgAutoCqs cqsData = cqsRepository.findByMaterialCode(materialCode)
                .orElse(new QrmfgAutoCqs(materialCode));
            
            // Update fields from the map
            for (Map.Entry<String, String> entry : cqsValues.entrySet()) {
                cqsData.setFieldValue(entry.getKey(), entry.getValue());
            }
            
            cqsData.setUpdatedBy(updatedBy);
            cqsData.setSyncStatus("ACTIVE");
            cqsData.setLastSyncDate(LocalDateTime.now());
            
            return cqsRepository.save(cqsData);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create/update CQS data for material: " + materialCode, e);
        }
    }
    
    /**
     * Get field mapping for questionnaire integration
     */
    public Map<String, String> getCqsFieldMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // Map CQS database field names to questionnaire field names
        mapping.put("narcotic_listed", "Narcotic Listed");
        mapping.put("flash_point_65", "Flash Point > 65°C");
        mapping.put("petroleum_class", "Petroleum Class");
        mapping.put("flash_point_21", "Flash Point < 21°C");
        mapping.put("is_corrosive", "Is Corrosive");
        mapping.put("highly_toxic", "Highly Toxic");
        mapping.put("spill_measures_provided", "Spill Measures Provided");
        mapping.put("is_poisonous", "Is Poisonous");
        mapping.put("antidote_specified", "Antidote Specified");
        mapping.put("cmvr_listed", "CMVR Listed");
        mapping.put("msihc_listed", "MSIHC Listed");
        mapping.put("factories_act_listed", "Factories Act Listed");
        mapping.put("recommended_ppe", "Recommended PPE");
        mapping.put("reproductive_toxicants", "Reproductive Toxicants");
        mapping.put("silica_content", "Silica Content");
        mapping.put("swarf_analysis", "SWARF Analysis");
        mapping.put("env_toxic", "Environmental Toxic");
        mapping.put("hhrm_category", "HHRM Category");
        mapping.put("psm_tier1_outdoor", "PSM Tier 1 Outdoor");
        mapping.put("psm_tier1_indoor", "PSM Tier 1 Indoor");
        mapping.put("psm_tier2_outdoor", "PSM Tier 2 Outdoor");
        mapping.put("psm_tier2_indoor", "PSM Tier 2 Indoor");
        mapping.put("compatibility_class", "Compatibility Class");
        mapping.put("sap_compatibility", "SAP Compatibility");
        mapping.put("is_explosive", "Is Explosive");
        mapping.put("autoignition_temp", "Autoignition Temperature");
        mapping.put("dust_explosion", "Dust Explosion");
        mapping.put("electrostatic_charge", "Electrostatic Charge");
        mapping.put("ld50_oral", "LD50 Oral");
        mapping.put("ld50_dermal", "LD50 Dermal");
        mapping.put("lc50_inhalation", "LC50 Inhalation");
        mapping.put("carcinogenic", "Carcinogenic");
        mapping.put("mutagenic", "Mutagenic");
        mapping.put("endocrine_disruptor", "Endocrine Disruptor");
        
        return mapping;
    }
    
    /**
     * Sync CQS data to all related plant-specific data records
     * This method is called whenever CQS data is created or updated
     */
    @Transactional
    public void syncCqsDataToPlantSpecificRecords(String materialCode, String updatedBy) {
        try {
            // Get CQS data for the material
            Optional<QrmfgAutoCqs> cqsDataOpt = cqsRepository.findByMaterialCode(materialCode);
            
            if (cqsDataOpt.isPresent()) {
                QrmfgAutoCqs cqsData = cqsDataOpt.get();
                
                // Convert CQS data to JSON format
                Map<String, Object> cqsDataMap = convertCqsEntityToMap(cqsData);
                String cqsJsonData = objectMapper.writeValueAsString(cqsDataMap);
                
                // Find all plant-specific data records for this material
                List<PlantSpecificData> plantRecords = plantSpecificDataRepository.findByMaterialCode(materialCode);
                
                // Update each plant-specific record with CQS data
                for (PlantSpecificData plantRecord : plantRecords) {
                    plantRecord.updateCqsData(cqsJsonData, updatedBy);
                    plantSpecificDataRepository.save(plantRecord);
                }
                
                System.out.println("Synced CQS data for material " + materialCode + " to " + plantRecords.size() + " plant-specific records");
                
            } else {
                System.out.println("No CQS data found for material: " + materialCode);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to sync CQS data for material " + materialCode + ": " + e.getMessage());
            throw new RuntimeException("Failed to sync CQS data to plant-specific records", e);
        }
    }
    
    /**
     * Sync CQS data for all materials that have CQS data
     * This method can be used for bulk synchronization
     */
    @Transactional
    public void syncAllCqsDataToPlantSpecificRecords(String updatedBy) {
        try {
            List<QrmfgAutoCqs> allCqsData = cqsRepository.findAll();
            
            for (QrmfgAutoCqs cqsData : allCqsData) {
                syncCqsDataToPlantSpecificRecords(cqsData.getMaterialCode(), updatedBy);
            }
            
            System.out.println("Completed bulk CQS sync for " + allCqsData.size() + " materials");
            
        } catch (Exception e) {
            System.err.println("Failed to perform bulk CQS sync: " + e.getMessage());
            throw new RuntimeException("Failed to sync all CQS data to plant-specific records", e);
        }
    }
    
    /**
     * Create or update plant-specific data record with CQS data
     * This ensures that plant-specific records exist and have CQS data populated
     */
    @Transactional
    public PlantSpecificData createOrUpdatePlantSpecificDataWithCqs(String plantCode, String materialCode, 
                                                                   Long workflowId, String updatedBy) {
        try {
            // Find or create plant-specific data record
            Optional<PlantSpecificData> existingRecord = plantSpecificDataRepository.findByPlantCodeAndMaterialCode(plantCode, materialCode)
                .stream().findFirst();
            
            PlantSpecificData plantRecord;
            if (existingRecord.isPresent()) {
                plantRecord = existingRecord.get();
            } else {
                plantRecord = new PlantSpecificData(plantCode, materialCode);
                plantRecord.setWorkflowId(workflowId);
                plantRecord.setCreatedBy(updatedBy);
            }
            
            // Get and populate CQS data if available
            Optional<QrmfgAutoCqs> cqsDataOpt = cqsRepository.findByMaterialCode(materialCode);
            if (cqsDataOpt.isPresent()) {
                QrmfgAutoCqs cqsData = cqsDataOpt.get();
                Map<String, Object> cqsDataMap = convertCqsEntityToMap(cqsData);
                String cqsJsonData = objectMapper.writeValueAsString(cqsDataMap);
                
                plantRecord.updateCqsData(cqsJsonData, updatedBy);
            } else {
                // Set CQS sync status to indicate no data available
                plantRecord.setCqsSyncStatus("NO_DATA");
                plantRecord.setLastCqsSync(LocalDateTime.now());
                plantRecord.setUpdatedBy(updatedBy);
            }
            
            return plantSpecificDataRepository.save(plantRecord);
            
        } catch (Exception e) {
            System.err.println("Failed to create/update plant-specific data with CQS for plant " + plantCode + 
                             ", material " + materialCode + ": " + e.getMessage());
            throw new RuntimeException("Failed to create/update plant-specific data with CQS", e);
        }
    }
    
    /**
     * Convert CQS entity to Map for JSON serialization
     */
    private Map<String, Object> convertCqsEntityToMap(QrmfgAutoCqs cqsEntity) {
        Map<String, Object> cqsDataMap = new HashMap<>();
        
        // Map all CQS fields
        cqsDataMap.put("materialCode", cqsEntity.getMaterialCode());
        cqsDataMap.put("syncStatus", cqsEntity.getSyncStatus());
        cqsDataMap.put("lastSyncDate", cqsEntity.getLastSyncDate());
        
        // CQS field data
        cqsDataMap.put("is_corrosive", cqsEntity.getIsCorrosive());
        cqsDataMap.put("highly_toxic", cqsEntity.getHighlyToxic());
        cqsDataMap.put("flash_point_65", cqsEntity.getFlashPoint65());
        cqsDataMap.put("petroleum_class", cqsEntity.getPetroleumClass());
        cqsDataMap.put("flash_point_21", cqsEntity.getFlashPoint21());
        cqsDataMap.put("ld50_oral", cqsEntity.getLd50Oral());
        cqsDataMap.put("ld50_dermal", cqsEntity.getLd50Dermal());
        cqsDataMap.put("lc50_inhalation", cqsEntity.getLc50Inhalation());
        cqsDataMap.put("carcinogenic", cqsEntity.getCarcinogenic());
        cqsDataMap.put("recommended_ppe", cqsEntity.getRecommendedPpe());
        cqsDataMap.put("is_poisonous", cqsEntity.getIsPoisonous());
        cqsDataMap.put("antidote_specified", cqsEntity.getAntidoteSpecified());
        cqsDataMap.put("cmvr_listed", cqsEntity.getCmvrListed());
        cqsDataMap.put("msihc_listed", cqsEntity.getMsihcListed());
        cqsDataMap.put("factories_act_listed", cqsEntity.getFactoriesActListed());
        
        // Additional CQS fields
        cqsDataMap.put("narcotic_listed", cqsEntity.getNarcoticListed());
        cqsDataMap.put("spill_measures_provided", cqsEntity.getSpillMeasuresProvided());
        cqsDataMap.put("reproductive_toxicants", cqsEntity.getReproductiveToxicants());
        cqsDataMap.put("silica_content", cqsEntity.getSilicaContent());
        cqsDataMap.put("swarf_analysis", cqsEntity.getSwarfAnalysis());
        cqsDataMap.put("env_toxic", cqsEntity.getEnvToxic());
        cqsDataMap.put("hhrm_category", cqsEntity.getHhrmCategory());
        cqsDataMap.put("psm_tier1_outdoor", cqsEntity.getPsmTier1Outdoor());
        cqsDataMap.put("psm_tier1_indoor", cqsEntity.getPsmTier1Indoor());
        cqsDataMap.put("psm_tier2_outdoor", cqsEntity.getPsmTier2Outdoor());
        cqsDataMap.put("psm_tier2_indoor", cqsEntity.getPsmTier2Indoor());
        cqsDataMap.put("compatibility_class", cqsEntity.getCompatibilityClass());
        cqsDataMap.put("sap_compatibility", cqsEntity.getSapCompatibility());
        cqsDataMap.put("is_explosive", cqsEntity.getIsExplosive());
        cqsDataMap.put("autoignition_temp", cqsEntity.getAutoignitionTemp());
        cqsDataMap.put("dust_explosion", cqsEntity.getDustExplosion());
        cqsDataMap.put("electrostatic_charge", cqsEntity.getElectrostaticCharge());
        cqsDataMap.put("mutagenic", cqsEntity.getMutagenic());
        cqsDataMap.put("endocrine_disruptor", cqsEntity.getEndocrineDisruptor());
        
        return cqsDataMap;
    }
    
    /**
     * Get CQS sync statistics across all plant-specific data records
     */
    public Map<String, Object> getCqsSyncStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<PlantSpecificData> allRecords = plantSpecificDataRepository.findAll();
            List<PlantSpecificData> syncedRecords = plantSpecificDataRepository.findByCqsSyncStatus("SYNCED");
            List<PlantSpecificData> pendingRecords = plantSpecificDataRepository.findByCqsSyncStatus("PENDING");
            List<PlantSpecificData> noDataRecords = plantSpecificDataRepository.findByCqsSyncStatus("NO_DATA");
            
            stats.put("totalRecords", allRecords.size());
            stats.put("syncedRecords", syncedRecords.size());
            stats.put("pendingRecords", pendingRecords.size());
            stats.put("noDataRecords", noDataRecords.size());
            stats.put("syncPercentage", allRecords.size() > 0 ? 
                (double) syncedRecords.size() / allRecords.size() * 100 : 0.0);
            
        } catch (Exception e) {
            System.err.println("Failed to get CQS sync statistics: " + e.getMessage());
            stats.put("error", "Failed to retrieve statistics");
        }
        
        return stats;
    }
}