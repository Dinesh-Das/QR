import { Button, Space, message, Alert } from 'antd';
import React, { useCallback } from 'react';

import UserErrorBoundary from '../components/ErrorBoundaries/UserErrorBoundary';
import UserModal from '../components/User/UserModal';
import UserTable from '../components/User/UserTable';
import { usePlantAssignment } from '../hooks/usePlantAssignment';
import { useUserManagement } from '../hooks/useUserManagement';
import { getCurrentUser, isAuthenticated, getUserRoles } from '../services/auth';
import { masterDataAPI } from '../services/masterDataAPI';
const Users = () => {
  // Use custom hooks for state management
  const { users, roles, loading, modalVisible, editingUser, actions } = useUserManagement();
  const { availablePlants } = usePlantAssignment();

  // Handle test API calls (keeping existing functionality)
  const handleTestLocationAPI = useCallback(async () => {
    try {
      const result = await masterDataAPI.testLocationMaster();
      message.success(result);
    } catch (error) {
      message.error(`Test failed: ${  error.message}`);
    }
  }, []);

  const handleTestSimpleAPI = useCallback(async () => {
    try {
      const result = await masterDataAPI.testSimpleLocation();
      message.success(result);
    } catch (error) {
      message.error(`Simple test failed: ${  error.message}`);
    }
  }, []);

  const handleDiagnosticAPI = useCallback(async () => {
    try {
      const result = await masterDataAPI.diagnosticLocationMaster();
      console.log('Diagnostic result:', result);
      message.info('Check console for diagnostic info');
    } catch (error) {
      message.error(`Diagnostic failed: ${  error.message}`);
    }
  }, []);

  // Handle retry for error boundary
  const handleRetry = useCallback(() => {
    const controller = new AbortController();
    actions.fetchUsers(controller.signal);
  }, [actions]);

  // Test user roles endpoint
  const handleTestUserRoles = useCallback(async () => {
    try {
      // Import the API client
      const apiClient = (await import('../api/client')).default;
      const result = await apiClient.get('/admin/test/user-roles');
      console.log('Test user roles result:', result);
      message.success('Test successful! Check console for user roles test data');
    } catch (error) {
      console.error('Test user roles failed:', error);
      if (error.status === 401) {
        message.error('Authentication failed. Please log in first.');
      } else {
        message.error(`Test failed: ${error.message}`);
      }
    }
  }, []);

  // Quick login redirect
  const handleGoToLogin = useCallback(() => {
    window.location.href = '/qrmfg/login';
  }, []);

  // Fix admin roles
  const handleFixAdminRoles = useCallback(async () => {
    try {
      const apiClient = (await import('../api/client')).default;
      const result = await apiClient.post('/admin/fix-admin-roles');
      console.log('Fix admin roles result:', result);
      message.success('Admin roles fixed! Refreshing user data...');
      // Refresh the users data
      const controller = new AbortController();
      actions.fetchUsers(controller.signal);
    } catch (error) {
      console.error('Fix admin roles failed:', error);
      message.error(`Fix failed: ${error.message}`);
    }
  }, [actions]);

  // Get current user info for debugging
  const currentUser = getCurrentUser();
  const userRoles = getUserRoles();
  const authenticated = isAuthenticated();

  return (
    <UserErrorBoundary context="user-management" onRetry={handleRetry}>
      <div style={{ padding: '24px' }}>
        {/* Debug info */}
        <Alert
          message={`Debug Info: User: ${currentUser || 'Not logged in'}, Authenticated: ${authenticated}, Roles: ${userRoles.join(', ') || 'None'}`}
          type={authenticated ? "info" : "warning"}
          style={{ marginBottom: 16 }}
          showIcon
          action={
            !authenticated && (
              <Button size="small" onClick={handleGoToLogin}>
                Go to Login
              </Button>
            )
          }
        />

        {/* Action buttons */}
        <div style={{ marginBottom: 16 }}>
          <Space wrap>
            <Button 
              type="primary" 
              onClick={actions.openAddModal}
              data-testid="add-user-button"
            >
              Add User
            </Button>
            <Button onClick={handleTestLocationAPI}>
              Test Location API
            </Button>
            <Button onClick={handleTestSimpleAPI}>
              Test Simple API
            </Button>
            <Button onClick={handleDiagnosticAPI}>
              Diagnostic
            </Button>
            <Button onClick={handleTestUserRoles}>
              Test User Roles
            </Button>
            <Button onClick={handleFixAdminRoles} type="dashed">
              Fix Admin Roles
            </Button>
          </Space>
        </div>

        {/* Users table */}
        <UserTable
          users={users}
          loading={loading}
          onEdit={actions.openEditModal}
          onDelete={actions.deleteUser}
          availablePlants={availablePlants}
        />

        {/* User modal */}
        <UserModal
          visible={modalVisible}
          editingUser={editingUser}
          roles={roles}
          onSave={actions.saveUser}
          onCancel={actions.closeModal}
          loading={loading}
        />
      </div>
    </UserErrorBoundary>
  );
};

export default Users;
