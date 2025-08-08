import { MailOutlined, SlackOutlined, BellOutlined, SettingOutlined } from '@ant-design/icons';
import {
  Card,
  Form,
  Switch,
  Select,
  Button,
  message,
  Divider,
  Typography,
  Space,
  Alert
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import apiClient from '../api/client';
import { getCurrentUser } from '../services/auth';

const { Title, Text } = Typography;
const { Option } = Select;

const NotificationPreferences = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [preferences, setPreferences] = useState({
    email: {
      enabled: true,
      address: '',
      workflowCreated: true,
      workflowExtended: true,
      workflowCompleted: true,
      workflowStateChanged: true,
      workflowOverdue: true,
      queryRaised: true,
      queryResolved: true,
      queryAssigned: true,
      queryOverdue: true
    },
    slack: {
      enabled: false,
      userId: '',
      workflowCreated: false,
      workflowExtended: true,
      workflowCompleted: true,
      workflowStateChanged: false,
      workflowOverdue: true,
      queryRaised: true,
      queryResolved: true,
      queryAssigned: true,
      queryOverdue: true
    },
    general: {
      frequency: 'immediate', // immediate, daily, weekly
      quietHours: {
        enabled: false,
        start: '18:00',
        end: '08:00'
      }
    }
  });

  useEffect(() => {
    const controller = new AbortController();

    const loadPreferencesWithAbort = async () => {
      setLoading(true);
      try {
        const user = getCurrentUser();
        const data = await apiClient.get(`/api/users/${user.username}/notification-preferences`, {
          signal: controller.signal
        });
        if (!controller.signal.aborted) {
          setPreferences(data);
          form.setFieldsValue(data);
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Failed to load notification preferences:', error);
          message.error('Failed to load notification preferences');
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    loadPreferencesWithAbort();

    return () => {
      controller.abort();
    };
  }, []); // Remove loadPreferences dependency

  const _loadPreferences = useCallback(
    async signal => {
      setLoading(true);
      try {
        const user = getCurrentUser();
        const data = await apiClient.get(`/api/users/${user.username}/notification-preferences`, {
          signal
        });
        if (!signal?.aborted) {
          setPreferences(data);
          form.setFieldsValue(data);
        }
      } catch (error) {
        if (!signal?.aborted) {
          console.error('Failed to load notification preferences:', error);
          message.error('Failed to load notification preferences');
        }
      } finally {
        if (!signal?.aborted) {
          setLoading(false);
        }
      }
    },
    [form]
  );

  const savePreferences = async values => {
    setLoading(true);
    try {
      const user = getCurrentUser();
      await apiClient.put(`/api/users/${user.username}/notification-preferences`, values);
      setPreferences(values);
      message.success('Notification preferences saved successfully');
    } catch (error) {
      console.error('Failed to save notification preferences:', error);
      message.error('Failed to save notification preferences');
    } finally {
      setLoading(false);
    }
  };

  const handleFormSubmit = values => {
    savePreferences(values);
  };

  const notificationTypes = [
    {
      key: 'workflowCreated',
      label: 'Workflow Created',
      description: 'When a new MSDS workflow is created'
    },
    {
      key: 'workflowExtended',
      label: 'Workflow Extended',
      description: 'When a workflow is extended to your team'
    },
    {
      key: 'workflowCompleted',
      label: 'Workflow Completed',
      description: 'When a workflow is completed'
    },
    {
      key: 'workflowStateChanged',
      label: 'Workflow State Changed',
      description: 'When workflow state transitions occur'
    },
    {
      key: 'workflowOverdue',
      label: 'Workflow Overdue',
      description: 'When workflows become overdue'
    },
    { key: 'queryRaised', label: 'Query Raised', description: 'When new queries are raised' },
    { key: 'queryResolved', label: 'Query Resolved', description: 'When queries are resolved' },
    {
      key: 'queryAssigned',
      label: 'Query Assigned',
      description: 'When queries are assigned to your team'
    },
    { key: 'queryOverdue', label: 'Query Overdue', description: 'When queries become overdue' }
  ];

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ marginBottom: '24px' }}>
        <Title level={2}>
          <BellOutlined style={{ marginRight: '8px' }} />
          Notification Preferences
        </Title>
        <Text type="secondary">
          Configure how and when you receive notifications about MSDS workflow events.
        </Text>
      </div>

      <Form form={form} layout="vertical" onFinish={handleFormSubmit} initialValues={preferences}>
        {/* Email Notifications */}
        <Card
          title={
            <Space>
              <MailOutlined />
              Email Notifications
            </Space>
          }
          style={{ marginBottom: '16px' }}
        >
          <Form.Item name={['email', 'enabled']} valuePropName="checked">
            <Switch checkedChildren="Enabled" unCheckedChildren="Disabled" />
          </Form.Item>

          <Form.Item
            name={['email', 'address']}
            label="Email Address"
            rules={[{ type: 'email', message: 'Please enter a valid email address' }]}
          >
            <input
              type="email"
              placeholder="your.email@company.com"
              style={{
                width: '100%',
                padding: '8px',
                border: '1px solid #d9d9d9',
                borderRadius: '4px'
              }}
            />
          </Form.Item>

          <Divider orientation="left">Email Notification Types</Divider>

          {notificationTypes.map(type => (
            <Form.Item
              key={type.key}
              name={['email', type.key]}
              valuePropName="checked"
              style={{ marginBottom: '8px' }}
            >
              <div
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
              >
                <div>
                  <Text strong>{type.label}</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {type.description}
                  </Text>
                </div>
                <Switch size="small" />
              </div>
            </Form.Item>
          ))}
        </Card>

        {/* Slack Notifications */}
        <Card
          title={
            <Space>
              <SlackOutlined />
              Slack Notifications
            </Space>
          }
          style={{ marginBottom: '16px' }}
        >
          <Alert
            message="Slack Integration"
            description="Slack notifications require additional setup by your system administrator."
            type="info"
            showIcon
            style={{ marginBottom: '16px' }}
          />

          <Form.Item name={['slack', 'enabled']} valuePropName="checked">
            <Switch checkedChildren="Enabled" unCheckedChildren="Disabled" />
          </Form.Item>

          <Form.Item
            name={['slack', 'userId']}
            label="Slack User ID"
            help="Your Slack user ID (e.g., @username or U1234567890)"
          >
            <input
              type="text"
              placeholder="@username or U1234567890"
              style={{
                width: '100%',
                padding: '8px',
                border: '1px solid #d9d9d9',
                borderRadius: '4px'
              }}
            />
          </Form.Item>

          <Divider orientation="left">Slack Notification Types</Divider>

          {notificationTypes.map(type => (
            <Form.Item
              key={type.key}
              name={['slack', type.key]}
              valuePropName="checked"
              style={{ marginBottom: '8px' }}
            >
              <div
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
              >
                <div>
                  <Text strong>{type.label}</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {type.description}
                  </Text>
                </div>
                <Switch size="small" />
              </div>
            </Form.Item>
          ))}
        </Card>

        {/* General Settings */}
        <Card
          title={
            <Space>
              <SettingOutlined />
              General Settings
            </Space>
          }
          style={{ marginBottom: '24px' }}
        >
          <Form.Item
            name={['general', 'frequency']}
            label="Notification Frequency"
            help="How often you want to receive notifications"
          >
            <Select>
              <Option value="immediate">Immediate</Option>
              <Option value="daily">Daily Digest</Option>
              <Option value="weekly">Weekly Summary</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name={['general', 'quietHours', 'enabled']}
            valuePropName="checked"
            label="Quiet Hours"
            help="Disable notifications during specified hours"
          >
            <Switch />
          </Form.Item>

          <div style={{ display: 'flex', gap: '16px' }}>
            <Form.Item
              name={['general', 'quietHours', 'start']}
              label="Start Time"
              style={{ flex: 1 }}
            >
              <input
                type="time"
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #d9d9d9',
                  borderRadius: '4px'
                }}
              />
            </Form.Item>
            <Form.Item name={['general', 'quietHours', 'end']} label="End Time" style={{ flex: 1 }}>
              <input
                type="time"
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #d9d9d9',
                  borderRadius: '4px'
                }}
              />
            </Form.Item>
          </div>
        </Card>

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} size="large">
            Save Preferences
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

export default NotificationPreferences;
