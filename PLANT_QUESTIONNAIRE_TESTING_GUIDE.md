# Plant Questionnaire Issues - Testing Guide

## Issues Fixed:

### 1. Progress showing 48/49 instead of correct completion
**Fix**: Updated `recalculateCompletionStats` method to properly count all template fields including Process Safety Management section.

### 2. Process Safety section not marked as completed
**Fix**: Added special handling for Process Safety Management fields - they are now automatically marked as completed since they are CQS auto-populated and require no plant input.

### 3. Submit clearing data and resetting status
**Fix**: Improved completion calculation logic to ensure accurate progress tracking and proper submission validation.

## Files Modified:

1. **Backend**:
   - `src/main/java/com/cqs/qrmfg/service/PlantQuestionnaireService.java`
   - `src/main/java/com/cqs/qrmfg/controller/PlantQuestionnaireController.java`

2. **Frontend**:
   - `frontend/src/utils/questionnaireUtils.js`
   - `frontend/src/components/PlantQuestionnaire.js`

## Testing Steps:

### Step 1: Test Progress Calculation Fix

1. **Access Plant Dashboard**:
   ```
   Navigate to: /plant-dashboard
   ```

2. **Check Progress Display**:
   - Look for materials showing progress like "48/49"
   - After fix, should show correct total (e.g., "85/91" or similar)

3. **Manual Recalculation** (if needed):
   ```bash
   # Use this API endpoint to force recalculation
   POST /api/v1/plant-questionnaire/recalculate-progress/{plantCode}/{materialCode}
   ```

### Step 2: Test Process Safety Section Completion

1. **Open Plant Questionnaire**:
   ```
   Navigate to: /questionnaire/{workflowId}
   ```

2. **Check Process Safety Management Section**:
   - Should appear between "Toxicity" and "Storage and Handling"
   - All fields should show as CQS auto-populated
   - Section should be marked as completed automatically
   - Fields include:
     - PSM Tier I Outdoor - Threshold quantity (kgs)
     - PSM Tier I Indoor - Threshold quantity (kgs)
     - PSM Tier II Outdoor - Threshold quantity (kgs)
     - PSM Tier II Indoor - Threshold quantity (kgs)

3. **Verify Section Status**:
   - Section should show green checkmark or "completed" status
   - Progress bar should include these fields in completion calculation

### Step 3: Test Submission Process

1. **Fill Required Plant Fields**:
   - Complete all non-CQS fields that are required
   - Leave Process Safety fields as they are (auto-populated)

2. **Check Overall Progress**:
   - Progress should show accurate percentage
   - Should be higher than before due to Process Safety fields being counted as complete

3. **Submit Questionnaire**:
   - Click "Submit Questionnaire" button
   - Should not get validation errors about incomplete Process Safety fields
   - Submission should succeed if other required fields are filled

4. **Verify Post-Submission**:
   - Questionnaire should be marked as "COMPLETED"
   - Data should remain intact (not cleared)
   - Status should show as submitted

## Expected Results:

### Before Fix:
- Progress: 48/49 (incorrect total)
- Process Safety: Not completed, blocking submission
- Submission: May fail due to incomplete Process Safety section

### After Fix:
- Progress: Correct field count (e.g., 85/91)
- Process Safety: Automatically completed
- Submission: Works correctly when plant fields are filled

## Verification Queries:

### Check Database Records:
```sql
-- Check completion stats
SELECT plant_code, material_code, total_fields, completed_fields, completion_percentage, completion_status
FROM plant_specific_data 
WHERE plant_code = 'YOUR_PLANT_CODE' AND material_code = 'YOUR_MATERIAL_CODE';

-- Check template field count
SELECT step_number, category, COUNT(*) as field_count
FROM question_template 
WHERE is_active = true 
GROUP BY step_number, category 
ORDER BY step_number;
```

### API Testing:
```bash
# Get completion stats
GET /api/v1/plant-questionnaire/stats?plantCode=1102&materialCode=R123456

# Get questionnaire status
GET /api/v1/plant-questionnaire/status?plantCode=1102&materialCode=R123456

# Force recalculation
POST /api/v1/plant-questionnaire/recalculate-progress/1102/R123456
```

## Troubleshooting:

### If Progress Still Shows 48/49:
1. Check if Process Safety section is in the template
2. Force recalculation using the API endpoint
3. Check server logs for field counting details

### If Process Safety Not Completed:
1. Verify step title matches "Process Safety Management"
2. Check that fields are marked as `isCqsAutoPopulated: true`
3. Review server logs for completion logic

### If Submission Still Fails:
1. Check validation logic in `validateQuestionnaireCompletion`
2. Verify completion percentage calculation
3. Review required field validation

## Monitoring:

Watch server logs for these messages:
- "Processing step X (Process Safety Management) with Y fields"
- "Process Safety field 'fieldName' marked as completed"
- "Recalculated completion stats for materialCode at plant plantCode"

The logs will show the detailed field counting and completion logic in action.