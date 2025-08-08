package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.enums.RoleType;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.PlantDataFilter;
import com.cqs.qrmfg.util.RBACConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PlantDataFilter for plant-based data filtering operations.
 */
@Service
public class PlantDataFilterImpl implements PlantDataFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(PlantDataFilterImpl.class);
    
    @Override
    public <T> List<T> filterByPlantAccess(List<T> data, Authentication auth, String plantField) {
        logger.debug("Filtering {} entities by plant access for field: {}", 
                    data != null ? data.size() : 0, plantField);
        
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!shouldApplyPlantFiltering(auth)) {
            logger.debug("No plant filtering required for user");
            return new ArrayList<>(data);
        }
        
        List<String> userPlants = getUserAccessiblePlants(auth);
        if (userPlants.isEmpty()) {
            logger.debug("User has no accessible plants, returning empty list");
            return new ArrayList<>();
        }
        
        List<T> filteredData = data.stream()
            .filter(entity -> {
                String plantCode = extractPlantCode(entity, plantField);
                boolean hasAccess = plantCode != null && userPlants.contains(plantCode.toUpperCase());
                logger.trace("Entity plant code: {}, user has access: {}", plantCode, hasAccess);
                return hasAccess;
            })
            .collect(Collectors.toList());
        
        logger.debug("Filtered {} entities down to {} entities", data.size(), filteredData.size());
        return filteredData;
    }
    
    @Override
    public <T> Specification<T> createPlantFilterSpecification(Authentication auth, String plantField) {
        logger.debug("Creating plant filter specification for field: {}", plantField);
        
        return (root, query, criteriaBuilder) -> {
            if (!shouldApplyPlantFiltering(auth)) {
                logger.debug("No plant filtering required, returning no restriction");
                return criteriaBuilder.conjunction(); // No restriction
            }
            
            List<String> userPlants = getUserAccessiblePlants(auth);
            if (userPlants.isEmpty()) {
                logger.debug("User has no accessible plants, returning false condition");
                return criteriaBuilder.disjunction(); // Always false
            }
            
            Path<String> plantPath = root.get(plantField);
            Predicate plantPredicate = plantPath.in(userPlants);
            
            logger.debug("Created plant filter specification for {} plants", userPlants.size());
            return plantPredicate;
        };
    }
    
    @Override
    public String buildPlantFilterQuery(Authentication auth, String plantField) {
        logger.debug("Building plant filter query for field: {}", plantField);
        
        if (!shouldApplyPlantFiltering(auth)) {
            logger.debug("No plant filtering required, returning empty condition");
            return "1=1"; // Always true
        }
        
        List<String> userPlants = getUserAccessiblePlants(auth);
        if (userPlants.isEmpty()) {
            logger.debug("User has no accessible plants, returning false condition");
            return "1=0"; // Always false
        }
        
        String plantCodes = userPlants.stream()
            .map(plant -> "'" + plant + "'")
            .collect(Collectors.joining(", "));
        
        String query = plantField + " IN (" + plantCodes + ")";
        logger.debug("Built plant filter query: {}", query);
        return query;
    }
    
    @Override
    public String extractPlantCode(Object entity, String plantField) {
        if (entity == null || plantField == null || plantField.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try getter method first (e.g., getPlantCode)
            String getterName = "get" + capitalize(plantField);
            Method getter = entity.getClass().getMethod(getterName);
            Object value = getter.invoke(entity);
            
            if (value != null) {
                String plantCode = value.toString().trim();
                logger.trace("Extracted plant code '{}' from entity using getter method", plantCode);
                return plantCode.isEmpty() ? null : plantCode.toUpperCase();
            }
        } catch (Exception e) {
            logger.trace("Failed to extract plant code using getter method: {}", e.getMessage());
        }
        
        try {
            // Try direct field access
            Field field = entity.getClass().getDeclaredField(plantField);
            field.setAccessible(true);
            Object value = field.get(entity);
            
            if (value != null) {
                String plantCode = value.toString().trim();
                logger.trace("Extracted plant code '{}' from entity using field access", plantCode);
                return plantCode.isEmpty() ? null : plantCode.toUpperCase();
            }
        } catch (Exception e) {
            logger.trace("Failed to extract plant code using field access: {}", e.getMessage());
        }
        
        logger.trace("Could not extract plant code from entity for field: {}", plantField);
        return null;
    }
    
    @Override
    public boolean hasPlantDataAccess(Authentication auth, String plantCode) {
        if (plantCode == null || plantCode.trim().isEmpty()) {
            return false;
        }
        
        if (!shouldApplyPlantFiltering(auth)) {
            return true; // Admin or unrestricted access
        }
        
        List<String> userPlants = getUserAccessiblePlants(auth);
        boolean hasAccess = userPlants.contains(plantCode.toUpperCase());
        
        logger.trace("User has access to plant {}: {}", plantCode, hasAccess);
        return hasAccess;
    }
    
    @Override
    public List<String> getUserAccessiblePlants(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            logger.debug("No authentication or principal, returning empty plant list");
            return Collections.emptyList();
        }
        
        try {
            if (auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                
                // Admin users have access to all plants
                if (user.hasRole(RoleType.ADMIN)) {
                    logger.debug("Admin user detected, has access to all plants");
                    return Collections.singletonList("*"); // Special marker for all plants
                }
                
                List<String> userPlants = user.getAssignedPlantsList();
                logger.debug("User has access to {} plants", userPlants.size());
                return userPlants;
            }
        } catch (Exception e) {
            logger.error("Error extracting user plant access from authentication", e);
        }
        
        logger.debug("Could not determine user plant access, returning empty list");
        return Collections.emptyList();
    }
    
    @Override
    public boolean shouldApplyPlantFiltering(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }
        
        try {
            if (auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                
                // Admin users don't need filtering
                if (user.hasRole(RoleType.ADMIN)) {
                    logger.debug("Admin user detected, no plant filtering required");
                    return false;
                }
                
                // Check if user has a role that supports plant filtering
                boolean supportsFiltering = user.supportsPlantFiltering();
                logger.debug("User supports plant filtering: {}", supportsFiltering);
                return supportsFiltering;
            }
        } catch (Exception e) {
            logger.error("Error determining if plant filtering should be applied", e);
        }
        
        // Default to applying filtering for security
        logger.debug("Defaulting to apply plant filtering for security");
        return true;
    }
    
    @Override
    public <T> Specification<T> createMultiFieldPlantFilterSpecification(Authentication auth, String... plantFields) {
        logger.debug("Creating multi-field plant filter specification for {} fields", plantFields.length);
        
        return (root, query, criteriaBuilder) -> {
            if (!shouldApplyPlantFiltering(auth)) {
                logger.debug("No plant filtering required, returning no restriction");
                return criteriaBuilder.conjunction(); // No restriction
            }
            
            List<String> userPlants = getUserAccessiblePlants(auth);
            if (userPlants.isEmpty()) {
                logger.debug("User has no accessible plants, returning false condition");
                return criteriaBuilder.disjunction(); // Always false
            }
            
            List<Predicate> plantPredicates = new ArrayList<>();
            
            for (String plantField : plantFields) {
                try {
                    Path<String> plantPath = root.get(plantField);
                    Predicate fieldPredicate = plantPath.in(userPlants);
                    plantPredicates.add(fieldPredicate);
                } catch (Exception e) {
                    logger.warn("Could not create predicate for plant field: {}", plantField, e);
                }
            }
            
            if (plantPredicates.isEmpty()) {
                logger.warn("No valid plant field predicates created, returning false condition");
                return criteriaBuilder.disjunction(); // Always false
            }
            
            // User has access if ANY of the plant fields match their accessible plants
            Predicate combinedPredicate = criteriaBuilder.or(plantPredicates.toArray(new Predicate[0]));
            
            logger.debug("Created multi-field plant filter specification with {} predicates", plantPredicates.size());
            return combinedPredicate;
        };
    }
    
    @Override
    public <T> List<T> filterByNestedPlantAccess(List<T> data, Authentication auth, String nestedPath) {
        logger.debug("Filtering {} entities by nested plant access for path: {}", 
                    data != null ? data.size() : 0, nestedPath);
        
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!shouldApplyPlantFiltering(auth)) {
            logger.debug("No plant filtering required for user");
            return new ArrayList<>(data);
        }
        
        List<String> userPlants = getUserAccessiblePlants(auth);
        if (userPlants.isEmpty()) {
            logger.debug("User has no accessible plants, returning empty list");
            return new ArrayList<>();
        }
        
        List<T> filteredData = data.stream()
            .filter(entity -> {
                String plantCode = extractNestedPlantCode(entity, nestedPath);
                boolean hasAccess = plantCode != null && userPlants.contains(plantCode.toUpperCase());
                logger.trace("Entity nested plant code: {}, user has access: {}", plantCode, hasAccess);
                return hasAccess;
            })
            .collect(Collectors.toList());
        
        logger.debug("Filtered {} entities down to {} entities using nested path", data.size(), filteredData.size());
        return filteredData;
    }
    
    @Override
    public <T> Specification<T> createNestedPlantFilterSpecification(Authentication auth, String nestedPath) {
        logger.debug("Creating nested plant filter specification for path: {}", nestedPath);
        
        return (root, query, criteriaBuilder) -> {
            if (!shouldApplyPlantFiltering(auth)) {
                logger.debug("No plant filtering required, returning no restriction");
                return criteriaBuilder.conjunction(); // No restriction
            }
            
            List<String> userPlants = getUserAccessiblePlants(auth);
            if (userPlants.isEmpty()) {
                logger.debug("User has no accessible plants, returning false condition");
                return criteriaBuilder.disjunction(); // Always false
            }
            
            try {
                // Parse nested path (e.g., "location.plantCode")
                String[] pathParts = nestedPath.split("\\.");
                Path<String> nestedPlantPath = root.get(pathParts[0]);
                
                for (int i = 1; i < pathParts.length; i++) {
                    nestedPlantPath = nestedPlantPath.get(pathParts[i]);
                }
                
                Predicate plantPredicate = nestedPlantPath.in(userPlants);
                
                logger.debug("Created nested plant filter specification for path: {}", nestedPath);
                return plantPredicate;
            } catch (Exception e) {
                logger.error("Error creating nested plant filter specification for path: {}", nestedPath, e);
                return criteriaBuilder.disjunction(); // Always false on error
            }
        };
    }
    
    /**
     * Extract plant code from nested object path
     */
    private String extractNestedPlantCode(Object entity, String nestedPath) {
        if (entity == null || nestedPath == null || nestedPath.trim().isEmpty()) {
            return null;
        }
        
        try {
            String[] pathParts = nestedPath.split("\\.");
            Object currentObject = entity;
            
            // Navigate through the nested path
            for (int i = 0; i < pathParts.length - 1; i++) {
                String part = pathParts[i];
                currentObject = getFieldValue(currentObject, part);
                if (currentObject == null) {
                    return null;
                }
            }
            
            // Extract the final plant code field
            String finalField = pathParts[pathParts.length - 1];
            Object value = getFieldValue(currentObject, finalField);
            
            if (value != null) {
                String plantCode = value.toString().trim();
                logger.trace("Extracted nested plant code '{}' from path: {}", plantCode, nestedPath);
                return plantCode.isEmpty() ? null : plantCode.toUpperCase();
            }
        } catch (Exception e) {
            logger.trace("Failed to extract nested plant code from path: {}", nestedPath, e);
        }
        
        return null;
    }
    
    /**
     * Get field value from object using getter method or direct field access
     */
    private Object getFieldValue(Object object, String fieldName) throws Exception {
        if (object == null || fieldName == null) {
            return null;
        }
        
        try {
            // Try getter method first
            String getterName = "get" + capitalize(fieldName);
            Method getter = object.getClass().getMethod(getterName);
            return getter.invoke(object);
        } catch (Exception e) {
            // Try direct field access
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        }
    }
    
    /**
     * Capitalize first letter of a string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}