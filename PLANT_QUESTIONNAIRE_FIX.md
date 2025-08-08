# Plant Questionnaire Progress Calculation Fix

## Issue Description
The plant questionnaire was showing incorrect progress and marking steps as completed prematurely because:

1. **Progress calculation included CQS auto-populated fields** - These fields are disabled and auto-populated by the system, but were being counted in the total field count
2. **Step completion logic included CQS fields** - Steps were being marked as complete when CQS fields were present, even if user-editable fields were empty
3. **Inconsistent field counting** - The total field count included all fields, but only user-editable fields should be counted for progress

## Root Cause
The progress calculation functions were counting all fields (including `isCqsAutoPopulated: true` and `disabled: true` fields) in the total, but these fields should be excluded since they're not meant to be filled by users.

## Solution Applied
Updated the following functions in `frontend/src/components/PlantQuestionnaire.js`:

### 1. `getOverallCompletionPercentage()`
- **Before**: Counted all fields in total
- **After**: Filters out CQS auto-populated and disabled fields using `!field.isCqsAutoPopulated && !field.disabled`

### 2. `getTotalFieldsPopulated()`
- **Before**: Counted all fields in total
- **After**: Only counts user-editable fields

### 3. Step completion logic (appears in two places)
- **Before**: Used all fields to determine step completion
- **After**: Only considers user-editable fields for step completion calculation

### 4. `getStepCompletionStatus()`
- **Before**: Included CQS fields in step statistics
- **After**: Filters out CQS fields for accurate step completion status

## Technical Details

### Fields Excluded from Progress Calculation:
- Fields with `isCqsAutoPopulated: true`
- Fields with `disabled: true`

### CQS Auto-populated Fields (Examples):
- `is_corrosive` - Physical properties
- `highly_toxic` - Toxicity information  
- `flash_point_65` - Flammability data
- `ld50_oral`, `ld50_dermal`, `lc50_inhalation` - Toxicity values
- `recommended_ppe` - PPE recommendations
- And many others marked with `isCqsAutoPopulated: true`

## Expected Behavior After Fix:
1. **Accurate Progress**: Progress percentage only reflects user-editable fields
2. **Correct Step Completion**: Steps are marked complete based on user input, not CQS fields
3. **Proper Field Count**: Total field count excludes auto-populated fields
4. **Consistent UI**: Progress indicators show meaningful completion status

## Additional Files Fixed:

### 5. `frontend/src/utils/questionnaireUtils.js` (New File)
- **Created utility functions** for consistent field count calculations across components
- `calculateCorrectFieldCounts()` - Calculates progress excluding CQS fields
- `getTemplateFieldCounts()` - Gets total field counts by type
- `recalculateWorkflowProgress()` - Recalculates workflow progress with correct counts

### 6. `frontend/src/hooks/usePlantWorkflows.js`
- **Updated workflow data processing** to use corrected field counts
- Applies `recalculateWorkflowProgress()` to fix backend data
- Both dashboard data and fallback data now use correct field counts

### 7. `frontend/src/screens/QuestionnaireViewerPage.js`
- **Fixed overview dashboard** to exclude CQS fields from progress calculation
- **Updated step progress calculation** to only count user-editable fields
- **Fixed completion statistics** to show accurate field counts and percentages

## Field Count Summary:
- **Total fields in questionnaire**: 87
- **CQS auto-populated fields**: ~25 fields (marked with `isCqsAutoPopulated: true`)
- **User-editable fields**: ~62 fields (what users actually need to complete)

## Testing Recommendations:
1. **Plant View Dashboard**: Check that workflow progress shows correct percentages
2. **Plant View Table**: Verify "X/Y fields" shows correct counts (should be out of ~62, not 87)
3. **Questionnaire Form**: Progress should only reflect user-editable fields
4. **Questionnaire Viewer**: All statistics should exclude CQS fields
5. **Step Completion**: Steps should only be marked complete based on user input
6. **CQS Fields**: Should remain disabled and not affect any progress calculations