import { Card, Descriptions, Tag } from 'antd';
import React from 'react';

import { getCurrentUser, getUserRole, isAuthenticated } from '../services/auth';

const DebugInfo = () => {
  const currentUser = getCurrentUser();
  const userRole = getUserRole();
  const isAuth = isAuthenticated();

  if (process.env.REACT_APP_DEBUG !== 'true') {
    return null;
  }

  return (
    <Card title="Debug Information" size="small" style={{ marginBottom: 16 }}>
      <Descriptions size="small" column={1}>
        <Descriptions.Item label="Authenticated">
          <Tag color={isAuth ? 'green' : 'red'}>{isAuth ? 'Yes' : 'No'}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Current User">{currentUser || 'Not available'}</Descriptions.Item>
        <Descriptions.Item label="User Role">{userRole || 'Not available'}</Descriptions.Item>
        <Descriptions.Item label="Mock Data">
          <Tag color={process.env.REACT_APP_USE_MOCK_DATA === 'true' ? 'blue' : 'default'}>
            {process.env.REACT_APP_USE_MOCK_DATA === 'true' ? 'Enabled' : 'Disabled'}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="API Base URL">
          {process.env.REACT_APP_API_BASE_URL || 'Default'}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
};

export default DebugInfo;
