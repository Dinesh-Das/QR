package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.model.QrmfgProjectItemMaster;
import com.cqs.qrmfg.repository.QrmfgLocationMasterRepository;
import com.cqs.qrmfg.repository.QrmfgProjectItemMasterRepository;
import com.cqs.qrmfg.service.MasterDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MasterDataServiceImpl implements MasterDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MasterDataServiceImpl.class);
    
    @Autowired
    private QrmfgLocationMasterRepository locationRepository;
    
    @Autowired
    private QrmfgProjectItemMasterRepository projectItemRepository;
    
    // Location Master operations
    @Override
    @Transactional(readOnly = true)
    public List<QrmfgLocationMaster> getAllLocations() {
        return locationRepository.findAllOrderByLocationCode();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QrmfgLocationMaster> getLocationByCode(String locationCode) {
        return locationRepository.findByLocationCode(locationCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QrmfgLocationMaster> searchLocations(String searchTerm) {
        return locationRepository.searchLocations(searchTerm);
    }
    
    @Override
    public QrmfgLocationMaster saveLocation(QrmfgLocationMaster location) {
        logger.debug("Saving location: {}", location.getLocationCode());
        return locationRepository.save(location);
    }
    
    @Override
    public void deleteLocation(String locationCode) {
        logger.debug("Deleting location: {}", locationCode);
        locationRepository.deleteById(locationCode);
    }
        
    // Project Item Master operations
    @Override
    @Transactional(readOnly = true)
    public List<QrmfgProjectItemMaster> getAllProjectItems() {
        return projectItemRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QrmfgProjectItemMaster> getItemsByProject(String projectCode) {
        return projectItemRepository.findByProjectCode(projectCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QrmfgProjectItemMaster> getProjectsByItem(String itemCode) {
        return projectItemRepository.findByItemCode(itemCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllProjectCodes() {
        return projectItemRepository.findDistinctProjectCodes();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllItemCodes() {
        return projectItemRepository.findDistinctItemCodes();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getItemCodesByProject(String projectCode) {
        return projectItemRepository.findDistinctItemCodesByProject(projectCode);
    }
    
    @Override
    public QrmfgProjectItemMaster saveProjectItem(QrmfgProjectItemMaster projectItem) {
        logger.debug("Saving project-item: {} - {}", projectItem.getProjectCode(), projectItem.getItemCode());
        return projectItemRepository.save(projectItem);
    }
    
    @Override
    public void deleteProjectItem(String projectCode, String itemCode) {
        logger.debug("Deleting project-item: {} - {}", projectCode, itemCode);
        QrmfgProjectItemMaster.ProjectItemId id = new QrmfgProjectItemMaster.ProjectItemId(projectCode, itemCode);
        projectItemRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsProjectItem(String projectCode, String itemCode) {
        return projectItemRepository.existsByProjectCodeAndItemCode(projectCode, itemCode);
    }
}