package com.cqs.qrmfg.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Configuration for database performance optimization
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.cqs.qrmfg.repository")
@EntityScan(basePackages = "com.cqs.qrmfg.model")
public class DatabaseOptimizationConfiguration {

    /**
     * Hibernate performance optimization properties
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // Disable second-level cache for now (can be enabled later with proper dependencies)
            hibernateProperties.put("hibernate.cache.use_second_level_cache", "false");
            hibernateProperties.put("hibernate.cache.use_query_cache", "false");
            hibernateProperties.put("hibernate.cache.region.factory_class", 
                    "org.hibernate.cache.internal.NoCacheRegionFactory");
            
            // Enable batch processing
            hibernateProperties.put("hibernate.jdbc.batch_size", "25");
            hibernateProperties.put("hibernate.order_inserts", "true");
            hibernateProperties.put("hibernate.order_updates", "true");
            hibernateProperties.put("hibernate.jdbc.batch_versioned_data", "true");
            
            // Connection pool optimization
            hibernateProperties.put("hibernate.connection.provider_disables_autocommit", "true");
            
            // Query optimization
            hibernateProperties.put("hibernate.query.plan_cache_max_size", "2048");
            hibernateProperties.put("hibernate.query.plan_parameter_metadata_max_size", "128");
            
            // Statistics for monitoring
            hibernateProperties.put("hibernate.generate_statistics", "true");
            hibernateProperties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "1000");
        };
    }

    /**
     * Database indexing recommendations
     * These should be applied as database migration scripts
     */
    public static class IndexingRecommendations {
        
        public static final String[] RECOMMENDED_INDEXES = {
            // Workflow table indexes for dashboard queries
            "CREATE INDEX idx_workflow_state ON qrmfg_material_workflows(workflow_state)",
            "CREATE INDEX idx_workflow_created_at ON qrmfg_material_workflows(created_at)",
            "CREATE INDEX idx_workflow_plant_state ON qrmfg_material_workflows(plant_code, workflow_state)",
            "CREATE INDEX idx_workflow_project_material ON qrmfg_material_workflows(project_code, material_code)",
            "CREATE INDEX idx_workflow_initiated_by ON qrmfg_material_workflows(initiated_by)",
            "CREATE INDEX idx_workflow_last_modified ON qrmfg_material_workflows(last_modified)",
            
            // Composite indexes for common dashboard queries
            "CREATE INDEX idx_workflow_state_created ON qrmfg_material_workflows(workflow_state, created_at)",
            "CREATE INDEX idx_workflow_plant_created ON qrmfg_material_workflows(plant_code, created_at)",
            "CREATE INDEX idx_workflow_project_state ON qrmfg_material_workflows(project_code, workflow_state)",
            
            // Query table indexes
            "CREATE INDEX idx_query_workflow_id ON qrmfg_queries(workflow_id)",
            "CREATE INDEX idx_query_status ON qrmfg_queries(status)",
            "CREATE INDEX idx_query_assigned_team ON qrmfg_queries(assigned_team)",
            "CREATE INDEX idx_query_created_at ON qrmfg_queries(created_at)",
            "CREATE INDEX idx_query_resolved_at ON qrmfg_queries(resolved_at)",
            
            // Composite indexes for query dashboard
            "CREATE INDEX idx_query_team_status ON qrmfg_queries(assigned_team, status)",
            "CREATE INDEX idx_query_workflow_status ON qrmfg_queries(workflow_id, status)",
            
            // Document table indexes
            "CREATE INDEX idx_document_workflow_id ON qrmfg_workflow_documents(workflow_id)",
            "CREATE INDEX idx_document_uploaded_at ON qrmfg_workflow_documents(uploaded_at)",
            "CREATE INDEX idx_document_file_type ON qrmfg_workflow_documents(file_type)",
            "CREATE INDEX idx_document_uploaded_by ON qrmfg_workflow_documents(uploaded_by)",
            
            // Reference table indexes for dropdown performance
            "CREATE INDEX idx_qrmfg_project_item_project_code ON qrmfg_project_item_master(project_code)",
            "CREATE INDEX idx_qrmfg_project_item_item_code ON qrmfg_project_item_master(item_code)",
            "CREATE INDEX idx_qrmfg_location_location_code ON qrmfg_location_master(location_code)",
            
            // Audit table indexes
            "CREATE INDEX idx_workflow_aud_rev ON qrmfg_material_workflows_aud(rev)",
            "CREATE INDEX idx_workflow_aud_id_rev ON qrmfg_material_workflows_aud(id, rev)",
            
            // Performance monitoring indexes
            "CREATE INDEX idx_workflow_completion_time ON qrmfg_material_workflows(completed_at) WHERE completed_at IS NOT NULL",
            "CREATE INDEX idx_workflow_sla_monitoring ON qrmfg_material_workflows(workflow_state, created_at, last_modified) WHERE workflow_state != 'COMPLETED'"
        };
        
        public static final String[] PERFORMANCE_VIEWS = {
            // Dashboard summary view
            "CREATE OR REPLACE VIEW v_workflow_dashboard_summary AS " +
            "SELECT " +
            "    w.workflow_state, " +
            "    w.plant_code, " +
            "    w.project_code, " +
            "    COUNT(*) as workflow_count, " +
            "    AVG((SYSDATE - w.created_at) * 24) as avg_age_hours, " +
            "    COUNT(CASE WHEN (SYSDATE - w.created_at) > 3 THEN 1 END) as overdue_count " +
            "FROM qrmfg_material_workflows w " +
            "WHERE w.workflow_state != 'COMPLETED' " +
            "GROUP BY w.workflow_state, w.plant_code, w.project_code",
            
            // Query performance view
            "CREATE OR REPLACE VIEW v_query_performance_summary AS " +
            "SELECT " +
            "    q.assigned_team, " +
            "    q.status, " +
            "    COUNT(*) as query_count, " +
            "    AVG((CASE WHEN q.resolved_at IS NOT NULL THEN q.resolved_at ELSE SYSDATE END - q.created_at) * 24) as avg_resolution_hours, " +
            "    COUNT(CASE WHEN q.status = 'OPEN' AND (SYSDATE - q.created_at) > 3 THEN 1 END) as overdue_count " +
            "FROM qrmfg_queries q " +
            "GROUP BY q.assigned_team, q.status",
            
            // Material reuse analytics view
            "CREATE OR REPLACE VIEW v_material_reuse_analytics AS " +
            "SELECT " +
            "    w.project_code, " +
            "    w.material_code, " +
            "    COUNT(DISTINCT w.id) as workflow_count, " +
            "    COUNT(DISTINCT w.plant_code) as plant_count, " +
            "    COUNT(DISTINCT wd.id) as document_count, " +
            "    MAX(w.created_at) as last_used " +
            "FROM qrmfg_material_workflows w " +
            "LEFT JOIN qrmfg_workflow_documents wd ON w.id = wd.workflow_id " +
            "GROUP BY w.project_code, w.material_code " +
            "HAVING COUNT(DISTINCT w.id) > 1"
        };
    }
}