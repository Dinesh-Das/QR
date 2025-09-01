# CQS Auto-Population Integration - Direct Form Field Population

This document describes the CQS (Chemical Questionnaire System) auto-population functionality that directly populates questionnaire form fields in the QRMFG application.

## Overview

The CQS integration automatically populates 33 chemical safety-related fields directly into the Plant Questionnaire form fields based on material codes. This eliminates manual data entry and ensures consistency across plant assessments. **CQS values are displayed directly in the form fields, not in a separate section.**

## Features

### 1. CQS Data Model
- **Table**: `QRMFG_AUTO_CQS`
- **Primary Key**: `material_code`
- **Fields**: 33 CQS-related fields covering hazard classification, toxicity, PSM thresholds, etc.

### 2. Direct Form Field Auto-Population
- Fields marked as CQS auto-populated are automatically filled directly in the questionnaire form
- CQS values appear as pre-filled form field values (radio buttons, text areas, etc.)
- Visual indicators (green borders, CQS tags) show which fields are auto-populated
- Users see CQS data immediately in the form interface, not in a separate section
- Seamless fallback to manual input when CQS data is unavailable

### 3. Excel Export
- Integrated with existing workflow Excel export functionality
- CQS auto-populated values included in the main questionnaire export
- Data source tracking shows whether values come from CQS or plant input

### 4. Frontend Integration
- Enhanced questionnaire form with direct CQS value population
- Visual indicators (green borders, CQS tags) for auto-populated fields
- CQS values pre-filled in radio buttons, text areas, and select fields
- Tooltips showing CQS status and values
- No separate CQS section - all data integrated into main form

## API Endpoints

### Questionnaire Endpoints
```
GET /api/v1/questionnaire/{workflowId}           - Get questionnaire with CQS data
GET /api/v1/questionnaire/{workflowId}/edit     - Get questionnaire for editing
GET /api/v1/workflows/{workflowId}/excel-report - Export to Excel (integrated with existing)
```

### CQS Admin Endpoints
```
GET /api/v1/admin/cqs/data                       - Get all CQS data
GET /api/v1/admin/cqs/data/{materialCode}        - Get CQS data for material
POST /api/v1/admin/cqs/data/{materialCode}       - Create/update CQS data (auto-syncs to plant records)
DELETE /api/v1/admin/cqs/data/{materialCode}     - Delete CQS data
GET /api/v1/admin/cqs/stats                      - Get CQS statistics
GET /api/v1/admin/cqs/field-mapping              - Get field mapping
GET /api/v1/admin/cqs/test/{materialCode}        - Test CQS integration

POST /api/v1/admin/cqs/sync/{materialCode}       - Sync CQS data to plant records for material
POST /api/v1/admin/cqs/sync-all                  - Sync all CQS data to plant records (bulk)
GET /api/v1/admin/cqs/sync-stats                 - Get CQS sync statistics
POST /api/v1/admin/cqs/plant-data                - Create plant-specific data with CQS sync
```

## Database Schema

### QRMFG_AUTO_CQS Table Structure
```sql
CREATE TABLE QRMFG_AUTO_CQS (
    material_code VARCHAR(50) PRIMARY KEY,
    
    -- Hazard Classification Fields
    narcotic_listed VARCHAR(255),
    flash_point_65 VARCHAR(255),
    petroleum_class VARCHAR(255),
    flash_point_21 VARCHAR(255),
    is_corrosive VARCHAR(255),
    highly_toxic VARCHAR(255),
    spill_measures_provided VARCHAR(255),
    is_poisonous VARCHAR(255),
    antidote_specified VARCHAR(255),
    cmvr_listed VARCHAR(255),
    msihc_listed VARCHAR(255),
    factories_act_listed VARCHAR(255),
    recommended_ppe VARCHAR(255),
    reproductive_toxicants VARCHAR(255),
    silica_content VARCHAR(255),
    swarf_analysis VARCHAR(255),
    env_toxic VARCHAR(255),
    hhrm_category VARCHAR(255),
    
    -- PSM Thresholds
    psm_tier1_outdoor VARCHAR(255),
    psm_tier1_indoor VARCHAR(255),
    psm_tier2_outdoor VARCHAR(255),
    psm_tier2_indoor VARCHAR(255),
    
    -- Compatibility and Safety
    compatibility_class VARCHAR(255),
    sap_compatibility VARCHAR(255),
    is_explosive VARCHAR(255),
    autoignition_temp VARCHAR(255),
    dust_explosion VARCHAR(255),
    electrostatic_charge VARCHAR(255),
    
    -- Toxicity Data
    ld50_oral VARCHAR(255),
    ld50_dermal VARCHAR(255),
    lc50_inhalation VARCHAR(255),
    carcinogenic VARCHAR(255),
    mutagenic VARCHAR(255),
    endocrine_disruptor VARCHAR(255),
    
    -- Audit Fields
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    sync_status VARCHAR(20) DEFAULT 'ACTIVE',
    last_sync_date TIMESTAMP
);
```

## Key Components

### Backend Components
1. **QrmfgAutoCqs** - Entity model for CQS data
2. **QrmfgAutoCqsRepository** - Data access layer
3. **CqsIntegrationService** - Business logic for CQS integration
4. **CqsExportController** - Excel export functionality
5. **CqsAdminController** - Admin endpoints for CQS management

