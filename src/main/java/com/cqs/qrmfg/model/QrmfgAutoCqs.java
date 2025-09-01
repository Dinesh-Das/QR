package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_AUTO_CQS")
public class QrmfgAutoCqs {
    
    @Id
    @Column(name = "material_code", length = 50)
    private String materialCode;
    
    // Hazard Classification Fields
    @Column(name = "narcotic_listed")
    private String narcoticListed;
    
    @Column(name = "flash_point_65")
    private String flashPoint65;
    
    @Column(name = "petroleum_class")
    private String petroleumClass;
    
    @Column(name = "flash_point_21")
    private String flashPoint21;
    
    @Column(name = "is_corrosive")
    private String isCorrosive;
    
    @Column(name = "highly_toxic")
    private String highlyToxic;
    
    @Column(name = "spill_measures_provided")
    private String spillMeasuresProvided;
    
    @Column(name = "is_poisonous")
    private String isPoisonous;
    
    @Column(name = "antidote_specified")
    private String antidoteSpecified;
    
    @Column(name = "cmvr_listed")
    private String cmvrListed;
    
    @Column(name = "msihc_listed")
    private String msihcListed;
    
    @Column(name = "factories_act_listed")
    private String factoriesActListed;
    
    @Column(name = "recommended_ppe")
    private String recommendedPpe;
    
    @Column(name = "reproductive_toxicants")
    private String reproductiveToxicants;
    
    @Column(name = "silica_content")
    private String silicaContent;
    
    @Column(name = "swarf_analysis")
    private String swarfAnalysis;
    
    @Column(name = "env_toxic")
    private String envToxic;
    
    @Column(name = "hhrm_category")
    private String hhrmCategory;
    
    // PSM Thresholds
    @Column(name = "psm_tier1_outdoor")
    private String psmTier1Outdoor;
    
    @Column(name = "psm_tier1_indoor")
    private String psmTier1Indoor;
    
    @Column(name = "psm_tier2_outdoor")
    private String psmTier2Outdoor;
    
    @Column(name = "psm_tier2_indoor")
    private String psmTier2Indoor;
    
    // Compatibility and Safety
    @Column(name = "compatibility_class")
    private String compatibilityClass;
    
    @Column(name = "sap_compatibility")
    private String sapCompatibility;
    
    @Column(name = "is_explosive")
    private String isExplosive;
    
    @Column(name = "autoignition_temp")
    private String autoignitionTemp;
    
    @Column(name = "dust_explosion")
    private String dustExplosion;
    
    @Column(name = "electrostatic_charge")
    private String electrostaticCharge;
    
    // Toxicity Data
    @Column(name = "ld50_oral")
    private String ld50Oral;
    
    @Column(name = "ld50_dermal")
    private String ld50Dermal;
    
    @Column(name = "lc50_inhalation")
    private String lc50Inhalation;
    
    @Column(name = "carcinogenic")
    private String carcinogenic;
    
    @Column(name = "mutagenic")
    private String mutagenic;
    
    @Column(name = "endocrine_disruptor")
    private String endocrineDisruptor;
    
