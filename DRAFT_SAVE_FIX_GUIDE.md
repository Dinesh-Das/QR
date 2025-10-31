# Draft Save Fix Guide

## Issues Identified

1. **Draft Save Not Working**: Draft save was not detecting changes properly and saving unnecessarily
2. **Plant Input Data Not Saved**: Plant input data was not being properly stored in the `plant_inputs` field of the `QRMFG_PLANT_SPECIFIC_DATA` table

## Root Causes

### 1. No Change Detection
- The original draft save endpoint saved data every time it was called, even when no changes occurred
- This caused unnecessary database writes and poor performance

### 2. Poor Error Handling
- Limited logging made it difficult to debug save issues
- No validation of required parameters
- No verification that data was actually saved

### 3. Incomplete Data Validation
- No proper validation of plant input data before saving
- No normalization of empty/null values

## Solution Implemented

### 1. Enhanced Draft Save Endpoint

**New Features:**
- **Change Detection**: Only saves when actual changes are detected
- **Better Logging**: Comprehensive logging for debugging
- **Validation**: Proper validation of required parameters
- **Response Enhancement**: Returns detailed information about the save operation

**API Changes:**
```java
// Old response: String
return ResponseEntity.ok("Draft saved successfully");

// New response: Map with detailed information
{
  "success": true,
  "message": "Draft saved successfully",
  "hasChanges": true,
  "savedFields": 15,
  "timestamp": "2024-01-15T10:30:00",
  "plantCode": "PLANT001",
  "materialCode": "R123456",
  "completionPercentage": 75,
  "totalFields": 20,
  "completedFields": 15
}
```

### 2. Change Detection Logic

**New Method: `hasPlantInputChanges()`**
- Compares existing plant inputs with new responses
- Normalizes values (treats null, empty string, and whitespace as equivalent)
- Field-by-field comparison
- Returns `true` only when actual changes are detected

**Benefits:**
- Reduces unnecessary database writes
- Improves performance
- Provides better user feedback

### 3. Enhanced Data Saving

**Improved `savePlantSpecificData()` Method:**
- Detailed logging for each step
- Validation of data before saving
- Verification that data was actually saved
- Better error handling and reporting

### 4. Debug Endpoint

**New Endpoint: `GET /api/v1/plant-questionnaire/debug/{plantCode}/{materialCode}`**

Provides comprehensive debugging information:
```json
{
  "plantCode": "PLANT001",
  "materialCode": "R123456",
  "dataExists": true,
  "cqsInputsEmpty": false,
  "plantInputsEmpty": false,
  "cqsSyncStatus": "SYNCED",
  "completionStatus": "IN_PROGRESS",
  "completionPercentage": 75,
  "totalFields": 20,
  "completedFields": 15,
  "lastUpdated": "2024-01-15T10:30:00",
  "updatedBy": "user123",
  "cqsInputsSize": 10,
  "plantInputsSize": 15,
  "plantInputsSample": {
    "field1": "value1",
    "field2": "value2"
  },
  "cqsDataExists": true,
  "cqsDataSize": 10
}
```

## Testing the Fix

### 1. Test Change Detection

**Test Case 1: No Changes**
```bash
# Make the same API call twice with identical data
curl -X POST "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/draft?workflowId=1001" \
  -H "Content-Type: application/json" \
  -d '{
    "plantCode": "PLANT001",
    "materialCode": "R123456",
    "responses": {
      "field1": "value1",
      "field2": "value2"
    }
  }'

# Second call should return: "hasChanges": false
```

**Test Case 2: With Changes**
```bash
# Make API call with different data
curl -X POST "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/draft?workflowId=1001" \
  -H "Content-Type: application/json" \
  -d '{
    "plantCode": "PLANT001",
    "materialCode": "R123456",
    "responses": {
      "field1": "new_value1",
      "field2": "value2"
    }
  }'

# Should return: "hasChanges": true
```

### 2. Test Plant Input Saving

**Verify Data in Database:**
```sql
-- Check if plant inputs are being saved
SELECT plant_code, material_code, 
       CASE 
           WHEN plant_inputs IS NULL THEN 'NULL'
           WHEN LENGTH(TRIM(plant_inputs)) = 0 THEN 'EMPTY'
           WHEN TRIM(plant_inputs) = '{}' THEN 'EMPTY_JSON'
           ELSE 'POPULATED'
       END as plant_inputs_status,
       LENGTH(plant_inputs) as plant_inputs_length,
       SUBSTR(plant_inputs, 1, 100) as plant_inputs_sample
FROM QRMFG_PLANT_SPECIFIC_DATA 
WHERE plant_code = 'PLANT001' AND material_code = 'R123456';
```

