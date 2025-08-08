import { SyncOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { Card, Button, Alert, Space, Typography, message, Divider } from 'antd';
import React, { useState } from 'react';

import { workflowAPI } from '../services/workflowAPI';

const { Title, Text } = Typography;

const MaterialNameUpdater = () => {
  const [loading, setLoading] = useState(false);
  const [lastUpdateResult, setLastUpdateResult] = useState(null);

  const handleUpdateAllMaterialNames = async () => {
    try {
      setLoading(true);
      setLastUpdateResult(null);

      const response = await workflowAPI.updateAllMaterialNamesFromProjectItemMaster();

      setLastUpdateResult({
        success: true,
        message: response.message,
        updatedBy: response.updatedBy,
        timestamp: new Date().toLocaleString()
      });

      message.success('Material names updated successfully from ProjectItemMaster');
    } catch (error) {
      console.error('Failed to update material names:', error);

      setLastUpdateResult({
        success: false,
        message: error.message || 'Failed to update material names',
        timestamp: new Date().toLocaleString()
      });

      message.error('Failed to update material names. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card
      title={
        <Space>
          <SyncOutlined />
          Material Name Synchronization
        </Space>
      }
      style={{ maxWidth: 600, margin: '0 auto' }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Alert
          message="Material Name Synchronization"
          description="This utility updates all workflow material names from the QrmfgProjectItemMaster table's ITEM_DESCRIPTION column. This ensures consistency across the portal."
          type="info"
          showIcon
        />

        <Divider />

        <div>
          <Title level={5}>Update All Material Names</Title>
          <Text type="secondary">
            This will update material names for all existing workflows by fetching the latest
            ITEM_DESCRIPTION from the QrmfgProjectItemMaster table based on PROJECT_CODE and
            ITEM_CODE.
          </Text>
        </div>

        <Button
          type="primary"
          icon={<SyncOutlined />}
          loading={loading}
          onClick={handleUpdateAllMaterialNames}
          size="large"
          style={{ width: '100%' }}
        >
          {loading ? 'Updating Material Names...' : 'Update All Material Names'}
        </Button>

        {lastUpdateResult && (
          <Alert
            message={lastUpdateResult.success ? 'Update Successful' : 'Update Failed'}
            description={
              <div>
                <div>{lastUpdateResult.message}</div>
                <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                  <strong>Timestamp:</strong> {lastUpdateResult.timestamp}
                  {lastUpdateResult.updatedBy && (
                    <>
                      <br />
                      <strong>Updated by:</strong> {lastUpdateResult.updatedBy}
                    </>
                  )}
                </div>
              </div>
            }
            type={lastUpdateResult.success ? 'success' : 'error'}
            showIcon
            icon={
              lastUpdateResult.success ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />
            }
          />
        )}

        <Divider />

        <div style={{ fontSize: '12px', color: '#666' }}>
          <Title level={5}>How it works:</Title>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li>Fetches all existing workflows from the database</li>
            <li>For each workflow, looks up the material name in QrmfgProjectItemMaster</li>
            <li>Updates the workflow's materialName field with the ITEM_DESCRIPTION</li>
            <li>Skips workflows where the material name is already up to date</li>
            <li>Logs all changes for audit purposes</li>
          </ul>
        </div>

        <Alert
          message="Important Notes"
          description={
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>This operation requires ADMIN privileges</li>
              <li>The update process may take a few minutes for large datasets</li>
              <li>All changes are logged and auditable</li>
              <li>New workflows automatically get material names from ProjectItemMaster</li>
            </ul>
          }
          type="warning"
          showIcon
        />
      </Space>
    </Card>
  );
};

export default MaterialNameUpdater;
