import {
  ClockCircleOutlined,
  UserOutlined,
  FileTextOutlined,
  MessageOutlined,
  CheckCircleOutlined,
  EditOutlined
} from '@ant-design/icons';
import { Timeline, Card, Tag, Spin, Alert, Button, Space, Collapse } from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import { auditAPI } from '../services/auditAPI';

// Hook to detect screen size
const useResponsive = () => {
  const [screenSize, setScreenSize] = useState({
    isMobile: window.innerWidth <= 768,
    isTablet: window.innerWidth > 768 && window.innerWidth <= 1024,
    isDesktop: window.innerWidth > 1024
  });

  useEffect(() => {
    const handleResize = () => {
      setScreenSize({
        isMobile: window.innerWidth <= 768,
        isTablet: window.innerWidth > 768 && window.innerWidth <= 1024,
        isDesktop: window.innerWidth > 1024
      });
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return screenSize;
};

const { Panel } = Collapse;

const AuditTimeline = ({ workflowId, entityType = 'workflow' }) => {
  const [loading, setLoading] = useState(true);
  const [auditHistory, setAuditHistory] = useState([]);
  const [error, setError] = useState(null);
  const [expandedItems, setExpandedItems] = useState(new Set());
  const { isMobile } = useResponsive();

  useEffect(() => {
    if (workflowId) {
      loadAuditHistory();
    }
  }, [workflowId, entityType, loadAuditHistory]);

  const loadAuditHistory = useCallback(async () => {
    try {
      setLoading(true);
      let history;

      switch (entityType) {
        case 'workflow':
          history = await auditAPI.getWorkflowAuditHistory(workflowId);
          break;
        case 'query':
          history = await auditAPI.getQueryAuditHistory(workflowId);
          break;
        case 'complete':
          history = await auditAPI.getCompleteWorkflowAuditTrail(workflowId);
          break;
        default:
          history = await auditAPI.getWorkflowAuditHistory(workflowId);
      }

      setAuditHistory(formatAuditHistory(history));
    } catch (err) {
      setError('Failed to load audit history');
      console.error('Audit history error:', err);
    } finally {
      setLoading(false);
    }
  }, [workflowId, entityType, formatAuditHistory]);

  const formatAuditHistory = useCallback(rawHistory => {
    return rawHistory.map((entry, index) => ({
      id: index,
      timestamp: entry.timestamp || entry.revisionDate,
      user: entry.username || entry.modifiedBy || 'System',
      action: entry.revisionType || entry.action || 'UPDATE',
      entityType: entry.entityType || 'MaterialWorkflow',
      entityId: entry.entityId || entry.id,
      changes: entry.changes || [],
      description: entry.description || generateDescription(entry),
      details: entry.details || entry
    }));
  }, []);

  const generateDescription = entry => {
    const action = entry.revisionType || entry.action;
    const entityType = entry.entityType || 'workflow';

    switch (action) {
      case 'ADD':
      case 'CREATE':
        return `Created new ${entityType.toLowerCase()}`;
      case 'MOD':
      case 'UPDATE':
        return `Updated ${entityType.toLowerCase()}`;
      case 'DEL':
      case 'DELETE':
        return `Deleted ${entityType.toLowerCase()}`;
      case 'STATE_CHANGE':
        return `Changed workflow state to ${entry.newState}`;
      case 'QUERY_CREATED':
        return `Created new query`;
      case 'QUERY_RESOLVED':
        return `Resolved query`;
      default:
        return `Modified ${entityType.toLowerCase()}`;
    }
  };

  const getActionIcon = action => {
    switch (action) {
      case 'ADD':
      case 'CREATE':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'MOD':
      case 'UPDATE':
        return <EditOutlined style={{ color: '#1890ff' }} />;
      case 'DEL':
      case 'DELETE':
        return <ClockCircleOutlined style={{ color: '#ff4d4f' }} />;
      case 'STATE_CHANGE':
        return <FileTextOutlined style={{ color: '#722ed1' }} />;
      case 'QUERY_CREATED':
      case 'QUERY_RESOLVED':
        return <MessageOutlined style={{ color: '#fa8c16' }} />;
      default:
        return <ClockCircleOutlined style={{ color: '#d9d9d9' }} />;
    }
  };

  const getActionColor = action => {
    switch (action) {
      case 'ADD':
      case 'CREATE':
        return 'green';
      case 'MOD':
      case 'UPDATE':
        return 'blue';
      case 'DEL':
      case 'DELETE':
        return 'red';
      case 'STATE_CHANGE':
        return 'purple';
      case 'QUERY_CREATED':
      case 'QUERY_RESOLVED':
        return 'orange';
      default:
        return 'default';
    }
  };

  const toggleExpanded = itemId => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedItems(newExpanded);
  };

  const formatTimestamp = timestamp => {
    if (!timestamp) {
      return 'Unknown time';
    }
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  const renderChangeDetails = changes => {
    if (!changes || changes.length === 0) {
      return null;
    }

    return (
      <div style={{ marginTop: 8 }}>
        <strong>Changes:</strong>
        <ul style={{ marginTop: 4, marginBottom: 0 }}>
          {changes.map((change, index) => (
            <li key={index}>
              <strong>{change.field}:</strong> {change.oldValue} → {change.newValue}
            </li>
          ))}
        </ul>
      </div>
    );
  };

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '20px' }}>
          <Spin size="large" data-testid="loading-spinner" />
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
            <Button size="small" onClick={loadAuditHistory}>
              Retry
            </Button>
          }
        />
      </Card>
    );
  }

  return (
    <Card
      title="Audit Timeline"
      extra={
        <Space>
          <Button size="small" onClick={loadAuditHistory}>
            {isMobile ? 'Refresh' : 'Refresh'}
          </Button>
        </Space>
      }
    >
      {auditHistory.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
          No audit history available
        </div>
      ) : (
        <Timeline
          mode={isMobile ? 'left' : 'left'}
          className={isMobile ? 'audit-timeline-mobile' : ''}
        >
          {auditHistory.map(entry => (
            <Timeline.Item key={entry.id} dot={getActionIcon(entry.action)}>
              <div>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: isMobile ? 4 : 8,
                    marginBottom: 4,
                    flexWrap: isMobile ? 'wrap' : 'nowrap'
                  }}
                >
                  <Tag color={getActionColor(entry.action)} size={isMobile ? 'small' : 'default'}>
                    {isMobile && entry.action.length > 6
                      ? entry.action.substring(0, 6)
                      : entry.action}
                  </Tag>
                  <span
                    style={{
                      fontWeight: 'bold',
                      fontSize: isMobile ? '13px' : '14px'
                    }}
                  >
                    {entry.description}
                  </span>
                </div>

                <div
                  style={{
                    fontSize: isMobile ? '11px' : '12px',
                    color: '#666',
                    marginBottom: 8,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                    flexWrap: isMobile ? 'wrap' : 'nowrap'
                  }}
                >
                  <UserOutlined />
                  <span>{entry.user}</span>
                  <span>•</span>
                  <span>
                    {isMobile
                      ? formatTimestamp(entry.timestamp).split(',')[0]
                      : formatTimestamp(entry.timestamp)}
                  </span>
                </div>

                {entry.changes && entry.changes.length > 0 && (
                  <div>
                    <Button
                      type="link"
                      size="small"
                      onClick={() => toggleExpanded(entry.id)}
                      style={{
                        padding: 0,
                        height: 'auto',
                        fontSize: isMobile ? '12px' : '14px'
                      }}
                    >
                      {expandedItems.has(entry.id) ? 'Hide' : 'Show'} Details
                    </Button>

                    {expandedItems.has(entry.id) && (
                      <Collapse ghost>
                        <Panel header="" key="1" showArrow={false}>
                          {renderChangeDetails(entry.changes)}
                          {entry.details && (
                            <div style={{ marginTop: 8 }}>
                              <strong>Additional Details:</strong>
                              <pre
                                style={{
                                  fontSize: isMobile ? '10px' : '11px',
                                  background: '#f5f5f5',
                                  padding: isMobile ? '6px' : '8px',
                                  borderRadius: '4px',
                                  marginTop: '4px',
                                  overflow: 'auto',
                                  maxHeight: isMobile ? '150px' : '200px'
                                }}
                              >
                                {JSON.stringify(entry.details, null, 2)}
                              </pre>
                            </div>
                          )}
                        </Panel>
                      </Collapse>
                    )}
                  </div>
                )}
              </div>
            </Timeline.Item>
          ))}
        </Timeline>
      )}
    </Card>
  );
};

export default AuditTimeline;
