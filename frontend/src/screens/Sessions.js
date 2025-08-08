import { Table, Button, Typography } from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';

const { Title } = Typography;

const Sessions = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchSessions = async signal => {
    setLoading(true);
    try {
      const response = await apiClient.get('/admin/sessions', { signal });
      if (!signal?.aborted) {
        // Ensure response is always an array
        const safeSessions = Array.isArray(response) ? response : [];
        setSessions(safeSessions);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch sessions:', error);
        // Set empty array on error
        setSessions([]);
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    const controller = new AbortController();

    fetchSessions(controller.signal);

    return () => {
      controller.abort();
    };
  }, []);

  const handleTerminate = async sessionId => {
    try {
      await apiClient.delete(`/admin/sessions/${sessionId}`);
      fetchSessions();
    } catch (error) {
      console.error('Failed to terminate session:', error);
    }
  };

  const columns = [
    {
      title: 'Session ID',
      dataIndex: 'id',
      key: 'id'
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user'
    },
    {
      title: 'Start Time',
      dataIndex: 'startTime',
      key: 'startTime'
    },
    {
      title: 'Last Activity',
      dataIndex: 'lastActivity',
      key: 'lastActivity'
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button type="primary" danger onClick={() => handleTerminate(record.id)}>
          Terminate
        </Button>
      )
    }
  ];

  return (
    <div>
      <Title level={2}>Active Sessions</Title>
      <Table columns={columns} dataSource={sessions} rowKey="id" loading={loading} />
    </div>
  );
};

export default Sessions;
