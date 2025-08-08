import {
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
  AppstoreOutlined,
  FileSearchOutlined,
  SettingOutlined,
  LogoutOutlined,
  DashboardOutlined,
  HomeOutlined,
  BankOutlined,
  ApiOutlined,
  AuditOutlined,
  UsergroupAddOutlined,
  KeyOutlined,
  ClockCircleOutlined,
  MonitorOutlined
} from '@ant-design/icons';
import { Layout, Menu, Button, Badge, Tooltip } from 'antd';
import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import { 
  isAuthenticated, 
  isAdmin, 
  removeToken, 
  getCurrentUser, 
  getPrimaryRoleType 
} from '../services/auth';
import RBACService from '../services/rbacService';

const { Sider } = Layout;

const Navigation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [navigationItems, setNavigationItems] = useState([]);
  const [collapsed, setCollapsed] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isAuthenticated()) {
      loadNavigationData();
    }
  }, []);

  const loadNavigationData = async () => {
    try {
      setLoading(true);
      
      // Get user info
      const currentUser = getCurrentUser();
      const primaryRole = getPrimaryRoleType();
      
      setUserInfo({
        username: currentUser,
        role: primaryRole
      });

      // Get navigation items based on role
      const items = RBACService.getNavigationItems();
      
      // Get accessible screens from backend (with fallback)
      try {
        const accessibleScreens = await RBACService.getAccessibleScreens();
        const filteredItems = items.filter(item => 
          accessibleScreens.includes(item.path) || item.path === '/qrmfg/settings'
        );
        setNavigationItems(filteredItems);
      } catch (error) {
        console.warn('Using fallback navigation items:', error);
        setNavigationItems(items);
      }
      
    } catch (error) {
      console.error('Error loading navigation data:', error);
      // Fallback to basic navigation
      setNavigationItems(RBACService.getNavigationItems());
    } finally {
      setLoading(false);
    }
  };

  if (!isAuthenticated()) {
    return null;
  }

  const handleLogout = () => {
    removeToken();
    navigate('/qrmfg/login');
  };

  // Icon mapping
  const iconMap = {
    'HomeOutlined': <HomeOutlined />,
    'DashboardOutlined': <DashboardOutlined />,
    'AppstoreOutlined': <AppstoreOutlined />,
    'UserOutlined': <UserOutlined />,
    'FileSearchOutlined': <FileSearchOutlined />,
    'SafetyOutlined': <SafetyOutlined />,
    'TeamOutlined': <TeamOutlined />,
    'BankOutlined': <BankOutlined />,
    'AuditOutlined': <AuditOutlined />,
    'KeyOutlined': <KeyOutlined />,
    'ClockCircleOutlined': <ClockCircleOutlined />,
    'UsergroupAddOutlined': <UsergroupAddOutlined />,
    'MonitorOutlined': <MonitorOutlined />,
    'ApiOutlined': <ApiOutlined />,
    'SettingOutlined': <SettingOutlined />
  };

  // Convert navigation items to menu items
  const menuItems = navigationItems.map(item => ({
    key: item.key,
    icon: iconMap[item.icon] || <AppstoreOutlined />,
    label: <Link to={item.path}>{item.label}</Link>
  }));

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      onCollapse={value => setCollapsed(value)}
      style={{
        overflow: 'auto',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0
      }}
      width={250}
      collapsedWidth={80}
      breakpoint="lg"
    >
      {/* Header with user info */}
      <div
        style={{
          height: 64,
          margin: '16px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          fontSize: collapsed ? 12 : 16,
          whiteSpace: 'nowrap',
          overflow: 'hidden'
        }}
      >
        <div style={{ fontSize: collapsed ? 14 : 20, fontWeight: 'bold' }}>
          {collapsed ? 'QR' : 'QR Manufacturing'}
        </div>
        {!collapsed && userInfo && (
          <div style={{ fontSize: 12, opacity: 0.8, marginTop: 4 }}>
            <Tooltip title={`Role: ${userInfo.role}`}>
              {userInfo.username} ({userInfo.role})
            </Tooltip>
          </div>
        )}
      </div>

      {/* Navigation Menu */}
      <div
        style={{
          height: 'calc(100vh - 160px)',
          overflowY: 'auto',
          overflowX: 'hidden'
        }}
      >
        {loading ? (
          <div style={{ padding: 16, textAlign: 'center', color: '#fff' }}>
            Loading...
          </div>
        ) : (
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            style={{
              borderRight: 0,
              height: '100%'
            }}
          />
        )}
      </div>

      {/* Footer with logout */}
      <div
        style={{
          padding: collapsed ? '8px 8px' : '16px',
          marginTop: 'auto',
          marginBottom: '16px',
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          backgroundColor: '#001529'
        }}
      >
        {!collapsed && userInfo && (
          <div style={{ 
            color: '#fff', 
            fontSize: 12, 
            marginBottom: 8, 
            textAlign: 'center',
            opacity: 0.7 
          }}>
            {isAdmin() && (
              <Badge status="success" text="Admin Access" />
            )}
          </div>
        )}
        <Button
          type="primary"
          icon={<LogoutOutlined />}
          danger
          onClick={handleLogout}
          block
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start'
          }}
        >
          {!collapsed && 'Logout'}
        </Button>
      </div>
    </Sider>
  );
};

export default Navigation;