### Frontend Components
1. **CqsFieldDisplay** - Component for displaying CQS field status
2. **CqsDataSummary** - Summary component showing CQS completion
3. **Enhanced PlantQuestionnaire** - Updated questionnaire with CQS integration

## Usage

### 1. Viewing CQS Data in Questionnaire
- Navigate to any questionnaire form
- CQS auto-populated fields show pre-filled values directly in form fields
- Green borders and CQS tags (✓ or ⏳) indicate auto-populated fields
- Radio buttons are pre-selected with CQS values
- Text areas contain CQS text data
- Hover over CQS tags to see detailed information
- No separate CQS section - all data integrated into main questionnaire

### 2. Exporting to Excel
- Click "Export to Excel" button in the questionnaire
- Excel file contains three sheets:
  - **CQS Questionnaire**: Complete form with all data
  - **CQS Data Summary**: CQS-specific data and statistics
  - **Completion Status**: Overall progress and status

### 3. Managing CQS Data (Admin)
- Use admin endpoints to view/edit CQS data
- Test CQS integration for specific materials
- Monitor completion statistics

## Sample Data

The system includes sample CQS data for materials MAT001-MAT005 to demonstrate functionality:
- **MAT001 (Acetone)**: Hazardous material with complete CQS data
- **MAT002 (Benzene)**: Hazardous material with complete CQS data
- **MAT003 (Toluene)**: Non-hazardous material with basic CQS data
- **MAT004 (Methanol)**: Hazardous material with complete CQS data
- **MAT005 (Ethanol)**: Non-hazardous material with basic CQS data

### Plant-Specific Data Integration
- Sample plant records created for PLANT001, PLANT002, PLANT003
- Each plant has records for all 5 sample materials
- CQS data automatically synced to `CQS_INPUTS` field in JSON format
- Sync status tracked for each plant-material combination

## Field Mapping

The system maps CQS database field names to user-friendly display names:

| Database Field | Display Name |
|----------------|--------------|
| narcotic_listed | Narcotic Listed |
| flash_point_65 | Flash Point > 65°C |
| petroleum_class | Petroleum Class |
| is_corrosive | Is Corrosive |
| highly_toxic | Highly Toxic |
| recommended_ppe | Recommended PPE |
| psm_tier1_outdoor | PSM Tier 1 Outdoor |
| carcinogenic | Carcinogenic |
| ... | ... |

## Configuration

### Application Properties
No additional configuration required. The system uses existing database connection settings.

### Frontend Configuration
CQS components are automatically integrated into the existing questionnaire form.

## Testing

### Test CQS Integration
```bash
# Initialize sample CQS data
curl -X POST "http://localhost:8080/api/v1/admin/cqs/init-sample-data"

# Test CQS data for a material
curl -X GET "http://localhost:8080/api/v1/admin/cqs/test/MAT001"

# Get CQS statistics
curl -X GET "http://localhost:8080/api/v1/admin/cqs/stats"

# Sync CQS data to plant-specific records for a material
curl -X POST "http://localhost:8080/api/v1/admin/cqs/sync/MAT001"

# Sync all CQS data to plant-specific records (bulk operation)
curl -X POST "http://localhost:8080/api/v1/admin/cqs/sync-all"

# Get CQS sync statistics
curl -X GET "http://localhost:8080/api/v1/admin/cqs/sync-stats"

# Export questionnaire to Excel (integrated with existing workflow export)
curl -X GET "http://localhost:8080/api/v1/workflows/1/excel-report" --output questionnaire.xlsx
```

### Sample CQS Data Creation
```bash
# Create CQS data for a new material (automatically syncs to plant records)
curl -X POST "http://localhost:8080/api/v1/admin/cqs/data/MAT006" \
  -H "Content-Type: application/json" \
  -d '{
    "narcotic_listed": "no",
    "flash_point_65": "yes",
    "is_corrosive": "no",
    "highly_toxic": "no",
    "recommended_ppe": "Safety glasses, work gloves"
  }'

# Create plant-specific data with CQS sync
curl -X POST "http://localhost:8080/api/v1/admin/cqs/plant-data" \
  -d "plantCode=PLANT001&materialCode=MAT006&workflowId=1001"
```

## Future Enhancements

1. **Real-time CQS Sync**: Integration with external CQS systems
2. **Validation Rules**: Enhanced validation based on CQS data
3. **Audit Trail**: Track CQS data changes and sync history
4. **Bulk Import**: Excel/CSV import for CQS data
5. **Advanced Reporting**: Detailed CQS analytics and reports

## Troubleshooting

### Common Issues

1. **CQS Data Not Showing**
   - Check if data exists in QRMFG_AUTO_CQS table
   - Verify material_code matches exactly
   - Check sync_status is 'ACTIVE'

2. **Export Not Working**
   - Ensure workflow ID is valid
   - Check user permissions
   - Verify Apache POI dependencies

3. **Field Not Auto-Populating**
   - Check if field is marked as CQS auto-populated in template
   - Verify field name mapping in CqsIntegrationService
   - Check CQS data completeness

### Logs
Check application logs for CQS-related errors:
```
grep -i "cqs" application.log
grep -i "auto.cqs" application.log
```

## Support

For issues or questions regarding CQS integration:
1. Check this documentation
2. Review application logs
3. Test using admin endpoints
4. Contact development team