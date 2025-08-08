import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { Table, Button } from 'antd';
import PropTypes from 'prop-types';
import React, { useMemo, useCallback } from 'react';

/**
 * UserTable Component
 * 
 * A performant, accessible table component for displaying and managing users.
 * Features include sorting, editing, deletion, and plant assignment display.
 * 
 * @component
 * @example
 * ```jsx
 * const users = [
 *   { id: '1', username: 'john', email: 'john@example.com', roles: ['USER'], plantAssignments: ['PLANT1'] }
 * ];
 * 
 * <UserTable
 *   users={users}
 *   loading={false}
 *   onEdit={(user) => console.log('Edit user:', user)}
 *   onDelete={(userId) => console.log('Delete user:', userId)}
 *   availablePlants={[{ value: 'PLANT1', label: 'Plant 1' }]}
 * />
 * ```
 * 
 * @param {Object} props - Component props
 * @param {Array<Object>} props.users - Array of user objects to display
 * @param {string} props.users[].id - Unique user identifier
 * @param {string} props.users[].username - User's username
 * @param {string} props.users[].email - User's email address
 * @param {Array<string>} props.users[].roles - User's assigned roles
 * @param {Array<string>} props.users[].plantAssignments - User's plant assignments
 * @param {boolean} props.loading - Whether the table is in loading state
 * @param {Function} props.onEdit - Callback function when edit button is clicked
 * @param {Function} props.onDelete - Callback function when delete button is clicked
 * @param {Array<Object>} [props.availablePlants=[]] - Available plants for display mapping
 * @param {string} props.availablePlants[].value - Plant code
 * @param {string} props.availablePlants[].label - Plant display name
 * 
 * @returns {React.ReactElement} Rendered UserTable component
 * 
 * @since 1.0.0
 * @author QRMFG Development Team
 */
const UserTable = React.memo(({ 
  users, 
  loading, 
  onEdit, 
  onDelete, 
  availablePlants = [] 
}) => {
  // Memoized function to get plant description by code
  const getPlantDescription = useCallback((plantCode) => {
    const plant = availablePlants.find(p => p.value === plantCode);
    return plant ? plant.label : plantCode;
  }, [availablePlants]);

  // Memoized column definitions
  const columns = useMemo(() => [
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username',
      sorter: (a, b) => a.username.localeCompare(b.username),
      'aria-label': 'Username column'
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      sorter: (a, b) => a.email.localeCompare(b.email),
      'aria-label': 'Email column'
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      'aria-label': 'Roles column',
      render: (roles) => {
        // Ensure roles is always an array
        const rolesArray = Array.isArray(roles) ? roles : [];
        const roleNames = rolesArray
          .map(role => role?.name || role || '')
          .filter(Boolean)
          .join(', ');
          
        return roleNames || 'No roles assigned';
      }
    },
    {
      title: 'Assigned Plants',
      key: 'plants',
      'aria-label': 'Assigned plants column',
      render: (_, record) => {
        // Handle multiple data sources for plant assignments
        let assignedPlants = [];
        let primaryPlant = null;
        
        // Check for plantAssignments (from hook)
        if (record.plantAssignments) {
          if (Array.isArray(record.plantAssignments)) {
            assignedPlants = record.plantAssignments;
          } else if (record.plantAssignments.assignedPlants && Array.isArray(record.plantAssignments.assignedPlants)) {
            assignedPlants = record.plantAssignments.assignedPlants;
            primaryPlant = record.plantAssignments.primaryPlant;
          }
        }
        
        // Check for assignedPlants directly from backend (comma-separated string)
        if (assignedPlants.length === 0 && record.assignedPlants) {
          if (Array.isArray(record.assignedPlants)) {
            assignedPlants = record.assignedPlants;
          } else if (typeof record.assignedPlants === 'string' && record.assignedPlants.trim()) {
            // Parse comma-separated string
            assignedPlants = record.assignedPlants.split(',').map(plant => plant.trim()).filter(Boolean);
          }
          primaryPlant = record.primaryPlant;
        }
        
        if (!assignedPlants || assignedPlants.length === 0) {
          return <span style={{ color: '#999' }} aria-label="No plants assigned">None</span>;
        }

        const plants = assignedPlants
          .map(plantCode => getPlantDescription(plantCode))
          .join(', ');

        return (
          <div>
            <div aria-label={`Assigned plants: ${plants}`}>{plants}</div>
            {primaryPlant && (
              <div 
                style={{ fontSize: '11px', color: '#666' }}
                aria-label={`Primary plant: ${getPlantDescription(primaryPlant)}`}
              >
                Primary: {getPlantDescription(primaryPlant)}
              </div>
            )}
          </div>
        );
      }
    },
    {
      title: 'Actions',
      key: 'actions',
      'aria-label': 'Actions column',
      render: (_, record) => (
        <div role="group" aria-label={`Actions for user ${record.username}`}>
          <Button 
            type="link" 
            icon={<EditOutlined />} 
            onClick={() => onEdit(record)}
            aria-label={`Edit user ${record.username}`}
            data-testid={`edit-button-${record.id}`}
          >
            Edit
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => onDelete(record.id)}
            aria-label={`Delete user ${record.username}`}
            data-testid={`delete-button-${record.id}`}
          >
            Delete
          </Button>
        </div>
      )
    }
  ], [onEdit, onDelete, getPlantDescription]);

  // Memoized table props with safety checks
  const tableProps = useMemo(() => {
    // Ensure users is always an array
    const safeUsers = Array.isArray(users) ? users : [];
    
    return {
      columns,
      dataSource: safeUsers,
      rowKey: 'id', // Use user.id instead of array index for better performance
      loading,
      pagination: {
        pageSize: 10,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total, range) => 
          `${range[0]}-${range[1]} of ${total} users`,
        'aria-label': 'User table pagination'
      },
      scroll: { x: 800 }, // Enable horizontal scroll on small screens
      size: 'middle',
      'aria-label': 'Users table',
      'data-testid': 'users-table'
    };
  }, [columns, users, loading]);

  // Show empty state when no users
  const safeUsers = Array.isArray(users) ? users : [];
  if (!loading && safeUsers.length === 0) {
    return (
      <div 
        style={{ 
          textAlign: 'center', 
          padding: '40px 0', 
          color: '#999' 
        }}
        role="status"
        aria-label="No users found"
      >
        No users found. Click "Add User" to create the first user.
      </div>
    );
  }

  return (
    <Table 
      {...tableProps}
      locale={{
        emptyText: 'No users found'
      }}
    />
  );
});

UserTable.displayName = 'UserTable';

UserTable.propTypes = {
  users: PropTypes.array, // Made more flexible to handle null/undefined gracefully
  loading: PropTypes.bool,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  availablePlants: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired
  }))
};

UserTable.defaultProps = {
  users: [],
  loading: false,
  availablePlants: []
};

export default UserTable;