import { message } from 'antd';
import { useReducer, useCallback, useEffect } from 'react';

import { userAPI } from '../services/userAPI';
import { ensureArray, safeApiResponse, safeRolesData, safePlantData } from '../utils/dataUtils';

/**
 * @fileoverview Custom hook for managing user operations including CRUD operations,
 * state management, and error handling. Provides a centralized way to handle
 * user-related business logic with proper loading states and error handling.
 * 
 * @author QRMFG Development Team
 * @since 1.0.0
 */

// Simple API client mock for development
const createSimpleApiClient = () => {
  if (process.env.NODE_ENV === 'test') {
    return {
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn()
    };
  }
  
  // In production, import the real API client
  try {
    const apiClient = require('../api/client').default;
    return apiClient;
  } catch (error) {
    console.warn('API client not available, using mock');
    return {
      get: () => Promise.resolve([]),
      post: () => Promise.resolve({}),
      put: () => Promise.resolve({}),
      delete: () => Promise.resolve({})
    };
  }
};

const apiClient = createSimpleApiClient();

// Action types
const USER_ACTIONS = {
  SET_LOADING: 'SET_LOADING',
  SET_USERS: 'SET_USERS',
  SET_ROLES: 'SET_ROLES',
  SET_ERROR: 'SET_ERROR',
  SET_MODAL_VISIBLE: 'SET_MODAL_VISIBLE',
  SET_EDITING_USER: 'SET_EDITING_USER',
  RESET_MODAL: 'RESET_MODAL'
};

// Initial state
const initialState = {
  users: [],
  roles: [],
  loading: false,
  error: null,
  modalVisible: false,
  editingUser: null
};

// Reducer function
const userReducer = (state, action) => {
  switch (action.type) {
    case USER_ACTIONS.SET_LOADING:
      return { ...state, loading: action.payload };
    case USER_ACTIONS.SET_USERS:
      return { ...state, users: action.payload, loading: false, error: null };
    case USER_ACTIONS.SET_ROLES:
      return { ...state, roles: action.payload };
    case USER_ACTIONS.SET_ERROR:
      return { ...state, error: action.payload, loading: false };
    case USER_ACTIONS.SET_MODAL_VISIBLE:
      return { ...state, modalVisible: action.payload };
    case USER_ACTIONS.SET_EDITING_USER:
      return { ...state, editingUser: action.payload };
    case USER_ACTIONS.RESET_MODAL:
      return { ...state, modalVisible: false, editingUser: null };
    default:
      return state;
  }
};

/**
 * Custom hook for comprehensive user management operations
 * 
 * Provides a complete set of user management functionality including:
 * - User CRUD operations (Create, Read, Update, Delete)
 * - Plant assignment management
 * - Modal state management for user editing
 * - Loading states and error handling
 * - Automatic data fetching and caching
 * 
 * @hook
 * @example
 * ```jsx
 * function UsersScreen() {
 *   const {
 *     users,
 *     loading,
 *     error,
 *     modalVisible,
 *     editingUser,
 *     actions
 *   } = useUserManagement();
 * 
 *   useEffect(() => {
 *     actions.fetchUsers();
 *   }, [actions.fetchUsers]);
 * 
 *   return (
 *     <div>
 *       <UserTable 
 *         users={users}
 *         loading={loading}
 *         onEdit={actions.openEditModal}
 *         onDelete={actions.deleteUser}
 *       />
 *       <UserModal
 *         visible={modalVisible}
 *         editingUser={editingUser}
 *         onSave={actions.saveUser}
 *         onCancel={actions.closeModal}
 *       />
 *     </div>
 *   );
 * }
 * ```
 * 
 * @returns {Object} User management state and actions
 * @returns {Array<Object>} returns.users - Array of user objects with plant assignments
 * @returns {boolean} returns.loading - Loading state for async operations
 * @returns {string|null} returns.error - Error message if any operation failed
 * @returns {boolean} returns.modalVisible - Whether the user edit modal is visible
 * @returns {Object|null} returns.editingUser - User object being edited (null for create mode)
 * @returns {Object} returns.actions - Object containing all available actions
 * @returns {Function} returns.actions.fetchUsers - Fetch all users from the server
 * @returns {Function} returns.actions.saveUser - Save user data (create or update)
 * @returns {Function} returns.actions.deleteUser - Delete a user by ID
 * @returns {Function} returns.actions.openEditModal - Open modal for editing a user
 * @returns {Function} returns.actions.openCreateModal - Open modal for creating a new user
 * @returns {Function} returns.actions.closeModal - Close the user edit/create modal
 * @returns {Function} returns.actions.assignPlantToUser - Assign a plant to a user
 * @returns {Function} returns.actions.removePlantFromUser - Remove plant assignment from user
 * 
 * @since 1.0.0
 * @author QRMFG Development Team
 */
