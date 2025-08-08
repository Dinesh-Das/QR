package com.cqs.qrmfg.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Interface for plant-based data filtering operations.
 * Provides methods to filter data based on user's plant access permissions.
 */
public interface PlantDataFilter {
    
    /**
     * Filter a list of entities based on user's plant access permissions
     * @param data the list of entities to filter
     * @param auth the authentication object containing user information
     * @param plantField the field name in the entity that contains the plant code
     * @param <T> the entity type
     * @return filtered list containing only entities the user has access to
     */
    <T> List<T> filterByPlantAccess(List<T> data, Authentication auth, String plantField);
    
    /**
     * Create a JPA Specification for plant-based filtering
     * @param auth the authentication object containing user information
     * @param plantField the field name in the entity that contains the plant code
     * @param <T> the entity type
     * @return JPA Specification for filtering queries
     */
    <T> Specification<T> createPlantFilterSpecification(Authentication auth, String plantField);
    
    /**
     * Build a SQL WHERE clause for plant-based filtering
     * @param auth the authentication object containing user information
     * @param plantField the field name in the entity that contains the plant code
     * @return SQL WHERE clause string for plant filtering
     */
    String buildPlantFilterQuery(Authentication auth, String plantField);
    
    /**
     * Extract plant code from an entity using reflection
     * @param entity the entity to extract plant code from
     * @param plantField the field name that contains the plant code
     * @return the plant code value, or null if not found
     */
    String extractPlantCode(Object entity, String plantField);
    
    /**
     * Check if user has access to view data for a specific plant
     * @param auth the authentication object containing user information
     * @param plantCode the plant code to check access for
     * @return true if user has access, false otherwise
     */
    boolean hasPlantDataAccess(Authentication auth, String plantCode);
    
    /**
     * Get list of plant codes the user has access to
     * @param auth the authentication object containing user information
     * @return list of accessible plant codes
     */
    List<String> getUserAccessiblePlants(Authentication auth);
    
    /**
     * Check if plant filtering should be applied for the authenticated user
     * @param auth the authentication object containing user information
     * @return true if filtering should be applied, false if user has unrestricted access
     */
    boolean shouldApplyPlantFiltering(Authentication auth);
    
    /**
     * Create a plant filter specification for multiple plant fields
     * @param auth the authentication object containing user information
     * @param plantFields array of field names that contain plant codes
     * @param <T> the entity type
     * @return JPA Specification for filtering queries with multiple plant fields
     */
    <T> Specification<T> createMultiFieldPlantFilterSpecification(Authentication auth, String... plantFields);
    
    /**
     * Filter entities that have plant codes in nested objects
     * @param data the list of entities to filter
     * @param auth the authentication object containing user information
     * @param nestedPath the path to the nested object containing the plant field (e.g., "location.plantCode")
     * @param <T> the entity type
     * @return filtered list containing only entities the user has access to
     */
    <T> List<T> filterByNestedPlantAccess(List<T> data, Authentication auth, String nestedPath);
    
    /**
     * Create a specification for filtering by nested plant fields
     * @param auth the authentication object containing user information
     * @param nestedPath the path to the nested object containing the plant field
     * @param <T> the entity type
     * @return JPA Specification for filtering queries with nested plant fields
     */
    <T> Specification<T> createNestedPlantFilterSpecification(Authentication auth, String nestedPath);
}