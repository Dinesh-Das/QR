package com.cqs.qrmfg.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for automatic plant-based data filtering.
 * This annotation can be applied to methods that return data that should be
 * filtered based on the user's assigned plant codes (primarily for PLANT_ROLE users).
 * 
 * The annotation works by intercepting method return values and applying
 * plant-based filtering logic automatically.
 * 
 * Usage examples:
 * - @PlantDataFilter - Uses default "plantCode" field for filtering
 * - @PlantDataFilter(entityField = "location") - Uses "location" field for filtering
 * - @PlantDataFilter(entityField = "plantCode", required = false) - Optional filtering
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlantDataFilter {
    
    /**
     * The field name in the entity/DTO that contains the plant code.
     * This field will be used to match against the user's assigned plant codes.
     * 
     * @return the field name containing plant code (default: "plantCode")
     */
    String entityField() default "plantCode";
    
    /**
     * Whether plant filtering is required for this method.
     * If true, the method will throw an exception if plant filtering cannot be applied.
     * If false, the method will return unfiltered data if filtering fails.
     * 
     * @return true if filtering is required (default), false if optional
     */
    boolean required() default true;
    
    /**
     * Whether to apply filtering only to PLANT_ROLE users.
     * If true, only PLANT_ROLE users will have their data filtered.
     * If false, all users (except ADMIN) will have plant-based filtering applied.
     * 
     * @return true to filter only PLANT_ROLE users (default), false to filter all non-ADMIN users
     */
    boolean plantRoleOnly() default true;
    
    /**
     * Custom error message to display when filtering fails and is required.
     * If not specified, a default message will be generated.
     * 
     * @return custom error message for filtering failures
     */
    String errorMessage() default "";
    
    /**
     * The type of collection/data structure being filtered.
     * This helps the aspect determine how to apply filtering logic.
     * 
     * AUTO - Automatically detect the return type
     * LIST - Return type is a List or Collection
     * PAGE - Return type is a Spring Data Page
     * SINGLE - Return type is a single entity
     * 
     * @return the data type being filtered
     */
    FilterType filterType() default FilterType.AUTO;
    
    /**
     * Enumeration for different types of data structures that can be filtered
     */
    enum FilterType {
        AUTO,    // Automatically detect the return type
        LIST,    // List or Collection return type
        PAGE,    // Spring Data Page return type  
        SINGLE   // Single entity return type
    }
}