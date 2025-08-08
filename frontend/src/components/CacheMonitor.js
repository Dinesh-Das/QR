import { 
  ReloadOutlined, 
  DeleteOutlined, 
  InfoCircleOutlined,
  BarChartOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { Card, Table, Button, Space, Statistic, Row, Col, Tag, Progress, Modal } from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';

/**
 * CacheMonitor component for debugging and monitoring API cache performance
 * Only available in development mode
 */
const CacheMonitor = () => {
  const [cacheStats, setCacheStats] = useState(null);
  const [cacheEntries, setCacheEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [selectedEntry, setSelectedEntry] = useState(null);

  // Only render in development mode
  if (process.env.NODE_ENV !== 'development') {
    return null;
  }

  /**
   * Load cache statistics and entries
   */
  const loadCacheData = async () => {
    setLoading(true);
    try {
      const stats = apiClient.getCacheStats();
      const entries = apiClient.getCacheInfo();
      
      setCacheStats(stats);
      setCacheEntries(entries);
    } catch (error) {
      console.error('Failed to load cache data:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Clear all cache entries
   */
  const handleClearCache = () => {
    apiClient.clearCache();
    loadCacheData();
  };

  /**
   * Show entry details
   */
  const showEntryDetails = (entry) => {
    setSelectedEntry(entry);
    setDetailsVisible(true);
  };

  /**
   * Format bytes to human readable format
   */
  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))  } ${  sizes[i]}`;
  };

  /**
   * Format remaining TTL
   */
  const formatTTL = (remainingTTL) => {
    if (remainingTTL <= 0) return 'Expired';
    
    const seconds = Math.floor(remainingTTL / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  };

  // Load data on component mount
  useEffect(() => {
    loadCacheData();
    
    // Auto-refresh every 5 seconds
    const interval = setInterval(loadCacheData, 5000);
    
    return () => clearInterval(interval);
  }, []);

  // Table columns for cache entries
  const columns = [
    {
      title: 'Cache Key',
      dataIndex: 'key',
      key: 'key',
      width: 300,
      render: (key) => (
        <div style={{ fontSize: '12px', fontFamily: 'monospace' }}>
          {key.length > 50 ? `${key.substring(0, 50)}...` : key}
        </div>
      )
    },
    {
      title: 'Size',
      dataIndex: 'size',
      key: 'size',
      width: 80,
      render: (size) => formatBytes(size),
      sorter: (a, b) => a.size - b.size
    },
    {
      title: 'TTL',
      dataIndex: 'remainingTTL',
      key: 'remainingTTL',
      width: 100,
      render: (remainingTTL, record) => (
        <div>
          <div>{formatTTL(remainingTTL)}</div>
          <Progress 
            percent={Math.max(0, (remainingTTL / record.ttl) * 100)} 
            size="small" 
            showInfo={false}
            strokeColor={remainingTTL > record.ttl * 0.5 ? '#52c41a' : remainingTTL > record.ttl * 0.2 ? '#faad14' : '#ff4d4f'}
          />
        </div>
      ),
      sorter: (a, b) => a.remainingTTL - b.remainingTTL
    },
    {
      title: 'Status',
      dataIndex: 'isExpired',
      key: 'isExpired',
      width: 80,
      render: (isExpired) => (
        <Tag color={isExpired ? 'red' : 'green'}>
          {isExpired ? 'Expired' : 'Active'}
        </Tag>
      ),
      filters: [
        { text: 'Active', value: false },
        { text: 'Expired', value: true }
      ],
      onFilter: (value, record) => record.isExpired === value
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (createdAt) => new Date(createdAt).toLocaleTimeString(),
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt)
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      render: (_, record) => (
        <Button 
          size="small" 
          icon={<InfoCircleOutlined />}
          onClick={() => showEntryDetails(record)}
        >
          Details
        </Button>
      )
    }
  ];

  return (
    <div style={{ padding: '16px' }}>
      <Card 
        title="API Cache Monitor" 
        extra={
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={loadCacheData}
              loading={loading}
            >
              Refresh
            </Button>
            <Button 
              icon={<DeleteOutlined />} 
              onClick={handleClearCache}
              danger
            >
              Clear Cache
            </Button>
          </Space>
        }
      >
        {/* Cache Statistics */}
        {cacheStats && (
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={4}>
              <Statistic
                title="Cache Size"
                value={cacheStats.cacheSize}
                prefix={<BarChartOutlined />}
              />
            </Col>
            <Col span={4}>
              <Statistic
                title="Hit Rate"
                value={cacheStats.hitRate}
                suffix="%"
                precision={1}
                valueStyle={{ color: cacheStats.hitRate > 50 ? '#3f8600' : '#cf1322' }}
              />
            </Col>
            <Col span={4}>
              <Statistic
                title="Total Requests"
                value={cacheStats.totalRequests}
              />
            </Col>
            <Col span={4}>
              <Statistic
                title="Cache Hits"
                value={cacheStats.hits}
                valueStyle={{ color: '#3f8600' }}
              />
            </Col>
            <Col span={4}>
              <Statistic
                title="Cache Misses"
                value={cacheStats.misses}
                valueStyle={{ color: '#cf1322' }}
              />
            </Col>
            <Col span={4}>
              <Statistic
                title="Memory Usage"
                value={cacheStats.memoryUsage?.kb || 0}
                suffix="KB"
                precision={1}
              />
            </Col>
          </Row>
        )}

        {/* Cache Entries Table */}
        <Table
          columns={columns}
          dataSource={cacheEntries}
          rowKey="key"
          size="small"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} entries`
          }}
          loading={loading}
          scroll={{ x: 800 }}
        />
      </Card>

      {/* Entry Details Modal */}
      <Modal
        title="Cache Entry Details"
        open={detailsVisible}
        onCancel={() => setDetailsVisible(false)}
        footer={null}
        width={800}
      >
        {selectedEntry && (
          <div>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={12}>
                <Card size="small" title="Basic Info">
                  <p><strong>Key:</strong> <code>{selectedEntry.key}</code></p>
                  <p><strong>Size:</strong> {formatBytes(selectedEntry.size)}</p>
                  <p><strong>TTL:</strong> {formatTTL(selectedEntry.remainingTTL)}</p>
                  <p><strong>Status:</strong> 
                    <Tag color={selectedEntry.isExpired ? 'red' : 'green'} style={{ marginLeft: 8 }}>
                      {selectedEntry.isExpired ? 'Expired' : 'Active'}
                    </Tag>
                  </p>
                </Card>
              </Col>
              <Col span={12}>
                <Card size="small" title="Timestamps">
                  <p><strong>Created:</strong> {new Date(selectedEntry.createdAt).toLocaleString()}</p>
                  <p><strong>Expires:</strong> {new Date(selectedEntry.expiresAt).toLocaleString()}</p>
                  <p><strong>Original TTL:</strong> {Math.round(selectedEntry.ttl / 1000)}s</p>
                  <p><strong>Remaining:</strong> {formatTTL(selectedEntry.remainingTTL)}</p>
                </Card>
              </Col>
            </Row>
            
            <Card size="small" title="Progress">
              <Progress 
                percent={Math.max(0, (selectedEntry.remainingTTL / selectedEntry.ttl) * 100)}
                strokeColor={
                  selectedEntry.remainingTTL > selectedEntry.ttl * 0.5 
                    ? '#52c41a' 
                    : selectedEntry.remainingTTL > selectedEntry.ttl * 0.2 
                    ? '#faad14' 
                    : '#ff4d4f'
                }
                format={(percent) => `${Math.round(percent)}% remaining`}
              />
            </Card>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default CacheMonitor;