import { MailOutlined, BellOutlined, SettingOutlined } from '@ant-design/icons';
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
  Alert,
  Input
} from 'antd';
import React, { useState, useEffect } from 'react';

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
        const username = getCurrentUser();
        if (!username) {
          console.error('No authenticated user found');
          message.error('Please log in to view notification preferences');
          return;
        }
        
        const data = await apiClient.get(`/users/${username}/notification-preferences`, {
          signal: controller.signal
        });
        if (!controller.signal.aborted) {
          console.log('API Response:', data);
          console.log('Email preferences from API:', data.email);
          setPreferences(data);
          
          // Reset form and set new values
          form.resetFields();
          form.setFieldsValue(data);
          
          console.log('Form values after setting:', form.getFieldsValue());
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
  }, [form]); // Add form to dependency array



  const savePreferences = async values => {
    setLoading(true);
    try {
      const username = getCurrentUser();
      if (!username) {
        console.error('No authenticated user found');
        message.error('Please log in to save notification preferences');
        return;
      }
      
      await apiClient.put(`/users/${username}/notification-preferences`, values);
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

      <Form form={form} layout="vertical" onFinish={handleFormSubmit}>
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
            <Input
              type="email"
              // placeholder="your.email@company.com"
            />
          </Form.Item>

          <Divider orientation="left">Email Notification Types</Divider>

          {notificationTypes.map(type => (
            <div key={type.key} style={{ marginBottom: '16px' }}>
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
                <Form.Item
                  name={['email', type.key]}
                  valuePropName="checked"
                  style={{ margin: 0 }}
                >
                  <Switch size="small" />
                </Form.Item>
              </div>
            </div>
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
              <Input type="time" style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name={['general', 'quietHours', 'end']} label="End Time" style={{ flex: 1 }}>
              <Input type="time" style={{ width: '100%' }} />
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
