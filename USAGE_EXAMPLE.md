# Query Workflow Assignment - Usage Example

## Scenario: Plant Team Questionnaire with Multiple Queries

### Initial Setup
A plant team is filling out a multi-step questionnaire for material "MAT-12345" and encounters several questions they need help with.

### Step 1: Plant Raises Multiple Queries

```java
// Plant user raises queries for different questions to different teams

// Q1: Question about material compatibility → JVC Team
Query query1 = queryService.createQuery(
    workflowId, 
    "What is the compatibility class for this material with existing plant equipment?",
    2, // Step 2
    "compatibility_class",
    "Material Compatibility",
    QueryTeam.JVC,
    "plant_user_001"
);
// Created at: 2024-01-15 09:00:00

// Q2: Question about safety data → CQS Team  
Query query2 = queryService.createQuery(
    workflowId,
    "Please provide the LD50 values for this chemical compound.",
    3, // Step 3
    "ld50_oral", 
    "Toxicity Information",
    QueryTeam.CQS,
    "plant_user_001"
);
// Created at: 2024-01-15 10:30:00

// Q3: Question about process parameters → TECH Team
Query query3 = queryService.createQuery(
    workflowId,
    "What are the recommended process temperature ranges for this material?",
    4, // Step 4
    "process_temp_range",
    "Process Parameters", 
    QueryTeam.TECH,
    "plant_user_001"
);
// Created at: 2024-01-15 11:15:00

// Q4: Another JVC question → JVC Team
Query query4 = queryService.createQuery(
    workflowId,
    "Is this material approved for use in Zone 2 hazardous areas?",
    5, // Step 5
    "hazardous_area_approval",
    "Hazardous Area Classification",
    QueryTeam.JVC, 
    "plant_user_001"
);
// Created at: 2024-01-15 12:00:00
```

**Result**: Workflow is now in `JVC_PENDING` state (first query was to JVC)

### Step 2: CQS Team Resolves Their Query

```java
// CQS team member resolves Q2
Query resolvedQuery = queryService.resolveQuery(
    query2.getId(),
    "LD50 Oral: 2500 mg/kg (rat). LD50 Dermal: >5000 mg/kg (rabbit). Material is classified as Category 4 for acute toxicity.",
    "cqs_user_001"
);
```

**System Logic**:
1. Query Q2 is marked as RESOLVED
2. System checks remaining open queries: Q1, Q3, Q4
3. Finds first chronological query: Q1 (JVC, 09:00:00)
4. Workflow stays in `JVC_PENDING` state

**Result**: Workflow remains `JVC_PENDING` (Q1 to JVC is still the earliest)

### Step 3: JVC Team Resolves First Query

```java
// JVC team member resolves Q1
Query resolvedQuery = queryService.resolveQuery(
    query1.getId(),
    "Material compatibility class is B. Compatible with stainless steel and PTFE equipment. Avoid contact with aluminum components.",
    "jvc_user_001"
);
```

**System Logic**:
1. Query Q1 is marked as RESOLVED  
2. System checks remaining open queries: Q3, Q4
3. Finds first chronological query: Q3 (TECH, 11:15:00)
4. Workflow transitions to `TECH_PENDING` state

**Result**: Workflow moves to `TECH_PENDING` (Q3 to TECH is now earliest)

### Step 4: TECH Team Resolves Their Query

```java
// TECH team member resolves Q3
Query resolvedQuery = queryService.resolveQuery(
    query3.getId(),
    "Recommended process temperature: 15-25°C. Maximum safe temperature: 40°C. Avoid temperatures below 5°C to prevent crystallization.",
    "tech_user_001"
);
```

**System Logic**:
1. Query Q3 is marked as RESOLVED
2. System checks remaining open queries: Q4
3. Finds first (and only) chronological query: Q4 (JVC, 12:00:00)  
4. Workflow transitions to `JVC_PENDING` state

**Result**: Workflow moves back to `JVC_PENDING` (Q4 to JVC is the only remaining)

### Step 5: JVC Team Resolves Final Query

```java
// JVC team member resolves Q4
Query resolvedQuery = queryService.resolveQuery(
    query4.getId(),
    "Yes, this material is approved for Zone 2 hazardous areas per IEC 60079-10-1. Certificate reference: ATEX-2024-001234.",
    "jvc_user_001"
);
```

**System Logic**:
1. Query Q4 is marked as RESOLVED
2. System checks remaining open queries: None
3. No queries remain
4. Workflow transitions to `PLANT_PENDING` state

**Result**: Workflow returns to `PLANT_PENDING` (Plant can continue with questionnaire)

## Summary of Workflow State Transitions

```
Initial State: PLANT_PENDING
↓ (Plant raises 4 queries)
JVC_PENDING (Q1-JVC is first chronologically)
↓ (CQS resolves Q2)  
JVC_PENDING (Q1-JVC still first among remaining)
↓ (JVC resolves Q1)
TECH_PENDING (Q3-TECH now first among remaining)
↓ (TECH resolves Q3)
JVC_PENDING (Q4-JVC is last remaining)
↓ (JVC resolves Q4)
PLANT_PENDING (No queries remain - back to plant)
```

## Key Benefits Demonstrated

1. **Fair Processing**: Queries handled in chronological order regardless of resolution order
2. **Clear Ownership**: Always know which team should handle the workflow next
3. **Efficient Workflow**: No confusion about who should act next
4. **Audit Trail**: Complete history of query assignments and resolutions
5. **Flexible Resolution**: Teams can resolve queries in any order without breaking the flow

## API Usage

### Check Current Assignment
```java
QueryTeam nextTeam = queryService.determineNextAssignedTeam(workflowId);
if (nextTeam == null) {
    // Workflow should return to plant
    workflowService.transitionToState(workflowId, WorkflowState.PLANT_PENDING, userId);
} else {
    // Workflow should be assigned to specific team
    WorkflowState nextState = nextTeam.getCorrespondingWorkflowState();
    workflowService.transitionToState(workflowId, nextState, userId);
}
```

### Get Query Analysis
```java
List<Query> allQueries = queryService.findAllQueriesByWorkflowOrderByCreatedAt(workflowId);
List<Query> openQueries = queryService.findQueriesByWorkflowAndStatus(workflowId, QueryStatus.OPEN);

// Analyze query distribution
Map<QueryTeam, Long> queryCountByTeam = openQueries.stream()
    .collect(Collectors.groupingBy(Query::getAssignedTeam, Collectors.counting()));
```

This implementation ensures that the plant questionnaire workflow is handled efficiently and fairly, with clear assignment rules that all teams can understand and follow.