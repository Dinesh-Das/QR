package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class CqsDataDto {
    private String materialCode;
    private String plantCode;
    private Map<String, Object> data;
    private String syncStatus;
    private LocalDateTime lastSyncTime;
    private String syncMessage;
    private Integer totalFields;
    private Integer populatedFields;
    private Double completionPercentage;
    
    public CqsDataDto() {}
    
    public CqsDataDto(String materialCode, String plantCode, Map<String, Object> data) {
        this.materialCode = materialCode;
        this.plantCode = plantCode;
        this.data = data;
        this.syncStatus = "SYNCED";
        this.lastSyncTime = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    
    public String getSyncMessage() { return syncMessage; }
    public void setSyncMessage(String syncMessage) { this.syncMessage = syncMessage; }
    
    public Integer getTotalFields() { return totalFields; }
    public void setTotalFields(Integer totalFields) { this.totalFields = totalFields; }
    
    public Integer getPopulatedFields() { return populatedFields; }
    public void setPopulatedFields(Integer populatedFields) { this.populatedFields = populatedFields; }
    
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
}