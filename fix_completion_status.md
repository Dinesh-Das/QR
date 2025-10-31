# Fix Completion Status Issue

## Problem
The UI shows materials with "COMPLETED" status but red progress indicators, indicating a mismatch between the completion status and progress calculation.

## Root Cause
The issue occurs when:
1. A questionnaire is submitted and marked as "COMPLETED"
2. But the completion percentage calculation doesn't reflect 100%
3. This causes the UI to show green "COMPLETED" status but red progress bars

## Solution
I've added several fixes to resolve this issue:

### 1. Force 100% Completion for Submitted Questionnaires
- Modified `submitPlantQuestionnaire()` to force completion percentage to 100% when submitted
- Updated dashboard data to always show 100% for submitted questionnaires

### 2. Added Debug Endpoints
- `/api/v1/plant-questionnaire/fix-completion-status/{plantCode}/{materialCode}` - Fixes completion status for specific materials
- `/api/v1/plant-questionnaire/recalculate-progress/{plantCode}/{materialCode}` - Recalculates progress stats

### 3. Enhanced Status Checking
- Added `getQuestionnaireStatus()` method to properly check submission status
- Added `isQuestionnaireReadOnly()` method to check if questionnaire is submitted

## How to Fix the Current Issue

### Option 1: Use the Fix Endpoint (Recommended)
For each material showing the issue (R123456, R123454, R123455), call:

```bash
POST /api/v1/plant-questionnaire/fix-completion-status/1102/R123456
POST /api/v1/plant-questionnaire/fix-completion-status/1102/R123454  
POST /api/v1/plant-questionnaire/fix-completion-status/1102/R123455
```

### Option 2: Use the Recalculate Endpoint
```bash
POST /api/v1/plant-questionnaire/recalculate-progress/1102/R123456
POST /api/v1/plant-questionnaire/recalculate-progress/1102/R123454
POST /api/v1/plant-questionnaire/recalculate-progress/1102/R123455
```

### Option 3: Direct Database Fix (if needed)
```sql
UPDATE QRMFG_PLANT_SPECIFIC_DATA 
SET completion_percentage = 100, 
    completion_status = 'COMPLETED'
WHERE plant_code = '1102' 
AND material_code IN ('R123456', 'R123454', 'R123455')
AND submitted_at IS NOT NULL;
```

## Testing
After applying the fix:
1. Refresh the dashboard page
2. The progress bars should now show 100% (blue) instead of red
3. The status should remain "COMPLETED"

## Prevention
The code changes ensure that:
- Any future submissions will automatically set completion to 100%
- Dashboard always shows 100% for submitted questionnaires
- Status checks are consistent across the application