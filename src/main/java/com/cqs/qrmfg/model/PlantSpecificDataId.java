package com.cqs.qrmfg.model;

import java.io.Serializable;
import java.util.Objects;

public class PlantSpecificDataId implements Serializable {
    private String plantCode;
    private String materialCode;
    
    public PlantSpecificDataId() {}
    
    public PlantSpecificDataId(String plantCode, String materialCode) {
        this.plantCode = plantCode;
        this.materialCode = materialCode;
    }
    
    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }
    
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlantSpecificDataId that = (PlantSpecificDataId) o;
        return Objects.equals(plantCode, that.plantCode) &&
               Objects.equals(materialCode, that.materialCode) ;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(plantCode, materialCode);
    }
    
    @Override
    public String toString() {
        return String.format("PlantSpecificDataId{plant='%s', material='%s'}", 
                           plantCode, materialCode);
    }
}