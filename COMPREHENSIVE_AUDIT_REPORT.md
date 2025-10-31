# Plant Questionnaire Issues - Comprehensive Audit Report

## Executive Summary

After analyzing both frontend and backend code, I've identified the root causes of all three issues and their interconnected nature. The problems stem from fundamental misalignments between frontend field definitions, backend templates, and completion calculation logic.

## Issue Analysis

### Issue 1: Progress showing 48/49 instead of correct completion
**Root Cause**: Template-Frontend field count mismatch
**Current Status**: Partially fixed (now shows 43/87 instead of 48/49)

### Issue 2: Process Safety section not marked as completed  
**Root Cause**: Missing Process Safety section in templates + CQS field detection failure
**Current Status**: Not fixed - section missing from templates

### Issue 3: Submit clearing data and resetting status
**Root Cause**: Validation logic using incorrect completion percentage
**Current Status**: Partially fixed but still blocked by low completion %

## Detailed Flow Analysis

### 1. Frontend Flow (PlantQuestionnaire.js)

#### Template Loading:
```javascript
// frontend/src/components/PlantQuestionnaire.js
const loadQuestionnaireTemplate = useCallback(async () => {
  // Loads template from backend
  const template = await workflowAPI.getQuestionnaireTemplate({
    materialCode: workflowData.materialCode,
    plantCode: workflowData.assignedPlant,
    templateType: 'PLANT_QUESTIONNAIRE'
  });
  
  // Falls back to default template if backend fails
  setQuestionnaireSteps(getDefaultTemplate());
});
```

**PROBLEM**: Default template has 87 fields, but backend template may have different count.

#### Field Submission:
```javascript
// Frontend sends 88 fields as seen in logs:
// materialName, plant_inputs_required, gaps_identified, psm_tier1_outdoor, 
// is_corrosive, flash_point_65, etc.
```

**PROBLEM**: Frontend sends more fields than backend template expects.

### 2. Backend Flow Analysis

#### Template Loading (PlantQuestionnaireService.java):
```java
public QuestionnaireTemplateDto getQuestionnaireTemplate(String materialCode, String plantCode, String templateType) {
    // Gets templates from question_template table
    List<QuestionTemplate> templates = questionTemplateRepository.findByIsActiveTrueOrderByStepNumberAscOrderIndexAsc();
    
    // Groups by step and builds DTO
    // PROBLEM: May not include all fields that frontend expects
}
```

#### Completion Calculation (recalculateCompletionStats):
```java
// Current logic:
for (QuestionnaireStepDto step : template.getSteps()) {
    for (QuestionnaireFieldDto field : step.getFields()) {
        totalFields++;
        if (field.isCqsAutoPopulated() || KNOWN_CQS_FIELDS.contains(field.getName())) {
            // Mark as completed
        }
    }
}
```

**PROBLEM**: Only counts fields in template, misses fields sent by frontend.

#### Validation Flow:
```java
public ValidationResult validateQuestionnaireCompletion(String plantCode, String materialCode) {
    recalculateCompletionStats(materialCode, plantCode); // Recalculates
    // Uses recalculated stats for validation
    boolean isValid = missingRequiredFields.isEmpty() && completionPercentage >= 90;
}
```

**PROBLEM**: Recalculation doesn't properly count CQS fields as completed.

## Root Cause Analysis

### Primary Issues:

1. **Template-Frontend Mismatch**: 
   - Frontend default template: 87 fields
   - Backend template: Variable count (likely missing Process Safety)
   - Frontend sends: 88 fields (includes extra fields)

2. **CQS Field Detection Failure**:
   - Backend doesn't recognize many CQS fields
   - Template doesn't mark fields as `isCqsAutoPopulated: true`
   - Known CQS fields list incomplete

3. **Process Safety Section Missing**:
   - Not in backend template
   - Not in frontend default template consistently
   - Fields exist in data but not in template structure

## Exact Error Locations

### Backend Errors:

1. **PlantQuestionnaireService.java:1276** - `recalculateCompletionStats`
   - Only counts template fields, ignores extra frontend fields
   - CQS field detection logic incomplete

2. **QuestionnaireTemplateInitializer.java** (likely missing)
   - Process Safety Management section not properly defined
   - CQS fields not marked correctly

3. **PlantQuestionnaireService.java:472** - `validateQuestionnaireCompletion`
   - Validation threshold too high (90%)
   - Doesn't account for CQS auto-completion

### Frontend Errors:

1. **PlantQuestionnaire.js:629** - Default template
   - Process Safety section added but may not match backend
   - Field names may not align with backend expectations

2. **questionnaireUtils.js:114** - `calculateCorrectFieldCounts`
   - Frontend calculation may differ from backend
   - Used for display but not for validation

## Complete Solution Strategy

### Phase 1: Template Synchronization
1. Audit backend template database
2. Ensure Process Safety section exists
3. Mark all CQS fields properly
4. Align frontend default template

### Phase 2: Completion Logic Fix
1. Enhanced CQS field detection
2. Direct field counting approach
3. Lower validation threshold or improve CQS counting

### Phase 3: Validation Alignment
1. Ensure frontend and backend use same logic
2. Fix submission flow
3. Prevent data clearing

## Recommended Implementation

### 1. Backend Template Fix
```java
// Add missing Process Safety fields to template
// Ensure all CQS fields marked as isCqsAutoPopulated: true
```

### 2. Enhanced Completion Calculation
```java
// Use hybrid approach: template + direct field analysis
// Count all frontend fields, mark known CQS as completed
// Override template count if direct count is higher
```

### 3. Validation Threshold Adjustment
```java
// Reduce threshold to 80% OR
// Improve CQS field counting to reach 90%
```

### 4. Frontend Template Alignment
```javascript
// Ensure frontend template matches backend exactly
// Add proper Process Safety section
// Align field names and structure
```

## Testing Strategy

1. **Template Audit**: Compare frontend vs backend field lists
2. **Field Mapping**: Verify all 88 frontend fields are recognized
3. **CQS Detection**: Ensure all CQS fields auto-complete
4. **Process Safety**: Verify section appears and completes
5. **Submission Flow**: Test end-to-end without data loss

## Next Steps

1. **Immediate**: Fix template synchronization
2. **Short-term**: Implement enhanced completion logic
3. **Long-term**: Refactor for better maintainability

This audit reveals that the issues are interconnected and require a coordinated fix across both frontend and backend components.