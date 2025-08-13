package com.cqs.qrmfg.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.envers.Audited;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "QRMFG_QUERIES")
// @Audited  // Temporarily disabled to fix constraint issues
public class Query {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "queries_seq")
    @SequenceGenerator(name = "queries_seq", sequenceName = "QRMFG_QUERIES_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnoreProperties({"queries", "responses", "documents"})
    private Workflow workflow;

    @Column(name = "question", nullable = false, length = 2000)
    private String question;

    @Column(name = "step_number")
    private Integer stepNumber;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "step_title", length = 200)
    private String stepTitle;

    @Column(name = "original_question", length = 2000)
    private String originalQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_team", nullable = false, length = 20)
    private QueryTeam assignedTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_status", nullable = false, length = 20)
    private QueryStatus status = QueryStatus.OPEN;

    @Column(name = "response", length = 2000)
    private String response;

    @Column(name = "raised_by", nullable = false, length = 100)
    private String raisedBy;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "priority_level", length = 20)
    private String priorityLevel = "NORMAL";

    @Column(name = "query_category", length = 50)
    private String queryCategory;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Document relationships for query and response attachments
    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"query", "workflow"})
    private List<Document> documents = new ArrayList<>();

    public Query() {}

    public Query(Workflow workflow, String question, QueryTeam assignedTeam, String raisedBy) {
        this.workflow = workflow;
        this.question = question;
        this.assignedTeam = assignedTeam;
        this.raisedBy = raisedBy;
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.createdBy = raisedBy;
        this.updatedBy = raisedBy;
    }

    public Query(Workflow workflow, String question, Integer stepNumber, String fieldName, 
                 QueryTeam assignedTeam, String raisedBy) {
        this(workflow, question, assignedTeam, raisedBy);
        this.stepNumber = stepNumber;
        this.fieldName = fieldName;
    }

    public Query(Workflow workflow, String question, Integer stepNumber, String fieldName, 
                 String stepTitle, QueryTeam assignedTeam, String raisedBy) {
        this(workflow, question, stepNumber, fieldName, assignedTeam, raisedBy);
        this.stepTitle = stepTitle;
    }

    public Query(Workflow workflow, String question, Integer stepNumber, String fieldName, 
                 String stepTitle, String originalQuestion, QueryTeam assignedTeam, String raisedBy) {
        this(workflow, question, stepNumber, fieldName, stepTitle, assignedTeam, raisedBy);
        this.originalQuestion = originalQuestion;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        lastModified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    // Business logic methods
    public void resolve(String response, String resolvedBy) {
        if (this.status == QueryStatus.RESOLVED) {
            throw new IllegalStateException("Query is already resolved");
        }
        
        this.response = response;
        this.resolvedBy = resolvedBy;
        this.status = QueryStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.updatedBy = resolvedBy;
        this.lastModified = LocalDateTime.now();
    }

    public int getDaysOpen() {
        LocalDateTime endTime = resolvedAt != null ? resolvedAt : LocalDateTime.now();
        return (int) java.time.Duration.between(createdAt, endTime).toDays();
    }

    public boolean isOverdue() {
        return status == QueryStatus.OPEN && getDaysOpen() > 3; // Business rule: overdue after 3 days
    }

    public boolean isHighPriority() {
        return "HIGH".equals(priorityLevel) || "URGENT".equals(priorityLevel);
    }

    public boolean isResolved() {
        return status == QueryStatus.RESOLVED;
    }

    public String getDisplayTitle() {
        if (fieldName != null && stepNumber != null) {
            return String.format("Step %d - %s", stepNumber, fieldName);
        } else if (fieldName != null) {
            return fieldName;
        } else if (stepNumber != null) {
            return String.format("Step %d", stepNumber);
        } else {
            return "General Query";
        }
    }

    // Document-related business methods
    public boolean hasDocuments() {
        return documents != null && !documents.isEmpty();
    }

    public int getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }

    public List<Document> getQueryDocuments() {
        if (documents == null) return new ArrayList<>();
        return documents.stream()
            .filter(doc -> doc.getResponseId() == null)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<Document> getResponseDocuments() {
        if (documents == null) return new ArrayList<>();
        return documents.stream()
            .filter(doc -> doc.getResponseId() != null)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<Document> getResponseDocuments(Long responseId) {
        if (documents == null || responseId == null) return new ArrayList<>();
        return documents.stream()
            .filter(doc -> responseId.equals(doc.getResponseId()))
            .collect(java.util.stream.Collectors.toList());
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Workflow getWorkflow() { return workflow; }
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getStepTitle() { return stepTitle; }
    public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }

    public String getOriginalQuestion() { return originalQuestion; }
    public void setOriginalQuestion(String originalQuestion) { this.originalQuestion = originalQuestion; }

    public QueryTeam getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(QueryTeam assignedTeam) { this.assignedTeam = assignedTeam; }

    public QueryStatus getStatus() { return status; }
    public void setStatus(QueryStatus status) { this.status = status; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getRaisedBy() { return raisedBy; }
    public void setRaisedBy(String raisedBy) { this.raisedBy = raisedBy; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public String getQueryCategory() { return queryCategory; }
    public void setQueryCategory(String queryCategory) { this.queryCategory = queryCategory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }

    @Override
    public String toString() {
        return String.format("Query{id=%d, workflow=%s, assignedTeam=%s, status=%s, documentCount=%d}", 
                           id, workflow != null ? workflow.getId() : null, assignedTeam, status, getDocumentCount());
    }
}