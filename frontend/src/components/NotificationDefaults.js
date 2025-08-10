import { SettingOutlined, UserOutlined, TeamOutlined, SaveOutlined, EyeOutlined } from '@ant-design/icons';
import {
  Card,
  Form,
  Switch,
  Button,
  message,
  Divider,
  Typography,
  Space,
  Tabs,
  Row,
  Col,
  Alert,
  Modal,
  Input
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import apiClient from '../api/client';

const { Title, Text } = Typography;

const NotificationDefaults = () => {
  const [generalForm] = Form.useForm();
  const [cqsForm] = Form.useForm();
  const [jvcForm] = Form.useForm();
  const [plantForm] = Form.useForm();
  const [techForm] = Form.useForm();
  
  const [loading, setLoading] = useState(false);
  const [config, setConfig] = useState({
    generalDefaults: {},
    roleBasedDefaults: {}
  });
  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [previewData, setPreviewData] = useState(null);

  const loadConfiguration = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiClient.get('/admin/notification-defaults/config');
      if (data.success) {
        setConfig(data);
        
        // Set form values
        generalForm.setFieldsValue(data.generalDefaults || {});
        cqsForm.setFieldsValue(data.roleBasedDefaults?.CQS || {});
        jvcForm.setFieldsValue(data.roleBasedDefaults?.JVC || {});
        plantForm.setFieldsValue(data.roleBasedDefaults?.PLANT || {});
        techForm.setFieldsValue(data.roleBasedDefaults?.TECH || {});
        
        message.success('Configuration loaded successfully');
      } else {
        message.error(`Failed to load configuration: ${data.message}`);
      }
    } catch (error) {
      console.error('Failed to load notification defaults configuration:', error);
      message.error('Failed to load configuration');
    } finally {
      setLoading(false);
    }
  }, [generalForm, cqsForm, jvcForm, plantForm, techForm]);

  useEffect(() => {
    loadConfiguration();
  }, [loadConfiguration]);

  const saveGeneralDefaults = async (values) => {
    setLoading(true);
    try {
      const data = await apiClient.post('/admin/notification-defaults/update-general-defaults', values);
      if (data.success) {
        message.success('General defaults saved successfully');
        loadConfiguration(); // Reload to get updated config
      } else {
        message.error(`Failed to save general defaults: ${data.message}`);
      }
    } catch (error) {
      console.error('Failed to save general defaults:', error);
      message.error('Failed to save general defaults');
    } finally {
      setLoading(false);
    }
  };

  const saveRoleDefaults = async (role, values) => {
    setLoading(true);
    try {
      const data = await apiClient.post(`/admin/notification-defaults/update-role-defaults?role=${role}`, values);
      if (data.success) {
        message.success(`${role} role defaults saved successfully`);
        loadConfiguration(); // Reload to get updated config
      } else {
        message.error(`Failed to save ${role} defaults: ${data.message}`);
      }
    } catch (error) {
      console.error(`Failed to save ${role} defaults:`, error);
      message.error(`Failed to save ${role} defaults`);
    } finally {
      setLoading(false);
    }
  };

  const previewDefaults = async (role) => {
    try {
      const data = await apiClient.get(`/admin/notification-defaults/preview?role=${role}`);
      if (data.success) {
        setPreviewData({ role, preferences: data.defaultPreferences });
        setPreviewModalVisible(true);
      } else {
        message.error(`Failed to preview defaults: ${data.message}`);
      }
    } catch (error) {
      console.error('Failed to preview defaults:', error);
      message.error('Failed to preview defaults');
    }
  };

  const notificationTypes = [
    {
      key: 'WORKFLOW_CREATED',
      label: 'Workflow Created',
      description: 'When a new MSDS workflow is created'
    },
    {
      key: 'WORKFLOW_COMPLETED',
      label: 'Workflow Completed',
      description: 'When a workflow is completed'
    },
    {
      key: 'WORKFLOW_STATE_CHANGED',
      label: 'Workflow State Changed',
      description: 'When workflow state transitions occur'
    },
    {
      key: 'WORKFLOW_OVERDUE',
      label: 'Workflow Overdue',
      description: 'When workflows become overdue'
    },
    {
      key: 'QUERY_RAISED',
      label: 'Query Raised',
      description: 'When new queries are raised'
    },
    {
      key: 'QUERY_RESOLVED',
      label: 'Query Resolved',
      description: 'When queries are resolved'
    },
    {
      key: 'QUERY_ASSIGNED',
      label: 'Query Assigned',
      description: 'When queries are assigned to teams'
    },
    {
      key: 'QUERY_OVERDUE',
      label: 'Query Overdue',
      description: 'When queries become overdue'
    }
  ];

  const renderNotificationForm = (form, onFinish, title, description) => (
    <Form form={form} layout="vertical" onFinish={onFinish}>
      <Alert
        message={title}
        description={description}
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />
      
      <Row gutter={[16, 16]}>
        {notificationTypes.map(type => (
          <Col xs={24} sm={12} md={8} key={type.key}>
            <Card size="small" style={{ height: '100%' }}>
              <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div style={{ flex: 1 }}>
                  <Text strong style={{ fontSize: '14px' }}>{type.label}</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {type.description}
                  </Text>
                </div>
                <div style={{ marginTop: 12, textAlign: 'center' }}>
                  <Form.Item
                    name={type.key}
                    valuePropName="checked"
                    style={{ margin: 0 }}
                  >
                    <Switch 
                      checkedChildren="ON" 
                      unCheckedChildren="OFF"
                      size="small"
                    />
                  </Form.Item>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>
      
      <div style={{ marginTop: 24, textAlign: 'center' }}>
        <Button 
          type="primary" 
          htmlType="submit" 
          loading={loading}
          icon={<SaveOutlined />}
          size="large"
        >
          Save Defaults
        </Button>
      </div>
    </Form>
  );

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px' }}>
        <Title level={2}>
          <SettingOutlined style={{ marginRight: '8px' }} />
          Default Notification Preferences
        </Title>
        <Text type="secondary">
          Configure which notification types are enabled by default when new users are created.
          Users can later modify their individual preferences.
        </Text>
      </div>

      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button 
            onClick={loadConfiguration}
            loading={loading}
            icon={<SettingOutlined />}
          >
            Reload Configuration
          </Button>
          <Button 
            onClick={() => previewDefaults('CQS')}
            icon={<EyeOutlined />}
          >
            Preview CQS Defaults
          </Button>
          <Button 
            onClick={() => previewDefaults('JVC')}
            icon={<EyeOutlined />}
          >
            Preview JVC Defaults
          </Button>
        </Space>
      </div>

      <Tabs 
        defaultActiveKey="general" 
        type="card"
        items={[
          {
            key: 'general',
            label: (
              <span>
                <UserOutlined />
                General Defaults
              </span>
            ),
            children: renderNotificationForm(
              generalForm,
              saveGeneralDefaults,
              "General Default Preferences",
              "These preferences apply to all new users unless overridden by role-specific settings."
            )
          },
          {
            key: 'cqs',
            label: (
              <span>
                <TeamOutlined />
                CQS Role
              </span>
            ),
            children: renderNotificationForm(
              cqsForm,
              (values) => saveRoleDefaults('CQS', values),
              "CQS Role Default Preferences",
              "Default preferences for users with CQS role. These override general defaults."
            )
          },
          {
            key: 'jvc',
            label: (
              <span>
                <TeamOutlined />
                JVC Role
              </span>
            ),
            children: renderNotificationForm(
              jvcForm,
              (values) => saveRoleDefaults('JVC', values),
              "JVC Role Default Preferences",
              "Default preferences for users with JVC role. These override general defaults."
            )
          },
          {
            key: 'plant',
            label: (
              <span>
                <TeamOutlined />
                Plant Role
              </span>
            ),
            children: renderNotificationForm(
              plantForm,
              (values) => saveRoleDefaults('PLANT', values),
              "Plant Role Default Preferences",
              "Default preferences for users with Plant role. These override general defaults."
            )
          },
          {
            key: 'tech',
            label: (
              <span>
                <TeamOutlined />
                Tech Role
              </span>
            ),
            children: renderNotificationForm(
              techForm,
              (values) => saveRoleDefaults('TECH', values),
              "Tech Role Default Preferences",
              "Default preferences for users with Tech role. These override general defaults."
            )
          }
        ]}
      />

      {/* Preview Modal */}
      <Modal
        title={`Preview Default Preferences - ${previewData?.role} Role`}
        visible={previewModalVisible}
        onCancel={() => setPreviewModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setPreviewModalVisible(false)}>
            Close
          </Button>
        ]}
        width={600}
      >
        {previewData && (
          <div>
            <Alert
              message={`Default preferences for ${previewData.role} role users`}
              description="These are the notification preferences that will be automatically created for new users with this role."
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            
            <Row gutter={[16, 8]}>
              {Object.entries(previewData.preferences).map(([type, enabled]) => (
                <Col xs={24} sm={12} key={type}>
                  <div style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    padding: '8px 12px',
                    backgroundColor: enabled ? '#f6ffed' : '#fff2f0',
                    border: `1px solid ${enabled ? '#b7eb8f' : '#ffccc7'}`,
                    borderRadius: '4px'
                  }}>
                    <Text>{type.replace(/_/g, ' ')}</Text>
                    <Text strong style={{ color: enabled ? '#52c41a' : '#ff4d4f' }}>
                      {enabled ? 'ENABLED' : 'DISABLED'}
                    </Text>
                  </div>
                </Col>
              ))}
            </Row>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default NotificationDefaults;