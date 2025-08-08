package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Simple standalone controller for location data that doesn't depend on any services
 * This is a fallback controller to ensure location data is always available
 */
@RestController
@RequestMapping("/api/v1/simple-locations")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class SimpleLocationController {
    
    @GetMapping
    public ResponseEntity<List<QrmfgLocationMaster>> getAllLocations() {
        try {
            System.out.println("=== SimpleLocationController.getAllLocations() called ===");
            
            List<QrmfgLocationMaster> locations = Arrays.asList(
                new QrmfgLocationMaster("1102", "Manufacturing Unit A - Mumbai"),
                new QrmfgLocationMaster("1103", "Manufacturing Unit B - Delhi"),
                new QrmfgLocationMaster("1104", "Manufacturing Unit C - Bangalore"),
                new QrmfgLocationMaster("1105", "Manufacturing Unit D - Chennai"),
                new QrmfgLocationMaster("1106", "Manufacturing Unit E - Pune"),
                new QrmfgLocationMaster("1107", "Research & Development Center - Hyderabad"),
                new QrmfgLocationMaster("1108", "Quality Control Lab - Kolkata"),
                new QrmfgLocationMaster("1109", "Warehouse & Distribution Center - Ahmedabad"),
                new QrmfgLocationMaster("1110", "Testing & Validation Lab - Gurgaon"),
                new QrmfgLocationMaster("1116", "Corporate Office - Mumbai")
            );
            
            System.out.println("Successfully created " + locations.size() + " locations");
            return ResponseEntity.ok(locations);
            
        } catch (Exception e) {
            System.err.println("=== ERROR in SimpleLocationController ===");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            
            // Return minimal fallback data
            try {
                List<QrmfgLocationMaster> fallback = Arrays.asList(
                    new QrmfgLocationMaster("1102", "Default Plant")
                );
                return ResponseEntity.ok(fallback);
            } catch (Exception fallbackError) {
                System.err.println("Even fallback failed: " + fallbackError.getMessage());
                return ResponseEntity.status(500).build();
            }
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        try {
            System.out.println("=== SimpleLocationController.test() called ===");
            return ResponseEntity.ok("Simple Location Controller is working!");
        } catch (Exception e) {
            System.err.println("Error in test endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Test failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/count")
    public ResponseEntity<String> getCount() {
        try {
            List<QrmfgLocationMaster> locations = getAllLocations().getBody();
            return ResponseEntity.ok("Simple controller has " + (locations != null ? locations.size() : 0) + " locations");
        } catch (Exception e) {
            return ResponseEntity.ok("Error getting count: " + e.getMessage());
        }
    }
    
    @GetMapping("/{locationCode}")
    public ResponseEntity<QrmfgLocationMaster> getLocationByCode(@PathVariable String locationCode) {
        // Simple implementation for specific location codes
        switch (locationCode.toUpperCase()) {
            case "1102":
                return ResponseEntity.ok(new QrmfgLocationMaster("1102", "Manufacturing Unit A - Mumbai"));
            case "1103":
                return ResponseEntity.ok(new QrmfgLocationMaster("1103", "Manufacturing Unit B - Delhi"));
            case "1104":
                return ResponseEntity.ok(new QrmfgLocationMaster("1104", "Manufacturing Unit C - Bangalore"));
            case "1106":
                return ResponseEntity.ok(new QrmfgLocationMaster("1106", "Manufacturing Unit D - Chennai"));
            case "1107":
                return ResponseEntity.ok(new QrmfgLocationMaster("1107", "Manufacturing Unit E - Pune"));
            default:
                return ResponseEntity.notFound().build();
        }
    }
}