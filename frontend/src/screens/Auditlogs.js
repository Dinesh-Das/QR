import {
  Typography,
  Table,
  Form,
  Input,
  Select,
  DatePicker,
  Button,
  Row,
  Col,
  message
} from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';

// eslint-disable-next-line no-undef
const { Title } = Typography;

const Auditlogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [filtering, setFiltering] = useState(false);

  const fetchLogs = async signal => {
    setLoading(true);
    try {
      const res = await apiClient.get('/audit/logs', { signal });
      if (!signal?.aborted) {
        // Ensure response is always an array
        const safeLogs = Array.isArray(res) ? res : [];
        setLogs(safeLogs);
      }
    } catch (err) {
      if (!signal?.aborted) {
        message.error('Failed to fetch audit logs');
        console.error('Error fetching audit logs:', err);
        // Set empty array on error
        setLogs([]);
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  };

  const fetchFilteredLogs = async filters => {
    setLoading(true);
    let url = '/audit/logs';
    if (filters) {
      if (filters.userId) {
        url = `/audit/logs/user/${filters.userId}`;
      } else if (filters.action) {
        url = `/audit/logs/action/${filters.action}`;
      } else if (filters.entityType) {
        url = `/audit/logs/entity/${filters.entityType}`;
      } else if (filters.severity) {
        url = `/audit/logs/severity/${filters.severity}`;
      } else if (filters.dateRange && filters.dateRange.length === 2) {
        const [start, end] = filters.dateRange;
        url = `/audit/logs/date?start=${start.toISOString()}&end=${end.toISOString()}`;
      }
    }
    try {
      const res = await apiClient.get(url);
      // Ensure response is always an array
      const safeLogs = Array.isArray(res) ? res : [];
      setLogs(safeLogs);
    } catch (err) {
      message.error('Failed to fetch audit logs');
      console.error('Error fetching filtered audit logs:', err);
      // Set empty array on error
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  const onFilter = values => {
    setFiltering(true);
    fetchFilteredLogs(values).then(() => setFiltering(false));
  };

  const onReset = () => {
    form.resetFields();
    fetchLogs();
  };

  useEffect(() => {
    const controller = new AbortController();

    fetchLogs(controller.signal);

    return () => {
      controller.abort();
    };
  }, []);

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      sorter: (a, b) => new Date(a.timestamp) - new Date(b.timestamp)
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user',
      sorter: (a, b) => a.user.localeCompare(b.user)
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      sorter: (a, b) => a.action.localeCompare(b.action)
    },
    {
      title: 'Details',
      dataIndex: 'details',
      key: 'details',
      width: '40%'
    }
  ];

  return (
    <div>
      <Title level={2}>Audit Logs</Title>
      <Form form={form} layout="vertical" onFinish={onFilter} style={{ marginBottom: 24 }}>
        <Row gutter={16}>
          <Col span={4}>
            <Form.Item name="userId" label="User ID">
              <Input placeholder="User ID" />
            </Form.Item>
          </Col>
          <Col span={4}>
            <Form.Item name="action" label="Action">
              <Input placeholder="Action" />
            </Form.Item>
          </Col>
          <Col span={4}>
            <Form.Item name="entityType" label="Entity">
              <Input placeholder="Entity Type" />
            </Form.Item>
          </Col>
          <Col span={4}>
            <Form.Item name="severity" label="Severity">
              <Select allowClear placeholder="Select severity">
                <Select.Option value="INFO">INFO</Select.Option>
                <Select.Option value="WARN">WARN</Select.Option>
                <Select.Option value="ERROR">ERROR</Select.Option>
                <Select.Option value="SECURITY">SECURITY</Select.Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="dateRange" label="Date Range">
              <DatePicker.RangePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={2} style={{ display: 'flex', alignItems: 'end' }}>
            <Button type="primary" htmlType="submit" loading={filtering}>
              Filter
            </Button>
          </Col>
          <Col span={2} style={{ display: 'flex', alignItems: 'end' }}>
            <Button onClick={onReset}>Reset</Button>
          </Col>
        </Row>
      </Form>
      <Table columns={columns} dataSource={logs} rowKey="id" loading={loading} bordered />
    </div>
  );
};

export default Auditlogs;
