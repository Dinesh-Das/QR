# Completion Calculation Mismatch Debug Guide

## 🚨 ISSUE IDENTIFIED

**Frontend vs Backend Mismatch**:
- **Frontend**: Shows 87/87 fields completed (100%)
- **Backend**: Shows only 48% complete (42/87 fields)
- **Breakdown**: CQS fields: 34/34 ✅, Plant fields: 8/53 ❌

**Root Cause**: The backend validation is not recognizing the plant input data that was saved by the frontend.

## 🔍 DEBUGGING STEPS

### Step 1: Check Plant Input Data Storage

```bash
# Use the debug endpoint to check what's actually stored
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/debug/1102/R123456"
```

**Look for**:
- `plantInputsSize`: Should show number of saved plant inputs
- `plantInputsSample`: Shows sample of saved data
- `completionPercentage`: Backend calculated percentage

### Step 2: Test Validation Directly

```bash
# Use the new test validation endpoint
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/test-validation/1102/R123456"
```

**Expected Response**:
```json
{
  "validation": {
    "isValid": false,
    "message": "Questionnaire is only 48% complete...",
    "completionPercentage": 48,
    "missingFieldsCount": 45,
    "missingFields": ["field1", "field2", ...]
  },
  "plantData": {
    "totalFields": 87,
    "completedFields": 42,
    "plantInputsSize": 53,
    "cqsInputsSize": 34,
    "completionPercentage": 48
  }
}
```

### Step 3: Check Application Logs

**Look for these log messages**:
```
PlantQuestionnaireService: Plant inputs data size: 53
PlantQuestionnaireService: Plant inputs keys: [field1, field2, field3, ...]
PlantQuestionnaireService: Plant field 'field_name' - value: 'user_input' (type: String), completed: true
```

### Step 4: Compare Field Names

The issue might be **field name mismatches** between:
1. **Frontend form field names** (what user fills)
2. **Backend template field names** (what validation expects)
3. **Saved plant input keys** (what's stored in database)

## 🔧 POTENTIAL CAUSES & FIXES

### Cause 1: Field Name Mismatch

**Problem**: Frontend saves data with field names like `"question_40"` but backend template expects `"reproductive_toxicant"`.

**Debug**:
```bash
# Check what field names are in the template
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/template?materialCode=R123456&plantCode=1102"
```

**Fix**: Update field name mapping in frontend or backend to ensure consistency.

### Cause 2: Data Type Issues

**Problem**: Frontend sends data as strings, but backend expects different types.

**Example**:
- Frontend sends: `"field1": "yes"`
- Backend expects: `"field1": true` or specific format

**Fix**: Normalize data types in validation logic.

### Cause 3: Empty/Null Value Handling

**Problem**: Frontend considers field completed, but backend sees empty values.

**Debug**: Check log messages for field values and completion status.

**Fix**: Improve empty value detection logic.

### Cause 4: Template vs Saved Data Mismatch

**Problem**: Template has different fields than what's saved in plant inputs.

**Debug**: Compare template field names with saved plant input keys.

## 🛠️ IMMEDIATE FIXES TO TRY

### Fix 1: Enhanced Logging (Already Applied)

Added detailed logging to show:
- Plant inputs data size and keys
- Individual field validation results
- Data types and values

### Fix 2: Field Name Normalization

Add this to the validation logic:

```java
// Try multiple field name variations
Object value = plantInputs.get(field.getName());
if (value == null) {
    // Try with "question_" prefix
    value = plantInputs.get("question_" + field.getOrderIndex());
}
if (value == null) {
    // Try with template field name variations
    value = plantInputs.get(field.getName().toLowerCase());
}
```

### Fix 3: Data Type Normalization

```java
// Normalize different data types
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
    } else if (value instanceof List) {
        isCompleted = !((List<?>) value).isEmpty();
    } else {
        isCompleted = true; // Other non-null values considered complete
    }
}
```

## 🧪 TESTING PROTOCOL

### Test 1: Verify Data Storage

1. **Fill out questionnaire** completely in frontend
2. **Save draft** and verify success message
3. **Check debug endpoint** to see stored data size
4. **Compare** frontend field count vs backend stored count

### Test 2: Field Name Mapping

1. **Get template** field names from API
2. **Get plant inputs** keys from debug endpoint  
3. **Compare** field names for mismatches
4. **Fix** any naming inconsistencies

### Test 3: Validation Logic

1. **Run test validation** endpoint
2. **Check logs** for individual field validation
3. **Identify** which fields are not being recognized
4. **Fix** validation logic for those fields

### Test 4: End-to-End Flow

1. **Clear all data** for the material
2. **Fill questionnaire** step by step
3. **Save draft** after each step
4. **Check completion** percentage after each save
5. **Identify** where the mismatch occurs

## 📊 EXPECTED RESULTS AFTER FIX

### Before Fix:
- **Frontend**: 87/87 (100%)
- **Backend**: 42/87 (48%)
- **Submission**: Blocked

### After Fix:
- **Frontend**: 87/87 (100%)
- **Backend**: 87/87 (100%) or close to it
- **Submission**: Allowed (≥90%)

## 🚨 CRITICAL DEBUGGING COMMANDS

```bash
# 1. Check current state
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/debug/1102/R123456"

# 2. Test validation
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/test-validation/1102/R123456"

# 3. Force recalculation
curl -X POST "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/recalculate-progress/1102/R123456"

# 4. Check template
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/template?materialCode=R123456&plantCode=1102"

# 5. Check plant data
curl -X GET "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/plant-data?plantCode=1102&materialCode=R123456"
```

## 🎯 ACTION PLAN

1. **Run debug commands** to identify the exact issue
2. **Check application logs** for detailed field validation
3. **Compare field names** between frontend, backend, and database
4. **Apply appropriate fix** based on findings
5. **Test submission** after fix
6. **Monitor** for similar issues with other materials

The key is to identify whether the issue is:
- **Field name mismatch** (most likely)
- **Data type handling** 
- **Empty value detection**
- **Template vs data inconsistency**

Once identified, the fix should be straightforward and will resolve the submission blocking issue.