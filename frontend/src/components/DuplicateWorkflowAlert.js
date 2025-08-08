import { ExclamationCircleOutlined, EyeOutlined } from '@ant-design/icons';
import { Alert, Button, Space, Tag, Descriptions } from 'antd';
import React from 'react';

const DuplicateWorkflowAlert = ({ existingWorkflow, _formData, onViewExisting, onClose }) => {
  const formatDate = dateString => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStateColor = state => {
    const stateColors = {
      JVC_PENDING: 'orange',
      PLANT_PENDING: 'blue',
      CQS_PENDING: 'purple',
      TECH_PENDING: 'cyan',
      COMPLETED: 'green'
    };
    return stateColors[state] || 'default';
  };

  return (
    <Alert
      message="Duplicate Workflow Detected"
      description={
        <div style={{ marginTop: 12 }}>
          <p style={{ marginBottom: 16 }}>
            A workflow already exists for this combination. Here are the details:
          </p>

          <Descriptions size="small" column={2} bordered>
            <Descriptions.Item label="Workflow ID">#{existingWorkflow.id}</Descriptions.Item>
            <Descriptions.Item label="Current State">
              <Tag color={getStateColor(existingWorkflow.state)}>
                {existingWorkflow.state.replace('_', ' ')}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Project Code">
              {existingWorkflow.projectCode}
            </Descriptions.Item>
            <Descriptions.Item label="Material Code">
              {existingWorkflow.materialCode}
            </Descriptions.Item>
            <Descriptions.Item label="Plant Code">{existingWorkflow.plantCode}</Descriptions.Item>
            <Descriptions.Item label="Created">
              {formatDate(existingWorkflow.createdAt)}
            </Descriptions.Item>
            <Descriptions.Item label="Initiated By">
              {existingWorkflow.initiatedBy}
            </Descriptions.Item>
            {existingWorkflow.documentCount > 0 && (
              <Descriptions.Item label="Documents">
                {existingWorkflow.documentCount} attached
              </Descriptions.Item>
            )}
            {existingWorkflow.openQueriesCount > 0 && (
              <Descriptions.Item label="Open Queries">
                <Tag color="red">{existingWorkflow.openQueriesCount}</Tag>
              </Descriptions.Item>
            )}
          </Descriptions>

          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Space>
              <Button
                type="primary"
                icon={<EyeOutlined />}
                onClick={() => onViewExisting(existingWorkflow)}
              >
                View Existing Workflow
              </Button>
              <Button onClick={onClose}>Close</Button>
            </Space>
          </div>
        </div>
      }
      type="warning"
      icon={<ExclamationCircleOutlined />}
      showIcon
      closable
      onClose={onClose}
      style={{ marginBottom: 16 }}
    />
  );
};

export default DuplicateWorkflowAlert;
