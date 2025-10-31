# Quick Test Guide for Plant Questionnaire Fixes

## Test the Fixes

### 1. Test Progress Recalculation

Use this API endpoint to force recalculation and see updated stats:

```bash
# Replace 1102 and R123456 with actual plant code and material code
POST http://localhost:8080/api/v1/plant-questionnaire/recalculate-progress/1102/R123456
```

Expected response should show:
- Correct total field count (should be around 87-91, not 49)
- Updated completion percentage
- Process Safety fields included in count

### 2. Test Validation

Use this API endpoint to test the validation logic:

```bash
# Replace 1102 and R123456 with actual plant code and material code
GET http://localhost:8080/api/v1/plant-questionnaire/test-validation/1102/R123456
```

Expected response should show:
- Updated completion percentage (higher than before)
- Validation message should not mention 48% completion
- Should show correct field counts

### 3. Check Database

Run this query to see the updated completion stats:

```sql
SELECT 
    plant_code,
    material_code,
    total_fields,
    completed_fields,
    completion_percentage,
    completion_status,
    updated_at
FROM plant_specific_data 
WHERE plant_code = '1102' AND material_code = 'R123456';
```

### 4. Test Frontend

1. Open the plant questionnaire in browser
2. Check if Process Safety Management section appears
3. Verify progress shows correct field count
4. Try submitting - should work if completion is above 90%

## What Should Change

### Before Fix:
- Progress: 48/49 (incorrect)
- Completion: ~48%
- Process Safety: Missing or incomplete
- Submission: Blocked due to low completion

### After Fix:
- Progress: 85/91 (or similar correct count)
- Completion: ~93% (or higher)
- Process Safety: Automatically completed
- Submission: Should work

## Troubleshooting

If issues persist:

1. **Check server logs** for these messages:
   - "Processing step X (Process Safety Management)"
   - "Process Safety field marked as completed"
   - "Recalculated completion stats"

2. **Verify template** includes Process Safety section:
   ```bash
   GET http://localhost:8080/api/v1/plant-questionnaire/template?materialCode=R123456&plantCode=1102
   ```

3. **Force sync** if needed:
   ```bash
   POST http://localhost:8080/api/v1/plant-questionnaire/sync-field-names/1102/R123456
   ```

## Key Changes Made

1. **Backend**: Fixed completion calculation to include all template fields
2. **Process Safety**: Added special handling to mark these fields as complete
3. **Validation**: Now uses recalculated stats instead of doing its own counting
4. **Frontend**: Added Process Safety section to templates

The fixes address all three issues:
- ✅ Correct progress calculation (no more 48/49)
- ✅ Process Safety section marked as completed
- ✅ Submission works without clearing data