    // Audit fields
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "sync_status", length = 20)
    private String syncStatus = "ACTIVE";
    
    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;
    
    public QrmfgAutoCqs() {}
    
    public QrmfgAutoCqs(String materialCode) {
        this.materialCode = materialCode;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    public boolean hasValue(String fieldName) {
        try {
            java.lang.reflect.Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            String value = (String) field.get(this);
            return value != null && !value.trim().isEmpty() && !"Pending IMP".equals(value);
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getFieldValue(String fieldName) {
        try {
            java.lang.reflect.Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }
    
    public void setFieldValue(String fieldName, String value) {
        try {
            java.lang.reflect.Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (Exception e) {
            // Log error but don't throw
            System.err.println("Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }
    
    // Getters and setters
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    
    public String getNarcoticListed() { return narcoticListed; }
    public void setNarcoticListed(String narcoticListed) { this.narcoticListed = narcoticListed; }
    
    public String getFlashPoint65() { return flashPoint65; }
    public void setFlashPoint65(String flashPoint65) { this.flashPoint65 = flashPoint65; }
    
    public String getPetroleumClass() { return petroleumClass; }
    public void setPetroleumClass(String petroleumClass) { this.petroleumClass = petroleumClass; }
    
    public String getFlashPoint21() { return flashPoint21; }
    public void setFlashPoint21(String flashPoint21) { this.flashPoint21 = flashPoint21; }
    
    public String getIsCorrosive() { return isCorrosive; }
    public void setIsCorrosive(String isCorrosive) { this.isCorrosive = isCorrosive; }
    
    public String getHighlyToxic() { return highlyToxic; }
    public void setHighlyToxic(String highlyToxic) { this.highlyToxic = highlyToxic; }
    
    public String getSpillMeasuresProvided() { return spillMeasuresProvided; }
    public void setSpillMeasuresProvided(String spillMeasuresProvided) { this.spillMeasuresProvided = spillMeasuresProvided; }
    
    public String getIsPoisonous() { return isPoisonous; }
    public void setIsPoisonous(String isPoisonous) { this.isPoisonous = isPoisonous; }
    
    public String getAntidoteSpecified() { return antidoteSpecified; }
    public void setAntidoteSpecified(String antidoteSpecified) { this.antidoteSpecified = antidoteSpecified; }
    
    public String getCmvrListed() { return cmvrListed; }
    public void setCmvrListed(String cmvrListed) { this.cmvrListed = cmvrListed; }
    
    public String getMsihcListed() { return msihcListed; }
    public void setMsihcListed(String msihcListed) { this.msihcListed = msihcListed; }
    
    public String getFactoriesActListed() { return factoriesActListed; }
    public void setFactoriesActListed(String factoriesActListed) { this.factoriesActListed = factoriesActListed; }
    
    public String getRecommendedPpe() { return recommendedPpe; }
    public void setRecommendedPpe(String recommendedPpe) { this.recommendedPpe = recommendedPpe; }
    
    public String getReproductiveToxicants() { return reproductiveToxicants; }
    public void setReproductiveToxicants(String reproductiveToxicants) { this.reproductiveToxicants = reproductiveToxicants; }
    
    public String getSilicaContent() { return silicaContent; }
    public void setSilicaContent(String silicaContent) { this.silicaContent = silicaContent; }
    
    public String getSwarfAnalysis() { return swarfAnalysis; }
    public void setSwarfAnalysis(String swarfAnalysis) { this.swarfAnalysis = swarfAnalysis; }
    
    public String getEnvToxic() { return envToxic; }
    public void setEnvToxic(String envToxic) { this.envToxic = envToxic; }
    
    public String getHhrmCategory() { return hhrmCategory; }
    public void setHhrmCategory(String hhrmCategory) { this.hhrmCategory = hhrmCategory; }
    
    public String getPsmTier1Outdoor() { return psmTier1Outdoor; }
    public void setPsmTier1Outdoor(String psmTier1Outdoor) { this.psmTier1Outdoor = psmTier1Outdoor; }
    
    public String getPsmTier1Indoor() { return psmTier1Indoor; }
    public void setPsmTier1Indoor(String psmTier1Indoor) { this.psmTier1Indoor = psmTier1Indoor; }
    
    public String getPsmTier2Outdoor() { return psmTier2Outdoor; }
    public void setPsmTier2Outdoor(String psmTier2Outdoor) { this.psmTier2Outdoor = psmTier2Outdoor; }
    
    public String getPsmTier2Indoor() { return psmTier2Indoor; }
    public void setPsmTier2Indoor(String psmTier2Indoor) { this.psmTier2Indoor = psmTier2Indoor; }
    
    public String getCompatibilityClass() { return compatibilityClass; }
    public void setCompatibilityClass(String compatibilityClass) { this.compatibilityClass = compatibilityClass; }
    
    public String getSapCompatibility() { return sapCompatibility; }
    public void setSapCompatibility(String sapCompatibility) { this.sapCompatibility = sapCompatibility; }
    
    public String getIsExplosive() { return isExplosive; }
    public void setIsExplosive(String isExplosive) { this.isExplosive = isExplosive; }
    
    public String getAutoignitionTemp() { return autoignitionTemp; }
    public void setAutoignitionTemp(String autoignitionTemp) { this.autoignitionTemp = autoignitionTemp; }
    
    public String getDustExplosion() { return dustExplosion; }
    public void setDustExplosion(String dustExplosion) { this.dustExplosion = dustExplosion; }
    
    public String getElectrostaticCharge() { return electrostaticCharge; }
    public void setElectrostaticCharge(String electrostaticCharge) { this.electrostaticCharge = electrostaticCharge; }
    
    public String getLd50Oral() { return ld50Oral; }
    public void setLd50Oral(String ld50Oral) { this.ld50Oral = ld50Oral; }
    
    public String getLd50Dermal() { return ld50Dermal; }
    public void setLd50Dermal(String ld50Dermal) { this.ld50Dermal = ld50Dermal; }
    
    public String getLc50Inhalation() { return lc50Inhalation; }
    public void setLc50Inhalation(String lc50Inhalation) { this.lc50Inhalation = lc50Inhalation; }
    
    public String getCarcinogenic() { return carcinogenic; }
    public void setCarcinogenic(String carcinogenic) { this.carcinogenic = carcinogenic; }
    
    public String getMutagenic() { return mutagenic; }
    public void setMutagenic(String mutagenic) { this.mutagenic = mutagenic; }
    
    public String getEndocrineDisruptor() { return endocrineDisruptor; }
    public void setEndocrineDisruptor(String endocrineDisruptor) { this.endocrineDisruptor = endocrineDisruptor; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    
    public LocalDateTime getLastSyncDate() { return lastSyncDate; }
    public void setLastSyncDate(LocalDateTime lastSyncDate) { this.lastSyncDate = lastSyncDate; }
    
    @Override
    public String toString() {
        return String.format("QrmfgAutoCqs{materialCode='%s', syncStatus='%s', lastSync=%s}", 
                           materialCode, syncStatus, lastSyncDate);
    }
}