package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QrmfgAutoCqs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrmfgAutoCqsRepository extends JpaRepository<QrmfgAutoCqs, String> {
    
    /**
     * Find CQS data by material code
     */
    Optional<QrmfgAutoCqs> findByMaterialCode(String materialCode);
    
    /**
     * Find all active CQS records
     */
    @Query("SELECT c FROM QrmfgAutoCqs c WHERE c.syncStatus = 'ACTIVE'")
    List<QrmfgAutoCqs> findAllActive();
    
    /**
     * Find CQS records by sync status
     */
    List<QrmfgAutoCqs> findBySyncStatus(String syncStatus);
    
    /**
     * Check if CQS data exists for material
     */
    boolean existsByMaterialCode(String materialCode);
    
    /**
     * Get count of populated fields for a material
     */
    @Query(value = "SELECT " +
           "CASE WHEN narcotic_listed IS NOT NULL AND narcotic_listed != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN flash_point_65 IS NOT NULL AND flash_point_65 != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN petroleum_class IS NOT NULL AND petroleum_class != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN flash_point_21 IS NOT NULL AND flash_point_21 != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN is_corrosive IS NOT NULL AND is_corrosive != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN highly_toxic IS NOT NULL AND highly_toxic != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN spill_measures_provided IS NOT NULL AND spill_measures_provided != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN is_poisonous IS NOT NULL AND is_poisonous != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN antidote_specified IS NOT NULL AND antidote_specified != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN cmvr_listed IS NOT NULL AND cmvr_listed != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN msihc_listed IS NOT NULL AND msihc_listed != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN factories_act_listed IS NOT NULL AND factories_act_listed != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN recommended_ppe IS NOT NULL AND recommended_ppe != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN reproductive_toxicants IS NOT NULL AND reproductive_toxicants != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN silica_content IS NOT NULL AND silica_content != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN swarf_analysis IS NOT NULL AND swarf_analysis != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN env_toxic IS NOT NULL AND env_toxic != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN hhrm_category IS NOT NULL AND hhrm_category != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN psm_tier1_outdoor IS NOT NULL AND psm_tier1_outdoor != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN psm_tier1_indoor IS NOT NULL AND psm_tier1_indoor != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN psm_tier2_outdoor IS NOT NULL AND psm_tier2_outdoor != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN psm_tier2_indoor IS NOT NULL AND psm_tier2_indoor != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN compatibility_class IS NOT NULL AND compatibility_class != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN sap_compatibility IS NOT NULL AND sap_compatibility != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN is_explosive IS NOT NULL AND is_explosive != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN autoignition_temp IS NOT NULL AND autoignition_temp != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN dust_explosion IS NOT NULL AND dust_explosion != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN electrostatic_charge IS NOT NULL AND electrostatic_charge != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN ld50_oral IS NOT NULL AND ld50_oral != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN ld50_dermal IS NOT NULL AND ld50_dermal != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN lc50_inhalation IS NOT NULL AND lc50_inhalation != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN carcinogenic IS NOT NULL AND carcinogenic != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN mutagenic IS NOT NULL AND mutagenic != '' THEN 1 ELSE 0 END + " +
           "CASE WHEN endocrine_disruptor IS NOT NULL AND endocrine_disruptor != '' THEN 1 ELSE 0 END " +
           "AS populated_fields_count " +
           "FROM QRMFG_AUTO_CQS WHERE material_code = :materialCode", 
           nativeQuery = true)
    Integer getPopulatedFieldsCount(@Param("materialCode") String materialCode);
    
    /**
     * Get total CQS fields count (constant)
     */
    default Integer getTotalFieldsCount() {
        return 33; // Total number of CQS fields
    }
    
    /**
     * Find materials with incomplete CQS data
     */
    @Query("SELECT c FROM QrmfgAutoCqs c WHERE c.syncStatus = 'INCOMPLETE' OR c.syncStatus = 'PENDING'")
    List<QrmfgAutoCqs> findIncompleteRecords();
    
    /**
     * Update sync status for a material
     */
    @Query("UPDATE QrmfgAutoCqs c SET c.syncStatus = :status, c.lastSyncDate = CURRENT_TIMESTAMP WHERE c.materialCode = :materialCode")
    void updateSyncStatus(@Param("materialCode") String materialCode, @Param("status") String status);
}