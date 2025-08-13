# QR Analytics Deployment Checklist

## Pre-Deployment Verification

### ✅ Backend Checklist
- [ ] **QRAnalyticsController** - New controller with role-based access
- [ ] **QRAnalyticsService** - Service layer with data filtering
- [ ] **QRAnalyticsDashboardDto** - DTO for dashboard data
- [ ] **LegacyMonitoringController** - Backward compatibility maintained
- [ ] **Unit Tests** - Controller and service tests passing
- [ ] **Integration Tests** - Database integration verified
- [ ] **Security Tests** - Role-based access validated

### ✅ Frontend Checklist
- [ ] **QRAnalytics Component** - New analytics dashboard
- [ ] **API Service Updates** - Updated monitoringAPI.js
- [ ] **Navigation Updates** - QR Analytics in menu for all roles
- [ ] **Routing** - New route added to App.js
- [ ] **Role-Based Access** - Plant filtering working
- [ ] **Chart Rendering** - All visualizations working
- [ ] **Export Functionality** - CSV export with filtering
- [ ] **Responsive Design** - Mobile/tablet compatibility

### ✅ Database Checklist
- [ ] **Schema Updates** - New tables created if needed
- [ ] **Indexes** - Performance indexes added
- [ ] **Data Migration** - Existing data preserved
- [ ] **Backup** - Full database backup completed
- [ ] **Connection Pool** - Optimized for new queries

## Deployment Steps

### Phase 1: Backend Deployment
```bash
# 1. Build the application
./mvnw clean package -DskipTests

# 2. Run tests
./mvnw test

# 3. Deploy to staging
# Copy JAR to staging server
scp target/qrmfg-*.jar user@staging-server:/opt/qrmfg/

# 4. Start staging application
ssh user@staging-server "cd /opt/qrmfg && java -jar qrmfg-*.jar --spring.profiles.active=staging"

# 5. Verify endpoints
curl -H "Authorization: Bearer $TOKEN" http://staging-server:8080/api/v1/qr-analytics/dashboard
```

### Phase 2: Frontend Deployment
```bash
# 1. Build frontend
cd frontend
npm run build

# 2. Deploy to web server
rsync -av build/ user@web-server:/var/www/qrmfg/

# 3. Update nginx configuration if needed
sudo nginx -t && sudo systemctl reload nginx
```

### Phase 3: Database Updates
```sql
-- Run any required schema updates
-- (See DATABASE_INTEGRATION_GUIDE.md for specific queries)

-- Verify data integrity
SELECT COUNT(*) FROM workflows;
SELECT COUNT(*) FROM quality_assessments;

-- Check indexes
SHOW INDEX FROM workflows;
```

## Post-Deployment Verification

### API Endpoint Testing
```bash
# Test QR Analytics endpoints
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://your-server/api/v1/qr-analytics/dashboard"

curl -H "Authorization: Bearer $PLANT_USER_TOKEN" \
  "http://your-server/api/v1/qr-analytics/dashboard?plantCode=Plant_A"

curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://your-server/api/v1/qr-analytics/production-metrics"

# Test legacy endpoints still work
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://your-server/api/v1/admin/monitoring/dashboard"
```

### Frontend Testing
- [ ] **Login as Admin** - Can access QR Analytics and see all plants
- [ ] **Login as Plant User** - Can access QR Analytics, sees only assigned plant
- [ ] **Login as JVC User** - Can access QR Analytics with appropriate data
- [ ] **Login as CQS User** - Can access QR Analytics with quality focus
- [ ] **Login as Tech User** - Can access both QR Analytics and legacy monitoring
- [ ] **Navigation** - QR Analytics appears in menu for all roles
- [ ] **Charts** - All visualizations render correctly
- [ ] **Filters** - Date range and plant filters work
- [ ] **Export** - CSV export works with proper filtering
- [ ] **Responsive** - Works on mobile and tablet

### Role-Based Access Testing
| User Role | Dashboard Access | Plant Filter | Data Scope | Export Access |
|-----------|-----------------|--------------|------------|---------------|
| ADMIN | ✅ Full | All Plants | Global | ✅ All Data |
| TECH_USER | ✅ Full | All Plants | Global | ✅ All Data |
| JVC_USER | ✅ Limited | All Plants | Workflow | ✅ Filtered |
| CQS_USER | ✅ Limited | All Plants | Quality | ✅ Filtered |
| PLANT_USER | ✅ Plant Only | Own Plant | Plant Data | ✅ Plant Only |

### Performance Testing
```bash
# Load test the new endpoints
ab -n 100 -c 10 -H "Authorization: Bearer $TOKEN" \
  http://your-server/api/v1/qr-analytics/dashboard

# Monitor database performance
# Check slow query log
# Monitor connection pool usage
# Verify cache hit rates
```

## Rollback Plan

### If Issues Occur
1. **Frontend Rollback**:
   ```bash
   # Restore previous frontend build
   rsync -av backup/build/ user@web-server:/var/www/qrmfg/
   ```

