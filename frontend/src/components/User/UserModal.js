import { Modal, Form, Input, Select } from 'antd';
import PropTypes from 'prop-types';
import React, { useCallback, useEffect, useMemo, useState } from 'react';

import { userAPI } from '../../services/userAPI';
import { ValidationRules } from '../../utils/inputValidation';
import SecureForm, { SecureInput, SecureFormItem } from '../SecureForm';

import PlantAssignmentForm from './PlantAssignmentForm';

/**
 * UserModal Component
 * 
 * A modal dialog for creating and editing user information including roles and plant assignments.
 * Supports both create and edit modes with form validation and error handling.
 * 
 * @component
 * @example
 * ```jsx
 * const roles = [
 *   { value: 'USER', label: 'User' },
 *   { value: 'ADMIN', label: 'Administrator' }
 * ];
 * 
 * <UserModal
 *   visible={true}
 *   editingUser={{ id: '1', username: 'john', email: 'john@example.com' }}
 *   roles={roles}
 *   onSave={(userData) => console.log('Save user:', userData)}
 *   onCancel={() => console.log('Cancel edit')}
 *   loading={false}
 * />
 * ```
 * 
 * @param {Object} props - Component props
 * @param {boolean} props.visible - Whether the modal is visible
 * @param {Object|null} props.editingUser - User object being edited (null for create mode)
 * @param {string} props.editingUser.id - User ID
 * @param {string} props.editingUser.username - Username
 * @param {string} props.editingUser.email - Email address
 * @param {Array<string>} props.editingUser.roles - User roles
 * @param {Array<Object>} props.roles - Available roles for selection
 * @param {string} props.roles[].value - Role value
 * @param {string} props.roles[].label - Role display name
 * @param {Function} props.onSave - Callback when user data is saved
 * @param {Function} props.onCancel - Callback when modal is cancelled
 * @param {boolean} [props.loading=false] - Whether save operation is in progress
 * 
 * @returns {React.ReactElement} Rendered UserModal component
 * 
 * @since 1.0.0
 * @author QRMFG Development Team
 */
const UserModal = React.memo(({ 
  visible, 
  editingUser, 
  roles, 
  onSave, 
  onCancel,
  loading = false 
}) => {
  const [form] = Form.useForm();
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [plantAssignmentData, setPlantAssignmentData] = useState({});

  // Determine if this is edit or create mode
  const isEditMode = useMemo(() => Boolean(editingUser), [editingUser]);

  // Modal title based on mode
  const modalTitle = useMemo(() => 
    isEditMode ? 'Edit User' : 'Add User', 
    [isEditMode]
  );

  // Form validation rules with enhanced security
  const validationRules = useMemo(() => ({
    username: ValidationRules.username,
    email: ValidationRules.email,
    password: isEditMode ? [] : ValidationRules.password,
    roles: [
      { required: true, message: 'Please select at least one role!' },
      { type: 'array', min: 1, message: 'Please select at least one role!' }
    ]
  }), [isEditMode]);

  // Role options for select
  const roleOptions = useMemo(() => 
    roles.map(role => ({
      label: role.name,
      value: role.id,
      key: role.id
    })), 
    [roles]
  );

  // Handle form submission
  const handleSubmit = useCallback(async (values) => {
    try {
      await onSave(values, editingUser);
      form.resetFields();
    } catch (error) {
      console.error('Error saving user:', error);
      // Error message is handled in the parent component
    }
  }, [onSave, editingUser, form]);

  // Handle modal cancel
  const handleCancel = useCallback(() => {
    form.resetFields();
    onCancel();
  }, [form, onCancel]);

  // Handle modal OK button
  const handleOk = useCallback(() => {
    form.submit();
  }, [form]);

  // Load plant assignment data for editing user
  const loadPlantAssignmentData = useCallback(async (username) => {
    try {
      const plantData = await userAPI.getUserPlantAssignments(username);
      setPlantAssignmentData(plantData || {});
      return plantData;
    } catch (error) {
      console.warn('Could not load plant assignments for user:', error);
      setPlantAssignmentData({});
      return {};
    }
  }, []);

  // Reset form when modal opens/closes or editing user changes
  useEffect(() => {
    if (visible && editingUser) {
      const userRoles = (editingUser.roles || []).map(r => r?.id).filter(Boolean);
      setSelectedRoles(userRoles);
      
      // Populate form with editing user data
      form.setFieldsValue({
        username: editingUser.username,
        email: editingUser.email,
        roles: userRoles
      });

      // Load plant assignments for editing user
      loadPlantAssignmentData(editingUser.username);
    } else if (visible && !editingUser) {
      // Reset form for new user
      setSelectedRoles([]);
      setPlantAssignmentData({});
      form.resetFields();
    }
  }, [visible, editingUser, form, loadPlantAssignmentData]);

  // Form layout configuration
  const formLayout = {
    labelCol: { span: 6 },
    wrapperCol: { span: 18 }
  };

  return (
    <Modal
      title={modalTitle}
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      destroyOnClose
      width={600}
      maskClosable={false}
      aria-labelledby="user-modal-title"
      data-testid="user-modal"
    >
      <SecureForm
        form={form}
        {...formLayout}
        onFinish={handleSubmit}
        preserve={false}
        aria-label={`${modalTitle} form`}
        data-testid="user-form"
        componentName="UserModal"
        enableSecurityLogging={true}
      >
        <SecureFormItem
          name="username"
          label="Username"
          validationType="username"
          hasFeedback
        >
          <SecureInput
            placeholder="Enter username"
            disabled={isEditMode} // Username cannot be changed in edit mode
            aria-label="Username input"
            data-testid="username-input"
            validationType="username"
            componentName="UserModal"
            fieldName="username"
          />
        </SecureFormItem>

        <SecureFormItem
          name="email"
          label="Email"
          validationType="email"
          hasFeedback
        >
          <SecureInput
            placeholder="Enter email address"
            aria-label="Email input"
            data-testid="email-input"
            validationType="email"
            componentName="UserModal"
            fieldName="email"
          />
        </SecureFormItem>

        {!isEditMode && (
          <SecureFormItem
            name="password"
            label="Password"
            validationType="password"
            hasFeedback
          >
            <Input.Password
              placeholder="Enter password"
              aria-label="Password input"
              data-testid="password-input"
            />
          </SecureFormItem>
        )}

        <Form.Item
          name="roles"
          label="Roles"
          rules={validationRules.roles}
          hasFeedback
        >
          <Select
            mode="multiple"
            placeholder="Select user roles"
            options={roleOptions}
            aria-label="Roles selection"
            data-testid="roles-select"
            showSearch
            filterOption={(input, option) =>
              option?.label?.toLowerCase().includes(input.toLowerCase())
            }
            maxTagCount="responsive"
            onChange={setSelectedRoles}
          />
        </Form.Item>

        {/* Plant Assignment Section */}
        <PlantAssignmentForm
          selectedRoles={selectedRoles}
          roles={roles}
          form={form}
          initialValues={plantAssignmentData}
        />
      </SecureForm>
    </Modal>
  );
});

UserModal.displayName = 'UserModal';

UserModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  editingUser: PropTypes.shape({
    id: PropTypes.string.isRequired,
    username: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    roles: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.string,
      name: PropTypes.string
    }))
  }),
  roles: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired
  })).isRequired,
  onSave: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  loading: PropTypes.bool
};

UserModal.defaultProps = {
  editingUser: null,
  loading: false
};

export default UserModal;