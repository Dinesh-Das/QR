package com.cqs.qrmfg.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class QueryResolveRequest {
    @NotBlank(message = "Response is required")
    @Size(max = 2000, message = "Response must not exceed 2000 characters")
    private String response;

    @Size(max = 20, message = "Priority level must not exceed 20 characters")
    private String priorityLevel;

    public QueryResolveRequest() {}

    public QueryResolveRequest(String response) {
        this.response = response;
    }

    public QueryResolveRequest(String response, String priorityLevel) {
        this.response = response;
        this.priorityLevel = priorityLevel;
    }

    // Getters and setters
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
}