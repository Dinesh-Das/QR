import { Form, Button, Alert, Spin, Input, Select, InputNumber } from 'antd';
import React, { useState, useEffect } from 'react';

import { handleFormError, getValidationRules } from '../services/errorHandler';
import { useOfflineState } from '../services/offlineHandler';

const ValidatedForm = ({
  form,
  onSubmit,
  onError,
  children,
  submitText = 'Submit',
  resetOnSuccess = false,
  showOfflineWarning = true,
  validateOnChange = true,
  submitButtonProps = {},
  formProps = {},
  className = '',
  ...props
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  // eslint-disable-next-line no-unused-vars
  // eslint-disable-next-line no-unused-vars
  const [_touched, setTouched] = useState({});
  const { isOnline } = useOfflineState();

  // Clear error when form values change
  useEffect(() => {
    if (error && validateOnChange) {
      setError(null);
    }
  }, [form?.getFieldsValue(), error, validateOnChange]);

  const handleSubmit = async values => {
    setLoading(true);
    setError(null);

    try {
      // Client-side validation
      await form.validateFields();

      // Call the submit handler
      const result = await onSubmit(values);

      // Reset form if requested
      if (resetOnSuccess) {
        form.resetFields();
        setTouched({});
      }

      return result;
    } catch (submitError) {
      // Handle form-specific errors
      const errorInfo = handleFormError(submitError, form, {
        showNotification: false, // We'll show it in the form
        context: 'form'
      });

      setError(errorInfo);

      // Call custom error handler if provided
      if (onError) {
        onError(errorInfo, submitError);
      }

      throw submitError;
    } finally {
      setLoading(false);
    }
  };

  const handleFieldChange = (changedFields, _allFields) => {
    // Track which fields have been touched
    changedFields.forEach(field => {
      setTouched(prev => ({
        ...prev,
        [field.name[0]]: true
      }));
    });

    // Clear field-specific errors
    if (error?.fieldErrors) {
      const updatedFieldErrors = { ...error.fieldErrors };
      changedFields.forEach(field => {
        delete updatedFieldErrors[field.name[0]];
      });

      if (Object.keys(updatedFieldErrors).length === 0) {
        setError(null);
      } else {
        setError(prev => ({
          ...prev,
          fieldErrors: updatedFieldErrors
        }));
      }
    }
  };

  const renderError = () => {
    if (!error) {
      return null;
    }

    return (
      <Alert
        type="error"
        message={error.message}
        description={
          error.fieldErrors &&
          Object.keys(error.fieldErrors).length > 0 && (
            <div style={{ marginTop: 8 }}>
              <strong>Field Errors:</strong>
              <ul style={{ margin: '4px 0 0 16px' }}>
                {Object.entries(error.fieldErrors).map(([field, message]) => (
                  <li key={field}>
                    <strong>{field.charAt(0).toUpperCase() + field.slice(1)}:</strong> {message}
                  </li>
                ))}
              </ul>
            </div>
          )
        }
        style={{ marginBottom: 16 }}
        closable
        onClose={() => setError(null)}
      />
    );
  };

  const renderOfflineWarning = () => {
    if (!showOfflineWarning || isOnline) {
      return null;
    }

    return (
      <Alert
        type="warning"
        message="You are offline"
        description="Form submissions will be queued and processed when connection is restored."
        style={{ marginBottom: 16 }}
        showIcon
      />
    );
  };

  return (
    <div className={`validated-form ${className}`} {...props}>
      {renderOfflineWarning()}
      {renderError()}

      <Form
        form={form}
        onFinish={handleSubmit}
        onFieldsChange={handleFieldChange}
        layout="vertical"
        validateTrigger={validateOnChange ? 'onChange' : 'onSubmit'}
        {...formProps}
      >
        {children}

        <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            disabled={!isOnline && !showOfflineWarning}
            {...submitButtonProps}
          >
            {loading ? <Spin size="small" style={{ marginRight: 8 }} /> : null}
            {submitText}
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

// Enhanced Form.Item with built-in validation
export const ValidatedFormItem = ({
  name,
  label,
  required = false,
  maxLength,
  minLength,
  pattern,
  customRules = [],
  validateTrigger = 'onChange',
  children,
  ...props
}) => {
  const rules = [
    ...getValidationRules(label || name, {
      required,
      maxLength,
      minLength,
      pattern
    }),
    ...customRules
  ];

  return (
    <Form.Item name={name} label={label} rules={rules} validateTrigger={validateTrigger} {...props}>
      {children}
    </Form.Item>
  );
};

// Specialized form components for workflow entities
export const WorkflowForm = ({ onSubmit, initialValues, ...props }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [initialValues, form]);

  return (
    <ValidatedForm form={form} onSubmit={onSubmit} submitText="Create Workflow" {...props}>
      <ValidatedFormItem name="materialCode" label="Material Code" required maxLength={100}>
        <Input placeholder="Enter material ID" />
      </ValidatedFormItem>

      <ValidatedFormItem name="materialName" label="Material Name" maxLength={200}>
        <Input placeholder="Enter material name" />
      </ValidatedFormItem>

      <ValidatedFormItem name="materialDescription" label="Material Description" maxLength={1000}>
        <Input.TextArea placeholder="Enter material description" rows={3} />
      </ValidatedFormItem>

      <ValidatedFormItem name="assignedPlant" label="Assigned Plant" required maxLength={100}>
        <Select placeholder="Select plant">
          <Select.Option value="Plant A">Plant A</Select.Option>
          <Select.Option value="Plant B">Plant B</Select.Option>
          <Select.Option value="Plant C">Plant C</Select.Option>
        </Select>
      </ValidatedFormItem>

      <ValidatedFormItem name="safetyDocumentsPath" label="Safety Documents" maxLength={500}>
        <Input placeholder="Enter documents path or URL" />
      </ValidatedFormItem>
    </ValidatedForm>
  );
};

export const QueryForm = ({ onSubmit, initialValues, ...props }) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [initialValues, form]);

  return (
    <ValidatedForm form={form} onSubmit={onSubmit} submitText="Raise Query" {...props}>
      <ValidatedFormItem name="question" label="Question" required maxLength={2000}>
        <Input.TextArea placeholder="Enter your question" rows={4} />
      </ValidatedFormItem>

      <ValidatedFormItem name="stepNumber" label="Step Number">
        <InputNumber placeholder="Enter step number" min={1} style={{ width: '100%' }} />
      </ValidatedFormItem>

      <ValidatedFormItem name="fieldName" label="Field Name" maxLength={100}>
        <Input placeholder="Enter field name" />
      </ValidatedFormItem>

      <ValidatedFormItem name="assignedTeam" label="Assigned Team" required>
        <Select placeholder="Select team">
          <Select.Option value="CQS">CQS Team</Select.Option>
          <Select.Option value="TECH">Tech Team</Select.Option>
          <Select.Option value="JVC">JVC Team</Select.Option>
        </Select>
      </ValidatedFormItem>

      <ValidatedFormItem name="queryCategory" label="Query Category" maxLength={50}>
        <Select placeholder="Select category">
          <Select.Option value="TECHNICAL">Technical</Select.Option>
          <Select.Option value="SAFETY">Safety</Select.Option>
          <Select.Option value="COMPLIANCE">Compliance</Select.Option>
          <Select.Option value="DOCUMENTATION">Documentation</Select.Option>
          <Select.Option value="OTHER">Other</Select.Option>
        </Select>
      </ValidatedFormItem>
    </ValidatedForm>
  );
};

export default ValidatedForm;
