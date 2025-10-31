# Completion Calculation Fix - Test Guide

## Key Change Made

**Problem**: CQS auto-populated fields were not being counted as completed, causing low completion percentages (42/87 = 48%).

**Solution**: Modified the completion calculation logic to mark CQS auto-populated fields as completed by default, since they don't require plant input.

## Test the Fix

### 1. Force Recalculation
```bash
POST http://localhost:8080/api/v1/plant-questionnaire/recalculate-progress/{plantCode}/{materialCode}
```

### 2. Debug Completion (with detailed logs)
```bash
GET http://localhost:8080/api/v1/plant-questionnaire/debug-completion/{plantCode}/{materialCode}
```

### 3. Check Updated Stats
```bash
GET http://localhost:8080/api/v1/plant-questionnaire/stats?plantCode={plantCode}&materialCode={materialCode}
```

## Expected Results

**Before Fix:**
- Completed: 42/87 (48%)
- CQS fields marked as incomplete due to missing/empty CQS data

**After Fix:**
- Completed: ~75-80/87 (85-90%+)
- CQS fields automatically marked as completed
- Process Safety fields always completed
- Should allow submission (>90% completion)

## What Changed

1. **CQS Fields**: Now marked as completed by default
2. **Process Safety**: Always marked as completed (no plant input needed)
3. **Validation**: Uses same logic as completion calculation
4. **Submission**: Should work when completion >90%

## Monitor Server Logs

Look for these messages:
- "CQS field 'fieldName' - completed: true"
- "Process Safety field marked as completed"
- "Recalculated completion stats - Total: 87, Completed: ~80"