### 3. Use Debug Endpoint

```bash
# Get comprehensive debug information
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/debug/PLANT001/R123456"
```

### 4. Monitor Logs

Look for these log messages:
```
PlantQuestionnaireController: Saving draft for PLANT001/R123456 with 15 responses
PlantQuestionnaireService: Change detected in field 'field1': 'old_value' -> 'new_value'
PlantQuestionnaireService: Updated plant inputs with 15 fields
PlantQuestionnaireService: Successfully saved plant-specific data. Plant inputs length: 1234
```

## Frontend Integration

The frontend should handle the new response format:

```javascript
const handleSaveDraft = async (formData) => {
  try {
    const response = await workflowAPI.saveDraftPlantResponses(workflowId, {
      plantCode: workflowData?.assignedPlant,
      materialCode: workflowData?.materialCode,
      responses: formData,
      modifiedBy: 'current_user'
    });

    if (response.success) {
      if (response.hasChanges) {
        message.success(`Draft saved successfully (${response.savedFields} fields)`);
      } else {
        // Don't show message for no changes - reduces noise
        console.log('No changes detected - draft not saved');
      }
    } else {
      message.error(response.message || 'Failed to save draft');
    }
  } catch (error) {
    message.error('Failed to save draft');
  }
};
```

## Troubleshooting

### Issue: Draft Save Returns "No Changes" When There Should Be Changes

**Possible Causes:**
1. Data normalization is too aggressive
2. Frontend is sending the same data repeatedly
3. Existing data comparison is incorrect

**Debug Steps:**
1. Use the debug endpoint to check current data state
2. Check application logs for change detection messages
3. Verify the data being sent from frontend

### Issue: Plant Inputs Still Empty After Save

**Possible Causes:**
1. Database transaction rollback
2. JSON serialization error
3. Database constraints preventing save

**Debug Steps:**
1. Check application logs for save operation details
2. Verify database permissions
3. Check for database constraints or triggers
4. Use debug endpoint to verify save status

### Issue: Performance Problems with Auto-Save

**Solutions:**
1. Increase debounce delay in frontend
2. Implement more aggressive change detection
3. Consider batching multiple field changes

## Database Verification Queries

```sql
-- 1. Check plant-specific data status
SELECT plant_code, material_code, completion_status, 
       completion_percentage, total_fields, completed_fields,
       cqs_sync_status, updated_at, updated_by
FROM QRMFG_PLANT_SPECIFIC_DATA 
ORDER BY updated_at DESC;

-- 2. Check plant inputs content
SELECT plant_code, material_code,
       CASE 
           WHEN plant_inputs IS NULL THEN 'NULL'
           WHEN LENGTH(TRIM(plant_inputs)) = 0 THEN 'EMPTY'
           WHEN TRIM(plant_inputs) = '{}' THEN 'EMPTY_JSON'
           ELSE 'POPULATED'
       END as status,
       LENGTH(plant_inputs) as length,
       SUBSTR(plant_inputs, 1, 200) as sample_content
FROM QRMFG_PLANT_SPECIFIC_DATA 
WHERE plant_inputs IS NOT NULL;

-- 3. Find records with issues
SELECT plant_code, material_code, 
       'Plant inputs empty but should have data' as issue
FROM QRMFG_PLANT_SPECIFIC_DATA 
WHERE (plant_inputs IS NULL OR TRIM(plant_inputs) = '{}' OR LENGTH(TRIM(plant_inputs)) = 0)
  AND completion_percentage > 0;
```

## Performance Monitoring

Monitor these metrics:
- Draft save frequency (should decrease with change detection)
- Database write operations
- Response times for draft save operations
- Frontend auto-save behavior

## Next Steps

1. **Deploy the Fix**: Apply the enhanced draft save functionality
2. **Monitor Logs**: Watch for the new logging messages to verify operation
3. **Test Thoroughly**: Use the test cases provided above
4. **Verify Database**: Check that plant inputs are being saved properly
5. **User Testing**: Have users test the draft save functionality
6. **Performance Check**: Monitor that unnecessary saves are eliminated

The fix addresses both the change detection issue and the plant input saving problem, providing a more robust and efficient draft save mechanism.