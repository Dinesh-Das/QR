import { ReloadOutlined } from '@ant-design/icons';
import { Form, Select, Button, Divider, Typography, Space, Alert } from 'antd';
import PropTypes from 'prop-types';
import React, { useCallback, useMemo, useEffect } from 'react';

import { usePlantAssignment } from '../../hooks/usePlantAssignment';

const { Text } = Typography;

const PlantAssignmentForm = React.memo(({ 
  selectedRoles, 
  roles, 
  form,
  initialValues = {}
}) => {
  const {
    availablePlants,
    plantsLoading,
    error,
    fetchPlants,
    filterPlants,
    validatePlantAssignments
  } = usePlantAssignment();

  // Check if selected roles include plant user role
  const isPlantUserSelected = useMemo(() => {
    if (!selectedRoles || !roles) return false;
    
    const plantRoleIds = roles
      .filter(role => role.name === 'ROLE_PLANT_USER' || role.name === 'PLANT_USER')
      .map(role => role.id);

    return selectedRoles.some(roleId => plantRoleIds.includes(roleId));
  }, [selectedRoles, roles]);

  // Handle refresh plants button click
  const handleRefreshPlants = useCallback(async () => {
    const controller = new AbortController();
    await fetchPlants(controller.signal);
  }, [fetchPlants]);

  // Custom validator for assigned plants
  const validateAssignedPlants = useCallback((_, value) => {
    if (!isPlantUserSelected) {
      return Promise.resolve();
    }

    if (!value || value.length === 0) {
      return Promise.reject(new Error('Please select at least one plant for plant users!'));
    }

    return Promise.resolve();
  }, [isPlantUserSelected]);

  // Custom validator for primary plant
  const validatePrimaryPlant = useCallback((_, value) => {
    if (!isPlantUserSelected) {
      return Promise.resolve();
    }

    const assignedPlants = form.getFieldValue('assignedPlants') || [];
    
    if (value && !assignedPlants.includes(value)) {
      return Promise.reject(new Error('Primary plant must be one of the assigned plants!'));
    }

    return Promise.resolve();
  }, [isPlantUserSelected, form]);

  // Handle assigned plants change to update primary plant options
  const handleAssignedPlantsChange = useCallback((values) => {
    const currentPrimaryPlant = form.getFieldValue('primaryPlant');
    
    // If current primary plant is not in the new assigned plants, clear it
    if (currentPrimaryPlant && !values.includes(currentPrimaryPlant)) {
      form.setFieldValue('primaryPlant', undefined);
    }
  }, [form]);

  // Get primary plant options based on assigned plants
  const primaryPlantOptions = useMemo(() => {
    const assignedPlants = form.getFieldValue('assignedPlants') || [];
    return availablePlants.filter(plant => assignedPlants.includes(plant.value));
  }, [availablePlants, form]);

  // Set initial values when component mounts or initialValues change
  useEffect(() => {
    if (initialValues.assignedPlants || initialValues.primaryPlant) {
      form.setFieldsValue({
        assignedPlants: initialValues.assignedPlants || [],
        primaryPlant: initialValues.primaryPlant || null
      });
    }
  }, [initialValues, form]);

  // Don't render if plant user role is not selected
  if (!isPlantUserSelected) {
    return null;
  }

  return (
    <>
      <Divider />
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8
        }}
      >
        <Text strong style={{ color: '#1890ff' }}>
          Plant Assignments
        </Text>
        <Button 
          size="small" 
          icon={<ReloadOutlined />}
          onClick={handleRefreshPlants} 
          loading={plantsLoading}
          aria-label="Refresh plants list"
          data-testid="refresh-plants-button"
        >
          Refresh Plants
        </Button>
      </div>

      <Text
        type="secondary"
        style={{ display: 'block', marginBottom: 16, fontSize: '12px' }}
      >
        Plant users need to be assigned to specific plants to access plant-specific data.
        Plants are loaded from Location Master.
      </Text>

      {error && (
        <Alert
          message="Plant Loading Error"
          description={`Failed to load plants: ${error}. Using fallback data.`}
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          data-testid="plant-error-alert"
        />
      )}

      <Form.Item
        name="assignedPlants"
        label="Assigned Plants"
        rules={[
          { validator: validateAssignedPlants }
        ]}
        hasFeedback
        tooltip="Select all plants this user should have access to"
      >
        <Select
          mode="multiple"
          placeholder="Select plants this user can access"
          loading={plantsLoading}
          options={availablePlants}
          showSearch
          filterOption={filterPlants}
          onChange={handleAssignedPlantsChange}
          aria-label="Assigned plants selection"
          data-testid="assigned-plants-select"
          maxTagCount="responsive"
          allowClear
        />
      </Form.Item>

      <Form.Item
        name="primaryPlant"
        label="Primary Plant"
        rules={[
          { validator: validatePrimaryPlant }
        ]}
        hasFeedback
        tooltip="The default plant that will be selected when the user logs in"
      >
        <Select
          placeholder="Select primary plant (optional)"
          allowClear
          loading={plantsLoading}
          options={primaryPlantOptions}
          showSearch
          filterOption={filterPlants}
          aria-label="Primary plant selection"
          data-testid="primary-plant-select"
          disabled={!form.getFieldValue('assignedPlants')?.length}
        />
      </Form.Item>

      {/* Helper text for primary plant */}
      {form.getFieldValue('assignedPlants')?.length > 0 && (
        <div style={{ marginTop: -16, marginBottom: 16 }}>
          <Text type="secondary" style={{ fontSize: '11px' }}>
            Primary plant will be auto-selected from assigned plants if not specified
          </Text>
        </div>
      )}
    </>
  );
});

PlantAssignmentForm.displayName = 'PlantAssignmentForm';

PlantAssignmentForm.propTypes = {
  selectedRoles: PropTypes.arrayOf(PropTypes.string),
  roles: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired
  })).isRequired,
  form: PropTypes.object.isRequired,
  initialValues: PropTypes.shape({
    assignedPlants: PropTypes.arrayOf(PropTypes.string),
    primaryPlant: PropTypes.string
  })
};

PlantAssignmentForm.defaultProps = {
  selectedRoles: [],
  initialValues: {}
};

export default PlantAssignmentForm;