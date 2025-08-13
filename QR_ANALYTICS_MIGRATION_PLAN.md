# QR Analytics Migration Plan

## Overview
This document outlines the step-by-step migration from WorkflowMonitoring to QR Analytics with enhanced role-based access and live data integration.

## Migration Steps Completed

### ✅ Phase 1: Backend API Enhancement
- **File**: `src/main/java/com/cqs/qrmfg/controller/AdminMonitoringController.java`
- **Changes**: 
  - Renamed controller to `QRAnalyticsController`
  - Updated endpoint mapping to `/api/v1/qr-analytics`
  - Enhanced role-based access to include all user roles
  - Added new QR-specific endpoints:
    - `/dashboard` - Main analytics dashboard
    - `/production-metrics` - Production analytics
    - `/quality-metrics` - Quality analytics  
    - `/workflow-efficiency` - Workflow efficiency metrics

### ✅ Phase 2: Service Layer Implementation
- **File**: `src/main/java/com/cqs/qrmfg/service/QRAnalyticsService.java`
- **Features**:
  - Role-based data filtering
  - Plant-specific data access for plant users
  - Live data integration points (ready for database implementation)
  - Comprehensive analytics methods

### ✅ Phase 3: DTO Enhancement
- **File**: `src/main/java/com/cqs/qrmfg/dto/QRAnalyticsDashboardDto.java`
- **Features**:
  - QR-specific data structure
  - Role and plant metadata
  - Timestamp tracking

### ✅ Phase 4: Frontend Service Update
- **File**: `frontend/src/services/monitoringAPI.js`
- **Changes**:
  - Renamed to `qrAnalyticsAPI`
  - Updated endpoints to match new backend
  - Added role-based filtering parameters
  - Maintained backward compatibility

### ✅ Phase 5: New QR Analytics Component
- **File**: `frontend/src/screens/QRAnalytics.js`
- **Features**:
  - Role-based access control integration
  - Plant-specific filtering for plant users
  - Live data visualization with Chart.js
  - Export functionality
  - Responsive design with Ant Design
  - Three main analytics tabs:
    - Production Analytics
    - Quality Analytics
    - Workflow Efficiency

### ✅ Phase 6: Navigation Updates
- **File**: `frontend/src/services/rbacService.js`
- **Changes**:
  - Added QR Analytics to navigation for all roles
  - Updated accessible screens list
  - Maintained legacy workflow monitoring access for admins

### ✅ Phase 7: Routing Integration
- **File**: `frontend/src/App.js`
- **Changes**:
  - Added QR Analytics route
  - Maintained legacy route for backward compatibility

### ✅ Phase 8: Admin Panel Integration
- **File**: `frontend/src/screens/admin.js`
- **Changes**:
  - Added legacy monitoring access for admins
  - Maintained all existing admin functionality

## Role-Based Access Implementation

### Access Matrix
| Role | Dashboard Access | Plant Filtering | Data Scope |
|------|-----------------|----------------|------------|
| ADMIN | ✅ Full Access | All Plants | Global Data |
| TECH_USER | ✅ Full Access | All Plants | Global Data |
| JVC_USER | ✅ Analytics Only | All Plants | Workflow Data |
| CQS_USER | ✅ Analytics Only | All Plants | Quality Data |
| PLANT_USER | ✅ Plant-Specific | Own Plant Only | Plant Data |
| VIEWER | ✅ Read-Only | Based on Assignment | Limited Data |

### Data Filtering Logic
1. **Admin/Tech Users**: See all data across all plants
2. **Plant Users**: Automatically filtered to their assigned plant(s)
3. **Other Roles**: See aggregated data with appropriate filtering
4. **Export**: Respects same filtering rules as dashboard view

## Live Data Integration Points

### Database Queries to Implement
1. **Production Metrics**:
   ```sql
   -- Daily production by plant and date range
   SELECT plant_code, DATE(created_date), COUNT(*) as production_count
   FROM workflows 
   WHERE created_date BETWEEN ? AND ?
   GROUP BY plant_code, DATE(created_date)
   ```

