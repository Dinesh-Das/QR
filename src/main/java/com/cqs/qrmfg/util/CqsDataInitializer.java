package com.cqs.qrmfg.util;

import com.cqs.qrmfg.model.QrmfgAutoCqs;
import com.cqs.qrmfg.model.PlantSpecificData;
import com.cqs.qrmfg.repository.QrmfgAutoCqsRepository;
import com.cqs.qrmfg.repository.PlantSpecificDataRepository;
import com.cqs.qrmfg.service.CqsIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Initializes sample CQS data for testing purposes
 * This component runs on application startup and populates the QRMFG_AUTO_CQS table
 * with sample data to demonstrate the CQS auto-population functionality.
 */
@Component
public class CqsDataInitializer implements CommandLineRunner {
    
    @Autowired
    private QrmfgAutoCqsRepository cqsRepository;
    
    @Autowired
    private PlantSpecificDataRepository plantSpecificDataRepository;
    
    @Autowired
    private CqsIntegrationService cqsIntegrationService;
    
    @Override
    public void run(String... args) throws Exception {
        // Only initialize if no data exists
        if (cqsRepository.count() == 0) {
            initializeSampleCqsData();
            initializeSamplePlantSpecificData();
            syncCqsDataToPlantRecords();
        }
    }
    
    private void initializeSampleCqsData() {
        System.out.println("Initializing sample CQS data...");
        
        List<QrmfgAutoCqs> sampleData = Arrays.asList(
            createSampleCqsData("MAT001", "Acetone", true),
            createSampleCqsData("MAT002", "Benzene", true),
            createSampleCqsData("MAT003", "Toluene", false),
            createSampleCqsData("MAT004", "Methanol", true),
            createSampleCqsData("MAT005", "Ethanol", false)
        );
        
        cqsRepository.saveAll(sampleData);
        System.out.println("Sample CQS data initialized successfully for " + sampleData.size() + " materials");
    }
    
