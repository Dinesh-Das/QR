import apiClient from '../api/client';
import { getToken, isAuthenticated } from '../services/auth';

/**
 * User API service providing user management functionality
 * Migrated to use unified APIClient with standardized error handling
 *
 * @namespace userAPI
 */
export const userAPI = {
  /**
   * Get current user's plant assignments from backend API
   * @param {string} username - Username to get plant assignments for
   * @param {Object} options - Additional options including signal for AbortController
   * @returns {Promise<Object>} Plant assignment data
   */
  getUserPlantAssignments: async (username, options = {}) => {
    try {
      console.log('Getting plant assignments for user:', username);
      console.log('Is authenticated:', isAuthenticated());

      // Check if user is authenticated first
      if (!isAuthenticated()) {
        console.warn('User is not authenticated, cannot get plant assignments');
        return {
          assignedPlants: [],
          primaryPlant: null,
          effectivePlant: null,
          isPlantUser: false
        };
      }

      // Primary method: Get plant info from JWT token validation
      const token = getToken();
      console.log('Retrieved token:', token ? 'Token found' : 'No token found');

      if (token) {
        try {
          const validationResponse = await apiClient.post('/auth/validate', { token }, options);
          console.log('Token validation response:', validationResponse);
          console.log('Plant codes in token:', validationResponse.plantCodes);
          console.log('Primary plant in token:', validationResponse.primaryPlant);
          console.log('Is plant user:', validationResponse.isPlantUser);

          if (validationResponse.valid) {
            // Check if we have plant data in the token
            if (validationResponse.plantCodes && validationResponse.plantCodes.length > 0) {
              console.log('Plant assignments from token:', validationResponse);
              return {
                assignedPlants: validationResponse.plantCodes || [],
                primaryPlant: validationResponse.primaryPlant || null,
                effectivePlant: validationResponse.primaryPlant || (validationResponse.plantCodes && validationResponse.plantCodes[0]) || null,
                isPlantUser: validationResponse.isPlantUser || false
              };
            }

            // Handle case where user has primaryPlant but empty plantCodes array
            if (validationResponse.primaryPlant && validationResponse.isPlantUser) {
              console.log('User has primary plant but empty plantCodes, using primary plant as assigned plant:', validationResponse.primaryPlant);
              return {
                assignedPlants: [validationResponse.primaryPlant],
                primaryPlant: validationResponse.primaryPlant,
                effectivePlant: validationResponse.primaryPlant,
                isPlantUser: validationResponse.isPlantUser || false
              };
            }

            // If no plant data in token but token is valid, user might not have plants assigned
            console.log('Token is valid but no plant data found in token');
          }
        } catch (tokenError) {
          console.warn('Failed to validate token for plant assignments:', tokenError);
        }
      }

      // Fallback: try to get from stored user data (check multiple storage keys)
      const possibleKeys = ['userData', 'qrmfg_user_data', 'user_info'];
      for (const key of possibleKeys) {
        const storedUserData = localStorage.getItem(key) || sessionStorage.getItem(key);
        if (storedUserData) {
          try {
            const userData = JSON.parse(storedUserData);
            if (userData.plantCodes || userData.assignedPlants) {
              console.log('Plant assignments from stored data:', userData);
              return {
                assignedPlants: userData.plantCodes || userData.assignedPlants || [],
                primaryPlant: userData.primaryPlant || null,
                effectivePlant: userData.primaryPlant || ((userData.plantCodes || userData.assignedPlants) && (userData.plantCodes || userData.assignedPlants)[0]) || null,
                isPlantUser: userData.isPlantUser || false
              };
            }
          } catch (parseError) {
            console.warn(`Failed to parse stored user data from ${key}:`, parseError);
          }
        }
      }

      // Last resort: Try admin endpoints only if user might have admin access
      try {
        // Only try admin endpoints if we think the user might be an admin
        const adminToken = getToken();
        const validationResponse = await apiClient.post('/auth/validate', { token: adminToken }, options);
        if (validationResponse.valid && validationResponse.isAdmin) {
          console.log('User appears to be admin, trying admin endpoints...');

          const userDetails = await apiClient.get('/admin/users', options);
          const currentUser = userDetails.users?.find(user => user.username === username);

          if (currentUser && currentUser.assignedPlants) {
            console.log('Admin endpoint: Using user details for plant assignments:', currentUser);

            return {
              assignedPlants: currentUser.assignedPlants || [],
              primaryPlant: currentUser.primaryPlant || null,
              effectivePlant: currentUser.primaryPlant || (currentUser.assignedPlants && currentUser.assignedPlants[0]) || null,
              isPlantUser: (currentUser.assignedPlants && currentUser.assignedPlants.length > 0) || false
            };
          }
        }
      } catch (adminError) {
        console.warn('Admin endpoints not accessible (expected for non-admin users):', adminError.message);
      }

      // If no plant data available, return empty structure
      console.warn('No plant assignment data available for user:', username);
      return {
        assignedPlants: [],
        primaryPlant: null,
        effectivePlant: null,
        isPlantUser: false
      };
    } catch (error) {
      console.error('Error getting user plant assignments:', error);
      // Return empty structure on error to prevent crashes
      return {
        assignedPlants: [],
        primaryPlant: null,
        effectivePlant: null,
        isPlantUser: false
      };
    }
  },

  /**
   * Update user's plant assignments
   * @param {string} username - Username to update plant assignments for
   * @param {Object} plantData - Plant assignment data
   * @returns {Promise<Object>} Updated plant assignments
   */
  updateUserPlantAssignments: (username, plantData) =>
    apiClient.put(`/admin/users/${encodeURIComponent(username)}/plants`, plantData),

  /**
   * Check if user is assigned to a specific plant
   * @param {string} username - Username to check
   * @param {string} plantCode - Plant code to check assignment for
   * @returns {Promise<boolean>} True if user is assigned to plant
   */
  checkUserPlantAssignment: (username, plantCode) =>
    apiClient.get(
      `/admin/users/${encodeURIComponent(username)}/plants/${encodeURIComponent(plantCode)}/check`
    ),

  /**
   * Get user information by ID
   * @param {string} id - User ID
   * @returns {Promise<Object>} User information
   */
  getUserById: id => apiClient.get(`/admin/users/${id}`),

  /**
   * Get all users (cached for 5 minutes)
   * @param {Object} options - Additional options including signal for AbortController
   * @returns {Promise<Array>} Array of all users
   */
  getAllUsers: (options = {}) => apiClient.get('/admin/users', {
    cacheTTL: 5 * 60 * 1000, // Cache for 5 minutes
    ...options
  }),

  /**
   * Get all users without cache (for real-time data)
   * @param {Object} options - Additional options including signal for AbortController
   * @returns {Promise<Array>} Array of all users
   */
  getAllUsersRealTime: (options = {}) => apiClient.get('/admin/users', {
    useCache: false,
    ...options
  })
};
