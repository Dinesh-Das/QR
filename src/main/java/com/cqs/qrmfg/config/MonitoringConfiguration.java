package com.cqs.qrmfg.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for application monitoring and metrics
 */
@Configuration
public class MonitoringConfiguration {

    // Workflow processing metrics
    private final ConcurrentHashMap<String, AtomicLong> workflowStateCounters = new ConcurrentHashMap<>();
    private final AtomicLong activeWorkflows = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong resolvedQueries = new AtomicLong(0);
    private final AtomicLong documentUploads = new AtomicLong(0);
    private final AtomicLong documentDownloads = new AtomicLong(0);
    private final AtomicLong notificationsSent = new AtomicLong(0);
    private final AtomicLong notificationFailures = new AtomicLong(0);

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "qrmfg-workflow");
    }

    @Bean
    public Timer workflowProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("workflow.processing.time")
                .description("Time taken to process workflow state transitions")
                .tag("component", "workflow")
                .register(meterRegistry);
    }

    @Bean
    public Timer queryResolutionTimer(MeterRegistry meterRegistry) {
        return Timer.builder("query.resolution.time")
                .description("Time taken to resolve queries")
                .tag("component", "query")
                .register(meterRegistry);
    }

    @Bean
    public Timer databaseQueryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("database.query.time")
                .description("Database query execution time")
                .tag("component", "database")
                .register(meterRegistry);
    }

    @Bean
    public Timer notificationProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("notification.processing.time")
                .description("Time taken to process notifications")
                .tag("component", "notification")
                .register(meterRegistry);
    }

    @Bean
    public Counter workflowStateTransitionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("workflow.state.transitions")
                .description("Number of workflow state transitions")
                .tag("component", "workflow")
                .register(meterRegistry);
    }

    @Bean
    public Counter queryCreatedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("query.created")
                .description("Number of queries created")
                .tag("component", "query")
                .register(meterRegistry);
    }

    @Bean
    public Counter queryResolvedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("query.resolved")
                .description("Number of queries resolved")
                .tag("component", "query")
                .register(meterRegistry);
    }

    @Bean
    public Counter documentUploadCounter(MeterRegistry meterRegistry) {
        return Counter.builder("document.uploads")
                .description("Number of document uploads")
                .tag("component", "document")
                .register(meterRegistry);
    }

    @Bean
    public Counter documentDownloadCounter(MeterRegistry meterRegistry) {
        return Counter.builder("document.downloads")
                .description("Number of document downloads")
                .tag("component", "document")
                .register(meterRegistry);
    }

    @Bean
    public Counter notificationSentCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification.sent")
                .description("Number of notifications sent")
                .tag("component", "notification")
                .register(meterRegistry);
    }

    @Bean
    public Counter notificationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification.failures")
                .description("Number of notification failures")
                .tag("component", "notification")
                .register(meterRegistry);
    }

    @Bean
    public Counter userActivityCounter(MeterRegistry meterRegistry) {
        return Counter.builder("user.activity")
                .description("User activity events")
                .tag("component", "user")
                .register(meterRegistry);
    }

    // Gauge metrics for current state
    @Bean
    public Gauge activeWorkflowsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("workflow.active.count", activeWorkflows, atomicLong -> atomicLong.doubleValue())
                .description("Number of active workflows")
                .tag("component", "workflow")
                .register(meterRegistry);
    }

    @Bean
    public Gauge pendingQueriesGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("query.pending.count", this, config -> (double)(config.totalQueries.get() - config.resolvedQueries.get()))
                .description("Number of pending queries")
                .tag("component", "query")
                .register(meterRegistry);
    }

    // Getters for metric tracking
    public AtomicLong getActiveWorkflows() {
        return activeWorkflows;
    }

    public AtomicLong getTotalQueries() {
        return totalQueries;
    }

    public AtomicLong getResolvedQueries() {
        return resolvedQueries;
    }

    public AtomicLong getDocumentUploads() {
        return documentUploads;
    }

    public AtomicLong getDocumentDownloads() {
        return documentDownloads;
    }

    public AtomicLong getNotificationsSent() {
        return notificationsSent;
    }

    public AtomicLong getNotificationFailures() {
        return notificationFailures;
    }

    public ConcurrentHashMap<String, AtomicLong> getWorkflowStateCounters() {
        return workflowStateCounters;
    }
}