    private QrmfgAutoCqs createSampleCqsData(String materialCode, String materialName, boolean isHazardous) {
        QrmfgAutoCqs cqsData = new QrmfgAutoCqs(materialCode);
        
        // Set common fields
        cqsData.setCreatedBy("SYSTEM");
        cqsData.setUpdatedBy("SYSTEM");
        cqsData.setSyncStatus("ACTIVE");
        cqsData.setLastSyncDate(LocalDateTime.now());
        
        if (isHazardous) {
            // Populate hazardous material data with lowercase values to match radio options
            cqsData.setNarcoticListed("no");
            cqsData.setFlashPoint65("no");
            cqsData.setPetroleumClass("class_b");
            cqsData.setFlashPoint21("yes");
            cqsData.setIsCorrosive("yes");
            cqsData.setHighlyToxic("yes");
            cqsData.setSpillMeasuresProvided("yes");
            cqsData.setIsPoisonous("yes");
            cqsData.setAntidoteSpecified("yes");
            cqsData.setCmvrListed("yes");
            cqsData.setMsihcListed("yes");
            cqsData.setFactoriesActListed("yes");
            cqsData.setRecommendedPpe("Chemical resistant gloves, safety goggles, respirator mask, chemical resistant apron");
            cqsData.setReproductiveToxicants("no");
            cqsData.setSilicaContent("< 1%");
            cqsData.setSwarfAnalysis("Not Required");
            cqsData.setEnvToxic("yes");
            cqsData.setHhrmCategory("Category 2");
            cqsData.setPsmTier1Outdoor("500 kg");
            cqsData.setPsmTier1Indoor("250 kg");
            cqsData.setPsmTier2Outdoor("1000 kg");
            cqsData.setPsmTier2Indoor("500 kg");
            cqsData.setCompatibilityClass("Group A");
            cqsData.setSapCompatibility("Compatible");
            cqsData.setIsExplosive("no");
            cqsData.setAutoignitionTemp("465°C");
            cqsData.setDustExplosion("na");
            cqsData.setElectrostaticCharge("Low Risk");
            cqsData.setLd50Oral("no");  // Less than 200 mg/kg threshold
            cqsData.setLd50Dermal("no"); // Less than 1000 mg/kg threshold
            cqsData.setLc50Inhalation("no"); // Less than 10 mg/L threshold
            cqsData.setCarcinogenic("no");
            cqsData.setMutagenic("no");
            cqsData.setEndocrineDisruptor("no");
        } else {
            // Populate non-hazardous material data with lowercase values
            cqsData.setNarcoticListed("no");
            cqsData.setFlashPoint65("yes");
            cqsData.setPetroleumClass("na");
            cqsData.setFlashPoint21("no");
            cqsData.setIsCorrosive("no");
            cqsData.setHighlyToxic("no");
            cqsData.setSpillMeasuresProvided("yes");
            cqsData.setIsPoisonous("no");
            cqsData.setAntidoteSpecified("no");
            cqsData.setCmvrListed("no");
            cqsData.setMsihcListed("no");
            cqsData.setFactoriesActListed("no");
            cqsData.setRecommendedPpe("Safety glasses, work gloves");
            cqsData.setReproductiveToxicants("no");
            cqsData.setSilicaContent("Not Applicable");
            cqsData.setSwarfAnalysis("Not Required");
            cqsData.setEnvToxic("no");
            cqsData.setHhrmCategory("Category 4");
            cqsData.setPsmTier1Outdoor("Not Applicable");
            cqsData.setPsmTier1Indoor("Not Applicable");
            cqsData.setPsmTier2Outdoor("Not Applicable");
            cqsData.setPsmTier2Indoor("Not Applicable");
            cqsData.setCompatibilityClass("Group D");
            cqsData.setSapCompatibility("Compatible");
            cqsData.setIsExplosive("no");
            cqsData.setAutoignitionTemp("> 400°C");
            cqsData.setDustExplosion("na");
            cqsData.setElectrostaticCharge("Very Low Risk");
            cqsData.setLd50Oral("yes");  // Greater than 200 mg/kg threshold
            cqsData.setLd50Dermal("yes"); // Greater than 1000 mg/kg threshold
            cqsData.setLc50Inhalation("yes"); // Greater than 10 mg/L threshold
            cqsData.setCarcinogenic("no");
            cqsData.setMutagenic("no");
            cqsData.setEndocrineDisruptor("no");
        }
        
        return cqsData;
    }
    
    private void initializeSamplePlantSpecificData() {
        System.out.println("Initializing sample plant-specific data...");
        
        // Sample plants
        String[] plants = {"PLANT001", "PLANT002", "PLANT003"};
        String[] materials = {"MAT001", "MAT002", "MAT003", "MAT004", "MAT005"};
        
        int recordCount = 0;
        for (String plant : plants) {
            for (String material : materials) {
                // Check if record already exists
                List<PlantSpecificData> existing = plantSpecificDataRepository.findByPlantCodeAndMaterialCode(plant, material);
                if (existing.isEmpty()) {
                    PlantSpecificData plantData = new PlantSpecificData(plant, material);
                    plantData.setWorkflowId((long) (recordCount + 1000)); // Sample workflow IDs
                    plantData.setCreatedBy("SYSTEM");
                    plantData.setUpdatedBy("SYSTEM");
                    plantData.setCqsSyncStatus("PENDING");
                    
                    plantSpecificDataRepository.save(plantData);
                    recordCount++;
                }
            }
        }
        
        System.out.println("Sample plant-specific data initialized: " + recordCount + " records created");
    }
    
    private void syncCqsDataToPlantRecords() {
        System.out.println("Syncing CQS data to plant-specific records...");
        
        try {
            // Sync all CQS data to plant-specific records
            cqsIntegrationService.syncAllCqsDataToPlantSpecificRecords("SYSTEM");
            
            // Get sync statistics
            Map<String, Object> stats = cqsIntegrationService.getCqsSyncStatistics();
            System.out.println("CQS sync completed. Statistics: " + stats);
            
        } catch (Exception e) {
            System.err.println("Failed to sync CQS data to plant records: " + e.getMessage());
        }
    }
}