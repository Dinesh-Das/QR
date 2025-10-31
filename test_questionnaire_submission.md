# Test Questionnaire Submission Fix

## Manual Testing Steps

### 1. Test Questionnaire Submission

```bash
# 1. Get questionnaire data for a workflow
curl -X GET "http://localhost:8080/api/v1/plant-questionnaire/workflow/123" \
  -H "Content-Type: application/json"

# 2. Save some draft responses
curl -X POST "http://localhost:8080/api/v1/plant-questionnaire/draft?workflowId=123" \
  -H "Content-Type: application/json" \
  -d '{
    "plantCode": "PLANT001",
    "materialCode": "MAT001",
    "modifiedBy": "testuser",
    "responses": {
      "msds_available": "yes",
      "missing_info": "no",
      "sourcing_asked": "yes",
      "cas_available": "yes"
    }
  }'

# 3. Check status before submission
curl -X GET "http://localhost:8080/api/v1/plant-questionnaire/status?plantCode=PLANT001&materialCode=MAT001"

# 4. Submit the questionnaire
curl -X POST "http://localhost:8080/api/v1/plant-questionnaire/submit?workflowId=123" \
  -H "Content-Type: application/json" \
  -d '{
    "plantCode": "PLANT001",
    "materialCode": "MAT001",
    "submittedBy": "testuser",
    "responses": {
      "msds_available": "yes",
      "missing_info": "no",
      "sourcing_asked": "yes",
      "cas_available": "yes",
      "additional_field": "test_value"
    }
  }'

# 5. Verify submission status
curl -X GET "http://localhost:8080/api/v1/plant-questionnaire/status?plantCode=PLANT001&materialCode=MAT001"

# 6. Check if questionnaire is read-only
curl -X GET "http://localhost:8080/api/v1/plant-questionnaire/readonly?plantCode=PLANT001&materialCode=MAT001"

# 7. Check dashboard data
curl -X GET "http://localhost:8080/api/v1/plant-questionnaire/dashboard?plantCode=PLANT001"

# 8. Try to submit again (should fail)
curl -X POST "http://localhost:8080/api/v1/plant-questionnaire/submit?workflowId=123" \
  -H "Content-Type: application/json" \
  -d '{
    "plantCode": "PLANT001",
    "materialCode": "MAT001",
    "submittedBy": "testuser2",
    "responses": {
      "msds_available": "no"
    }
  }'
```

### 2. Expected Results

#### After Step 4 (Submission):
```json
{
  "success": true,
  "message": "Questionnaire submitted successfully",
  "submittedAt": "2024-01-15T10:30:00",
  "submittedBy": "testuser",
  "completionPercentage": 85,
  "completionStatus": "COMPLETED",
  "totalFields": 87,
  "completedFields": 74,
  "isReadOnly": true
}
```

#### After Step 5 (Status Check):
```json
{
  "exists": true,
  "isSubmitted": true,
  "isWorkflowCompleted": true,
  "isReadOnly": true,
  "completionPercentage": 85,
  "completionStatus": "COMPLETED",
  "submittedAt": "2024-01-15T10:30:00",
  "submittedBy": "testuser",
  "totalFields": 87,
  "completedFields": 74,
  "canSubmit": false,
  "validationMessage": "Questionnaire has been submitted"
}
```

#### After Step 6 (Read-Only Check):
```json
{
  "isReadOnly": true,
  "plantCode": "PLANT001",
  "materialCode": "MAT001"
}
```

#### After Step 7 (Dashboard):
```json
{
  "workflows": [
    {
      "workflowId": 123,
      "materialCode": "MAT001",
      "completionStatus": "COMPLETED",
      "completionPercentage": 85,
      "isSubmitted": true,
      "submittedAt": "2024-01-15T10:30:00",
      "submittedBy": "testuser"
    }
  ],
  "completedCount": 1,
  "totalWorkflows": 1
}
```

#### After Step 8 (Duplicate Submission):
```json
{
  "success": false,
  "message": "Cannot submit - questionnaire has already been submitted",
  "isReadOnly": true,
  "isSubmitted": true,
  "submittedAt": "2024-01-15T10:30:00"
}
```

### 3. Database Verification

Check the database to ensure data persistence:

```sql
-- Check plant-specific data
SELECT 
    plant_code,
    material_code,
    completion_status,
    completion_percentage,
    total_fields,
    completed_fields,
    submitted_at,
    submitted_by,
    plant_inputs
FROM QRMFG_PLANT_SPECIFIC_DATA 
WHERE plant_code = 'PLANT001' AND material_code = 'MAT001';

-- Check workflow status
SELECT 
    id,
    material_code,
    plant_code,
    state,
    last_modified
FROM QRMFG_WORKFLOW 
WHERE material_code = 'MAT001' AND plant_code = 'PLANT001';
```

### 4. Key Validation Points

✅ **Completion Status Preserved**: `completion_status` should be "COMPLETED"  
✅ **Submission Timestamp Set**: `submitted_at` should have a valid timestamp  
✅ **Progress Maintained**: `completion_percentage` should not be reset to 0  
✅ **Field Counts Preserved**: `total_fields` and `completed_fields` should maintain their values  
✅ **Read-Only Enforced**: Further edit attempts should be rejected  
✅ **Dashboard Updated**: Plant dashboard should show "COMPLETED" status  
✅ **Duplicate Prevention**: Second submission attempts should fail gracefully  

### 5. Troubleshooting

If issues persist:

1. **Check Logs**: Look for PlantQuestionnaireService log messages
2. **Verify Database**: Ensure `submitted_at` field is populated
3. **Test Read-Only**: Confirm `isQuestionnaireReadOnly()` returns true
4. **Check Workflow State**: Verify workflow transitions to COMPLETED
5. **Validate Field Mapping**: Ensure frontend field names match backend expectations