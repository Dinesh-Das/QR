import { Form, Input, Button, Card, Alert, Typography } from 'antd';
import React, { useState } from 'react';

import apiClient from '../api/client';
import SecureForm, { SecureInput, SecureFormItem } from '../components/SecureForm';
import { setToken } from '../services/auth';
import { notifySuccess, notifyError } from '../services/notify';

const { Title } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const onFinish = async values => {
    setLoading(true);
    setError('');
    console.log('Form submitted!', values); // Debug: see if form submits
    try {
      const response = await apiClient.post('/auth/login', values);
      if (response && response.token) {
        setToken(response.token);
        notifySuccess('Login Successful', 'Welcome!');
        window.location.href = '/qrmfg';
      } else {
        notifyError('Login Failed', 'No token returned');
        setError('Login failed: No token returned');
      }
    } catch (err) {
      const errorMessage =
        err.data && err.data.message
          ? err.data.message
          : err.message || 'Invalid username or password';

      notifyError('Login Failed', errorMessage);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        height: '100vh',
        width: '100vw',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '20px',
        margin: 0,
        overflow: 'auto'
      }}
    >
      <Card
        style={{
          width: 400,
          borderRadius: '16px',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
          border: 'none',
          overflow: 'hidden'
        }}
        styles={{ body: { padding: '40px 32px' } }}
      >
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div
            style={{
              width: '64px',
              height: '64px',
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              borderRadius: '16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              fontSize: '24px',
              fontWeight: 'bold',
              color: 'white'
            }}
          >
            QR
          </div>
          <Title
            level={2}
            style={{
              margin: 0,
              color: '#1a1a1a',
              fontWeight: '600',
              fontSize: '28px'
            }}
          >
            QRMFG Portal
          </Title>
          <p style={{ color: '#666', margin: '8px 0 0', fontSize: '14px' }}>
            Welcome back! Please sign in to continue
          </p>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            style={{
              marginBottom: 24,
              borderRadius: '8px',
              border: 'none'
            }}
          />
        )}

        <SecureForm
          layout="vertical"
          onFinish={onFinish}
          autoComplete="off"
          initialValues={{ username: '', password: '' }}
          componentName="LoginForm"
          enableSecurityLogging={true}
        >
          <SecureFormItem
            name="username"
            label={<span style={{ color: '#333', fontWeight: '500' }}>Username</span>}
            validationType="username"
            style={{ marginBottom: '20px' }}
          >
            <SecureInput
              size="large"
              autoFocus
              placeholder="Enter your username"
              autoComplete="username"
              validationType="username"
              componentName="LoginForm"
              fieldName="username"
              style={{
                borderRadius: '8px',
                border: '1px solid #e1e5e9',
                fontSize: '14px'
              }}
            />
          </SecureFormItem>

          <Form.Item
            name="password"
            label={<span style={{ color: '#333', fontWeight: '500' }}>Password</span>}
            rules={[{ required: true, message: 'Please enter your password' }]}
            style={{ marginBottom: '24px' }}
          >
            <Input.Password
              size="large"
              placeholder="Enter your password"
              autoComplete="current-password"
              style={{
                borderRadius: '8px',
                border: '1px solid #e1e5e9',
                fontSize: '14px'
              }}
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              size="large"
              loading={loading}
              style={{
                height: '48px',
                borderRadius: '8px',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                border: 'none',
                fontSize: '16px',
                fontWeight: '500',
                boxShadow: '0 4px 12px rgba(102, 126, 234, 0.4)'
              }}
            >
              Sign In
            </Button>
          </Form.Item>
        </SecureForm>
      </Card>
    </div>
  );
};

export default Login;
