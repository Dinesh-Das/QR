import { Table, Tabs, Button, Typography, message } from 'antd';
import React, { useEffect, useState } from 'react';

import apiClient from '../api/client';

const { Title } = Typography;
const { TabPane } = Tabs;

const exportToCSV = (data, filename) => {
  if (!data || !data.length) {
    return;
  }
  const keys = Object.keys(data[0]);
  const csv = [keys.join(',')]
    .concat(data.map(row => keys.map(k => JSON.stringify(row[k] ?? '')).join(',')))
    .join('\n');
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
};

const Reports = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [activity, setActivity] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    const fetchReports = async () => {
      setLoading(true);
      try {
        const [usersResponse, rolesResponse, activityResponse] = await Promise.all([
          apiClient.get('/reports/users', { signal: controller.signal }),
          apiClient.get('/reports/roles', { signal: controller.signal }),
          apiClient.get('/reports/activity', { signal: controller.signal })
        ]);

        if (!controller.signal.aborted) {
          // Ensure all responses are arrays
          setUsers(Array.isArray(usersResponse) ? usersResponse : []);
          setRoles(Array.isArray(rolesResponse) ? rolesResponse : []);
          setActivity(Array.isArray(activityResponse) ? activityResponse : []);
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          message.error('Failed to fetch reports');
          console.error('Error fetching reports:', error);
          // Set empty arrays on error
          setUsers([]);
          setRoles([]);
          setActivity([]);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchReports();

    return () => {
      controller.abort();
    };
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Reports</Title>
      <Tabs defaultActiveKey="users">
        <TabPane tab="Users" key="users">
          <Button
            onClick={() => exportToCSV(users, 'users_report.csv')}
            style={{ marginBottom: 8 }}
          >
            Export CSV
          </Button>
          <Table dataSource={users} rowKey="id" loading={loading} bordered scroll={{ x: true }} />
        </TabPane>
        <TabPane tab="Roles" key="roles">
          <Button
            onClick={() => exportToCSV(roles, 'roles_report.csv')}
            style={{ marginBottom: 8 }}
          >
            Export CSV
          </Button>
          <Table dataSource={roles} rowKey="id" loading={loading} bordered scroll={{ x: true }} />
        </TabPane>
        <TabPane tab="Activity" key="activity">
          <Button
            onClick={() => exportToCSV(activity, 'activity_report.csv')}
            style={{ marginBottom: 8 }}
          >
            Export CSV
          </Button>
          <Table
            dataSource={activity}
            rowKey="id"
            loading={loading}
            bordered
            scroll={{ x: true }}
          />
        </TabPane>
      </Tabs>
    </div>
  );
};

export default Reports;