2. **Quality Metrics**:
   ```sql
   -- Quality distribution by plant
   SELECT quality_status, COUNT(*) as count
   FROM quality_assessments qa
   JOIN workflows w ON qa.workflow_id = w.id
   WHERE w.plant_code = ? AND qa.assessment_date BETWEEN ? AND ?
   GROUP BY quality_status
   ```

3. **Workflow Efficiency**:
   ```sql
   -- Average time in each workflow stage
   SELECT workflow_state, AVG(TIMESTAMPDIFF(HOUR, state_entered, state_exited)) as avg_hours
   FROM workflow_state_history
   WHERE plant_code = ? AND state_entered BETWEEN ? AND ?
   GROUP BY workflow_state
   ```

## Testing Strategy

### Unit Tests Required
1. **QRAnalyticsService**:
   - Role-based filtering logic
   - Plant access validation
   - Data aggregation methods

2. **QRAnalyticsController**:
   - Endpoint security
   - Parameter validation
   - Response formatting

### Integration Tests Required
1. **Role-based access**:
   - Each role sees appropriate data
   - Plant filtering works correctly
   - Export respects access rules

2. **Frontend integration**:
   - API calls work correctly
   - Charts render with live data
   - Filters apply properly

## Deployment Steps

### Backend Deployment
1. Deploy new controller and service classes
2. Update database with any required schema changes
3. Test API endpoints with different user roles
4. Verify data filtering works correctly

### Frontend Deployment
1. Deploy new QR Analytics component
2. Update navigation and routing
3. Test role-based access in UI
4. Verify charts and exports work

### Rollback Plan
1. Legacy WorkflowMonitoring component remains available
2. Admin users can access via `/qrmfg/admin/workflow-monitoring`
3. API endpoints maintain backward compatibility
4. Database changes are additive only

## Performance Considerations

### Optimization Points
1. **Caching**: Implement Redis caching for frequently accessed metrics
2. **Database Indexing**: Add indexes on date ranges and plant codes
3. **Lazy Loading**: Load chart data on tab activation
4. **Pagination**: Implement for large data sets

### Monitoring
1. **API Response Times**: Monitor new analytics endpoints
2. **Database Query Performance**: Track slow queries
3. **User Access Patterns**: Monitor which roles access which features
4. **Export Usage**: Track export frequency and size

## Security Considerations

### Data Protection
1. **Role Validation**: Server-side validation of user roles
2. **Plant Access**: Strict enforcement of plant-based data filtering
3. **Audit Logging**: Log all analytics access and exports
4. **Data Sanitization**: Ensure no sensitive data leaks between plants

### Access Control
1. **JWT Validation**: Verify tokens on all endpoints
2. **Role Hierarchy**: Respect role inheritance (Admin > Tech > Others)
3. **Plant Assignment**: Validate plant access on every request
4. **Export Permissions**: Control who can export what data

## Future Enhancements

### Phase 2 Features
1. **Real-time Updates**: WebSocket integration for live metrics
2. **Advanced Filtering**: More granular date and category filters
3. **Custom Dashboards**: User-configurable dashboard layouts
4. **Mobile Optimization**: Responsive design improvements

### Phase 3 Features
1. **Predictive Analytics**: ML-based trend prediction
2. **Automated Alerts**: Threshold-based notifications
3. **Comparative Analysis**: Plant-to-plant comparisons
4. **Historical Trending**: Long-term trend analysis

## Success Metrics

### Technical Metrics
- [ ] All user roles can access QR Analytics
- [ ] Plant users see only their plant data
- [ ] API response times < 2 seconds
- [ ] Zero data leakage between plants
- [ ] 100% backward compatibility maintained

### Business Metrics
- [ ] Increased user engagement with analytics
- [ ] Faster decision-making with live data
- [ ] Improved workflow efficiency visibility
- [ ] Enhanced quality tracking capabilities
- [ ] Better plant performance insights

## Support and Maintenance

### Documentation Updates
- [ ] Update user manuals for new QR Analytics
- [ ] Create role-specific user guides
- [ ] Document new API endpoints
- [ ] Update troubleshooting guides

### Training Requirements
- [ ] Train users on new analytics features
- [ ] Educate plant users on their specific views
- [ ] Update admin training for legacy access
- [ ] Create video tutorials for key features