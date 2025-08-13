# Database Integration Guide for QR Analytics

## Overview
This guide provides the SQL queries and database integration steps needed to replace the mock data in QRAnalyticsService with live database queries.

## Required Database Tables

### Existing Tables (Assumed)
- `workflows` - Main workflow data
- `workflow_state_history` - State transition tracking
- `quality_assessments` - Quality metrics
- `users` - User information
- `user_roles` - User role assignments
- `plants` - Plant information

### New Tables (If Needed)
```sql
-- Production metrics table
CREATE TABLE production_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plant_code VARCHAR(50) NOT NULL,
    production_date DATE NOT NULL,
    production_count INT NOT NULL,
    target_count INT,
    quality_score DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plant_date (plant_code, production_date)
);

-- Quality distribution table
CREATE TABLE quality_distribution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    quality_category VARCHAR(50) NOT NULL,
    assessment_date DATE NOT NULL,
    score DECIMAL(5,2),
    FOREIGN KEY (workflow_id) REFERENCES workflows(id),
    INDEX idx_date_category (assessment_date, quality_category)
);
```

## Service Method Implementations

### 1. Production Metrics

Replace the mock methods in `QRAnalyticsService.java`:

```java
@Autowired
private JdbcTemplate jdbcTemplate;

private Long getTotalProduction(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT COUNT(*) FROM workflows w WHERE 1=1"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (startDate != null) {
        sql.append(" AND w.created_date >= ?");
        params.add(startDate);
    }
    
    if (endDate != null) {
        sql.append(" AND w.created_date <= ?");
        params.add(endDate);
    }
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
}

private Double getQualityScore(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT AVG(qa.score) FROM quality_assessments qa " +
        "JOIN workflows w ON qa.workflow_id = w.id WHERE 1=1"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (startDate != null) {
        sql.append(" AND qa.assessment_date >= ?");
        params.add(startDate.toLocalDate());
    }
    
    if (endDate != null) {
        sql.append(" AND qa.assessment_date <= ?");
        params.add(endDate.toLocalDate());
    }
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    Double result = jdbcTemplate.queryForObject(sql.toString(), Double.class, params.toArray());
    return result != null ? result : 0.0;
}

private Long getActiveWorkflows(String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT COUNT(*) FROM workflows w WHERE w.status NOT IN ('COMPLETED', 'CANCELLED')"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
}

private Long getCompletedToday(String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT COUNT(*) FROM workflows w WHERE w.status = 'COMPLETED' " +
        "AND DATE(w.completed_date) = CURDATE()"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
}
```

### 2. Daily Production Data

```java
private Map<String, Object> getDailyProduction(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT DATE(w.created_date) as production_date, COUNT(*) as count " +
        "FROM workflows w WHERE 1=1"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (startDate != null) {
        sql.append(" AND w.created_date >= ?");
        params.add(startDate);
    }
    
    if (endDate != null) {
        sql.append(" AND w.created_date <= ?");
        params.add(endDate);
    }
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    sql.append(" GROUP BY DATE(w.created_date) ORDER BY production_date");
    
    Map<String, Object> dailyData = new HashMap<>();
    
    jdbcTemplate.query(sql.toString(), params.toArray(), (rs) -> {
        dailyData.put(rs.getDate("production_date").toString(), rs.getInt("count"));
    });
    
    return dailyData;
}
```

### 3. Quality Distribution

```java
private Map<String, Object> getQualityDistribution(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT qd.quality_category, COUNT(*) as count " +
        "FROM quality_distribution qd " +
        "JOIN workflows w ON qd.workflow_id = w.id WHERE 1=1"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (startDate != null) {
        sql.append(" AND qd.assessment_date >= ?");
        params.add(startDate.toLocalDate());
    }
    
    if (endDate != null) {
        sql.append(" AND qd.assessment_date <= ?");
        params.add(endDate.toLocalDate());
    }
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    sql.append(" GROUP BY qd.quality_category");
    
    Map<String, Object> distribution = new HashMap<>();
    
    jdbcTemplate.query(sql.toString(), params.toArray(), (rs) -> {
        distribution.put(rs.getString("quality_category"), rs.getInt("count"));
    });
    
    return distribution;
}
```

### 4. Workflow Efficiency

```java
private Map<String, Object> getEfficiencyByStage(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
    StringBuilder sql = new StringBuilder(
        "SELECT wsh.workflow_state, " +
        "AVG(TIMESTAMPDIFF(HOUR, wsh.state_entered, wsh.state_exited)) as avg_hours " +
        "FROM workflow_state_history wsh " +
        "JOIN workflows w ON wsh.workflow_id = w.id " +
        "WHERE wsh.state_exited IS NOT NULL"
    );
    
    List<Object> params = new ArrayList<>();
    
    if (startDate != null) {
        sql.append(" AND wsh.state_entered >= ?");
        params.add(startDate);
    }
    
    if (endDate != null) {
        sql.append(" AND wsh.state_entered <= ?");
        params.add(endDate);
    }
    
    if (plantCode != null) {
        sql.append(" AND w.plant_code = ?");
        params.add(plantCode);
    }
    
    sql.append(" GROUP BY wsh.workflow_state");
    
    Map<String, Object> efficiency = new HashMap<>();
    
    jdbcTemplate.query(sql.toString(), params.toArray(), (rs) -> {
        String state = rs.getString("workflow_state");
        Double avgHours = rs.getDouble("avg_hours");
        // Convert to efficiency percentage (assuming 24 hours is 100% efficient)
        Double efficiencyPercent = Math.max(0, 100 - (avgHours / 24.0 * 100));
        efficiency.put(state, efficiencyPercent);
    });
    
    return efficiency;
}
```

