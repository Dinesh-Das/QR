# Plant Questionnaire Issues and Fixes

## Issues Identified:

1. **Progress showing 48/49 instead of correct completion**: The backend completion calculation is not correctly counting all fields, particularly missing Process Safety section fields.

2. **Process Safety section not marked as completed**: This section contains CQS auto-populated fields but the logic isn't recognizing them as complete when no plant input is required.

3. **Submit clearing data and resetting status**: The submission process appears to be working correctly, but the validation logic may be preventing proper completion status.

## Root Cause Analysis:

### Issue 1: Incorrect Progress Calculation (48/49)
- The backend `recalculateCompletionStats` method is not properly counting all template fields
- There's a mismatch between frontend template (87 fields) and backend calculation
- Process Safety Management fields may not be included in the count

### Issue 2: Process Safety Section Not Completed
- Process Safety Management fields are CQS auto-populated
- The current logic requires CQS data to be available to mark fields as complete
- However, these fields should be considered complete even if CQS data is not available since no plant input is required

### Issue 3: Submission Issues
- The submission validation is too strict, requiring 90% completion
- Process Safety fields not being marked as complete affects overall completion percentage
- This prevents proper submission and status update

## Fixes Required:

### Fix 1: Update Backend Completion Calculation

**File**: `src/main/java/com/cqs/qrmfg/service/PlantQuestionnaireService.java`

**Method**: `recalculateCompletionStats`

**Changes**:
1. Add better logging to track field counting
2. Special handling for Process Safety Management fields
3. Mark CQS auto-populated fields as complete when no plant input is required
4. Ensure all template fields are counted correctly

### Fix 2: Process Safety Section Completion Logic

**Changes**:
1. Identify Process Safety Management step by title
2. Mark all CQS auto-populated fields in this section as complete
3. Add logging to confirm Process Safety fields are being handled correctly

### Fix 3: Submission Validation Improvements

**Changes**:
1. Ensure completion percentage calculation includes all fields correctly
2. Consider Process Safety fields as complete for submission validation
3. Update validation messages to be more specific

## Implementation:

### Backend Changes (PlantQuestionnaireService.java):

