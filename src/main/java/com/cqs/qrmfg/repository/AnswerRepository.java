package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Answer;
import com.cqs.qrmfg.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    List<Answer> findByWorkflowOrderByStepNumberAscDisplayOrderAsc(Workflow workflow);
    
    List<Answer> findByWorkflowAndStepNumberOrderByDisplayOrderAsc(Workflow workflow, Integer stepNumber);
    
    Optional<Answer> findByWorkflowAndStepNumberAndFieldName(Workflow workflow, Integer stepNumber, String fieldName);
    
    List<Answer> findByWorkflowAndIsDraftTrue(Workflow workflow);
    
    List<Answer> findByWorkflowAndValidationStatus(Workflow workflow, String validationStatus);
    
    @Query("SELECT a FROM Answer a WHERE a.workflow = :workflow AND a.isRequired = true AND (a.fieldValue IS NULL OR a.fieldValue = '')")
    List<Answer> findRequiredEmptyAnswers(@Param("workflow") Workflow workflow);
    
    @Query("SELECT a FROM Answer a WHERE a.workflow = :workflow AND a.stepNumber = :stepNumber AND a.isRequired = true AND (a.fieldValue IS NULL OR a.fieldValue = '')")
    List<Answer> findRequiredEmptyAnswersByStep(@Param("workflow") Workflow workflow, @Param("stepNumber") Integer stepNumber);
    
    @Query("SELECT DISTINCT a.stepNumber FROM Answer a WHERE a.workflow = :workflow ORDER BY a.stepNumber")
    List<Integer> findDistinctStepNumbersByWorkflow(@Param("workflow") Workflow workflow);
    
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.workflow = :workflow AND a.fieldValue IS NOT NULL AND a.fieldValue != ''")
    long countCompletedAnswers(@Param("workflow") Workflow workflow);
    
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.workflow = :workflow AND a.stepNumber = :stepNumber AND a.fieldValue IS NOT NULL AND a.fieldValue != ''")
    long countCompletedAnswersByStep(@Param("workflow") Workflow workflow, @Param("stepNumber") Integer stepNumber);
    
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.workflow = :workflow AND a.isRequired = true")
    long countRequiredAnswers(@Param("workflow") Workflow workflow);
    
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.workflow = :workflow AND a.stepNumber = :stepNumber AND a.isRequired = true")
    long countRequiredAnswersByStep(@Param("workflow") Workflow workflow, @Param("stepNumber") Integer stepNumber);
    
    List<Answer> findByPlantCodeAndMaterialCode(String plantCode, String materialCode);
    
    List<Answer> findByModifiedByAndLastModifiedAfter(String modifiedBy, LocalDateTime after);
    
    @Query("SELECT a FROM Answer a WHERE a.workflow.id IN :workflowIds ORDER BY a.workflow.id, a.stepNumber, a.displayOrder")
    List<Answer> findByWorkflowIds(@Param("workflowIds") List<Long> workflowIds);
    
    void deleteByWorkflow(Workflow workflow);
    
    void deleteByWorkflowAndStepNumber(Workflow workflow, Integer stepNumber);
    
    boolean existsByWorkflowAndStepNumberAndFieldName(Workflow workflow, Integer stepNumber, String fieldName);
}