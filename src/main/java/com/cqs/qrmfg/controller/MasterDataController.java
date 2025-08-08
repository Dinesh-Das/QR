package com.cqs.qrmfg.controller;


import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.model.QrmfgProjectItemMaster;
import com.cqs.qrmfg.service.MasterDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/master-data")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class MasterDataController {
    
    @Autowired(required = false)
    private MasterDataService masterDataService;
    
    // Location Master endpoints
    @GetMapping("/locations")
    public ResponseEntity<List<QrmfgLocationMaster>> getAllLocations() {
        try {
            System.out.println("=== MasterDataController.getAllLocations() called ===");
            
            if (masterDataService == null) {
                System.err.println("MasterDataService is null, returning fallback data");
                return getFallbackLocations();
            }
            
            System.out.println("Calling masterDataService.getAllLocations()...");
            List<QrmfgLocationMaster> locations = masterDataService.getAllLocations();
            System.out.println("Service returned " + (locations != null ? locations.size() : 0) + " locations");
            
            return ResponseEntity.ok(locations);
            
        } catch (Exception e) {
            // Log the error and return fallback data
            System.err.println("=== ERROR in MasterDataController.getAllLocations() ===");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            
            System.out.println("Attempting to return fallback data...");
            return getFallbackLocations();
        }
    }
    
    @GetMapping("/locations/{locationCode}")
    public ResponseEntity<QrmfgLocationMaster> getLocationByCode(@PathVariable String locationCode) {
        Optional<QrmfgLocationMaster> location = masterDataService.getLocationByCode(locationCode);
        return location.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/locations/search")
    public ResponseEntity<List<QrmfgLocationMaster>> searchLocations(@RequestParam String term) {
        List<QrmfgLocationMaster> locations = masterDataService.searchLocations(term);
        return ResponseEntity.ok(locations);
    }
    
    @PostMapping("/locations")
    public ResponseEntity<QrmfgLocationMaster> createLocation(@RequestBody QrmfgLocationMaster location) {
        QrmfgLocationMaster savedLocation = masterDataService.saveLocation(location);
        return ResponseEntity.ok(savedLocation);
    }
    
    @PutMapping("/locations/{locationCode}")
    public ResponseEntity<QrmfgLocationMaster> updateLocation(@PathVariable String locationCode, 
                                                             @RequestBody QrmfgLocationMaster location) {
        location.setLocationCode(locationCode);
        QrmfgLocationMaster updatedLocation = masterDataService.saveLocation(location);
        return ResponseEntity.ok(updatedLocation);
    }
    
    @DeleteMapping("/locations/{locationCode}")
    public ResponseEntity<Void> deleteLocation(@PathVariable String locationCode) {
        masterDataService.deleteLocation(locationCode);
        return ResponseEntity.noContent().build();
    }
    
    
    
    // Project Item Master endpoints
    @GetMapping("/project-items")
    public ResponseEntity<List<QrmfgProjectItemMaster>> getAllProjectItems() {
        List<QrmfgProjectItemMaster> projectItems = masterDataService.getAllProjectItems();
        return ResponseEntity.ok(projectItems);
    }
    
    @GetMapping("/project-items/projects/{projectCode}")
    public ResponseEntity<List<QrmfgProjectItemMaster>> getItemsByProject(@PathVariable String projectCode) {
        List<QrmfgProjectItemMaster> items = masterDataService.getItemsByProject(projectCode);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/project-items/items/{itemCode}")
    public ResponseEntity<List<QrmfgProjectItemMaster>> getProjectsByItem(@PathVariable String itemCode) {
        List<QrmfgProjectItemMaster> projects = masterDataService.getProjectsByItem(itemCode);
        return ResponseEntity.ok(projects);
    }
    
    @GetMapping("/project-codes")
    public ResponseEntity<List<String>> getAllProjectCodes() {
        List<String> projectCodes = masterDataService.getAllProjectCodes();
        return ResponseEntity.ok(projectCodes);
    }
    
    @GetMapping("/item-codes")
    public ResponseEntity<List<String>> getAllItemCodes() {
        List<String> itemCodes = masterDataService.getAllItemCodes();
        return ResponseEntity.ok(itemCodes);
    }
    
    @GetMapping("/project-codes/{projectCode}/items")
    public ResponseEntity<List<String>> getItemCodesByProject(@PathVariable String projectCode) {
        List<String> itemCodes = masterDataService.getItemCodesByProject(projectCode);
        return ResponseEntity.ok(itemCodes);
    }
    
    @PostMapping("/project-items")
    public ResponseEntity<QrmfgProjectItemMaster> createProjectItem(@RequestBody QrmfgProjectItemMaster projectItem) {
        QrmfgProjectItemMaster savedProjectItem = masterDataService.saveProjectItem(projectItem);
        return ResponseEntity.ok(savedProjectItem);
    }
    
    @DeleteMapping("/project-items/{projectCode}/{itemCode}")
    public ResponseEntity<Void> deleteProjectItem(@PathVariable String projectCode, @PathVariable String itemCode) {
        masterDataService.deleteProjectItem(projectCode, itemCode);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/project-items/{projectCode}/{itemCode}/exists")
    public ResponseEntity<Boolean> existsProjectItem(@PathVariable String projectCode, @PathVariable String itemCode) {
        boolean exists = masterDataService.existsProjectItem(projectCode, itemCode);
        return ResponseEntity.ok(exists);
    }
    
    // Test endpoint to verify location master functionality
    @GetMapping("/locations/test")
    public ResponseEntity<String> testLocationMaster() {
        try {
            List<QrmfgLocationMaster> locations = masterDataService.getAllLocations();
            return ResponseEntity.ok("Location Master API is working. Found " + locations.size() + " locations.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Location Master API error: " + e.getMessage());
        }
    }
    
    // Fallback endpoint that returns hardcoded location data
    @GetMapping("/locations/fallback")
    public ResponseEntity<List<QrmfgLocationMaster>> getFallbackLocations() {
        try {
            System.out.println("=== MasterDataController.getFallbackLocations() called ===");
            
            List<QrmfgLocationMaster> fallbackLocations = java.util.Arrays.asList(
                new QrmfgLocationMaster("1102", "1102"),
                new QrmfgLocationMaster("1103", "1103"),
                new QrmfgLocationMaster("1104", "1104"),
                new QrmfgLocationMaster("1106", "1106"),
                new QrmfgLocationMaster("1107", "1107")
            );
            
            System.out.println("Successfully created fallback locations: " + fallbackLocations.size());
            return ResponseEntity.ok(fallbackLocations);
            
        } catch (Exception e) {
            System.err.println("=== ERROR in MasterDataController fallback ===");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            
            // Return empty list as last resort
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }
    
    // Simple test endpoint that doesn't use any services
    @GetMapping("/locations/simple-test")
    public ResponseEntity<String> simpleTest() {
        return ResponseEntity.ok("Simple test endpoint is working");
    }
    
    // Diagnostic endpoint to check service status
    @GetMapping("/locations/diagnostic")
    public ResponseEntity<String> diagnostic() {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostic Information:\n");
        sb.append("MasterDataService is null: ").append(masterDataService == null).append("\n");
        
        if (masterDataService != null) {
            try {
                List<QrmfgLocationMaster> locations = masterDataService.getAllLocations();
                sb.append("Service call successful. Found ").append(locations.size()).append(" locations.\n");
            } catch (Exception e) {
                sb.append("Service call failed: ").append(e.getMessage()).append("\n");
                sb.append("Exception type: ").append(e.getClass().getSimpleName()).append("\n");
            }
        }
        
        return ResponseEntity.ok(sb.toString());
    }
}