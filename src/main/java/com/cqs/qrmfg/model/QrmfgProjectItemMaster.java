package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "QRMFG_PROJECT_ITEMS")
@IdClass(QrmfgProjectItemMaster.ProjectItemId.class)
public class QrmfgProjectItemMaster {
    
    @Id
    @Column(name = "PROJECT_CODE", length = 50)
    private String projectCode;
    
    @Id
    @Column(name = "ITEM_CODE", length = 50)
    private String itemCode;
    
    @Column(name = "ITEM_DESCRIPTION", length = 50)
    private String itemDescription;
    
    public QrmfgProjectItemMaster() {}
    
    public QrmfgProjectItemMaster(String projectCode, String itemCode) {
        this.projectCode = projectCode;
        this.itemCode = itemCode;
    }
    
    public QrmfgProjectItemMaster(String projectCode, String itemCode, String itemDescription) {
        this.projectCode = projectCode;
        this.itemCode = itemCode;
        this.itemDescription = itemDescription;
    }
    
    // Getters and setters
    public String getProjectCode() {
        return projectCode;
    }
    
    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }
    
    public String getItemCode() {
        return itemCode;
    }
    
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    @Override
    public String toString() {
        return String.format("QrmfgProjectItemMaster{projectCode='%s', itemCode='%s', itemDescription='%s'}", 
                           projectCode, itemCode, itemDescription);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QrmfgProjectItemMaster that = (QrmfgProjectItemMaster) o;
        return (projectCode != null ? projectCode.equals(that.projectCode) : that.projectCode == null) &&
               (itemCode != null ? itemCode.equals(that.itemCode) : that.itemCode == null);
    }
    
    @Override
    public int hashCode() {
        int result = projectCode != null ? projectCode.hashCode() : 0;
        result = 31 * result + (itemCode != null ? itemCode.hashCode() : 0);
        return result;
    }
    
    // Composite key class
    public static class ProjectItemId implements Serializable {
        private String projectCode;
        private String itemCode;
        
        public ProjectItemId() {}
        
        public ProjectItemId(String projectCode, String itemCode) {
            this.projectCode = projectCode;
            this.itemCode = itemCode;
        }
        
        public String getProjectCode() {
            return projectCode;
        }
        
        public void setProjectCode(String projectCode) {
            this.projectCode = projectCode;
        }
        
        public String getItemCode() {
            return itemCode;
        }
        
        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProjectItemId that = (ProjectItemId) o;
            return (projectCode != null ? projectCode.equals(that.projectCode) : that.projectCode == null) &&
                   (itemCode != null ? itemCode.equals(that.itemCode) : that.itemCode == null);
        }
        
        @Override
        public int hashCode() {
            int result = projectCode != null ? projectCode.hashCode() : 0;
            result = 31 * result + (itemCode != null ? itemCode.hashCode() : 0);
            return result;
        }
    }
}