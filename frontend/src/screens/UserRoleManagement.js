import { UserOutlined, TeamOutlined, EditOutlined } from '@ant-design/icons';
import { Table, Card, Button, message, Spin, Typography, Modal, Transfer, Space, Tabs } from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';
import { PAGINATION } from '../constants';
import { ensureArray } from '../utils/dataUtils';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

const UserRoleManagement = () => {
  const [loading, setLoading] = useState(true);
  const [userRoles, setUserRoles] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [assignedRoleIds, setAssignedRoleIds] = useState([]);
  const [saveLoading, setSaveLoading] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    fetchUserRoles(controller.signal);

    return () => {
      controller.abort();
    };
  }, []);

  const fetchUserRoles = async signal => {
    setLoading(true);
    try {
      const response = await apiClient.get('/admin/monitoring/user-roles', { signal });
      if (!signal?.aborted) {
        // Ensure response is always an array
        const safeResponse = ensureArray(response);
        setUserRoles(safeResponse);
      }
    } catch (error) {
      if (!signal?.aborted) {
        message.error('Failed to load user role assignments');
        console.error('Error fetching user roles:', error);
        // Set empty array on error
        setUserRoles([]);
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  };

  const handleEditRoles = user => {
    setSelectedUser(user);
    setAvailableRoles([
      ...(user.assignedRoles || []).map(role => ({
        ...role,
        key: role.id.toString()
      })),
      ...(user.availableRoles || []).map(role => ({
        ...role,
        key: role.id.toString()
      }))
    ]);
    setAssignedRoleIds((user.assignedRoles || []).map(role => role.id.toString()));
    setModalVisible(true);
  };

  const handleTransferChange = nextTargetKeys => {
    setAssignedRoleIds(nextTargetKeys);
  };

  const handleSaveRoles = async () => {
    if (!selectedUser) {
      return;
    }

    setSaveLoading(true);
    try {
      const roleIds = assignedRoleIds.map(id => parseInt(id, 10));
      await apiClient.put(`/admin/monitoring/user-roles/${selectedUser.userId}`, roleIds);
      message.success('User roles updated successfully');
      setModalVisible(false);
      fetchUserRoles();
    } catch (error) {
      message.error('Failed to update user roles');
      console.error('Error updating user roles:', error);
    } finally {
      setSaveLoading(false);
    }
  };

  const columns = [
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username',
      sorter: (a, b) => a.username.localeCompare(b.username)
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email'
    },
    {
      title: 'Assigned Roles',
      dataIndex: 'assignedRoles',
      key: 'assignedRoles',
      render: roles => (roles || []).map(role => role.name).join(', ')
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button type="primary" icon={<EditOutlined />} onClick={() => handleEditRoles(record)}>
          Edit Roles
        </Button>
      )
    }
  ];

  return (
    <div>
      <Title level={2}>User Role Management</Title>

      <Tabs defaultActiveKey="1">
        <TabPane
          tab={
            <span>
              <UserOutlined />
              User Roles
            </span>
          }
          key="1"
        >
          <Card>
            <Button type="primary" onClick={fetchUserRoles} style={{ marginBottom: 16 }}>
              Refresh User Roles
            </Button>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <p>Loading user role assignments...</p>
              </div>
            ) : (
              <Table
                dataSource={userRoles}
                columns={columns}
                rowKey="userId"
                pagination={{ pageSize: PAGINATION.DEFAULT_PAGE_SIZE }}
              />
            )}
          </Card>
        </TabPane>

        <TabPane
          tab={
            <span>
              <TeamOutlined />
              Workflow Permissions
            </span>
          }
          key="2"
        >
          <Card>
            <div style={{ padding: '20px' }}>
              <Title level={4}>Workflow Role Permissions</Title>

              <Space direction="vertical" style={{ width: '100%' }}>
                <Card title="JVC User Permissions" size="small">
                  <ul>
                    <li>Initiate MSDS workflows for materials</li>
                    <li>Assign workflows to plant teams</li>
                    <li>View JVC dashboard with pending extensions</li>
                    <li>Respond to queries from plant teams</li>
                  </ul>
                </Card>

                <Card title="Plant User Permissions" size="small">
                  <ul>
                    <li>Complete multi-step questionnaires</li>
                    <li>Raise queries to CQS/Tech/JVC teams</li>
                    <li>View plant dashboard with assigned materials</li>
                    <li>Mark materials as completed</li>
                  </ul>
                </Card>

                <Card title="CQS User Permissions" size="small">
                  <ul>
                    <li>Receive and resolve queries from plant teams</li>
                    <li>View CQS dashboard with pending queries</li>
                    <li>Access material context for queries</li>
                  </ul>
                </Card>

                <Card title="Tech User Permissions" size="small">
                  <ul>
                    <li>Receive and resolve technical queries from plant teams</li>
                    <li>View Tech dashboard with pending queries</li>
                    <li>Access material context for queries</li>
                  </ul>
                </Card>

                <Card title="Admin Permissions" size="small">
                  <ul>
                    <li>Access workflow monitoring dashboard</li>
                    <li>Generate query SLA reports</li>
                    <li>Manage user roles and permissions</li>
                    <li>Export audit logs and workflow reports</li>
                    <li>View all workflows regardless of state</li>
                  </ul>
                </Card>
              </Space>

              <Text type="secondary" style={{ marginTop: 16, display: 'block' }}>
                Note: Users can have multiple roles assigned to them. The permissions are
                cumulative.
              </Text>
            </div>
          </Card>
        </TabPane>
      </Tabs>

      <Modal
        title="Edit User Roles"
        open={modalVisible}
        onOk={handleSaveRoles}
        onCancel={() => setModalVisible(false)}
        width={800}
        confirmLoading={saveLoading}
      >
        {selectedUser && (
          <>
            <p>
              <strong>User:</strong> {selectedUser.username} ({selectedUser.email})
            </p>
            <p>Assign or remove roles using the transfer control below:</p>
            <Transfer
              dataSource={availableRoles}
              titles={['Available Roles', 'Assigned Roles']}
              targetKeys={assignedRoleIds}
              onChange={handleTransferChange}
              render={item => item.name}
              listStyle={{
                width: 350,
                height: 300
              }}
            />
          </>
        )}
      </Modal>
    </div>
  );
};

export default UserRoleManagement;
