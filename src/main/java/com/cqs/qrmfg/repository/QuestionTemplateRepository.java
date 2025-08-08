package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    
    List<QuestionTemplate> findByIsActiveTrueOrderByOrderIndexAsc();
    
    List<QuestionTemplate> findByIsActiveTrueOrderByStepNumberAscOrderIndexAsc();
    
    List<QuestionTemplate> findByStepNumberAndIsActiveTrueOrderByOrderIndexAsc(Integer stepNumber);
    
    List<QuestionTemplate> findByCategoryAndIsActiveTrueOrderByOrderIndexAsc(String category);
    
    Optional<QuestionTemplate> findBySrNoAndIsActiveTrue(Integer srNo);
    
    @Query("SELECT DISTINCT qt.stepNumber FROM QuestionTemplate qt WHERE qt.isActive = true ORDER BY qt.stepNumber")
    List<Integer> findDistinctStepNumbers();
    
    @Query("SELECT DISTINCT qt.category FROM QuestionTemplate qt WHERE qt.isActive = true ORDER BY qt.category")
    List<String> findDistinctCategories();
    
    @Query("SELECT qt FROM QuestionTemplate qt WHERE qt.responsible = :responsible AND qt.isActive = true ORDER BY qt.orderIndex")
    List<QuestionTemplate> findByResponsible(@Param("responsible") String responsible);
    
    @Query("SELECT qt FROM QuestionTemplate qt WHERE qt.questionType = :questionType AND qt.isActive = true ORDER BY qt.orderIndex")
    List<QuestionTemplate> findByQuestionType(@Param("questionType") String questionType);
    
    @Query("SELECT qt FROM QuestionTemplate qt WHERE qt.dependsOnQuestionId IS NOT NULL AND qt.isActive = true ORDER BY qt.orderIndex")
    List<QuestionTemplate> findConditionalQuestions();
    
    @Query("SELECT qt FROM QuestionTemplate qt WHERE qt.dependsOnQuestionId = :questionId AND qt.isActive = true ORDER BY qt.orderIndex")
    List<QuestionTemplate> findDependentQuestions(@Param("questionId") String questionId);
    
    long countByIsActiveTrue();
    
    long countByStepNumberAndIsActiveTrue(Integer stepNumber);
    
    boolean existsBySrNoAndIsActiveTrue(Integer srNo);
    
    @Query("SELECT MAX(qt.srNo) FROM QuestionTemplate qt WHERE qt.isActive = true")
    Integer findMaxSrNo();
    
    @Query("SELECT MAX(qt.orderIndex) FROM QuestionTemplate qt WHERE qt.isActive = true")
    Integer findMaxOrderIndex();
}