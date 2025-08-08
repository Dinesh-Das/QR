package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByMaterialCodeAndIsActiveTrue(String materialCode);
    
    List<Question> findByMaterialCodeAndStepNumberAndIsActiveTrue(String materialCode, Integer stepNumber);
    
    List<Question> findByMaterialCodeAndCategoryAndIsActiveTrue(String materialCode, String category);
    
    Optional<Question> findByQuestionIdAndIsActiveTrue(String questionId);
    
    List<Question> findByMaterialCodeAndIsActiveTrueOrderByOrderIndexAsc(String materialCode);
    
    List<Question> findByMaterialCodeAndStepNumberAndIsActiveTrueOrderByOrderIndexAsc(String materialCode, Integer stepNumber);
    
    @Query("SELECT DISTINCT q.stepNumber FROM Question q WHERE q.materialCode = :materialCode AND q.isActive = true ORDER BY q.stepNumber")
    List<Integer> findDistinctStepNumbersByMaterialCode(@Param("materialCode") String materialCode);
    
    @Query("SELECT DISTINCT q.category FROM Question q WHERE q.materialCode = :materialCode AND q.isActive = true ORDER BY q.category")
    List<String> findDistinctCategoriesByMaterialCode(@Param("materialCode") String materialCode);
    
    @Query("SELECT q FROM Question q WHERE q.materialCode = :materialCode AND q.responsible = :responsible AND q.isActive = true ORDER BY q.orderIndex")
    List<Question> findByMaterialCodeAndResponsible(@Param("materialCode") String materialCode, @Param("responsible") String responsible);
    
    @Query("SELECT q FROM Question q WHERE q.materialCode = :materialCode AND q.questionType = :questionType AND q.isActive = true ORDER BY q.orderIndex")
    List<Question> findByMaterialCodeAndQuestionType(@Param("materialCode") String materialCode, @Param("questionType") String questionType);
    
    @Query("SELECT q FROM Question q WHERE q.dependsOnQuestionId = :questionId AND q.isActive = true ORDER BY q.orderIndex")
    List<Question> findDependentQuestions(@Param("questionId") String questionId);
    
    long countByMaterialCodeAndIsActiveTrue(String materialCode);
    
    long countByMaterialCodeAndStepNumberAndIsActiveTrue(String materialCode, Integer stepNumber);
    
    boolean existsByQuestionIdAndIsActiveTrue(String questionId);
    
    // Additional methods needed by service implementations
    long countByMaterialCode(String materialCode);
    List<Question> findByMaterialCodeOrderByOrderIndex(String materialCode);
    
    @Query("SELECT q FROM Question q WHERE q.isActive = true ORDER BY q.id DESC")
    Optional<Question> findLatestVersion();
    
    // Additional methods needed by ProjectServiceImpl
    @Query("SELECT q FROM Question q WHERE q.isActive = true ORDER BY q.orderIndex")
    List<Question> findActiveQuestionnaires();
    
    @Query("SELECT q FROM Question q WHERE q.stepNumber = :stepNumber AND q.isActive = true ORDER BY q.orderIndex")
    List<Question> findByStepNumber(@Param("stepNumber") Integer stepNumber);
    
    @Query("SELECT q FROM Question q WHERE q.category = :category AND q.isActive = true ORDER BY q.orderIndex")
    List<Question> findByCategory(@Param("category") String category);
    
    @Query("SELECT q FROM Question q WHERE q.questionId = :questionId AND q.isActive = true")
    Optional<Question> findByQuestionId(@Param("questionId") String questionId);
    
    @Query("SELECT DISTINCT q.stepNumber FROM Question q WHERE q.isActive = true ORDER BY q.stepNumber")
    List<Integer> findDistinctStepNumbers();
    
    @Query("SELECT DISTINCT q.category FROM Question q WHERE q.isActive = true ORDER BY q.category")
    List<String> findDistinctCategories();
}