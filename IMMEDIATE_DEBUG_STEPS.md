# Immediate Debug Steps for Completion Issue

## Current Status
- Frontend sending: 88 fields
- Backend counting: 41/87 completed (47%)
- Issue: CQS fields not being marked as completed

## Debug Steps

### 1. Compare Frontend vs Backend Fields
```bash
GET http://localhost:8081/qrmfg/api/v1/plant-questionnaire/compare-fields/1102/R123456
```
This will show:
- Template field count vs frontend field count
- Which fields are in frontend but not in template
- Which fields are in template but not in frontend

### 2. Force Recalculation with Enhanced Logging
```bash
POST http://localhost:8081/qrmfg/api/v1/plant-questionnaire/recalculate-progress/1102/R123456
```
Check server logs for:
- "Processing step X (stepName) with Y fields"
- "CQS field 'fieldName' - completed: true/false"
- "SUMMARY - Total fields: X, CQS fields: Y/Z"

### 3. Debug Completion Details
```bash
GET http://localhost:8081/qrmfg/api/v1/plant-questionnaire/debug-completion/1102/R123456
```

## Expected Findings

The issue is likely one of:

1. **Template Mismatch**: Backend template has different fields than frontend
2. **CQS Field Detection**: Fields not being identified as CQS auto-populated
3. **Field Name Mismatch**: Template field names don't match frontend field names

## Quick Fix Test

Try this API call to see current completion stats:
```bash
GET http://localhost:8081/qrmfg/api/v1/plant-questionnaire/stats?plantCode=1102&materialCode=R123456
```

## What to Look For

1. **Field Count Mismatch**: If template has 87 fields but frontend sends 88
2. **CQS Field Count**: Should be ~34 CQS fields marked as completed
3. **Process Safety Fields**: Should see "psm_tier1_outdoor" etc. in logs

## Next Steps

Based on the debug results, we can:
1. Fix template field definitions
2. Adjust field name matching logic
3. Force CQS field completion regardless of data availability