### 5. Role and Plant Access

```java
private String getUserPrimaryRole(String username) {
    String sql = "SELECT r.role_name FROM users u " +
                "JOIN user_roles ur ON u.id = ur.user_id " +
                "JOIN roles r ON ur.role_id = r.id " +
                "WHERE u.username = ? AND ur.is_primary = true";
    
    try {
        return jdbcTemplate.queryForObject(sql, String.class, username);
    } catch (EmptyResultDataAccessException e) {
        return "USER"; // Default role
    }
}

private String getUserPlantAccess(String username, String requestedPlantCode) {
    // If user is not plant-specific, return requested plant
    if (!isPlantUser(getUserPrimaryRole(username))) {
        return requestedPlantCode;
    }
    
    String sql = "SELECT up.plant_code FROM users u " +
                "JOIN user_plants up ON u.id = up.user_id " +
                "WHERE u.username = ? AND up.is_primary = true";
    
    try {
        String userPlant = jdbcTemplate.queryForObject(sql, String.class, username);
        
        // If user requests specific plant, validate they have access
        if (requestedPlantCode != null) {
            String accessSql = "SELECT COUNT(*) FROM users u " +
                              "JOIN user_plants up ON u.id = up.user_id " +
                              "WHERE u.username = ? AND up.plant_code = ?";
            
            Integer hasAccess = jdbcTemplate.queryForObject(accessSql, Integer.class, username, requestedPlantCode);
            return hasAccess > 0 ? requestedPlantCode : userPlant;
        }
        
        return userPlant;
    } catch (EmptyResultDataAccessException e) {
        return null; // No plant access
    }
}
```

## Configuration Updates

### Add JdbcTemplate Dependency

In `QRAnalyticsService.java`, add:

```java
@Autowired
private JdbcTemplate jdbcTemplate;

@Autowired
private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
```

### Database Connection Configuration

Ensure your `application.properties` has proper database configuration:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/qrmfg_db
spring.datasource.username=${DB_USERNAME:qrmfg_user}
spring.datasource.password=${DB_PASSWORD:your_password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

## Performance Optimization

### 1. Database Indexes

```sql
-- Indexes for performance
CREATE INDEX idx_workflows_plant_created ON workflows(plant_code, created_date);
CREATE INDEX idx_workflows_status_plant ON workflows(status, plant_code);
CREATE INDEX idx_quality_assessments_date ON quality_assessments(assessment_date);
CREATE INDEX idx_workflow_state_history_state ON workflow_state_history(workflow_state, state_entered);
```

### 2. Caching Configuration

Add Redis caching for frequently accessed data:

```java
@Cacheable(value = "qr-analytics", key = "#plantCode + '_' + #startDate + '_' + #endDate")
public QRAnalyticsDashboardDto getQRAnalyticsDashboard(LocalDateTime startDate, LocalDateTime endDate, String plantCode) {
    // Implementation
}
```

### 3. Connection Pooling

Configure HikariCP for optimal database connections:

```properties
# HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
```

## Testing Database Integration

### Integration Test Example

```java
@SpringBootTest
@Transactional
class QRAnalyticsServiceIntegrationTest {

    @Autowired
    private QRAnalyticsService qrAnalyticsService;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testGetQRAnalyticsDashboardWithRealData() {
        // Create test data
        Workflow workflow = new Workflow();
        workflow.setPlantCode("TEST_PLANT");
        workflow.setCreatedDate(LocalDateTime.now());
        workflow.setStatus("ACTIVE");
        entityManager.persistAndFlush(workflow);

        // Test the service
        QRAnalyticsDashboardDto result = qrAnalyticsService.getQRAnalyticsDashboard(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            "TEST_PLANT"
        );

        assertNotNull(result);
        assertTrue(result.getTotalProduction() > 0);
    }
}
```

## Migration Steps

1. **Backup Current Data**: Always backup before making changes
2. **Create New Tables**: Run the table creation scripts
3. **Update Service Methods**: Replace mock methods with database queries
4. **Test Thoroughly**: Run integration tests with real data
5. **Monitor Performance**: Check query performance and optimize as needed
6. **Deploy Gradually**: Consider feature flags for gradual rollout

## Troubleshooting

### Common Issues

1. **Slow Queries**: Add appropriate indexes
2. **Connection Pool Exhaustion**: Increase pool size or optimize queries
3. **Data Inconsistency**: Ensure proper transaction management
4. **Role Access Issues**: Verify user role and plant assignment queries

### Monitoring Queries

```sql
-- Check query performance
SHOW PROCESSLIST;

-- Analyze slow queries
SELECT * FROM mysql.slow_log WHERE start_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- Check index usage
EXPLAIN SELECT * FROM workflows WHERE plant_code = 'Plant A' AND created_date >= '2025-08-01';
```