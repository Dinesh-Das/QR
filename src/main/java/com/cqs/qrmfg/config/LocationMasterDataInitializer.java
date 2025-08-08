package com.cqs.qrmfg.config;

import com.cqs.qrmfg.model.QrmfgLocationMaster;
import com.cqs.qrmfg.repository.QrmfgLocationMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the Location Master table with sample data on application startup
 */
@Component
public class LocationMasterDataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationMasterDataInitializer.class);
    
    @Autowired
    private QrmfgLocationMasterRepository locationRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeLocationMasterData();
    }
    
    private void initializeLocationMasterData() {
        try {
            // Check if data already exists
            long count = locationRepository.count();
            if (count > 0) {
                logger.info("Location Master data already exists. Count: {}", count);
                return;
            }
            
            logger.info("Initializing Location Master data...");
            
            // Create sample location data
            QrmfgLocationMaster[] locations = {
                new QrmfgLocationMaster("1102", "Manufacturing Unit A - Mumbai"),
                new QrmfgLocationMaster("1103", "Manufacturing Unit B - Delhi"),
                new QrmfgLocationMaster("1104", "Manufacturing Unit C - Bangalore"),
                new QrmfgLocationMaster("1116", "Corporate Office - Mumbai")
            };
            
            // Save all locations
            for (QrmfgLocationMaster location : locations) {
                locationRepository.save(location);
                logger.debug("Created location: {} - {}", location.getLocationCode(), location.getDescription());
            }
            
            logger.info("Successfully initialized {} location master records", locations.length);
            
        } catch (Exception e) {
            logger.error("Failed to initialize Location Master data", e);
        }
    }
}