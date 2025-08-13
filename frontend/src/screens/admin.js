import { Menu } from 'antd';
import React from 'react';
import { Link, Routes, Route, useLocation, useNavigate } from 'react-router-dom';

import NotificationDefaults from '../components/NotificationDefaults';
import { AdminOnly } from '../components/RoleBasedComponent';
import { useRoleBasedAccess } from '../hooks/useRoleBasedAccess';

import AuditLogs from './Auditlogs';
import Roles from './Roles';
import Sessions from './Sessions';
import UserRoleManagement from './UserRoleManagement';
import Users from './Users';
import WorkflowMonitoring from './WorkflowMonitoring';

const AdminPanel = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const currentPath = location.pathname;

  // Use role-based access control
  const { isAdmin } = useRoleBasedAccess();

  const menuItems = [
    { key: '/qrmfg/admin/users', label: 'Users' },
    { key: '/qrmfg/admin/roles', label: 'Roles' },
    { key: '/qrmfg/admin/auditlogs', label: 'Audit Logs' },
    { key: '/qrmfg/admin/sessions', label: 'Sessions' },
    { key: '/qrmfg/admin/user-role-management', label: 'User Role Management' },
    { key: '/qrmfg/admin/notification-defaults', label: 'Notification Defaults' },
    { key: '/qrmfg/admin/workflow-monitoring', label: 'Legacy Monitoring' }
  ];

  // If we're at /qrmfg/admin, redirect to /qrmfg/admin/users
  React.useEffect(() => {
    if (currentPath === '/qrmfg/admin') {
      navigate('/qrmfg/admin/users');
    }
  }, [currentPath, navigate]);

  return (
    <AdminOnly>
      <div style={{ padding: 24 }}>
        <h2>Admin Panel</h2>
        <Menu
          mode="horizontal"
          selectedKeys={[currentPath]}
          items={menuItems.map(item => ({
            ...item,
            label: <Link to={item.key}>{item.label}</Link>
          }))}
          style={{ marginBottom: 24 }}
        />
        <div style={{ marginTop: 24 }}>
          <Routes>
            <Route path="users" element={<Users />} />
            <Route path="roles" element={<Roles />} />
            <Route path="auditlogs" element={<AuditLogs />} />
            <Route path="sessions" element={<Sessions />} />
            <Route path="user-role-management" element={<UserRoleManagement />} />
            <Route path="notification-defaults" element={<NotificationDefaults />} />
            <Route path="workflow-monitoring" element={<WorkflowMonitoring />} />
          </Routes>
        </div>
      </div>
    </AdminOnly>
  );
};

export default AdminPanel;
