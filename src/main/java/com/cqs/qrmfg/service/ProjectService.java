package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.MaterialOption;
import com.cqs.qrmfg.dto.PlantOption;
import com.cqs.qrmfg.dto.ProjectOption;
import com.cqs.qrmfg.dto.QuestionnaireStepDto;
import com.cqs.qrmfg.dto.QuestionnaireTemplateDto;

import java.util.List;

/**
 * Service interface for project and reference data operations
 */
public interface ProjectService {

    /**
     * Get all active projects
     */
    List<ProjectOption> getActiveProjects();

    /**
     * Get materials for a specific project
     */
    List<MaterialOption> getMaterialsByProject(String projectCode);

    /**
     * Get all plant codes
     */
    List<PlantOption> getPlantCodes();



    /**
     * Validate project code exists
     */
    boolean isValidProjectCode(String projectCode);

    /**
     * Validate material code exists for project
     */
    boolean isValidMaterialCode(String projectCode, String materialCode);

    /**
     * Validate plant code exists
     */
    boolean isValidPlantCode(String plantCode);



    /**
     * Get all active questionnaire templates
     */
    List<QuestionnaireTemplateDto> getQuestionnaireTemplates();

    /**
     * Get questionnaire templates organized by steps
     */
    List<QuestionnaireStepDto> getQuestionnaireSteps();

    /**
     * Get questionnaire templates for a specific step
     */
    List<QuestionnaireTemplateDto> getQuestionnaireTemplatesByStep(Integer stepNumber);

    /**
     * Get questionnaire templates by category
     */
    List<QuestionnaireTemplateDto> getQuestionnaireTemplatesByCategory(String category);

    /**
     * Get questionnaire template by question ID
     */
    QuestionnaireTemplateDto getQuestionnaireTemplateByQuestionId(String questionId);

    /**
     * Get all distinct step numbers
     */
    List<Integer> getQuestionnaireStepNumbers();

    /**
     * Get all distinct categories
     */
    List<String> getQuestionnaireCategories();

    /**
     * Clear all caches
     */
    void clearCache();

    /**
     * Clear specific cache
     */
    void clearCache(String cacheName);

    /**
     * Search projects by partial code or name
     */
    List<ProjectOption> searchProjects(String searchTerm);

    /**
     * Search materials by partial code or description within a project
     */
    List<MaterialOption> searchMaterials(String projectCode, String searchTerm);

    /**
     * Search plants by partial code
     */
    List<PlantOption> searchPlants(String searchTerm);



    /**
     * Get projects with material count for performance insights
     */
    List<ProjectOption> getProjectsWithMaterialCount();



    /**
     * Bulk validate project codes
     */
    java.util.Map<String, Boolean> validateProjectCodes(java.util.List<String> projectCodes);

    /**
     * Bulk validate material codes for projects
     */
    java.util.Map<String, Boolean> validateMaterialCodes(java.util.List<String> projectCodes, java.util.List<String> materialCodes);

    /**
     * Get cache statistics for monitoring
     */
    java.util.Map<String, Object> getCacheStatistics();
}