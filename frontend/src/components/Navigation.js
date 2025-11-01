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
  MonitorOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons';
import { Layout, Menu, Button, Badge, Tooltip, Avatar, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import APLogo from '../assets/APLOGO.jpg';
import {
  isAuthenticated,
  isAdmin,
  removeToken,
  getCurrentUser,
  getPrimaryRoleType
} from '../services/auth';
import RBACService from '../services/rbacService';
import './Navigation.css';

const { Sider } = Layout;
const { Text } = Typography;

const Navigation = ({ onCollapse }) => {
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

      // Use the items directly - they're already filtered by role
      console.log('Setting navigation items:', items);
      setNavigationItems(items);

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

  // Convert navigation items to menu items with enhanced styling
  const menuItems = navigationItems.map(item => ({
    key: item.key,
    icon: iconMap[item.icon] || <AppstoreOutlined />,
    label: collapsed ? (
      <Tooltip title={item.label} placement="right" mouseEnterDelay={0.5}>
        <span style={{ display: 'none' }}>{item.label}</span>
      </Tooltip>
    ) : (
      <Link to={item.path}>{item.label}</Link>
    ),
    onClick: collapsed ? () => navigate(item.path) : undefined
  }));

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      onCollapse={value => {
        setCollapsed(value);
        onCollapse?.(value);
      }}
      trigger={null}
      style={{
        overflow: 'hidden',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
        background: 'linear-gradient(180deg, #1a2332 0%, #0f1419 100%)',
        boxShadow: '2px 0 8px rgba(0, 0, 0, 0.15)',
        zIndex: 1000
      }}
      width={280}
      collapsedWidth={70}
    >
      {/* Custom Collapse Toggle */}
      <div
        className="sidebar-toggle"
        style={{
          position: 'absolute',
          top: 20,
          right: -15,
          zIndex: 1001,
          background: '#1890ff',
          borderRadius: '50%',
          width: 30,
          height: 30,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
          transition: 'all 0.3s ease'
        }}
        onClick={() => {
          const newCollapsed = !collapsed;
          setCollapsed(newCollapsed);
          onCollapse?.(newCollapsed);
        }}
      >
        {collapsed ? (
          <MenuUnfoldOutlined style={{ color: '#fff', fontSize: 14 }} />
        ) : (
          <MenuFoldOutlined style={{ color: '#fff', fontSize: 14 }} />
        )}
      </div>

      {/* Header with branding */}
      <div
        className="sidebar-branding"
        style={{
          padding: collapsed ? '20px 8px' : '28px 24px 24px 24px',
          textAlign: 'center',
          borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          background: 'rgba(255, 255, 255, 0.05)',
          minHeight: collapsed ? 'auto' : '180px'
        }}
      >
        {/* Company Logo */}
        <div
          style={{
            marginBottom: collapsed ? 12 : 16,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <div
            className="company-logo"
            style={{
              width: collapsed ? 50 : 100,
              height: collapsed ? 50 : 100,
              borderRadius: '50%',
              background: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'all 0.3s ease',
              boxShadow: '0 6px 16px rgba(0, 0, 0, 0.4), 0 0 12px rgba(24, 144, 255, 0.3)',
              padding: collapsed ? '8px' : '16px',
              border: '2px solid rgba(255, 255, 255, 0.8)',
              overflow: 'hidden'
            }}
          >
            <img
              src={APLogo}
              alt="Asian Paints Logo"
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'contain',
                objectPosition: 'center center',
                filter: 'contrast(1.2) brightness(1.1) saturate(1.1)'
              }}
            />
          </div>
        </div>

        {/* Company Name */}
        <div
          style={{
            fontSize: collapsed ? 12 : 18,
            fontWeight: 'bold',
            color: '#fff',
            letterSpacing: '0.6px',
            marginBottom: collapsed ? 0 : 6,
            lineHeight: 1.2,
            textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)',
            textAlign: 'center'
          }}
        >
          {collapsed ? 'AP' : 'Asian Paints'}
        </div>

        {!collapsed && (
          <Text
            style={{
              color: 'rgba(255, 255, 255, 0.85)',
              fontSize: 12,
              display: 'block',
              fontWeight: 500,
              letterSpacing: '0.4px',
              textAlign: 'center',
              textShadow: '0 1px 2px rgba(0, 0, 0, 0.3)',
              marginTop: 2
            }}
          >
            QR Manufacturing Portal
          </Text>
        )}
      </div>

      {/* User Profile Section */}
      {userInfo && (
        <div
          style={{
            padding: collapsed ? '14px 8px' : '18px 24px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
            background: 'rgba(255, 255, 255, 0.03)'
          }}
        >
          {collapsed ? (
            <div style={{ textAlign: 'center' }}>
              <Tooltip title={`${userInfo.username} (${userInfo.role})`} placement="right">
                <Avatar
                  size={36}
                  style={{
                    backgroundColor: isAdmin() ? '#52c41a' : '#1890ff',
                    fontSize: 14,
                    fontWeight: 'bold',
                    cursor: 'pointer'
                  }}
                >
                  {userInfo.username?.charAt(0)?.toUpperCase() || 'U'}
                </Avatar>
              </Tooltip>
            </div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Avatar
                size={40}
                style={{
                  backgroundColor: isAdmin() ? '#52c41a' : '#1890ff',
                  fontSize: 16,
                  fontWeight: 'bold'
                }}
              >
                {userInfo.username?.charAt(0)?.toUpperCase() || 'U'}
              </Avatar>
              <div style={{ flex: 1, minWidth: 0 }}>
                <Text
                  strong
                  style={{
                    color: '#fff',
                    fontSize: 14,
                    display: 'block',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  }}
                >
                  {userInfo.username}
                </Text>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                  <Badge
                    status={isAdmin() ? 'success' : 'processing'}
                    style={{ fontSize: 10 }}
                  />
                  <Text
                    style={{
                      color: 'rgba(255, 255, 255, 0.65)',
                      fontSize: 12,
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px'
                    }}
                  >
                    {userInfo.role}
                  </Text>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Navigation Menu */}
      <div
        style={{
          height: collapsed ? 'calc(100vh - 160px)' : 'calc(100vh - 240px)',
          overflowY: 'auto',
          overflowX: 'hidden',
          padding: '16px 0',
          position: 'relative'
        }}
      >
        {loading ? (
          <div className="sidebar-loading" style={{ padding: 24, textAlign: 'center', color: 'rgba(255, 255, 255, 0.65)' }}>
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
              background: 'transparent',
              fontSize: 14
            }}
            className="custom-sidebar-menu"
          />
        )}
      </div>

      {/* Footer with logout */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          padding: collapsed ? '16px 8px' : '20px 24px',
          background: 'rgba(0, 0, 0, 0.2)',
          borderTop: '1px solid rgba(255, 255, 255, 0.1)'
        }}
      >
        <Button
          type="primary"
          icon={<LogoutOutlined />}
          onClick={handleLogout}
          block
          size="large"
          className="logout-button"
          style={{
            background: 'linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%)',
            border: 'none',
            borderRadius: 8,
            height: 44,
            fontSize: 14,
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            gap: collapsed ? 0 : 8,
            boxShadow: '0 2px 8px rgba(255, 77, 79, 0.3)',
            transition: 'all 0.3s ease'
          }}
        >
          {!collapsed && 'Sign Out'}
        </Button>
      </div>
    </Sider>
  );
};

export default Navigation;
