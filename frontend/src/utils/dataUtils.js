/**
 * @fileoverview Utility functions for safe data handling in React components
 * Provides functions to ensure data is always in the expected format for Table components
 * 
 * @author QRMFG Development Team
 * @since 1.0.0
 */

/**
 * Ensures the provided data is always an array
 * This prevents the "G.some is not a function" error in Ant Design Table components
 * 
 * @param {any} data - The data to validate
 * @param {Array} fallback - Fallback array to use if data is not valid (default: [])
 * @returns {Array} Safe array that can be used with Table components
 * 
 * @example
 * ```javascript
 * const safeUsers = ensureArray(users);
 * const safeRoles = ensureArray(roles, []);
 * ```
 */
export const ensureArray = (data, fallback = []) => {
  if (Array.isArray(data)) {
    return data;
  }

  console.warn('Data is not an array, using fallback:', { data, fallback });
  return fallback;
};

/**
 * Safely processes API response data for Table components
 * Handles common API response patterns and ensures array output
 * 
 * @param {any} response - API response data
 * @param {string} [dataKey] - Key to extract array from response object
 * @param {Array} [fallback] - Fallback array if extraction fails
 * @returns {Array} Safe array for Table components
 * 
 * @example
 * ```javascript
 * // For direct array responses
 * const users = safeApiResponse(response);
 * 
 * // For nested data
 * const users = safeApiResponse(response, 'users');
 * const roles = safeApiResponse(response, 'data.roles');
 * ```
 */
export const safeApiResponse = (response, dataKey = null, fallback = []) => {
  if (!response) {
    return fallback;
  }

  // If no dataKey specified, expect direct array
  if (!dataKey) {
    return ensureArray(response, fallback);
  }

  // Handle nested keys like 'data.users'
  const keys = dataKey.split('.');
  let data = response;

  for (const key of keys) {
    if (data && typeof data === 'object' && key in data) {
      data = data[key];
    } else {
      console.warn(`Key '${key}' not found in response:`, response);
      return fallback;
    }
  }

  return ensureArray(data, fallback);
};

/**
 * Safely handles user roles data which can come in various formats
 * 
 * @param {any} roles - Roles data from API
 * @returns {Array} Array of role objects with consistent structure
 */
export const safeRolesData = (roles) => {
  const safeRoles = ensureArray(roles);

  return safeRoles.map(role => {
    if (typeof role === 'string') {
      return { name: role, id: role };
    }
    if (typeof role === 'object' && role !== null) {
      return {
        name: role.name || role.roleName || role.id || 'Unknown Role',
        id: role.id || role.name || role.roleName || Math.random().toString(36),
        ...role
      };
    }
    return { name: 'Unknown Role', id: Math.random().toString(36) };
  });
};

/**
 * Safely handles plant assignment data which can come in various formats
 * 
 * @param {any} plantData - Plant assignment data from API
 * @returns {Object} Normalized plant assignment object
 */
export const safePlantData = (plantData) => {
  if (!plantData) {
    return { assignedPlants: [], primaryPlant: null };
  }

  // If plantData is directly an array
  if (Array.isArray(plantData)) {
    return {
      assignedPlants: plantData,
      primaryPlant: plantData.length > 0 ? plantData[0] : null
    };
  }

  // If plantData is an object
  if (typeof plantData === 'object') {
    return {
      assignedPlants: ensureArray(plantData.assignedPlants || plantData.plants),
      primaryPlant: plantData.primaryPlant || plantData.primary || null
    };
  }

  return { assignedPlants: [], primaryPlant: null };
};

/**
 * Creates a safe error handler for API calls that sets empty arrays on error
 * 
 * @param {Function} setStateFunction - React setState function
 * @param {string} [errorMessage] - Custom error message
 * @returns {Function} Error handler function
 */
export const createSafeErrorHandler = (setStateFunction, errorMessage = 'Failed to fetch data') => {
  return (error, signal) => {
    if (!signal?.aborted) {
      console.error(errorMessage, error);
      setStateFunction([]);
    }
  };
};