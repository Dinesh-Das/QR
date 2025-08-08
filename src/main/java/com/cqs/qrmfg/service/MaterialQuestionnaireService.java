package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Question;
import com.cqs.qrmfg.model.QuestionTemplate;
import com.cqs.qrmfg.repository.QuestionRepository;
import com.cqs.qrmfg.repository.QuestionTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialQuestionnaireService {

    @Autowired
    private QuestionTemplateRepository templateRepository;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Create material-specific questionnaire from template
     * This is what JVC calls when creating a new material workflow
     */
    @Transactional
    public void createMaterialQuestionnaire(String materialCode) {
        // Check if questionnaire already exists for this material
        if (questionRepository.countByMaterialCodeAndIsActiveTrue(materialCode) > 0) {
            throw new RuntimeException("Questionnaire already exists for material: " + materialCode);
        }

        // Get all active template questions
        List<QuestionTemplate> templates = templateRepository.findByIsActiveTrueOrderByOrderIndexAsc();
        
        if (templates.isEmpty()) {
            throw new RuntimeException("No questionnaire template found. Please ensure template data is loaded.");
        }

        // Create material-specific master questions from templates
        List<Question> masterQuestions = templates.stream()
            .map(template -> template.createMasterQuestion(materialCode))
            .collect(Collectors.toList());

        // Save all master questions
        questionRepository.saveAll(masterQuestions);
    }

    /**
     * Create material-specific questionnaire from specific template version
     */
    @Transactional
    public void createMaterialQuestionnaireFromVersion(String materialCode, Integer version) {
        // Check if questionnaire already exists for this material
        if (questionRepository.countByMaterialCodeAndIsActiveTrue(materialCode) > 0) {
            throw new RuntimeException("Questionnaire already exists for material: " + materialCode);
        }

        // Get template questions for specific version (Note: need to add this method to repository)
        List<QuestionTemplate> templates = templateRepository.findByIsActiveTrueOrderByOrderIndexAsc();
        
        if (templates.isEmpty()) {
            throw new RuntimeException("No questionnaire template found for version: " + version);
        }

        // Create material-specific master questions from templates
        List<Question> masterQuestions = templates.stream()
            .map(template -> template.createMasterQuestion(materialCode))
            .collect(Collectors.toList());

        // Save all master questions
        questionRepository.saveAll(masterQuestions);
    }

    /**
     * Get all available template categories
     */
    public List<String> getTemplateCategories() {
        return templateRepository.findDistinctCategories();
    }

    /**
     * Get all template questions for preview
     */
    public List<QuestionTemplate> getTemplateQuestions() {
        return templateRepository.findByIsActiveTrueOrderByOrderIndexAsc();
    }

    /**
     * Get template questions by category
     */
    public List<QuestionTemplate> getTemplateQuestionsByCategory(String category) {
        return templateRepository.findByCategoryAndIsActiveTrueOrderByOrderIndexAsc(category);
    }

    /**
     * Get CQS template questions
     */
    public List<QuestionTemplate> getCQSTemplateQuestions() {
        return templateRepository.findByResponsible("CQS");
    }

    /**
     * Get Plant template questions
     */
    public List<QuestionTemplate> getPlantTemplateQuestions() {
        return templateRepository.findByResponsible("Plant");
    }

    /**
     * Get template statistics
     */
    public TemplateStatistics getTemplateStatistics() {
        TemplateStatistics stats = new TemplateStatistics();
        stats.totalQuestions = templateRepository.countByIsActiveTrue();
        stats.cqsQuestions = (long) templateRepository.findByResponsible("CQS").size();
        stats.plantQuestions = (long) templateRepository.findByResponsible("Plant").size();
        stats.displayOnlyQuestions = (long) templateRepository.findByResponsible("NONE").size();
        stats.categories = templateRepository.findDistinctCategories();
        stats.latestVersion = 1; // Default version since versioning is not implemented
        return stats;
    }

    /**
     * Check if material questionnaire exists
     */
    public boolean materialQuestionnaireExists(String materialCode) {
        return questionRepository.countByMaterialCodeAndIsActiveTrue(materialCode) > 0;
    }

    /**
     * Delete material questionnaire (for cleanup/reset)
     */
    @Transactional
    public void deleteMaterialQuestionnaire(String materialCode) {
        List<Question> questions = questionRepository.findByMaterialCodeAndIsActiveTrue(materialCode);
        questionRepository.deleteAll(questions);
    }

    /**
     * Update material questionnaire from latest template (for template updates)
     */
    @Transactional
    public void updateMaterialQuestionnaireFromTemplate(String materialCode) {
        // Delete existing questionnaire
        deleteMaterialQuestionnaire(materialCode);
        
        // Create new questionnaire from latest template
        createMaterialQuestionnaire(materialCode);
    }

    // Inner class for statistics
    public static class TemplateStatistics {
        public Long totalQuestions;
        public Long cqsQuestions;
        public Long plantQuestions;
        public Long displayOnlyQuestions;
        public List<String> categories;
        public Integer latestVersion;

        @Override
        public String toString() {
            return String.format("TemplateStatistics{total=%d, cqs=%d, plant=%d, display=%d, categories=%d, version=%d}", 
                               totalQuestions, cqsQuestions, plantQuestions, displayOnlyQuestions, 
                               categories != null ? categories.size() : 0, latestVersion);
        }
    }
}