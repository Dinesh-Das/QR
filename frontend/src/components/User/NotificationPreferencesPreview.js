import { BellOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { Card, Alert, Row, Col, Typography, Tag, Spin } from 'antd';
import PropTypes from 'prop-types';
import React, { useState, useEffect, useCallback } from 'react';

import apiClient from '../../api/client';

const { Text, Title } = Typography;

/**
 * NotificationPreferencesPreview Component
 * 
 * Shows a preview of what notification preferences will be created for a new user
 * based on their selected roles and the current admin configuration.
 * 
 * @component
 */
const NotificationPreferencesPreview = ({ selectedRoles, roles, visible }) => {
  const [previewData, setPreviewData] = useState(null);
  const [loading, setLoading] = useState(false);

  // Get the primary role name from selected role IDs
  const getPrimaryRoleName = useCallback(() => {
    if (!selectedRoles || selectedRoles.length === 0) return null;
    
    const primaryRoleId = selectedRoles[0];
    const role = roles.find(r => r.id === primaryRoleId);
    
    if (!role) return null;
    
    // Convert role name to match backend format
    // e.g., "ROLE_CQS_USER" -> "CQS" or "JVC_USER" -> "JVC_USER"
    let roleName = role.name;
    if (roleName.startsWith('ROLE_') && roleName.endsWith('_USER')) {
      roleName = roleName.substring(5, roleName.length - 5);
    } else if (roleName.startsWith('ROLE_')) {
      roleName = roleName.substring(5);
    }
    // If the role name is already in format like "JVC_USER", keep it as is
    // The backend will handle both "JVC" and "JVC_USER" mappings
    
    return roleName;
  }, [selectedRoles, roles]);

  // Load preview data when roles change
  useEffect(() => {
    const loadPreview = async () => {
      const primaryRole = getPrimaryRoleName();
      if (!primaryRole || !visible) {
        setPreviewData(null);
        return;
      }

      setLoading(true);
      try {
        const data = await apiClient.get(`/admin/notification-defaults/preview?role=${primaryRole}`);
        if (data.success) {
          setPreviewData({
            role: primaryRole,
            preferences: data.defaultPreferences
          });
        }
      } catch (error) {
        console.error('Failed to load notification preferences preview:', error);
        setPreviewData(null);
      } finally {
        setLoading(false);
      }
    };

    loadPreview();
  }, [selectedRoles, roles, visible, getPrimaryRoleName]);

  if (!visible || !selectedRoles || selectedRoles.length === 0) {
    return null;
  }

  const primaryRole = getPrimaryRoleName();

  return (
    <Card
      title={
        <span>
          <BellOutlined style={{ marginRight: 8 }} />
          Notification Preferences Preview
        </span>
      }
      size="small"
      style={{ marginTop: 16 }}
    >
      <Alert
        message="Default Notification Preferences"
        description={`The following notification preferences will be automatically created for this user based on their ${primaryRole} role.`}
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        style={{ marginBottom: 16 }}
      />

      {loading ? (
        <div style={{ textAlign: 'center', padding: 20 }}>
          <Spin size="small" />
          <Text style={{ marginLeft: 8 }}>Loading preview...</Text>
        </div>
      ) : previewData ? (
        <div>
          <div style={{ marginBottom: 12 }}>
            <Text strong>Primary Role: </Text>
            <Tag color="blue">{previewData.role}</Tag>
          </div>
          
          <Title level={5} style={{ marginBottom: 12 }}>
            Notification Types:
          </Title>
          
          <Row gutter={[8, 8]}>
            {Object.entries(previewData.preferences).map(([type, enabled]) => (
              <Col xs={24} sm={12} md={8} key={type}>
                <div style={{
                  padding: '6px 10px',
                  backgroundColor: enabled ? '#f6ffed' : '#fff2f0',
                  border: `1px solid ${enabled ? '#b7eb8f' : '#ffccc7'}`,
                  borderRadius: '4px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}>
                  <Text style={{ fontSize: '12px' }}>
                    {type.replace(/_/g, ' ')}
                  </Text>
                  <Tag 
                    color={enabled ? 'success' : 'default'} 
                    size="small"
                  >
                    {enabled ? 'ON' : 'OFF'}
                  </Tag>
                </div>
              </Col>
            ))}
          </Row>
          
          <Alert
            message="User Customization"
            description="After creation, the user can modify these preferences in their settings."
            type="success"
            showIcon
            style={{ marginTop: 12 }}
            size="small"
          />
        </div>
      ) : (
        <Alert
          message="Preview Unavailable"
          description="Could not load notification preferences preview for the selected role."
          type="warning"
          showIcon
          size="small"
        />
      )}
    </Card>
  );
};

NotificationPreferencesPreview.propTypes = {
  selectedRoles: PropTypes.array,
  roles: PropTypes.array.isRequired,
  visible: PropTypes.bool.isRequired
};

NotificationPreferencesPreview.defaultProps = {
  selectedRoles: []
};

export default NotificationPreferencesPreview;