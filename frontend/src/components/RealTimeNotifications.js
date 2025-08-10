import { BellOutlined, CheckOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons';
import { notification, Badge, Dropdown, Button, List, Typography, Empty, Spin } from 'antd';
import React, { useState, useEffect, useRef } from 'react';

import apiClient from '../api/client';
import { getCurrentUser } from '../services/auth';

const { Text } = Typography;

const RealTimeNotifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);

  useEffect(() => {
    loadNotifications();
    connectWebSocket();

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, []);

  const connectWebSocket = () => {
    const user = getCurrentUser();
    if (!user) {
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/notifications?user=${user.username}`;

    wsRef.current = new WebSocket(wsUrl);

    wsRef.current.onopen = () => {
      console.log('WebSocket connected for notifications');
    };

    wsRef.current.onmessage = event => {
      try {
        const notificationData = JSON.parse(event.data);
        handleNewNotification(notificationData);
      } catch (error) {
        console.error('Error parsing notification:', error);
      }
    };

    wsRef.current.onclose = () => {
      console.log('WebSocket disconnected, attempting to reconnect...');
      reconnectTimeoutRef.current = setTimeout(connectWebSocket, 5000);
    };

    wsRef.current.onerror = error => {
      console.error('WebSocket error:', error);
    };
  };

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const user = getCurrentUser();
      const data = await apiClient.get(`/api/notifications/${user.username}`);
      setNotifications(data);
      setUnreadCount(data.filter(n => !n.read).length);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleNewNotification = notificationData => {
    const newNotification = {
      id: Date.now(),
      type: notificationData.type,
      title: notificationData.title,
      message: notificationData.message,
      timestamp: new Date(),
      read: false,
      data: notificationData.data
    };

    setNotifications(prev => [newNotification, ...prev]);
    setUnreadCount(prev => prev + 1);

    // Show browser notification
    showBrowserNotification(newNotification);

    // Show Ant Design notification
    showAntNotification(newNotification);
  };

  const showBrowserNotification = notif => {
    if (Notification.permission === 'granted') {
      new Notification(notif.title, {
        body: notif.message,
        icon: '/favicon.ico',
        tag: notif.id
      });
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          showBrowserNotification(notif);
        }
      });
    }
  };

  const showAntNotification = notif => {
    const config = {
      message: notif.title,
      description: notif.message,
      duration: 6,
      placement: 'topRight'
    };

    switch (notif.type) {
      case 'workflow_created':
      case 'workflow_extended':
        notification.info(config);
        break;
      case 'workflow_completed':
        notification.success(config);
        break;
      case 'workflow_overdue':
      case 'query_overdue':
        notification.error(config);
        break;
      case 'query_raised':
      case 'query_assigned':
        notification.warning(config);
        break;
      case 'query_resolved':
        notification.success(config);
        break;
      default:
        notification.open(config);
    }
  };

  const markAsRead = async notificationId => {
    try {
      const user = getCurrentUser();
      await apiClient.put(`/api/notifications/${user.username}/${notificationId}/read`);

      setNotifications(prev => prev.map(n => (n.id === notificationId ? { ...n, read: true } : n)));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  const markAllAsRead = async () => {
    try {
      const user = getCurrentUser();
      await apiClient.put(`/api/notifications/${user.username}/read-all`);

      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error);
    }
  };

  const deleteNotification = async notificationId => {
    try {
      const user = getCurrentUser();
      await apiClient.delete(`/api/notifications/${user.username}/${notificationId}`);

      const notification = notifications.find(n => n.id === notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
      if (notification && !notification.read) {
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
    } catch (error) {
      console.error('Failed to delete notification:', error);
    }
  };

  const clearAllNotifications = async () => {
    try {
      const user = getCurrentUser();
      await apiClient.delete(`/api/notifications/${user.username}/clear`);

      setNotifications([]);
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to clear notifications:', error);
    }
  };

  const getNotificationIcon = type => {
    switch (type) {
      case 'workflow_created':
      case 'workflow_extended':
        return '📋';
      case 'workflow_completed':
        return '✅';
      case 'workflow_overdue':
        return '🚨';
      case 'query_raised':
      case 'query_assigned':
        return '❓';
      case 'query_resolved':
        return '✅';
      case 'query_overdue':
        return '⚠️';
      default:
        return '📢';
    }
  };

  const formatTimestamp = timestamp => {
    const now = new Date();
    const notifTime = new Date(timestamp);
    const diffMs = now - notifTime;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) {
      return 'Just now';
    }
    if (diffMins < 60) {
      return `${diffMins}m ago`;
    }
    if (diffHours < 24) {
      return `${diffHours}h ago`;
    }
    if (diffDays < 7) {
      return `${diffDays}d ago`;
    }
    return notifTime.toLocaleDateString();
  };

  const notificationMenu = (
    <div style={{ width: 400, maxHeight: 500, overflow: 'hidden' }}>
      <div
        style={{
          padding: '12px 16px',
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Text strong>Notifications</Text>
        <div>
          {unreadCount > 0 && (
            <Button type="link" size="small" onClick={markAllAsRead} icon={<CheckOutlined />}>
              Mark all read
            </Button>
          )}
          <Button
            type="link"
            size="small"
            onClick={clearAllNotifications}
            icon={<DeleteOutlined />}
            danger
          >
            Clear all
          </Button>
        </div>
      </div>

      <div style={{ maxHeight: 400, overflowY: 'auto' }}>
        {loading ? (
          <div style={{ padding: '20px', textAlign: 'center' }}>
            <Spin />
          </div>
        ) : notifications.length === 0 ? (
          <Empty description="No notifications" style={{ padding: '20px' }} />
        ) : (
          <List
            dataSource={notifications}
            renderItem={item => (
              <List.Item
                style={{
                  padding: '12px 16px',
                  backgroundColor: item.read ? 'transparent' : '#f6ffed',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: 'pointer'
                }}
                onClick={() => !item.read && markAsRead(item.id)}
                actions={[
                  <Button
                    key="delete"
                    type="text"
                    size="small"
                    icon={<DeleteOutlined />}
                    onClick={e => {
                      e.stopPropagation();
                      deleteNotification(item.id);
                    }}
                  />
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <span style={{ fontSize: '16px' }}>{getNotificationIcon(item.type)}</span>
                  }
                  title={
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text strong={!item.read}>{item.title}</Text>
                      <Text type="secondary" style={{ fontSize: '12px' }}>
                        {formatTimestamp(item.timestamp)}
                      </Text>
                    </div>
                  }
                  description={
                    <Text type="secondary" style={{ fontSize: '13px' }}>
                      {item.message}
                    </Text>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </div>

      <div
        style={{
          padding: '8px 16px',
          borderTop: '1px solid #f0f0f0',
          textAlign: 'center'
        }}
      >
        <Button
          type="link"
          size="small"
          icon={<SettingOutlined />}
          onClick={() => {
            setVisible(false);
            // Navigate to notification settings
            window.location.href = '/qrmfg/settings#notifications';
          }}
        >
          Notification Settings
        </Button>
      </div>
    </div>
  );

  return (
    <Dropdown
      overlay={notificationMenu}
      trigger={['click']}
      open={visible}
      onOpenChange={setVisible}
      placement="bottomRight"
    >
      <Badge count={unreadCount} size="small">
        <Button
          type="text"
          icon={<BellOutlined />}
          style={{
            border: 'none',
            boxShadow: 'none'
          }}
        />
      </Badge>
    </Dropdown>
  );
};

export default RealTimeNotifications;
