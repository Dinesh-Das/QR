# Frontend Draft Save Testing Guide

## Changes Made to Frontend

### 1. Enhanced Response Handling

**Updated `PlantQuestionnaire.js`** to handle the new backend response format:

**Before:**
```javascript
await workflowAPI.saveDraftPlantResponses(workflowId, draftData);
message.success('Draft saved successfully');
```

**After:**
```javascript
const response = await workflowAPI.saveDraftPlantResponses(workflowId, draftData);

if (response.success) {
  if (response.hasChanges) {
    message.success(`Draft saved successfully (${response.savedFields || 0} fields)`);
  } else {
    // Don't show message for no changes to reduce noise
    console.log('No changes detected - draft not saved');
  }
} else {
  message.warning(response.message || 'Draft save may have failed');
}
```

### 2. Enhanced Error Handling

**Improved error messages** to show specific backend error responses:

```javascript
if (serverError.response && serverError.response.data && serverError.response.data.message) {
  message.error(`Draft save failed: ${serverError.response.data.message}`);
} else {
  message.warning('Draft saved locally. Will sync when connection is restored.');
}
```

### 3. Debug Functionality

**Added debug capabilities** for troubleshooting:

- **Debug Function**: `debugPlantData()` - calls the backend debug endpoint
- **Keyboard Shortcut**: `Ctrl+Shift+D` - triggers debug info
- **Debug Button**: Visible in development mode only
- **Enhanced Logging**: Console logs for draft save operations

### 4. New API Method

**Added to `workflowAPI.js`**:
```javascript
debugPlantData: (plantCode, materialCode) =>
  apiClient.get(`/plant-questionnaire/debug/${encodeURIComponent(plantCode)}/${encodeURIComponent(materialCode)}`)
```

## Testing the Frontend Changes

### 1. Test Draft Save Messages

**Test Case 1: First Save (With Changes)**
1. Open a plant questionnaire
2. Fill in some fields
3. Click "Save Draft" or use Ctrl+S
4. **Expected**: Message shows "Draft saved successfully (X fields)"

**Test Case 2: Subsequent Save (No Changes)**
1. Immediately click "Save Draft" again without making changes
2. **Expected**: No success message shown, check console for "No changes detected"

**Test Case 3: Save with Changes**
1. Modify a field value
2. Click "Save Draft"
3. **Expected**: Message shows "Draft saved successfully (X fields)"

### 2. Test Error Handling

**Test Case 1: Backend Error**
1. Stop the backend server
2. Try to save draft
3. **Expected**: Message shows "Draft saved locally. Will sync when connection is restored."

**Test Case 2: Validation Error**
1. Use browser dev tools to send invalid data
2. **Expected**: Specific error message from backend

### 3. Test Debug Functionality

**Method 1: Keyboard Shortcut**
1. Open a plant questionnaire
2. Press `Ctrl+Shift+D` (or `Cmd+Shift+D` on Mac)
3. **Expected**: "Debug info logged to console" message
4. Check browser console for detailed debug information

**Method 2: Debug Button (Development Only)**
1. Ensure `NODE_ENV=development`
2. Look for "Debug" button next to "Save Draft"
3. Click the debug button
4. **Expected**: Same as keyboard shortcut

**Method 3: Console Command**
```javascript
// In browser console, if you have access to the component
debugPlantData();
```

### 4. Test Console Logging

**Check Browser Console** for these log messages:

```
PlantQuestionnaire: Saving draft with data: {
  plantCode: "PLANT001",
  materialCode: "R123456", 
  responseCount: 15,
  workflowId: 1001
}

PlantQuestionnaire: Draft save response: {
  success: true,
  hasChanges: true,
  savedFields: 15,
  ...
}
```

## Debug Information Available

When using the debug functionality, you'll see:

```json
{
  "plantCode": "PLANT001",
  "materialCode": "R123456",
  "dataExists": true,
  "cqsInputsEmpty": false,
  "plantInputsEmpty": false,
  "cqsSyncStatus": "SYNCED",
  "completionStatus": "IN_PROGRESS",
  "completionPercentage": 75,
  "totalFields": 20,
  "completedFields": 15,
  "lastUpdated": "2024-01-15T10:30:00",
  "updatedBy": "user123",
  "cqsInputsSize": 10,
  "plantInputsSize": 15,
  "plantInputsSample": {
    "field1": "value1",
    "field2": "value2"
  }
}
```

## User Experience Improvements

### 1. Reduced Message Noise
- No success message when no changes are detected
- Only shows meaningful save confirmations
- Clearer error messages

### 2. Better Feedback
- Shows number of fields saved
- Indicates when no changes were detected
- Specific error messages for different failure types

### 3. Developer Tools
- Debug button in development mode
- Keyboard shortcuts for quick debugging
- Comprehensive console logging

## Troubleshooting Guide

### Issue: No Success Messages Showing

**Possible Causes:**
1. Backend returning old string format instead of new object format
2. Network error preventing response from reaching frontend
3. Response parsing error

**Debug Steps:**
1. Check browser console for response data
2. Use debug functionality to check backend status
3. Verify backend is returning new response format

### Issue: "Draft save may have failed" Message

**Possible Causes:**
1. Backend returning `success: false`
2. Validation errors on backend
3. Database save failures

**Debug Steps:**
1. Check browser console for full error details
2. Use debug endpoint to check current data state
3. Check backend logs for error messages

### Issue: Debug Button Not Visible

**Possible Causes:**
1. Not in development mode (`NODE_ENV !== 'development'`)
2. Build configuration issue

**Solutions:**
1. Set `NODE_ENV=development`
2. Use keyboard shortcut `Ctrl+Shift+D` instead
3. Call `debugPlantData()` directly in console

## Browser Console Commands

**Useful commands for testing:**

```javascript
// Check current form data
console.log('Current form data:', form.getFieldsValue());

// Check workflow data
console.log('Workflow data:', workflowData);

// Manually trigger debug
debugPlantData();

// Check if online/offline
console.log('Is offline:', isOffline);

// Check pending changes
console.log('Pending changes:', pendingChanges);
```

## Performance Monitoring

**Watch for these improvements:**

1. **Reduced API Calls**: Fewer draft save requests when no changes
2. **Better User Feedback**: Users know when saves actually happen
3. **Faster Response**: No unnecessary database writes
4. **Cleaner UI**: Less message spam

## Next Steps

1. **Test All Scenarios**: Use the test cases above
2. **Monitor Console**: Watch for the new log messages
3. **Verify Backend Integration**: Ensure new response format works
4. **User Testing**: Have users test the improved draft save experience
5. **Performance Check**: Monitor reduced API call frequency

The frontend changes ensure proper handling of the new backend response format while providing better user experience and debugging capabilities.