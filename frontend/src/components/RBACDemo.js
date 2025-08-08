import React, { useState } from 'react';
import { Card, Button, Space, Typography, Divider, Tag, Alert, Spin } from 'antd';

import RoleBasedComponent, {
  AdminOnly,
  JvcOnly,
  CqsOnly,
  TechOnly,
  PlantOnly,
  NonViewerOnly,
  useRoleAccess
} from './RoleBasedComponent';
import { useRoleBasedAccess, useScreenAccess, useDataAccess } from '../hooks/useRoleBasedAccess';
import { ROLES, getRoleDisplayName } from '../constants/roles';

const { Title, Text, Paragraph } = Typography;

/**
 * Demo component showing how to use the RBAC system
 */
const RBACDemo = () => {
  const [selectedScreen, setSelectedScreen] = useState('/qrmfg/admin');
  const [selectedDataType, setSelectedDataType] = useState('workflow');

  // Use the role-based access hook
  const {
    loading,
    isAuthenticated,
    currentUser,
    primaryRole,
    userPlants,
    primaryPlant,
    isAdmin,
    isJvcUser,
    isCqsUser,
    isTechUser,
    isPlantUser,
    isViewer,
    accessSummary,
    hasRole,
    hasAnyRole,
    checkScreenAccess,
    checkDataAccess,
    filterByPlantAccess
  } = useRoleBasedAccess();

  // Use specific access hooks
  const { hasAccess: hasAdminAccess, loading: adminLoading } = useScreenAccess('/qrmfg/admin');
  const { hasAccess: hasWorkflowAccess, loading: workflowLoading } = useDataAccess('workflow');

  // Use the role access hook
  const roleAccess = useRoleAccess();

  if (loading) {
    return <Spin size="large" />;
  }

  const handleScreenCheck = async () => {
    const access = await checkScreenAccess(selectedScreen);
    alert(`Access to ${selectedScreen}: ${access ? 'GRANTED' : 'DENIED'}`);
  };

  const handleDataCheck = async () => {
    const access = await checkDataAccess(selectedDataType);
    alert(`Access to ${selectedDataType} data: ${access ? 'GRANTED' : 'DENIED'}`);
  };

  const sampleData = [
    { id: 1, name: 'Item 1', plantCode: '1102' },
    { id: 2, name: 'Item 2', plantCode: '1103' },
    { id: 3, name: 'Item 3', plantCode: '1104' }
  ];

  const filteredData = filterByPlantAccess(sampleData, item => item.plantCode);

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>RBAC System Demo</Title>
      <Paragraph>
        This demo shows how to use the Role-Based Access Control system in the frontend.
      </Paragraph>

      <Divider />

      {/* User Information */}
      <Card title="Current User Information" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Text strong>Authenticated: </Text>
            <Tag color={isAuthenticated ? 'green' : 'red'}>
              {isAuthenticated ? 'Yes' : 'No'}
            </Tag>
          </div>

          {isAuthenticated && (
            <>
              <div>
                <Text strong>Username: </Text>
                <Text code>{currentUser}</Text>
              </div>

              <div>
                <Text strong>Primary Role: </Text>
                <Tag color="blue">{getRoleDisplayName(primaryRole)}</Tag>
              </div>

              {userPlants && userPlants.length > 0 && (
                <div>
                  <Text strong>Assigned Plants: </Text>
                  {userPlants.map(plant => (
                    <Tag key={plant} color={plant === primaryPlant ? 'gold' : 'default'}>
                      {plant} {plant === primaryPlant && '(Primary)'}
                    </Tag>
                  ))}
                </div>
              )}

              <div>
                <Text strong>Role Checks: </Text>
                <Space wrap>
                  <Tag color={isAdmin ? 'red' : 'default'}>Admin: {isAdmin ? 'Yes' : 'No'}</Tag>
                  <Tag color={isJvcUser ? 'blue' : 'default'}>JVC: {isJvcUser ? 'Yes' : 'No'}</Tag>
                  <Tag color={isCqsUser ? 'green' : 'default'}>CQS: {isCqsUser ? 'Yes' : 'No'}</Tag>
                  <Tag color={isTechUser ? 'orange' : 'default'}>Tech: {isTechUser ? 'Yes' : 'No'}</Tag>
                  <Tag color={isPlantUser ? 'purple' : 'default'}>Plant: {isPlantUser ? 'Yes' : 'No'}</Tag>
                  <Tag color={isViewer ? 'gray' : 'default'}>Viewer: {isViewer ? 'Yes' : 'No'}</Tag>
                </Space>
              </div>
            </>
          )}
        </Space>
      </Card>

      {/* Role-Based Components Demo */}
      <Card title="Role-Based Component Examples" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <AdminOnly>
            <Alert message="This content is only visible to Admins" type="success" showIcon />
          </AdminOnly>

          <JvcOnly>
            <Alert message="This content is only visible to JVC Users" type="info" showIcon />
          </JvcOnly>

          <CqsOnly>
            <Alert message="This content is only visible to CQS Users" type="warning" showIcon />
          </CqsOnly>

          <TechOnly>
            <Alert message="This content is only visible to Tech Users" type="error" showIcon />
          </TechOnly>

          <PlantOnly>
            <Alert message="This content is only visible to Plant Users" type="success" showIcon />
          </PlantOnly>

          <NonViewerOnly>
            <Alert message="This content is hidden from Viewers" type="info" showIcon />
          </NonViewerOnly>

          <RoleBasedComponent roles={[ROLES.ADMIN, ROLES.TECH_USER]}>
            <Alert message="This content requires Admin OR Tech role" type="warning" showIcon />
          </RoleBasedComponent>

          <RoleBasedComponent roles={[ROLES.JVC_USER, ROLES.CQS_USER]} requireAll>
            <Alert message="This content requires BOTH JVC AND CQS roles" type="error" showIcon />
          </RoleBasedComponent>
        </Space>
      </Card>

      {/* Access Checks Demo */}
      <Card title="Dynamic Access Checks" style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Text strong>Screen Access Check: </Text>
            <Space>
              <Button onClick={handleScreenCheck}>
                Check Access to {selectedScreen}
              </Button>
              <Text>
                Admin Screen Access: {adminLoading ? 'Loading...' : (hasAdminAccess ? 'GRANTED' : 'DENIED')}
              </Text>
            </Space>
          </div>

          <div>
            <Text strong>Data Access Check: </Text>
            <Space>
              <Button onClick={handleDataCheck}>
                Check Access to {selectedDataType} data
              </Button>
              <Text>
                Workflow Data Access: {workflowLoading ? 'Loading...' : (hasWorkflowAccess ? 'GRANTED' : 'DENIED')}
              </Text>
            </Space>
          </div>

          <div>
            <Text strong>Role Helper Functions: </Text>
            <Space wrap>
              <Tag color={hasRole(ROLES.ADMIN) ? 'green' : 'red'}>
                hasRole('ADMIN'): {hasRole(ROLES.ADMIN) ? 'True' : 'False'}
              </Tag>
              <Tag color={hasAnyRole([ROLES.JVC_USER, ROLES.CQS_USER]) ? 'green' : 'red'}>
                hasAnyRole(['JVC_USER', 'CQS_USER']): {hasAnyRole([ROLES.JVC_USER, ROLES.CQS_USER]) ? 'True' : 'False'}
              </Tag>
            </Space>
          </div>
        </Space>
      </Card>

      {/* Plant Data Filtering Demo */}
      {isPlantUser && (
        <Card title="Plant Data Filtering Demo" style={{ marginBottom: 16 }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>
              <Text strong>Original Data: </Text>
              <Text code>{JSON.stringify(sampleData, null, 2)}</Text>
            </div>

            <div>
              <Text strong>Filtered Data (based on plant access): </Text>
              <Text code>{JSON.stringify(filteredData, null, 2)}</Text>
            </div>

            <Alert
              message="Plant users only see data for their assigned plants"
              type="info"
              showIcon
            />
          </Space>
        </Card>
      )}

      {/* Access Summary */}
      {accessSummary && (
        <Card title="Access Summary from Backend" style={{ marginBottom: 16 }}>
          <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4 }}>
            {JSON.stringify(accessSummary, null, 2)}
          </pre>
        </Card>
      )}

      {/* Usage Examples */}
      <Card title="Code Usage Examples">
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Text strong>1. Role-based component rendering:</Text>
            <pre style={{ background: '#f5f5f5', padding: 8, margin: '8px 0' }}>
              {`<AdminOnly>
  <AdminPanel />
</AdminOnly>

<RoleBasedComponent roles={['JVC_USER', 'CQS_USER']}>
  <WorkflowManagement />
</RoleBasedComponent>`}
            </pre>
          </div>

          <div>
            <Text strong>2. Using the access hook:</Text>
            <pre style={{ background: '#f5f5f5', padding: 8, margin: '8px 0' }}>
              {`const { isAdmin, hasRole, checkScreenAccess } = useRoleBasedAccess();

if (isAdmin) {
  // Show admin features
}

if (hasRole('JVC_USER')) {
  // Show JVC features
}

const hasAccess = await checkScreenAccess('/admin');`}
            </pre>
          </div>

          <div>
            <Text strong>3. Plant data filtering:</Text>
            <pre style={{ background: '#f5f5f5', padding: 8, margin: '8px 0' }}>
              {`const { filterByPlantAccess } = useRoleBasedAccess();

const filteredWorkflows = filterByPlantAccess(
  workflows, 
  workflow => workflow.plantCode
);`}
            </pre>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default RBACDemo;