package com.cqs.qrmfg.model;

import javax.persistence.*;

@Entity
@Table(name = "QRMFG_LOCATIONS")
public class QrmfgLocationMaster {
    
    @Id
    @Column(name = "LOCATION_CODE", length = 50)
    private String locationCode;
    
    @Column(name = "DESCRIPTION", length = 200)
    private String description;
    
    public QrmfgLocationMaster() {}
    
    public QrmfgLocationMaster(String locationCode, String description) {
        this.locationCode = locationCode;
        this.description = description;
    }
    
    // Getters and setters
    public String getLocationCode() {
        return locationCode;
    }
    
    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return String.format("QrmfgLocationMaster{locationCode='%s', description='%s'}", 
                           locationCode, description);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QrmfgLocationMaster that = (QrmfgLocationMaster) o;
        return locationCode != null ? locationCode.equals(that.locationCode) : that.locationCode == null;
    }
    
    @Override
    public int hashCode() {
        return locationCode != null ? locationCode.hashCode() : 0;
    }
}