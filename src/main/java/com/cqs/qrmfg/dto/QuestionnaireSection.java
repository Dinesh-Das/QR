package com.cqs.qrmfg.dto;

import com.cqs.qrmfg.model.Question;
import com.cqs.qrmfg.model.Answer;

import java.util.List;

public class QuestionnaireSection {
    private String sectionName;
    private Integer stepNumber;
    private List<Question> questions;
    private List<Answer> responses;
    private int totalQuestions;
    private int completedQuestions;
    private boolean isCompleted;

    public QuestionnaireSection() {}

    public QuestionnaireSection(String sectionName, Integer stepNumber) {
        this.sectionName = sectionName;
        this.stepNumber = stepNumber;
    }

    // Getters and setters
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { 
        this.questions = questions;
        this.totalQuestions = questions != null ? questions.size() : 0;
    }

    public List<Answer> getResponses() { return responses; }
    public void setResponses(List<Answer> responses) { 
        this.responses = responses;
        if (responses != null) {
            this.completedQuestions = (int) responses.stream()
                .filter(r -> !r.isEmpty())
                .count();
            this.isCompleted = this.completedQuestions == this.totalQuestions;
        }
    }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCompletedQuestions() { return completedQuestions; }
    public void setCompletedQuestions(int completedQuestions) { this.completedQuestions = completedQuestions; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public double getCompletionPercentage() {
        return totalQuestions > 0 ? (double) completedQuestions / totalQuestions * 100 : 0;
    }
}