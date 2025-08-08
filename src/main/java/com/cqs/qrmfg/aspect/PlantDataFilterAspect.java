package com.cqs.qrmfg.aspect;

import com.cqs.qrmfg.annotation.PlantDataFilter;
import com.cqs.qrmfg.exception.PlantAccessDeniedException;
import com.cqs.qrmfg.service.RBACAuthorizationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AOP Aspect for processing @PlantDataFilter annotations.
 * This aspect intercepts method return values and applies plant-based filtering
 * to ensure users only see data for plants they have access to.
 */
@Aspect
@Component
public class PlantDataFilterAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PlantDataFilterAspect.class);
    
    @Autowired
    private RBACAuthorizationService rbacAuthorizationService;
    
    /**
     * Intercept method calls annotated with @PlantDataFilter and apply filtering to return values
     */
    @Around("@annotation(plantDataFilter)")
    public Object applyPlantDataFiltering(ProceedingJoinPoint joinPoint, PlantDataFilter plantDataFilter) throws Throwable {
        logger.debug("Applying plant data filtering for method: {}", joinPoint.getSignature().getName());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            logger.warn("No authenticated user found for plant data filtering on method: {}", joinPoint.getSignature().getName());
            if (plantDataFilter.required()) {
                throw new PlantAccessDeniedException("Authentication required for plant data access");
            }
            return joinPoint.proceed();
        }
        
        // Check if filtering should be applied for this user
        if (!shouldApplyFiltering(auth, plantDataFilter)) {
            logger.debug("Plant filtering not required for user, proceeding without filtering");
            return joinPoint.proceed();
        }
        
        // Execute the original method
        Object result = joinPoint.proceed();
        
        // Apply filtering to the result
        return applyFilteringToResult(result, auth, plantDataFilter, joinPoint);
    }
    
    /**
     * Determine if plant filtering should be applied for the current user
     */
    private boolean shouldApplyFiltering(Authentication auth, PlantDataFilter plantDataFilter) {
        // Admin users bypass filtering unless explicitly configured otherwise
        if (rbacAuthorizationService.isUserAdmin(auth)) {
            return false;
        }
        
        // If plantRoleOnly is true, only apply filtering to PLANT_ROLE users
        if (plantDataFilter.plantRoleOnly()) {
            return rbacAuthorizationService.isUserPlantUser(auth);
        }
        
        // Apply filtering to all non-admin users
        return true;
    }
    
    /**
     * Apply plant-based filtering to the method result
     */
    private Object applyFilteringToResult(Object result, Authentication auth, PlantDataFilter plantDataFilter, ProceedingJoinPoint joinPoint) {
        if (result == null) {
            return null;
        }
        
        try {
            PlantDataFilter.FilterType filterType = plantDataFilter.filterType();
            
            // Auto-detect filter type if not specified
            if (filterType == PlantDataFilter.FilterType.AUTO) {
                filterType = detectFilterType(result);
            }
            
            switch (filterType) {
                case LIST:
                    return filterListResult(result, auth, plantDataFilter);
                case PAGE:
                    return filterPageResult(result, auth, plantDataFilter);
                case SINGLE:
                    return filterSingleResult(result, auth, plantDataFilter);
                default:
                    logger.warn("Unknown filter type for method: {}, returning unfiltered result", joinPoint.getSignature().getName());
                    return result;
            }
        } catch (Exception e) {
            logger.error("Error applying plant data filtering for method: {}", joinPoint.getSignature().getName(), e);
            
            if (plantDataFilter.required()) {
                String errorMessage = plantDataFilter.errorMessage().isEmpty() ? 
                        "Failed to apply plant-based data filtering" : plantDataFilter.errorMessage();
                throw new PlantAccessDeniedException(errorMessage, true);
            }
            
            // Return unfiltered result if filtering is not required
            return result;
        }
    }
    
    /**
     * Auto-detect the filter type based on the result object
     */
    private PlantDataFilter.FilterType detectFilterType(Object result) {
        if (result instanceof Page) {
            return PlantDataFilter.FilterType.PAGE;
        } else if (result instanceof Collection) {
            return PlantDataFilter.FilterType.LIST;
        } else {
            return PlantDataFilter.FilterType.SINGLE;
        }
    }
    
    /**
     * Filter a List or Collection result
     */
    @SuppressWarnings("unchecked")
    private Object filterListResult(Object result, Authentication auth, PlantDataFilter plantDataFilter) {
        if (!(result instanceof Collection)) {
            logger.warn("Expected Collection but got: {}", result.getClass().getSimpleName());
            return result;
        }
        
        Collection<Object> collection = (Collection<Object>) result;
        if (collection.isEmpty()) {
            return result;
        }
        
        List<String> userPlantCodes = rbacAuthorizationService.getUserPlantCodes(auth);
        if (userPlantCodes.isEmpty()) {
            logger.debug("User has no assigned plants, returning empty collection");
            return collection instanceof List ? new ArrayList<>() : new HashSet<>();
        }
        
        Function<Object, String> plantExtractor = createPlantExtractor(plantDataFilter.entityField());
        
        List<Object> filteredList = collection.stream()
                .filter(item -> {
                    String plantCode = plantExtractor.apply(item);
                    // Allow items with null plant codes or matching plant codes
                    return plantCode == null || userPlantCodes.contains(plantCode);
                })
                .collect(Collectors.toList());
        
        logger.debug("Filtered collection from {} to {} items based on plant access", 
                collection.size(), filteredList.size());
        
        // Return the same collection type as input
        if (result instanceof List) {
            return filteredList;
        } else if (result instanceof Set) {
            return new HashSet<>(filteredList);
        } else {
            return filteredList;
        }
    }
    
    /**
     * Filter a Page result
     */
    @SuppressWarnings("unchecked")
    private Object filterPageResult(Object result, Authentication auth, PlantDataFilter plantDataFilter) {
        if (!(result instanceof Page)) {
            logger.warn("Expected Page but got: {}", result.getClass().getSimpleName());
            return result;
        }
        
        Page<Object> page = (Page<Object>) result;
        if (page.isEmpty()) {
            return result;
        }
        
        List<String> userPlantCodes = rbacAuthorizationService.getUserPlantCodes(auth);
        if (userPlantCodes.isEmpty()) {
            logger.debug("User has no assigned plants, returning empty page");
            return new PageImpl<>(Collections.emptyList(), page.getPageable(), 0);
        }
        
        Function<Object, String> plantExtractor = createPlantExtractor(plantDataFilter.entityField());
        
        List<Object> filteredContent = page.getContent().stream()
                .filter(item -> {
                    String plantCode = plantExtractor.apply(item);
                    // Allow items with null plant codes or matching plant codes
                    return plantCode == null || userPlantCodes.contains(plantCode);
                })
                .collect(Collectors.toList());
        
        logger.debug("Filtered page content from {} to {} items based on plant access", 
                page.getContent().size(), filteredContent.size());
        
        return new PageImpl<>(filteredContent, page.getPageable(), filteredContent.size());
    }
    
    /**
     * Filter a single entity result
     */
    private Object filterSingleResult(Object result, Authentication auth, PlantDataFilter plantDataFilter) {
        List<String> userPlantCodes = rbacAuthorizationService.getUserPlantCodes(auth);
        if (userPlantCodes.isEmpty()) {
            logger.debug("User has no assigned plants, returning null for single entity");
            return null;
        }
        
        Function<Object, String> plantExtractor = createPlantExtractor(plantDataFilter.entityField());
        String plantCode = plantExtractor.apply(result);
        
        // Allow access to entities with null plant codes (e.g., JVC-initiated workflows)
        if (plantCode == null) {
            logger.debug("Single entity access granted for entity with null plant code");
            return result;
        }
        
        if (userPlantCodes.contains(plantCode)) {
            logger.debug("Single entity access granted for plant: {}", plantCode);
            return result;
        } else {
            logger.debug("Single entity access denied for plant: {}", plantCode);
            if (plantDataFilter.required()) {
                throw new PlantAccessDeniedException(plantCode, userPlantCodes);
            }
            return null;
        }
    }
    
    /**
     * Create a function to extract plant code from entities using reflection
     */
    private Function<Object, String> createPlantExtractor(String fieldName) {
        return entity -> {
            if (entity == null) {
                return null;
            }
            
            try {
                // Handle nested field paths (e.g., "location.plantCode")
                if (fieldName.contains(".")) {
                    return extractNestedFieldValue(entity, fieldName);
                } else {
                    return extractFieldValue(entity, fieldName);
                }
            } catch (Exception e) {
                logger.warn("Failed to extract plant code from field '{}' in entity: {}", 
                        fieldName, entity.getClass().getSimpleName(), e);
                return null;
            }
        };
    }
    
    /**
     * Extract field value using reflection
     */
    private String extractFieldValue(Object entity, String fieldName) throws Exception {
        Class<?> clazz = entity.getClass();
        
        // Try to find the field directly
        Field field = findField(clazz, fieldName);
        if (field != null) {
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? value.toString() : null;
        }
        
        // Try getter method
        String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Object value = clazz.getMethod(getterName).invoke(entity);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.debug("Getter method '{}' not found or failed for class: {}", getterName, clazz.getSimpleName());
        }
        
        return null;
    }
    
    /**
     * Extract nested field value (e.g., "location.plantCode")
     */
    private String extractNestedFieldValue(Object entity, String nestedPath) throws Exception {
        String[] parts = nestedPath.split("\\.");
        Object current = entity;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = extractFieldValue(current, parts[i]);
            if (current == null) {
                return null;
            }
        }
        
        return extractFieldValue(current, parts[parts.length - 1]);
    }
    
    /**
     * Find field in class hierarchy
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}