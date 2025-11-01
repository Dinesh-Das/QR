# Enhanced Query Workflow Assignment Implementation

## Overview
This implementation enhances the Plant Questionnaire query system to handle multiple queries to different teams and assign workflows based on the chronological order of query creation.

## Business Logic Implemented

### Current Scenario
When Plant team fills a multi-step questionnaire form, they can raise queries to different teams:
- **Q1** → JVC Team
- **Q2** → CQS Team  
- **Q3** → TECH Team
- **Q4** → JVC Team

The form becomes "pending with queries" (4 queries in this example).

### Enhanced Resolution Logic
When any team resolves a query, the system now:

1. **Checks remaining open queries** for the workflow
2. **If no queries remain** → Assigns workflow back to **PLANT_PENDING** state
3. **If multiple queries remain** → Assigns workflow to the team that raised the **first query chronologically**

### Example Flow
```
Initial State: 4 queries raised
- Q1 (JVC) - Created: 2024-01-01 09:00
- Q2 (CQS) - Created: 2024-01-01 10:00  
- Q3 (TECH) - Created: 2024-01-01 11:00
- Q4 (JVC) - Created: 2024-01-01 12:00

Workflow State: JVC_PENDING (first query was to JVC)

Step 1: CQS resolves Q2
- Remaining queries: Q1, Q3, Q4
- First remaining query: Q1 (JVC) - Created: 09:00
- Workflow State: JVC_PENDING (stays with JVC)

Step 2: JVC resolves Q1  
- Remaining queries: Q3, Q4
- First remaining query: Q3 (TECH) - Created: 11:00
- Workflow State: TECH_PENDING (moves to TECH)

Step 3: TECH resolves Q3
- Remaining queries: Q4
- First remaining query: Q4 (JVC) - Created: 12:00  
- Workflow State: JVC_PENDING (moves back to JVC)

Step 4: JVC resolves Q4
- Remaining queries: None
- Workflow State: PLANT_PENDING (returns to Plant)
```

## Code Changes Made

### 1. Enhanced QueryServiceImpl.resolveQuery() Method
**File:** `src/main/java/com/cqs/qrmfg/service/impl/QueryServiceImpl.java`

**Key Changes:**
- Replaced simple "no open queries" check with enhanced logic
- Added chronological ordering of remaining queries
- Implemented team assignment based on first query creation time
- Added comprehensive logging for debugging

### 2. Updated QueryRepository
**File:** `src/main/java/com/cqs/qrmfg/repository/QueryRepository.java`

**New Methods:**
- `findOpenQueriesByWorkflow()` - Returns open queries ordered by creation time
- `findAllQueriesByWorkflowOrderByCreatedAt()` - Returns all queries for workflow analysis

### 3. Enhanced QueryService Interface
**File:** `src/main/java/com/cqs/qrmfg/service/QueryService.java`

**New Methods:**
- `determineNextAssignedTeam(Long workflowId)` - Determines next team assignment
- `findAllQueriesByWorkflowOrderByCreatedAt(Long workflowId)` - Helper method for analysis

## Technical Implementation Details

### Query Resolution Logic
```java
// Get all remaining open queries for this workflow
List<Query> remainingOpenQueries = queryRepository.findOpenQueriesByWorkflow(workflow.getId());

if (remainingOpenQueries.isEmpty()) {
    // No more open queries - return to plant
    workflowService.transitionToState(workflow.getId(), WorkflowState.PLANT_PENDING, resolvedBy);
} else {
    // Multiple queries exist - assign to the team that raised the first query chronologically
    Query firstQuery = remainingOpenQueries.stream()
        .min((q1, q2) -> q1.getCreatedAt().compareTo(q2.getCreatedAt()))
        .orElse(remainingOpenQueries.get(0));
    
    QueryTeam nextAssignedTeam = firstQuery.getAssignedTeam();
    WorkflowState nextState = nextAssignedTeam.getCorrespondingWorkflowState();
    
    workflowService.transitionToState(workflow.getId(), nextState, resolvedBy);
}
```

### Database Query Optimization
- Queries are ordered by `createdAt ASC` to ensure chronological processing
- Uses `JOIN FETCH` to avoid N+1 query problems
- Efficient single query to get all open queries for a workflow

## Benefits

1. **Fair Processing**: Queries are handled in the order they were raised
2. **Clear Workflow States**: Always know which team should handle the workflow next
3. **Audit Trail**: Complete logging of workflow transitions and reasoning
4. **Performance**: Optimized database queries with proper ordering
5. **Maintainability**: Clean separation of concerns with dedicated methods

## Testing Scenarios

### Scenario 1: Single Query Resolution
- Plant raises 1 query to CQS
- CQS resolves query
- **Expected**: Workflow returns to PLANT_PENDING

### Scenario 2: Multiple Queries to Same Team
- Plant raises 3 queries to CQS (Q1, Q2, Q3)
- CQS resolves Q2
- **Expected**: Workflow stays CQS_PENDING (Q1 is still first)

### Scenario 3: Multiple Queries to Different Teams
- Plant raises Q1→JVC, Q2→CQS, Q3→TECH
- CQS resolves Q2
- **Expected**: Workflow stays JVC_PENDING (Q1 to JVC was first)

### Scenario 4: Complex Resolution Order
- Plant raises Q1→JVC, Q2→CQS, Q3→TECH, Q4→JVC
- Resolution order: Q3(TECH), Q1(JVC), Q4(JVC), Q2(CQS)
- **Expected**: JVC→CQS→JVC→PLANT transitions

## Monitoring and Debugging

The implementation includes comprehensive logging:
- Query resolution details
- Remaining query analysis  
- Workflow state transitions
- Team assignment reasoning
- Error handling and fallbacks

## Future Enhancements

1. **Priority-based Assignment**: Consider query priority in addition to chronological order
2. **Team Workload Balancing**: Factor in team capacity when multiple queries exist
3. **SLA Tracking**: Monitor resolution times per team
4. **Notification System**: Alert teams when workflow is assigned to them
5. **Dashboard Integration**: Visual representation of query flows and assignments

## Compatibility

This implementation is backward compatible with existing functionality:
- Existing single-query workflows continue to work as before
- No database schema changes required
- All existing API endpoints remain functional
- Maintains existing security and validation rules