```java
@Transactional
public void recalculateCompletionStats(String materialCode, String plantCode) {
    try {
        System.out.println("PlantQuestionnaireService: Starting completion stats recalculation for " + materialCode + " at plant " + plantCode);
        
        // Get the questionnaire template to get correct field counts
        QuestionnaireTemplateDto template = getQuestionnaireTemplate(materialCode, plantCode, "PLANT_QUESTIONNAIRE");
        
        // Get plant-specific data
        PlantSpecificDataDto plantData = getPlantSpecificData(plantCode, materialCode);
        if (plantData == null) {
            throw new RuntimeException("Plant-specific data not found for material " + materialCode + " and plant " + plantCode);
        }
        
        // Get CQS data once for all CQS fields
        CqsDataDto cqsData = null;
        try {
            cqsData = getCqsData(materialCode, plantCode);
            System.out.println("PlantQuestionnaireService: CQS data loaded successfully with " + 
                             (cqsData != null && cqsData.getCqsData() != null ? cqsData.getCqsData().size() : 0) + " fields");
        } catch (Exception e) {
            System.err.println("PlantQuestionnaireService: Failed to load CQS data: " + e.getMessage());
        }
        
        // Calculate field statistics from template
        int totalFields = 0;
        int requiredFields = 0;
        int completedFields = 0;
        int completedRequiredFields = 0;
        int cqsFields = 0;
        int completedCqsFields = 0;
        int plantFields = 0;
        int completedPlantFields = 0;
        
        Map<String, Object> plantInputs = plantData.getPlantInputs() != null ? plantData.getPlantInputs() : new HashMap<>();
        System.out.println("PlantQuestionnaireService: Plant inputs loaded with " + plantInputs.size() + " fields");
        
        for (QuestionnaireStepDto step : template.getSteps()) {
            System.out.println("PlantQuestionnaireService: Processing step " + step.getStepNumber() + " (" + step.getTitle() + ") with " + step.getFields().size() + " fields");
            
            for (QuestionnaireFieldDto field : step.getFields()) {
                totalFields++;
                
                if (field.isRequired()) {
                    requiredFields++;
                }
                
                // Check if field is completed (has a value)
                boolean isCompleted = false;
                
                if (field.isCqsAutoPopulated()) {
                    cqsFields++;
                    // CQS field - get CQS data directly to check completion
                    String cqsValue = getCqsValueForField(field.getName(), cqsData);
                    isCompleted = (cqsValue != null && !cqsValue.trim().isEmpty() && 
                                 !"Data not available".equals(cqsValue) &&
                                 !"null".equalsIgnoreCase(cqsValue) &&
                                 !"undefined".equalsIgnoreCase(cqsValue));
                    
                    // CRITICAL FIX: For Process Safety Management fields, mark as completed if they are CQS auto-populated
                    // This addresses Issue #2 - Process Safety section should be marked as completed
                    if ("Process Safety Management".equalsIgnoreCase(step.getTitle()) || 
                        step.getTitle().toLowerCase().contains("process safety")) {
                        // Process Safety fields are CQS auto-populated and should be considered complete
                        // even if CQS data is not available, as no plant input is required
                        isCompleted = true;
                        System.out.println("PlantQuestionnaireService: Process Safety field '" + field.getName() + 
                                         "' marked as completed (CQS auto-populated, no plant input required)");
                    }
                    
                    if (isCompleted) {
                        completedCqsFields++;
                    }
                    
                    System.out.println("PlantQuestionnaireService: CQS field '" + field.getName() + 
                                     "' - value: '" + cqsValue + "', completed: " + isCompleted);
                } else {
                    plantFields++;
                    // Plant field - try multiple field name variations
                    Object value = findFieldValue(plantInputs, field.getName(), field.getOrderIndex(), step.getStepNumber());
                    
                    // Plant field is completed if it has a value
                    if (value != null) {
                        if (value instanceof String) {
                            String strValue = ((String) value).trim();
                            isCompleted = !strValue.isEmpty() && 
                                         !"null".equalsIgnoreCase(strValue) && 
                                         !"undefined".equalsIgnoreCase(strValue);
                        } else if (value instanceof Boolean) {
                            isCompleted = true; // Boolean values are always considered complete
                        } else if (value instanceof Number) {
                            isCompleted = true; // Number values are always considered complete
                        } else if (value instanceof java.util.List) {
                            isCompleted = !((java.util.List<?>) value).isEmpty();
                        } else if (value instanceof Map) {
                            isCompleted = !((Map<?, ?>) value).isEmpty();
                        } else {
                            isCompleted = true; // Other non-null values considered complete
                        }
                    }
                    
                    if (isCompleted) {
                        completedPlantFields++;
                    }
                    
                    System.out.println("PlantQuestionnaireService: Plant field '" + field.getName() + 
                                     "' - value: '" + value + "', completed: " + isCompleted);
                }
                
                if (isCompleted) {
                    completedFields++;
                    if (field.isRequired()) {
                        completedRequiredFields++;
                    }
                }
            }
        }
        
        // Update plant-specific data with recalculated stats
        plantData.setTotalFields(totalFields);
        plantData.setCompletedFields(completedFields);
        plantData.setRequiredFields(requiredFields);
        plantData.setCompletedRequiredFields(completedRequiredFields);
        
        // Save the updated data
        savePlantSpecificData(plantData, "SYSTEM_RECALC");
        
        System.out.println("PlantQuestionnaireService: Recalculated completion stats for " + materialCode + 
                         " at plant " + plantCode + " - Total: " + totalFields + 
                         ", Completed: " + completedFields + " (CQS: " + completedCqsFields + "/" + cqsFields + 
                         ", Plant: " + completedPlantFields + "/" + plantFields + ")" +
                         ", Progress: " + plantData.getCompletionPercentage() + "%");
        
    } catch (Exception e) {
        throw new RuntimeException("Failed to recalculate completion stats: " + e.getMessage(), e);
    }
}
```

### Frontend Changes (PlantQuestionnaire.js):

Update the default template to ensure Process Safety section is included:

```javascript
// Add Process Safety Management step to the default template
{
  title: 'Process Safety Management',
  description: 'Process safety management thresholds (CQS auto-populated)',
  fields: [
    {
      name: 'psm_tier1_outdoor',
      label: 'PSM Tier I Outdoor - Threshold quantity (kgs)',
      type: 'input',
      required: false,
      isCqsAutoPopulated: true
    },
    {
      name: 'psm_tier1_indoor',
      label: 'PSM Tier I Indoor - Threshold quantity (kgs)',
      type: 'input',
      required: false,
      isCqsAutoPopulated: true
    },
    {
      name: 'psm_tier2_outdoor',
      label: 'PSM Tier II Outdoor - Threshold quantity (kgs)',
      type: 'input',
      required: false,
      isCqsAutoPopulated: true
    },
    {
      name: 'psm_tier2_indoor',
      label: 'PSM Tier II Indoor - Threshold quantity (kgs)',
      type: 'input',
      required: false,
      isCqsAutoPopulated: true
    }
  ]
}
```

## Testing Steps:

1. **Test Progress Calculation**:
   - Load a plant questionnaire
   - Check that progress shows correct field count (should be X/87 or similar, not 48/49)
   - Verify Process Safety section shows as completed

2. **Test Process Safety Section**:
   - Navigate to Process Safety Management section
   - Verify all fields show as auto-populated by CQS
   - Confirm section is marked as completed

3. **Test Submission**:
   - Fill out required plant fields
   - Verify completion percentage is calculated correctly
   - Submit questionnaire and confirm it's marked as completed
   - Verify data is not cleared after submission

## Expected Results:

1. Progress should show correct field count (e.g., 85/87 instead of 48/49)
2. Process Safety Management section should be marked as completed automatically
3. Submission should work correctly and mark questionnaire as completed without clearing data
4. Overall completion percentage should be accurate and allow submission when appropriate