2. **Backend Rollback**:
   ```bash
   # Stop current application
   sudo systemctl stop qrmfg
   
   # Restore previous JAR
   cp backup/qrmfg-previous.jar /opt/qrmfg/qrmfg.jar
   
   # Start previous version
   sudo systemctl start qrmfg
   ```

3. **Database Rollback**:
   ```sql
   -- If schema changes were made, restore from backup
   -- mysql -u root -p qrmfg_db < backup_before_deployment.sql
   ```

### Rollback Verification
- [ ] Legacy WorkflowMonitoring still accessible
- [ ] All existing functionality works
- [ ] No data loss occurred
- [ ] Performance is restored

## Monitoring and Alerts

### Application Monitoring
```bash
# Check application logs
tail -f /var/log/qrmfg/application.log | grep -E "(ERROR|WARN|QRAnalytics)"

# Monitor JVM metrics
jstat -gc -t $(pgrep java) 5s

# Check database connections
mysql -e "SHOW PROCESSLIST;" | grep qrmfg
```

### Set Up Alerts
- [ ] **High Response Time** - Alert if API responses > 5 seconds
- [ ] **Database Connection Pool** - Alert if pool utilization > 80%
- [ ] **Error Rate** - Alert if error rate > 5%
- [ ] **Memory Usage** - Alert if JVM heap > 85%

### Health Checks
```bash
# Application health
curl http://your-server/actuator/health

# Database connectivity
curl http://your-server/actuator/health/db

# Custom QR Analytics health check
curl -H "Authorization: Bearer $TOKEN" \
  http://your-server/api/v1/qr-analytics/dashboard | jq '.lastUpdated'
```

## User Communication

### Announcement Template
```
Subject: New QR Analytics Dashboard Available

Dear QRMFG Users,

We're excited to announce the launch of our new QR Analytics dashboard, providing enhanced insights into manufacturing processes.

Key Features:
✅ Real-time production metrics
✅ Quality analytics and trends
✅ Workflow efficiency insights
✅ Role-based data access
✅ Plant-specific filtering for plant users

Access: The QR Analytics dashboard is now available in your main navigation menu.

Training: [Link to training materials]
Support: [Contact information]

The previous monitoring system remains available for administrators during the transition period.

Best regards,
IT Manufacturing Automation Team
```

### Training Materials Needed
- [ ] **User Guide** - How to use QR Analytics
- [ ] **Role-Specific Guides** - Different views for different roles
- [ ] **Video Tutorials** - Key features demonstration
- [ ] **FAQ Document** - Common questions and answers

## Success Metrics

### Technical Metrics (Week 1)
- [ ] **Uptime** - 99.9% availability
- [ ] **Response Time** - < 2 seconds average
- [ ] **Error Rate** - < 1%
- [ ] **User Adoption** - 80% of active users access QR Analytics

### Business Metrics (Month 1)
- [ ] **User Engagement** - Increased time spent on analytics
- [ ] **Decision Speed** - Faster issue identification and resolution
- [ ] **Data Accuracy** - Reduced reliance on manual reports
- [ ] **User Satisfaction** - Positive feedback from stakeholders

### Long-term Metrics (Quarter 1)
- [ ] **Process Improvement** - Measurable efficiency gains
- [ ] **Quality Improvements** - Better quality scores
- [ ] **Cost Reduction** - Reduced manual reporting effort
- [ ] **Scalability** - System handles increased load

## Support and Maintenance

### Immediate Support (First 48 Hours)
- [ ] **On-call Support** - Technical team available
- [ ] **User Support** - Help desk prepared for questions
- [ ] **Monitoring** - Enhanced monitoring during initial rollout

### Ongoing Maintenance
- [ ] **Regular Updates** - Monthly feature updates
- [ ] **Performance Optimization** - Quarterly performance reviews
- [ ] **Security Updates** - Regular security patches
- [ ] **User Feedback** - Continuous improvement based on feedback

## Documentation Updates

### Technical Documentation
- [ ] **API Documentation** - Updated with new endpoints
- [ ] **Database Schema** - Updated ERD and table documentation
- [ ] **Deployment Guide** - This checklist and procedures
- [ ] **Troubleshooting Guide** - Common issues and solutions

### User Documentation
- [ ] **User Manual** - Updated with QR Analytics section
- [ ] **Quick Start Guide** - Getting started with QR Analytics
- [ ] **Feature Comparison** - QR Analytics vs Legacy Monitoring
- [ ] **Best Practices** - How to get the most from QR Analytics

---

## Final Deployment Approval

**Deployment Lead**: _________________ Date: _________

**Technical Lead**: _________________ Date: _________

**Business Owner**: _________________ Date: _________

**QA Lead**: _________________ Date: _________

---

**Deployment Status**: 
- [ ] ✅ APPROVED - Ready for production deployment
- [ ] ❌ BLOCKED - Issues need resolution
- [ ] ⏸️ DELAYED - Deployment postponed

**Notes**: 
_________________________________
_________________________________
_________________________________