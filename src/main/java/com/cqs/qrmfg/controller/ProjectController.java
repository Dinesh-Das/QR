package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.repository.QrmfgLocationMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    
    @Autowired
    private QrmfgLocationMasterRepository locationRepository;
    
    @GetMapping("/plants")
    public ResponseEntity<List<Map<String, Object>>> getPlants() {
        logger.info("ProjectController: Getting all plants");
        try {
            List<QrmfgLocationMaster> locations = locationRepository.findAllOrderByLocationCode();
            
            List<Map<String, Object>> plants = new ArrayList<>();
            for (QrmfgLocationMaster location : locations) {
                Map<String, Object> plant = new HashMap<>();
                plant.put("code", location.getLocationCode());
                plant.put("name", location.getDescription());
                plant.put("id", location.getLocationCode()); // Use locationCode as ID
                plants.add(plant);
            }
            
            logger.info("ProjectController: Retrieved {} plants", plants.size());
            return ResponseEntity.ok(plants);
        } catch (Exception e) {
            logger.error("ProjectController: Error retrieving plants", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/plants/{plantCode}/validate")
    public ResponseEntity<Map<String, Object>> validatePlantCode(@PathVariable String plantCode) {
        logger.info("ProjectController: Validating plant code: {}", plantCode);
        try {
            boolean exists = locationRepository.existsByLocationCode(plantCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", exists);
            response.put("plantCode", plantCode);
            
            logger.info("ProjectController: Plant code {} is {}", plantCode, exists ? "valid" : "invalid");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ProjectController: Error validating plant code: {}", plantCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/plants/search")
    public ResponseEntity<List<Map<String, Object>>> searchPlants(@RequestParam String searchTerm) {
        logger.info("ProjectController: Searching plants with term: {}", searchTerm);
        try {
            List<QrmfgLocationMaster> locations = locationRepository.findAllOrderByLocationCode();
            
            List<Map<String, Object>> plants = new ArrayList<>();
            String lowerSearchTerm = searchTerm.toLowerCase();
            
            for (QrmfgLocationMaster location : locations) {
                if (location.getLocationCode().toLowerCase().contains(lowerSearchTerm) ||
                    location.getDescription().toLowerCase().contains(lowerSearchTerm)) {
                    Map<String, Object> plant = new HashMap<>();
                    plant.put("code", location.getLocationCode());
                    plant.put("name", location.getDescription());
                    plant.put("id", location.getLocationCode()); // Use locationCode as ID
                    plants.add(plant);
                }
            }
            
            logger.info("ProjectController: Found {} plants matching '{}'", plants.size(), searchTerm);
            return ResponseEntity.ok(plants);
        } catch (Exception e) {
            logger.error("ProjectController: Error searching plants with term: {}", searchTerm, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getProjects() {
        logger.info("ProjectController: Getting all projects");
        try {
            // For now, return empty list as projects functionality is not implemented
            List<Map<String, Object>> projects = new ArrayList<>();
            
            logger.info("ProjectController: Retrieved {} projects", projects.size());
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            logger.error("ProjectController: Error retrieving projects", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{projectCode}/materials")
    public ResponseEntity<List<Map<String, Object>>> getMaterialsByProject(@PathVariable String projectCode) {
        logger.info("ProjectController: Getting materials for project: {}", projectCode);
        try {
            // For now, return empty list as materials functionality is not implemented
            List<Map<String, Object>> materials = new ArrayList<>();
            
            logger.info("ProjectController: Retrieved {} materials for project {}", materials.size(), projectCode);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            logger.error("ProjectController: Error retrieving materials for project: {}", projectCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{projectCode}/validate")
    public ResponseEntity<Map<String, Object>> validateProjectCode(@PathVariable String projectCode) {
        logger.info("ProjectController: Validating project code: {}", projectCode);
        try {
            // For now, return false as project validation is not implemented
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("projectCode", projectCode);
            
            logger.info("ProjectController: Project code {} validation not implemented", projectCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ProjectController: Error validating project code: {}", projectCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}