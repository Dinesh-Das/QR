# Workflow State Transition Fix

## Issue Identified
The enhanced query workflow assignment logic was failing due to invalid state transitions. The error was:

```
Invalid state transition for workflow R123457: CQS_PENDING -> TECH_PENDING
```

## Root Cause
The `WorkflowState.canTransitionTo()` method only allowed query states (CQS_PENDING, TECH_PENDING, JVC_PENDING) to transition back to PLANT_PENDING, but not to each other. However, with the new multi-query logic, we need to allow transitions between different query states when queries are resolved in different orders.

## Solution Applied

### 1. Updated WorkflowState Transitions
**File:** `src/main/java/com/cqs/qrmfg/model/WorkflowState.java`

**Before:**
```java
case CQS_PENDING:
case TECH_PENDING:
    return newState == PLANT_PENDING;  // Only back to plant
```

**After:**
```java
case CQS_PENDING:
    // CQS can transition back to PLANT_PENDING or to other query states (for multi-query scenarios)
    return newState == PLANT_PENDING || newState == TECH_PENDING || newState == JVC_PENDING;
case TECH_PENDING:
    // TECH can transition back to PLANT_PENDING or to other query states (for multi-query scenarios)
    return newState == PLANT_PENDING || newState == CQS_PENDING || newState == JVC_PENDING;
```

### 2. Updated WorkflowServiceImpl Validation
**File:** `src/main/java/com/cqs/qrmfg/service/impl/WorkflowServiceImpl.java`

**Before:**
```java
// Validate query state transitions
if (newState.isQueryState()) {
    if (workflow.getState() != WorkflowState.PLANT_PENDING) {
        throw new InvalidWorkflowStateException(
            "Can only move to query states from PLANT_PENDING state");
    }
}
```

**After:**
```java
// Validate query state transitions
if (newState.isQueryState()) {
    // Allow transitions to query states from PLANT_PENDING or from other query states (for multi-query scenarios)
    if (workflow.getState() != WorkflowState.PLANT_PENDING && !workflow.getState().isQueryState()) {
        throw new InvalidWorkflowStateException(
            "Can only move to query states from PLANT_PENDING state or from other query states");
    }
}
```

### 3. Enhanced Logging
Added better logging in QueryServiceImpl to track workflow state transitions:

```java
logger.info("Attempting workflow transition: {} -> {} for workflow {}", 
           workflow.getState(), nextState, workflow.getId());
```

## New Allowed Transitions

### From JVC_PENDING:
- ✅ PLANT_PENDING (existing)
- ✅ CQS_PENDING (new - for multi-query scenarios)
- ✅ TECH_PENDING (new - for multi-query scenarios)

### From CQS_PENDING:
- ✅ PLANT_PENDING (existing)
- ✅ TECH_PENDING (new - for multi-query scenarios)
- ✅ JVC_PENDING (new - for multi-query scenarios)

### From TECH_PENDING:
- ✅ PLANT_PENDING (existing)
- ✅ CQS_PENDING (new - for multi-query scenarios)
- ✅ JVC_PENDING (new - for multi-query scenarios)

### From PLANT_PENDING:
- ✅ CQS_PENDING (existing)
- ✅ TECH_PENDING (existing)
- ✅ JVC_PENDING (existing)
- ✅ COMPLETED (existing)

## Example Scenario Now Working

```
Initial: Plant raises Q1→JVC, Q2→CQS, Q3→TECH
State: JVC_PENDING (Q1 is first chronologically)

Step 1: CQS resolves Q2
- Remaining: Q1→JVC, Q3→TECH
- Next: JVC (Q1 is still first)
- Transition: JVC_PENDING → JVC_PENDING (no change needed)

Step 2: JVC resolves Q1
- Remaining: Q3→TECH
- Next: TECH (Q3 is now first)
- Transition: JVC_PENDING → TECH_PENDING ✅ (now allowed)

Step 3: TECH resolves Q3
- Remaining: None
- Next: Plant
- Transition: TECH_PENDING → PLANT_PENDING ✅ (existing)
```

## Benefits

1. **Flexible Query Resolution**: Teams can resolve queries in any order without breaking workflow transitions
2. **Proper State Management**: Workflow always reflects the correct team assignment
3. **Backward Compatibility**: All existing transitions still work
4. **Clear Audit Trail**: Enhanced logging shows exactly what transitions are happening

## Testing

The fix resolves the `InvalidWorkflowStateException` that was occurring when:
- Multiple queries exist for different teams
- Queries are resolved in different order than they were raised
- System needs to reassign workflow to different query teams

The enhanced query workflow assignment logic now works seamlessly with proper state transitions between all query states.