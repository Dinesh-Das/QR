import { Typography, Button, Space, Modal, Select, message } from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import PlantErrorBoundary from '../components/ErrorBoundaries/PlantErrorBoundary';
import FilterPanel from '../components/Plant/FilterPanel';
import PlantDashboard from '../components/Plant/PlantDashboard';
import WorkflowTable from '../components/Plant/WorkflowTable';
import PlantQuestionnaire from '../components/PlantQuestionnaire';
import { PlantOnly } from '../components/RoleBasedComponent';
import { usePlantWorkflows } from '../hooks/usePlantWorkflows';
import { useRoleBasedAccess } from '../hooks/useRoleBasedAccess';
import { useWorkflowFilters } from '../hooks/useWorkflowFilters';
import { getCurrentUser, isAuthenticated } from '../services/auth';
import { masterDataAPI } from '../services/masterDataAPI';
import { userAPI } from '../services/userAPI';
import { workflowAPI } from '../services/workflowAPI';

const { Title } = Typography;
const { Option } = Select;

const PlantView = () => {
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [questionnaireVisible, setQuestionnaireVisible] = useState(false);
  const [currentPlant, setCurrentPlant] = useState(null);
  const [userPlantData, setUserPlantData] = useState(null);
  const [availablePlants, setAvailablePlants] = useState([]);

  // Use role-based access control
  const { isPlantUser, isAdmin, userPlants, primaryPlant, filterByPlantAccess } = useRoleBasedAccess();

  // Use custom hooks for workflow management and filtering
  const {
    workflows,
    loading,
    error,
    dashboardStats,
    refreshWorkflows
  } = usePlantWorkflows(currentPlant, userPlantData);

  const {
    filteredWorkflows,
    searchText,
    statusFilter,
    completionFilter,
    updateSearchText,
    updateStatusFilter,
    updateCompletionFilter,
    clearAllFilters,
    applyPreset,
    saveFilters,
    filterPresets,
    filterSummary
  } = useWorkflowFilters(workflows);



  useEffect(() => {
    const controller = new AbortController();

    const fetchData = async () => {
      try {
        await loadUserPlantData(controller.signal);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error loading user plant data:', error);
        }
      }
    };

    fetchData();

    return () => {
      controller.abort();
    };
  }, []);

  const loadUserPlantData = async signal => {
    try {
      // Check if user is authenticated first
      if (!isAuthenticated()) {
        console.warn('User is not authenticated');
        message.error('You are not logged in. Please log in to access the plant dashboard.');
        return;
      }

      const currentUser = getCurrentUser();
      if (!currentUser) {
        console.warn('No current user found');
        message.error('Unable to identify current user. Please log in again.');
        return;
      }

      console.log('Loading plant data for user:', currentUser);

      // Load user plant assignments and location master data in parallel
      const [plantData, locations] = await Promise.all([
        userAPI.getUserPlantAssignments(currentUser, { signal }),
        masterDataAPI.getAllLocations({ signal })
      ]);

      console.log('User plant data received:', plantData);
      console.log('Assigned plants:', plantData?.assignedPlants);
      console.log('Primary plant:', plantData?.primaryPlant);
      setUserPlantData(plantData);

      // Set current plant to user's primary plant (or first assigned plant if no primary)
      const effectivePlant = plantData.primaryPlant || (plantData.assignedPlants && plantData.assignedPlants[0]);
      if (effectivePlant && plantData.assignedPlants && plantData.assignedPlants.includes(effectivePlant)) {
        setCurrentPlant(effectivePlant);
        localStorage.setItem('userPlant', effectivePlant);
        console.log('Set current plant to primary/effective plant:', effectivePlant);
      } else if (plantData.assignedPlants && plantData.assignedPlants.length > 0) {
        // Fallback to first assigned plant if primary is not available
        const firstPlant = plantData.assignedPlants[0];
        setCurrentPlant(firstPlant);
        localStorage.setItem('userPlant', firstPlant);
        console.log('Set current plant to first assigned plant:', firstPlant);
      }

      // Update available plants to only show assigned plants with descriptions
      if (plantData.assignedPlants && plantData.assignedPlants.length > 0) {
        const plantOptions = plantData.assignedPlants.map(plantCode => {
          const location = locations.find(loc => loc.locationCode === plantCode);
          const isPrimary = plantCode === plantData.primaryPlant;
          return {
            value: plantCode,
            label: location
              ? `${plantCode} - ${location.description}${isPrimary ? ' (Primary)' : ''}`
              : `${plantCode}${isPrimary ? ' (Primary)' : ''}`
          };
        });
        setAvailablePlants(plantOptions);
        console.log('Available plants set:', plantOptions);
      }
    } catch (error) {
      console.error('Failed to load user plant data:', error);
      // Set empty plant data to trigger the "no assignments" message
      setUserPlantData({
        assignedPlants: [],
        primaryPlant: null,
        effectivePlant: null,
        isPlantUser: false
      });
      setAvailablePlants([]);
      setCurrentPlant(null);
      message.error('Unable to load plant assignments. Please contact administrator to assign plants to your user.');
    }
  };



  const handlePlantChange = useCallback((newPlantCode) => {
    // Validate that user is assigned to this plant
    if (userPlantData && !userPlantData.assignedPlants.includes(newPlantCode)) {
      message.error(`You are not assigned to plant: ${newPlantCode}`);
      return;
    }

    localStorage.setItem('userPlant', newPlantCode);
    setCurrentPlant(newPlantCode);
    message.success(`Switched to plant: ${newPlantCode}`);
  }, [userPlantData]);

  const handleStartQuestionnaire = useCallback((workflow) => {
    setSelectedWorkflow(workflow);
    setQuestionnaireVisible(true);
  }, []);

  const handleQuestionnaireComplete = useCallback((_formData) => {
    setQuestionnaireVisible(false);
    setSelectedWorkflow(null);
    refreshWorkflows(); // Refresh the list
    message.success('Questionnaire completed successfully!');
  }, [refreshWorkflows]);

  const handleSaveDraft = useCallback((_formData) => {
    message.success('Draft saved successfully');
  }, []);



  // Check if user is authenticated
  if (!isAuthenticated()) {
    return (
      <PlantOnly>
        <div style={{ padding: 24, textAlign: 'center' }}>
          <Title level={3}>Authentication Required</Title>
          <p>You need to be logged in to access the plant dashboard.</p>
          <Button
            type="primary"
            onClick={() => window.location.href = '/qrmfg/login'}
          >
            Go to Login
          </Button>
        </div>
      </PlantOnly>
    );
  }

  // Show loading state while plant data is being fetched
  if (userPlantData === null) {
    return (
      <PlantOnly>
        <div style={{ padding: 24, textAlign: 'center' }}>
          <Title level={3}>Loading Plant Assignments...</Title>
          <p>Please wait while we load your plant assignments.</p>
        </div>
      </PlantOnly>
    );
  }

  // Show message if user has no plant assignments
  if (userPlantData && (!userPlantData.assignedPlants || userPlantData.assignedPlants.length === 0)) {
    return (
      <PlantOnly>
        <div style={{ padding: 24, textAlign: 'center' }}>
          <Title level={3}>No Plant Assignments</Title>
          <p>You are not assigned to any plants. Please contact your administrator to assign plants to your user account.</p>
          <div style={{ marginTop: 16, padding: 16, background: '#f5f5f5', borderRadius: 4 }}>
            <p><strong>Debug Info:</strong></p>
            <p>Current User: {getCurrentUser()}</p>
            <p>Is Authenticated: {isAuthenticated().toString()}</p>
            <p>User Plant Data: {JSON.stringify(userPlantData, null, 2)}</p>
            <Space>
              <Button
                onClick={() => {
                  console.log('Current user:', getCurrentUser());
                  console.log('User plant data:', userPlantData);
                  console.log('Available plants:', availablePlants);
                  loadUserPlantData();
                }}
              >
                Debug & Retry
              </Button>
              <Button
                type="primary"
                onClick={async () => {
                  try {
                    const currentUser = getCurrentUser();
                    if (!currentUser) {
                      message.error('No current user found');
                      return;
                    }

                    // Try to fix the plant assignments by setting assignedPlants to match primaryPlant
                    const primaryPlant = '1102'; // From the token validation response
                    const assignedPlants = [primaryPlant];

                    await userAPI.updateUserPlantAssignments(currentUser, {
                      assignedPlants,
                      primaryPlant
                    });

                    message.success('Plant assignments fixed! Please refresh the page or log out and log back in.');
                    await loadUserPlantData();
                  } catch (error) {
                    console.error('Failed to assign plants:', error);
                    if (error.status === 403) {
                      message.error('You need admin permissions to assign plants. Please ask an administrator to assign plants to your user account.');
                    } else {
                      message.error(`Failed to assign plants: ${error.message}`);
                    }
                  }
                }}
              >
                Fix Plant Assignments (Admin Required)
              </Button>
              <Button
                onClick={() => {
                  message.info('Logging out to refresh token...');
                  localStorage.clear();
                  sessionStorage.clear();
                  window.location.href = '/qrmfg/login';
                }}
              >
                Logout & Re-login
              </Button>
            </Space>
          </div>
        </div>
      </PlantOnly>
    );
  }

  return (
    <PlantOnly>
      <PlantErrorBoundary plantCode={currentPlant}>
        <div style={{ padding: 24 }}>
          {/* Header */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 24
            }}
          >
            <Title level={2} style={{ margin: 0 }}>
              Plant Dashboard
            </Title>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: '14px', fontWeight: '500' }}>Plant:</span>
                <Select
                  value={currentPlant}
                  onChange={handlePlantChange}
                  style={{ width: 250 }}
                  size="small"
                  disabled={!userPlantData || availablePlants.length <= 1}
                  showSearch
                  filterOption={(input, option) =>
                    option?.children?.toLowerCase().includes(input.toLowerCase())
                  }
                  placeholder="Select assigned plant"
                >
                  {availablePlants.map(plant => (
                    <Option key={plant.value} value={plant.value}>
                      {plant.label}
                    </Option>
                  ))}
                </Select>
              </div>
              <Space>
                <Button onClick={refreshWorkflows} loading={loading}>
                  Refresh Data
                </Button>
              </Space>
            </div>
          </div>

          {/* Dashboard Statistics */}
          {currentPlant ? (
            <PlantDashboard
              dashboardStats={dashboardStats}
              loading={loading}
              error={error}
              currentPlant={currentPlant}
              onRefresh={refreshWorkflows}
            />
          ) : (
            <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
              <Title level={4}>Select a Plant</Title>
              <p>Please select a plant from the dropdown above to view the dashboard and assigned materials.</p>
            </div>
          )}

          {/* Filter Panel and Workflows Table - only show when plant is selected */}
          {currentPlant && (
            <>
              <FilterPanel
                searchText={searchText}
                statusFilter={statusFilter}
                completionFilter={completionFilter}
                onSearchTextChange={updateSearchText}
                onStatusFilterChange={updateStatusFilter}
                onCompletionFilterChange={updateCompletionFilter}
                onClearFilters={clearAllFilters}
                onApplyPreset={applyPreset}
                onSaveFilters={saveFilters}
                filterPresets={filterPresets}
                filterSummary={filterSummary}
              />

              <WorkflowTable
                workflows={filteredWorkflows}
                loading={loading}
                onStartQuestionnaire={handleStartQuestionnaire}
                onRefresh={refreshWorkflows}
              />
            </>
          )}

          {/* Questionnaire Modal */}
          <Modal
            title={`Material Questionnaire - ${selectedWorkflow?.materialCode}`}
            open={questionnaireVisible}
            onCancel={() => {
              setQuestionnaireVisible(false);
              setSelectedWorkflow(null);
            }}
            footer={null}
            width="95%"
            style={{ top: 20 }}
            destroyOnClose
          >
            {selectedWorkflow && (
              <PlantQuestionnaire
                workflowId={selectedWorkflow.id}
                onComplete={handleQuestionnaireComplete}
                onSaveDraft={handleSaveDraft}
              />
            )}
          </Modal>
        </div>
      </PlantErrorBoundary>
    </PlantOnly>
  );
};

export default PlantView;
