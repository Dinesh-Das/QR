import {
  FormOutlined,
  EyeOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { Table, Button, Space, Tag, Progress, Input } from 'antd';
import PropTypes from 'prop-types';
import React, { useMemo, useCallback } from 'react';

import { PAGINATION } from '../../constants';

/**
 * WorkflowTable component displays workflows in a table format with sorting, filtering, and actions
 * Optimized with React.memo and performance optimizations
 */
const WorkflowTable = React.memo(({ 
  workflows, 
  loading, 
  onStartQuestionnaire, 
  onViewWorkflow,
  onRefresh 
}) => {
  /**
   * Get status color for workflow status tags
   */
  const getStatusColor = useCallback((status) => {
    const colors = {
      DRAFT: 'default',
      IN_PROGRESS: 'processing',
      COMPLETED: 'success'
    };
    return colors[status] || 'default';
  }, []);

  /**
   * Get completion color based on percentage
   */
  const getCompletionColor = useCallback((percentage) => {
    if (percentage === 100) return '#52c41a';
    if (percentage >= 75) return '#1890ff';
    if (percentage >= 50) return '#faad14';
    if (percentage >= 25) return '#fa8c16';
    return '#ff4d4f';
  }, []);

  /**
   * Calculate days in current state
   */
  const getDaysInState = useCallback((lastModified) => {
    if (!lastModified) return 0;
    const now = new Date();
    const modified = new Date(lastModified);
    const diffTime = Math.abs(now - modified);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }, []);

  /**
   * Handle start questionnaire action
   */
  const handleStartQuestionnaire = useCallback((workflow) => {
    if (onStartQuestionnaire) {
      onStartQuestionnaire(workflow);
    }
  }, [onStartQuestionnaire]);

  /**
   * Handle view workflow action
   */
  const handleViewWorkflow = useCallback((workflow) => {
    if (onViewWorkflow) {
      onViewWorkflow(workflow);
    } else {
      console.log('View workflow:', workflow.id);
    }
  }, [onViewWorkflow]);

  /**
   * Table columns configuration with memoization
   */
  const columns = useMemo(() => [
    {
      title: 'Material Code',
      dataIndex: 'materialCode',
      key: 'materialCode',
      width: 140,
      sorter: (a, b) => a.materialCode.localeCompare(b.materialCode),
      filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }) => (
        <div style={{ padding: 8 }}>
          <Input
            placeholder="Search Material Code"
            value={selectedKeys[0]}
            onChange={e => setSelectedKeys(e.target.value ? [e.target.value] : [])}
            onPressEnter={() => confirm()}
            style={{ width: 188, marginBottom: 8, display: 'block' }}
          />
          <Space>
            <Button
              type="primary"
              onClick={() => confirm()}
              icon={<SearchOutlined />}
              size="small"
              style={{ width: 90 }}
            >
              Search
            </Button>
            <Button onClick={() => clearFilters()} size="small" style={{ width: 90 }}>
              Reset
            </Button>
          </Space>
        </div>
      ),
      filterIcon: filtered => (
        <SearchOutlined style={{ color: filtered ? '#1890ff' : undefined }} />
      ),
      onFilter: (value, record) => record.materialCode.toLowerCase().includes(value.toLowerCase()),
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>{text}</div>
          <div style={{ fontSize: '12px', color: '#666' }}>
            {record.materialName || record.itemDescription || 'Material Name'}
          </div>
        </div>
      )
    },
    {
      title: 'Plant Code',
      dataIndex: 'plantCode',
      key: 'plantCode',
      width: 120,
      sorter: (a, b) => (a.plantCode || '').localeCompare(b.plantCode || ''),
      filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }) => (
        <div style={{ padding: 8 }}>
          <Input
            placeholder="Search Plant Code"
            value={selectedKeys[0]}
            onChange={e => setSelectedKeys(e.target.value ? [e.target.value] : [])}
            onPressEnter={() => confirm()}
            style={{ width: 188, marginBottom: 8, display: 'block' }}
          />
          <Space>
            <Button
              type="primary"
              onClick={() => confirm()}
              icon={<SearchOutlined />}
              size="small"
              style={{ width: 90 }}
            >
              Search
            </Button>
            <Button onClick={() => clearFilters()} size="small" style={{ width: 90 }}>
              Reset
            </Button>
          </Space>
        </div>
      ),
      filterIcon: filtered => (
        <SearchOutlined style={{ color: filtered ? '#1890ff' : undefined }} />
      ),
      onFilter: (value, record) =>
        (record.plantCode || '').toLowerCase().includes(value.toLowerCase()),
      render: text => (
        <Tag color="blue" style={{ fontWeight: 'bold' }}>
          {text || 'N/A'}
        </Tag>
      )
    },

    {
      title: 'Status',
      dataIndex: 'completionStatus',
      key: 'completionStatus',
      width: 120,
      filters: [
        { text: 'Draft', value: 'DRAFT' },
        { text: 'In Progress', value: 'IN_PROGRESS' },
        { text: 'Completed', value: 'COMPLETED' }
      ],
      onFilter: (value, record) => record.completionStatus === value,
      render: status => <Tag color={getStatusColor(status)}>{status.replace('_', ' ')}</Tag>
    },
    {
      title: 'Progress',
      key: 'progress',
      width: 150,
      sorter: (a, b) => a.completionPercentage - b.completionPercentage,
      render: (_, record) => {
        const percentage = record.completionPercentage || 0;
        return (
          <div>
            <Progress
              percent={percentage}
              size="small"
              strokeColor={getCompletionColor(percentage)}
              format={() => `${percentage}%`}
            />
            <div style={{ fontSize: '11px', color: '#666', marginTop: 2 }}>
              {record.completedFields || 0} / {record.totalFields || 0} fields
            </div>
          </div>
        );
      }
    },
    {
      title: 'Days Pending',
      key: 'daysPending',
      width: 100,
      sorter: (a, b) => getDaysInState(a.lastModified) - getDaysInState(b.lastModified),
      render: (_, record) => {
        const days = getDaysInState(record.lastModified);
        return <span style={{ color: days > 3 ? '#ff4d4f' : 'inherit' }}>{days}</span>;
      }
    },
    {
      title: 'Open Queries',
      dataIndex: 'openQueries',
      key: 'openQueries',
      width: 100,
      sorter: (a, b) => (a.openQueries || 0) - (b.openQueries || 0),
      render: count => (count > 0 ? <Tag color="red">{count}</Tag> : <Tag color="green">0</Tag>)
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<FormOutlined />}
            onClick={() => handleStartQuestionnaire(record)}
            disabled={record.isSubmitted || record.completionStatus === 'COMPLETED'}
          >
            {record.completionPercentage > 0 ? 'Continue' : 'Start'}
          </Button>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewWorkflow(record)}
          >
            View
          </Button>
        </Space>
      )
    }
  ], [getStatusColor, getCompletionColor, getDaysInState, handleStartQuestionnaire, handleViewWorkflow]);

  /**
   * Row class name for styling overdue rows
   */
  const getRowClassName = useCallback((record) => {
    const days = getDaysInState(record.lastModified);
    return days > 3 ? 'overdue-row' : '';
  }, [getDaysInState]);

  /**
   * Pagination configuration
   */
  const paginationConfig = useMemo(() => ({
    pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} materials`,
    pageSizeOptions: ['10', '20', '50', '100']
  }), []);

  return (
    <>
      <Table
        dataSource={workflows}
        columns={columns}
        loading={loading}
        rowKey="id"
        pagination={paginationConfig}
        rowClassName={getRowClassName}
        scroll={{ x: 1200 }}
        size="small"
        bordered
        title={() => (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 'bold' }}>
              Assigned Materials ({workflows.length})
            </span>
            {onRefresh && (
              <Button onClick={onRefresh} loading={loading} size="small">
                Refresh
              </Button>
            )}
          </div>
        )}
      />
      
      {/* Custom styles for overdue rows */}
      <style>{`
        .overdue-row {
          background-color: #fff2f0;
        }
        .overdue-row:hover {
          background-color: #ffebe6 !important;
        }
        .ant-table-tbody > tr.overdue-row > td {
          border-bottom: 1px solid #ffccc7;
        }
      `}</style>
    </>
  );
});

WorkflowTable.displayName = 'WorkflowTable';

WorkflowTable.propTypes = {
  workflows: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string.isRequired,
    materialCode: PropTypes.string.isRequired,
    plantCode: PropTypes.string,

    completionStatus: PropTypes.string.isRequired,
    completionPercentage: PropTypes.number,
    totalFields: PropTypes.number,
    completedFields: PropTypes.number,
    lastModified: PropTypes.string,
    openQueries: PropTypes.number,
    isSubmitted: PropTypes.bool,
    materialName: PropTypes.string,
    itemDescription: PropTypes.string
  })).isRequired,
  loading: PropTypes.bool,
  onStartQuestionnaire: PropTypes.func,
  onViewWorkflow: PropTypes.func,
  onRefresh: PropTypes.func
};

WorkflowTable.defaultProps = {
  loading: false,
  onStartQuestionnaire: null,
  onViewWorkflow: null,
  onRefresh: null
};

export default WorkflowTable;