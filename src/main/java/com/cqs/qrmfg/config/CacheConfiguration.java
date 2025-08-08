package com.cqs.qrmfg.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for project and reference data
 * Optimized for MSDS workflow automation system performance
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Define cache names for project service - optimized for reference data
        cacheManager.setCacheNames(java.util.Arrays.asList(
            // Core reference data caches (high frequency access)
            "projects",
            "materials", 
            "plants",
            "blocks",
            
            // Questionnaire template caches (medium frequency access)
            "questionnaireTemplates",
            "questionnaireSteps",
            "questionnaireTemplatesByStep",
            "questionnaireTemplatesByCategory",
            "questionnaireTemplateByQuestionId",
            "questionnaireStepNumbers",
            "questionnaireCategories",
            
            // Search and filtering caches (dynamic content)
            "searchProjects",
            "searchMaterials",
            "searchPlants",
            
            // Performance insight caches (low frequency access)
            "projectsWithMaterialCount",
            
            // Validation caches (high frequency access)
            "projectValidation",
            "materialValidation",
            "plantValidation",
            "blockValidation"
        ));
        
        // Optimize cache configuration for reference data
        cacheManager.setAllowNullValues(false); // Don't cache null values to save memory
        
        return cacheManager;
    }
}