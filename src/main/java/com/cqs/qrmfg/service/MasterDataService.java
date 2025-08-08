package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.model.QrmfgProjectItemMaster;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing master data entities
 */
public interface MasterDataService {
    
    // Location Master operations
    List<QrmfgLocationMaster> getAllLocations();
    Optional<QrmfgLocationMaster> getLocationByCode(String locationCode);
    List<QrmfgLocationMaster> searchLocations(String searchTerm);
    QrmfgLocationMaster saveLocation(QrmfgLocationMaster location);
    void deleteLocation(String locationCode);
    
    
    // Project Item Master operations
    List<QrmfgProjectItemMaster> getAllProjectItems();
    List<QrmfgProjectItemMaster> getItemsByProject(String projectCode);
    List<QrmfgProjectItemMaster> getProjectsByItem(String itemCode);
    List<String> getAllProjectCodes();
    List<String> getAllItemCodes();
    List<String> getItemCodesByProject(String projectCode);
    QrmfgProjectItemMaster saveProjectItem(QrmfgProjectItemMaster projectItem);
    void deleteProjectItem(String projectCode, String itemCode);
    boolean existsProjectItem(String projectCode, String itemCode);
}