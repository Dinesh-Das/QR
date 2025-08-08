import {
  InfoCircleOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  ExperimentOutlined
} from '@ant-design/icons';
import { Card, Descriptions, Tag, Spin, Alert, Collapse, Typography, Space, Button } from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import apiClient from '../api/client';

const { Panel } = Collapse;
const { Text } = Typography;

const MaterialContextDisplay = ({ materialCode, workflowId, compact = false }) => {
  const [loading, setLoading] = useState(false);
  const [materialData, setMaterialData] = useState(null);
  const [workflowData, setWorkflowData] = useState(null);
  const [error, setError] = useState(null);

  const loadMaterialContext = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const promises = [];

      if (workflowId) {
        promises.push(
          apiClient.get(`/workflows/${workflowId}`).then(data => setWorkflowData(data))
        );
      }

      if (materialCode) {
        // In a real implementation, this would fetch material details from a materials API
        promises.push(
          Promise.resolve({
            id: materialCode,
            name: `Material ${materialCode}`,
            description: 'Sample material description',
            category: 'Chemical',
            supplier: 'Sample Supplier',
            hazardClass: 'Class 3',
            physicalState: 'Liquid',
            flashPoint: '25°C',
            autoIgnitionTemp: '300°C',
            density: '0.85 g/cm³',
            solubility: 'Soluble in water',
            ph: '7.2',
            viscosity: '2.5 cP'
          }).then(data => setMaterialData(data))
        );
      }

      await Promise.all(promises);
    } catch (error) {
      console.error('Failed to load material context:', error);
      setError('Failed to load material information');
    } finally {
      setLoading(false);
    }
  }, [workflowId, materialCode]);

  useEffect(() => {
    if (materialCode || workflowId) {
      loadMaterialContext();
    }
  }, [materialCode, workflowId, loadMaterialContext]);

  const getWorkflowStateColor = state => {
    const colors = {
      JVC_PENDING: 'blue',
      PLANT_PENDING: 'orange',
      CQS_PENDING: 'red',
      TECH_PENDING: 'purple',
      COMPLETED: 'green'
    };
    return colors[state] || 'default';
  };

  const getHazardClassColor = hazardClass => {
    const colors = {
      'Class 1': 'red',
      'Class 2': 'orange',
      'Class 3': 'yellow',
      'Class 4': 'blue',
      'Class 5': 'green',
      'Class 6': 'purple',
      'Class 7': 'magenta',
      'Class 8': 'cyan',
      'Class 9': 'lime'
    };
    return colors[hazardClass] || 'default';
  };

  if (loading) {
    return (
      <Card size="small">
        <div style={{ textAlign: 'center', padding: '20px' }}>
          <Spin size="small" />
          <div style={{ marginTop: 8 }}>Loading material context...</div>
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Alert message="Context Error" description={error} type="warning" showIcon size="small" />
    );
  }

  if (compact) {
    return (
      <Card size="small" title="Material Context" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          {materialData && (
            <div>
              <Text strong>Material:</Text> {materialData.name} ({materialData.id})
              <br />
              <Text strong>Category:</Text> {materialData.category}
              <br />
              <Text strong>Hazard Class:</Text>{' '}
              <Tag color={getHazardClassColor(materialData.hazardClass)}>
                {materialData.hazardClass}
              </Tag>
            </div>
          )}

          {workflowData && (
            <div>
              <Text strong>Workflow State:</Text>{' '}
              <Tag color={getWorkflowStateColor(workflowData.state)}>
                {workflowData.state?.replace('_', ' ')}
              </Tag>
              <br />
              <Text strong>Plant:</Text> {workflowData.assignedPlant}
            </div>
          )}
        </Space>
      </Card>
    );
  }

  return (
    <Card
      title={
        <Space>
          <InfoCircleOutlined />
          <span>Material Context</span>
        </Space>
      }
      size="small"
      style={{ marginBottom: 16 }}
    >
      <Collapse size="small" ghost>
        {materialData && (
          <Panel
            header={
              <Space>
                <ExperimentOutlined />
                <span>Material Properties</span>
              </Space>
            }
            key="material"
          >
            <Descriptions size="small" column={2} bordered>
              <Descriptions.Item label="Material ID">{materialData.id}</Descriptions.Item>
              <Descriptions.Item label="Name">{materialData.name}</Descriptions.Item>
              <Descriptions.Item label="Category">
                <Tag color="blue">{materialData.category}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Supplier">{materialData.supplier}</Descriptions.Item>
              <Descriptions.Item label="Hazard Class">
                <Tag color={getHazardClassColor(materialData.hazardClass)}>
                  {materialData.hazardClass}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Physical State">
                {materialData.physicalState}
              </Descriptions.Item>
              <Descriptions.Item label="Flash Point">
                <Text type="danger">{materialData.flashPoint}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Auto-Ignition Temp">
                <Text type="danger">{materialData.autoIgnitionTemp}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Density">{materialData.density}</Descriptions.Item>
              <Descriptions.Item label="Solubility">{materialData.solubility}</Descriptions.Item>
              <Descriptions.Item label="pH">{materialData.ph}</Descriptions.Item>
              <Descriptions.Item label="Viscosity">{materialData.viscosity}</Descriptions.Item>
            </Descriptions>

            {materialData.description && (
              <div style={{ marginTop: 12 }}>
                <Text strong>Description:</Text>
                <div
                  style={{
                    marginTop: 4,
                    padding: 8,
                    background: '#f5f5f5',
                    borderRadius: 4
                  }}
                >
                  {materialData.description}
                </div>
              </div>
            )}
          </Panel>
        )}

        {workflowData && (
          <Panel
            header={
              <Space>
                <FileTextOutlined />
                <span>Workflow Information</span>
              </Space>
            }
            key="workflow"
          >
            <Descriptions size="small" column={2} bordered>
              <Descriptions.Item label="Workflow ID">{workflowData.id}</Descriptions.Item>
              <Descriptions.Item label="Current State">
                <Tag color={getWorkflowStateColor(workflowData.state)}>
                  {workflowData.state?.replace('_', ' ')}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Assigned Plant">
                {workflowData.assignedPlant}
              </Descriptions.Item>
              <Descriptions.Item label="Initiated By">{workflowData.initiatedBy}</Descriptions.Item>
              <Descriptions.Item label="Created">
                {workflowData.createdAt && new Date(workflowData.createdAt).toLocaleString()}
              </Descriptions.Item>
              <Descriptions.Item label="Last Modified">
                {workflowData.lastModified && new Date(workflowData.lastModified).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
          </Panel>
        )}

        <Panel
          header={
            <Space>
              <SafetyCertificateOutlined />
              <span>Safety Guidelines</span>
            </Space>
          }
          key="safety"
        >
          <Alert
            message="Safety Reminders"
            description={
              <ul style={{ marginBottom: 0, paddingLeft: 20 }}>
                <li>Always verify material properties before providing guidance</li>
                <li>Consider plant-specific safety protocols and equipment</li>
                <li>Reference latest SDS (Safety Data Sheet) when available</li>
                <li>Escalate to safety team for Class 1-2 hazardous materials</li>
                <li>Ensure compliance with local and international regulations</li>
              </ul>
            }
            type="info"
            showIcon
          />
        </Panel>
      </Collapse>

      <div style={{ marginTop: 12, textAlign: 'right' }}>
        <Button size="small" icon={<InfoCircleOutlined />} onClick={loadMaterialContext}>
          Refresh Context
        </Button>
      </div>
    </Card>
  );
};

export default MaterialContextDisplay;
