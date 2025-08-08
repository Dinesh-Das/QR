package com.cqs.qrmfg.dto;

import java.util.List;

public class QuestionnaireFieldDto {
    private String name;
    private String label;
    private String type;
    private boolean required;
    private boolean disabled;
    private boolean isCqsAutoPopulated;
    private String cqsValue;
    private String placeholder;
    private String helpText;
    private List<OptionDto> options;
    private String validationRules;
    private String conditionalLogic;
    private String dependsOnField;
    private Integer orderIndex;
    
    public QuestionnaireFieldDto() {}
    
    public QuestionnaireFieldDto(String name, String label, String type, boolean required) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
    }
    
    // Static factory methods for common field types
    public static QuestionnaireFieldDto createInput(String name, String label, boolean required, boolean cqsAutoPopulated) {
        QuestionnaireFieldDto field = new QuestionnaireFieldDto(name, label, "input", required);
        field.setCqsAutoPopulated(cqsAutoPopulated);
        if (cqsAutoPopulated) {
            field.setDisabled(true);
            field.setCqsValue("Pending IMP");
            field.setPlaceholder("Auto-populated by CQS (Pending Implementation)");
        }
        return field;
    }
    
    public static QuestionnaireFieldDto createTextarea(String name, String label, boolean required) {
        return new QuestionnaireFieldDto(name, label, "textarea", required);
    }
    
    public static QuestionnaireFieldDto createSelect(String name, String label, boolean required, 
                                                   List<OptionDto> options, boolean cqsAutoPopulated) {
        QuestionnaireFieldDto field = new QuestionnaireFieldDto(name, label, "select", required);
        field.setOptions(options);
        field.setCqsAutoPopulated(cqsAutoPopulated);
        if (cqsAutoPopulated) {
            field.setDisabled(true);
            field.setCqsValue("Pending IMP");
        }
        return field;
    }
    
    public static QuestionnaireFieldDto createRadio(String name, String label, boolean required, 
                                                  List<OptionDto> options, boolean cqsAutoPopulated) {
        QuestionnaireFieldDto field = new QuestionnaireFieldDto(name, label, "radio", required);
        field.setOptions(options);
        field.setCqsAutoPopulated(cqsAutoPopulated);
        if (cqsAutoPopulated) {
            field.setDisabled(true);
            field.setCqsValue("Pending IMP");
        }
        return field;
    }
    
    public static QuestionnaireFieldDto createCheckbox(String name, String label, boolean required, 
                                                     List<OptionDto> options, boolean cqsAutoPopulated) {
        QuestionnaireFieldDto field = new QuestionnaireFieldDto(name, label, "checkbox", required);
        field.setOptions(options);
        field.setCqsAutoPopulated(cqsAutoPopulated);
        if (cqsAutoPopulated) {
            field.setDisabled(true);
            field.setCqsValue("Pending IMP");
        }
        return field;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    
    public boolean isCqsAutoPopulated() { return isCqsAutoPopulated; }
    public void setCqsAutoPopulated(boolean cqsAutoPopulated) { this.isCqsAutoPopulated = cqsAutoPopulated; }
    
    public String getCqsValue() { return cqsValue; }
    public void setCqsValue(String cqsValue) { this.cqsValue = cqsValue; }
    
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    
    public List<OptionDto> getOptions() { return options; }
    public void setOptions(List<OptionDto> options) { this.options = options; }
    
    public String getValidationRules() { return validationRules; }
    public void setValidationRules(String validationRules) { this.validationRules = validationRules; }
    
    public String getConditionalLogic() { return conditionalLogic; }
    public void setConditionalLogic(String conditionalLogic) { this.conditionalLogic = conditionalLogic; }
    
    public String getDependsOnField() { return dependsOnField; }
    public void setDependsOnField(String dependsOnField) { this.dependsOnField = dependsOnField; }
    
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    
    public static class OptionDto {
        private String value;
        private String label;
        
        public OptionDto() {}
        
        public OptionDto(String value, String label) {
            this.value = value;
            this.label = label;
        }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}