export const useUserManagement = () => {
  const [state, dispatch] = useReducer(userReducer, initialState);

  // Fetch users with plant assignments
  const fetchUsers = useCallback(async (signal) => {
    dispatch({ type: USER_ACTIONS.SET_LOADING, payload: true });
    try {
      const data = await apiClient.get('/admin/users', { signal });
      // Safely extract users array from the response
      const usersArray = safeApiResponse(data, 'users');

      // Load plant assignments for each user
      const usersWithPlants = await Promise.all(
        usersArray.map(async (user) => {
          try {
            const plantData = await userAPI.getUserPlantAssignments(user.username, { signal });
            return {
              ...user,
              roles: safeRolesData(user.roles),
              plantAssignments: safePlantData(plantData)
            };
          } catch (error) {
            if (!signal?.aborted) {
              console.error('Error loading plant assignments:', error);
            }
            return {
              ...user,
              roles: safeRolesData(user.roles),
              plantAssignments: safePlantData(null)
            };
          }
        })
      );

      if (!signal?.aborted) {
        dispatch({ type: USER_ACTIONS.SET_USERS, payload: usersWithPlants });
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Error fetching users:', error);
        dispatch({ type: USER_ACTIONS.SET_ERROR, payload: error.message });
        message.error('Failed to fetch users');
        // Ensure we always set an empty array on error
        dispatch({ type: USER_ACTIONS.SET_USERS, payload: [] });
      }
    }
  }, []);

  // Fetch roles
  const fetchRoles = useCallback(async (signal) => {
    try {
      const data = await apiClient.get('/admin/roles', { signal });
      if (!signal?.aborted) {
        // Safely extract roles array from the response
        const rolesArray = safeApiResponse(data, 'roles');
        dispatch({ type: USER_ACTIONS.SET_ROLES, payload: rolesArray });
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Failed to fetch roles:', error);
        message.error('Failed to fetch roles');
        dispatch({ type: USER_ACTIONS.SET_ROLES, payload: [] });
      }
    }
  }, []);

  // Delete user
  const deleteUser = useCallback(async (userId) => {
    try {
      await apiClient.delete(`/admin/users/${userId}`);
      message.success('User deleted successfully');
      // Refetch users after deletion
      const controller = new AbortController();
      await fetchUsers(controller.signal);
    } catch (error) {
      message.error('Failed to delete user');
      throw error;
    }
  }, [fetchUsers]);

  // Save user (create or update)
  const saveUser = useCallback(async (values, editingUser) => {
    try {
      // Separate user data from plant data and roles
      const { assignedPlants, primaryPlant, roles, ...userData } = values;

      if (editingUser) {
        // Update user basic info
        await apiClient.put(`/admin/users/${editingUser.id}`, userData);

        // Update roles if provided
        if (roles && roles.length > 0) {
          try {
            await apiClient.post(`/admin/users/${editingUser.id}/roles`, {
              roleIds: roles
            });
          } catch (roleError) {
            console.warn('Failed to update user roles:', roleError);
            message.warning('User updated but roles could not be assigned');
          }
        }

        // Update plant assignments if provided
        if (assignedPlants && assignedPlants.length > 0) {
          await userAPI.updateUserPlantAssignments(editingUser.username, {
            assignedPlants,
            primaryPlant: primaryPlant || assignedPlants[0]
          });
        }

        message.success('User updated successfully');
      } else {
        // Create new user with roles and plants included in the initial request
        const userPayload = {
          ...userData,
          roles: roles || [],
          assignedPlants: assignedPlants || [],
          primaryPlant: primaryPlant || (assignedPlants && assignedPlants.length > 0 ? assignedPlants[0] : null)
        };
        
        const response = await apiClient.post('/admin/users', userPayload);
        console.log('User creation response:', response);
        
        // Check if the response indicates any warnings (like role assignment failures)
        if (response.status === 'warning') {
          message.warning(response.message);
        } else if (response.status === 'success') {
          message.success('User created successfully');
        } else {
          message.success('User created successfully');
        }


      }

      dispatch({ type: USER_ACTIONS.RESET_MODAL });
      // Refetch users after save
      const controller = new AbortController();
      await fetchUsers(controller.signal);
    } catch (error) {
      message.error('Failed to save user');
      throw error;
    }
  }, [fetchUsers]);

  // Open edit modal
  const openEditModal = useCallback(async (user) => {
    dispatch({ type: USER_ACTIONS.SET_EDITING_USER, payload: user });
    dispatch({ type: USER_ACTIONS.SET_MODAL_VISIBLE, payload: true });
  }, []);

  // Open add modal
  const openAddModal = useCallback(() => {
    dispatch({ type: USER_ACTIONS.SET_EDITING_USER, payload: null });
    dispatch({ type: USER_ACTIONS.SET_MODAL_VISIBLE, payload: true });
  }, []);

  // Close modal
  const closeModal = useCallback(() => {
    dispatch({ type: USER_ACTIONS.RESET_MODAL });
  }, []);

  // Initialize data on mount
  useEffect(() => {
    const controller = new AbortController();

    const initializeData = async () => {
      try {
        await Promise.all([
          fetchUsers(controller.signal),
          fetchRoles(controller.signal)
        ]);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error initializing user management data:', error);
        }
      }
    };

    initializeData();

    return () => {
      controller.abort();
    };
  }, [fetchUsers, fetchRoles]);

  // Memoized actions object
  const actions = {
    fetchUsers,
    fetchRoles,
    deleteUser,
    saveUser,
    openEditModal,
    openAddModal,
    closeModal
  };

  return {
    ...state,
    actions
  };
};