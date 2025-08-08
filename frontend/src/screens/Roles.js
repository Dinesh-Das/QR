import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { Table, Button, Form, Input, Modal, message, Space, Popconfirm, Typography } from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';

const { Title } = Typography;

const Roles = () => {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [form] = Form.useForm();

  const fetchRoles = async signal => {
    setLoading(true);
    try {
      const response = await apiClient.get('/admin/roles', { signal });
      if (!signal?.aborted) {
        // Ensure response is always an array
        const safeRoles = Array.isArray(response) ? response : [];
        setRoles(safeRoles);
      }
    } catch (error) {
      if (!signal?.aborted) {
        message.error('Failed to fetch roles');
        console.error('Error fetching roles:', error);
        // Set empty array on error
        setRoles([]);
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    const controller = new AbortController();

    fetchRoles(controller.signal);

    return () => {
      controller.abort();
    };
  }, []);

  const handleAdd = () => {
    setEditingRole(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = role => {
    setEditingRole(role);
    form.setFieldsValue(role);
    setModalVisible(true);
  };

  const handleDelete = async id => {
    try {
      await apiClient.delete(`/admin/roles/${id}`);
      message.success('Role deleted successfully');
      fetchRoles();
    } catch (error) {
      message.error('Failed to delete role');
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      if (editingRole) {
        await apiClient.put(`/admin/roles/${editingRole.id}`, values);
        message.success('Role updated successfully');
      } else {
        await apiClient.post('/admin/roles', values);
        message.success('Role created successfully');
      }
      setModalVisible(false);
      form.resetFields();
      fetchRoles();
    } catch (error) {
      if (!error.errorFields) {
        message.error('Failed to save role');
      }
    }
  };

  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description'
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm
            title="Are you sure you want to delete this role?"
            onConfirm={() => handleDelete(record.id)}
            okText="Yes"
            cancelText="No"
          >
            <Button icon={<DeleteOutlined />} danger />
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Title level={2}>Roles</Title>
      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={handleAdd}
        style={{ marginBottom: 16 }}
      >
        Add Role
      </Button>
      <Table columns={columns} dataSource={roles} rowKey="id" loading={loading} />
      <Modal
        title={editingRole ? 'Edit Role' : 'Add Role'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Name"
            rules={[{ required: true, message: 'Please input the role name!' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="description"
            label="Description"
            rules={[{ required: true, message: 'Please input the role description!' }]}
          >
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Roles;
