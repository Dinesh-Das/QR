import {
  HistoryOutlined,
  EyeOutlined,
  DownloadOutlined,
  DiffOutlined,
  UserOutlined
} from '@ant-design/icons';
import { Card, Tag, Button, Space, Table, Spin, Alert, Modal, Descriptions } from 'antd';
import React, { useState, useEffect } from 'react';

import { PAGINATION } from '../constants';
import { auditAPI } from '../services/auditAPI';

// const { Panel } = Collapse; // Not currently used

const VersionHistory = ({ workflowId, entityType = 'response' }) => {
  const [loading, setLoading] = useState(true);
  const [versions, setVersions] = useState([]);
  const [error, setError] = useState(null);
  const [selectedVersion, setSelectedVersion] = useState(null);
  const [compareMode, setCompareMode] = useState(false);
  const [compareVersions, setCompareVersions] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);

  useEffect(() => {
    if (workflowId) {
      loadVersionHistory();
    }
  }, [workflowId, entityType]);

  const loadVersionHistory = async () => {
    try {
      setLoading(true);
      let history;

      switch (entityType) {
        case 'response':
          history = await auditAPI.getQuestionnaireResponseAuditHistory(workflowId);
          break;
        case 'workflow':
          history = await auditAPI.getWorkflowAuditHistory(workflowId);
          break;
        case 'query':
          history = await auditAPI.getQueryAuditHistory(workflowId);
          break;
        default:
          history = await auditAPI.getQuestionnaireResponseVersions(workflowId);
      }

      setVersions(formatVersionHistory(history));
    } catch (err) {
      setError('Failed to load version history');
      console.error('Version history error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatVersionHistory = rawHistory => {
    return rawHistory.map((entry, index) => ({
      ...entry,
      version: rawHistory.length - index,
      isCurrent: index === 0
    }));
  };

  const handleViewVersion = version => {
    setSelectedVersion(version);
    setModalVisible(true);
  };

  const handleCompareToggle = () => {
    setCompareMode(!compareMode);
    setCompareVersions([]);
  };

  const handleVersionSelect = version => {
    if (!compareMode) {
      return;
    }

    if (compareVersions.includes(version.id)) {
      setCompareVersions(compareVersions.filter(id => id !== version.id));
    } else if (compareVersions.length < 2) {
      setCompareVersions([...compareVersions, version.id]);
    }
  };

  const handleCompareVersions = () => {
    if (compareVersions.length === 2) {
      const version1 = versions.find(v => v.id === compareVersions[0]);
      const version2 = versions.find(v => v.id === compareVersions[1]);
      // Open comparison modal or navigate to comparison view
      console.log('Comparing versions:', version1, version2);
    }
  };

  const exportVersionHistory = async (format = 'csv') => {
    try {
      const exportData = await auditAPI.exportAuditLogs(workflowId, format);
      const blob = new Blob([exportData], {
        type: format === 'csv' ? 'text/csv' : 'application/json'
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `version_history_${workflowId}.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Export error:', err);
    }
  };

  // const getVersionIcon = action => { // Not currently used
  //   switch (action) {
  //     case 'CREATE':
  //       return <ClockCircleOutlined style={{ color: '#52c41a' }} />;
  //     case 'UPDATE':
  //       return <EditOutlined style={{ color: '#1890ff' }} />;
  //     case 'DELETE':
  //       return <ClockCircleOutlined style={{ color: '#ff4d4f' }} />;
  //     default:
  //       return <ClockCircleOutlined style={{ color: '#d9d9d9' }} />;
  //   }
  // };

  const getVersionColor = action => {
    switch (action) {
      case 'CREATE':
        return 'green';
      case 'UPDATE':
        return 'blue';
      case 'DELETE':
        return 'red';
      default:
        return 'default';
    }
  };

  const renderVersionDetails = version => {
    return (
      <Descriptions size="small" column={1}>
        <Descriptions.Item label="Version">{version.version}</Descriptions.Item>
        <Descriptions.Item label="Action">{version.action}</Descriptions.Item>
        <Descriptions.Item label="User">{version.username}</Descriptions.Item>
        <Descriptions.Item label="Timestamp">
          {new Date(version.timestamp || version.revisionDate).toLocaleString()}
        </Descriptions.Item>
        {version.fieldName && (
          <Descriptions.Item label="Field">{version.fieldName}</Descriptions.Item>
        )}
        {version.fieldValue && (
          <Descriptions.Item label="Value">{version.fieldValue}</Descriptions.Item>
        )}
        {version.previousValue && (
          <Descriptions.Item label="Previous Value">{version.previousValue}</Descriptions.Item>
        )}
        <Descriptions.Item label="Description">{version.description}</Descriptions.Item>
      </Descriptions>
    );
  };

  const columns = [
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      render: (version, record) => (
        <Space>
          <Tag color={record.isCurrent ? 'gold' : 'default'}>v{version}</Tag>
          {record.isCurrent && <Tag color="green">Current</Tag>}
        </Space>
      )
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      width: 100,
      render: action => <Tag color={getVersionColor(action)}>{action}</Tag>
    },
    {
      title: 'User',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: username => (
        <Space>
          <UserOutlined />
          {username}
        </Space>
      )
    },
    {
      title: 'Timestamp',
      key: 'timestamp',
      width: 180,
      render: (_, record) => new Date(record.timestamp || record.revisionDate).toLocaleString()
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewVersion(record)}
          >
            View
          </Button>
          {compareMode && (
            <Button
              type={compareVersions.includes(record.id) ? 'primary' : 'default'}
              size="small"
              onClick={() => handleVersionSelect(record)}
              disabled={!compareVersions.includes(record.id) && compareVersions.length >= 2}
            >
              {compareVersions.includes(record.id) ? 'Selected' : 'Select'}
            </Button>
          )}
        </Space>
      )
    }
  ];

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '20px' }}>
          <Spin size="large" />
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <Alert
          message="Error"
          description={error}
          type="error"
          showIcon
          action={
            <Button size="small" onClick={loadVersionHistory}>
              Retry
            </Button>
          }
        />
      </Card>
    );
  }

  return (
    <>
      <Card
        title={
          <Space>
            <HistoryOutlined />
            Version History
          </Space>
        }
        extra={
          <Space>
            <Button
              type={compareMode ? 'primary' : 'default'}
              size="small"
              icon={<DiffOutlined />}
              onClick={handleCompareToggle}
            >
              {compareMode ? 'Exit Compare' : 'Compare'}
            </Button>
            {compareMode && compareVersions.length === 2 && (
              <Button type="primary" size="small" onClick={handleCompareVersions}>
                Compare Selected
              </Button>
            )}
            <Button
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => exportVersionHistory('csv')}
            >
              Export CSV
            </Button>
            <Button size="small" onClick={() => exportVersionHistory('json')}>
              Export JSON
            </Button>
            <Button size="small" onClick={loadVersionHistory}>
              Refresh
            </Button>
          </Space>
        }
      >
        {versions.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
            No version history available
          </div>
        ) : (
          <Table
            columns={columns}
            dataSource={versions}
            rowKey="id"
            size="small"
            pagination={{
              pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} versions`
            }}
            rowSelection={
              compareMode
                ? {
                    selectedRowKeys: compareVersions,
                    onChange: selectedRowKeys => setCompareVersions(selectedRowKeys),
                    getCheckboxProps: () => ({
                      disabled: false
                    })
                  }
                : null
            }
          />
        )}
      </Card>

      <Modal
        title={`Version ${selectedVersion?.version} Details`}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setModalVisible(false)}>
            Close
          </Button>
        ]}
        width={600}
      >
        {selectedVersion && renderVersionDetails(selectedVersion)}
      </Modal>
    </>
  );
};

export default VersionHistory;
