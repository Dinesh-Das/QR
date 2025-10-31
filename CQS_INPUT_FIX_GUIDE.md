# CQS Input Field Population Fix

## Issue Description

The CQS auto-populated fields are visible in the frontend questionnaire, and the CQS sync status shows as "synced", but the `cqs_inputs` field in the `QRMFG_PLANT_SPECIFIC_DATA` table is empty.

## Root Cause

The issue occurs because:

1. CQS data exists in the `QRMFG_AUTO_CQS` table
2. The CQS integration service can retrieve and display the data in the frontend
3. However, the `cqs_inputs` field in the plant-specific data table is not being populated during the sync process
4. This happens when plant-specific data records are created without triggering the CQS sync process

## Solution Implemented

### 1. Enhanced Plant Data Creation Process

Modified `PlantQuestionnaireService.getOrCreatePlantSpecificData()` to:
- Check if CQS inputs are empty when retrieving existing records
- Automatically trigger CQS sync if inputs are empty but CQS data exists
- Ensure new records get CQS data populated immediately

### 2. Added CQS Sync Methods

**New Methods Added:**

- `PlantQuestionnaireService.syncCqsDataForPlantRecord()` - Internal method to sync CQS data for a specific plant record
- `PlantQuestionnaireService.forceSyncCqsData()` - Public method to force sync for a specific plant/material combination
- `CqsIntegrationService.fixEmptyCqsInputs()` - Bulk fix method to repair all records with empty CQS inputs

### 3. New API Endpoints

**PlantQuestionnaireController:**
- `POST /api/v1/plant-questionnaire/sync-cqs-data/{plantCode}/{materialCode}` - Force sync CQS data for specific plant/material

**CqsAdminController:**
- `POST /api/v1/admin/cqs/fix-empty-cqs-inputs` - Bulk fix all empty CQS inputs

## How to Fix the Issue

### Option 1: Automatic Fix (Recommended)

The system now automatically detects and fixes empty CQS inputs when:
- Accessing plant-specific data through `getOrCreatePlantSpecificData()`
- Loading questionnaire templates
- Creating new plant-specific records

### Option 2: Manual Fix for Specific Records

Use the new API endpoint to force sync for specific plant/material combinations:

```bash
curl -X POST "http://localhost:8081/qrmfg/api/v1/plant-questionnaire/sync-cqs-data/{plantCode}/{materialCode}"
```

### Option 3: Bulk Fix for All Records

Use the admin endpoint to fix all records with empty CQS inputs:

```bash
curl -X POST "http://localhost:8081/qrmfg/api/v1/admin/cqs/fix-empty-cqs-inputs"
```

## Technical Details

### Data Flow

1. **CQS Data Storage**: CQS data is stored in `QRMFG_AUTO_CQS` table
2. **Plant Data Creation**: Plant-specific records are created in `QRMFG_PLANT_SPECIFIC_DATA` table
3. **CQS Sync Process**: CQS data is converted to JSON and stored in the `cqs_inputs` field
4. **Frontend Display**: The questionnaire loads both CQS data and plant inputs to display the complete form

### Sync Process

```java
// Get CQS data from QRMFG_AUTO_CQS
CqsDataDto cqsData = cqsIntegrationService.getCqsData(materialCode, plantCode);

// Convert to JSON format
String cqsJsonData = objectMapper.writeValueAsString(cqsData.getCqsData());

// Update plant record
plantRecord.updateCqsData(cqsJsonData, updatedBy);
plantSpecificDataRepository.save(plantRecord);
```

### Status Tracking

The system tracks CQS sync status with these values:
- `PENDING` - Sync not yet attempted
- `SYNCED` - CQS data successfully synced
- `NO_DATA` - No CQS data available for this material
- `FAILED` - Sync attempt failed

## Verification

After applying the fix, verify that:

1. **Database Check**: `cqs_inputs` field in `QRMFG_PLANT_SPECIFIC_DATA` contains JSON data
2. **API Response**: Plant-specific data API returns populated CQS inputs
3. **Frontend Display**: CQS auto-populated fields show correct values
4. **Sync Status**: `cqs_sync_status` shows as "SYNCED"

## Prevention

To prevent this issue in the future:

1. Always use `getOrCreatePlantSpecificData()` when creating plant records
2. Ensure CQS data exists before creating plant-specific records
3. Monitor CQS sync status regularly using the admin endpoints
4. Use the bulk fix endpoint periodically to catch any missed records

## Monitoring

Use these endpoints to monitor CQS sync health:

- `GET /api/v1/admin/cqs/sync-stats` - Get overall sync statistics
- `GET /api/v1/admin/cqs/stats` - Get CQS data statistics
- `GET /api/v1/plant-questionnaire/status?plantCode=X&materialCode=Y` - Check specific record status

## Troubleshooting

If CQS inputs are still empty after applying the fix:

1. **Check CQS Data**: Verify CQS data exists in `QRMFG_AUTO_CQS` table
2. **Check Permissions**: Ensure the service has write access to the database
3. **Check Logs**: Look for sync errors in application logs
4. **Manual Sync**: Use the force sync endpoint for specific records
5. **Database Constraints**: Check for any database constraints preventing updates

## Example Usage

```java
// Automatic sync when getting plant data
PlantSpecificDataDto plantData = plantQuestionnaireService.getOrCreatePlantSpecificData(
    "PLANT001", "R123456", 1001L);

// Manual force sync
plantQuestionnaireService.forceSyncCqsData("PLANT001", "R123456");

// Bulk fix all empty records
Map<String, Object> result = cqsIntegrationService.fixEmptyCqsInputs();
```

This fix ensures that CQS auto-populated data is properly stored in the plant-specific data table and available for all downstream processes including exports, reports, and data analysis.