import apiClient from '../api/client';

import { 
  getCurrentUser, 
  getPrimaryRoleType, 
  getUserPlantCodes, 
  getPrimaryPlantCode,
  isAdmin,
  isJvcUser,
  isCqsUser,
  isTechUser,
  isPlantUser,
  isViewer
} from './auth';

/**
 * RBAC Service for frontend role-based access control
 * Integrates with backend RBAC implementation
 */
class RBACService {
  
  /**
   * Gets user's access summary from backend
   * @returns {Promise<Object>} User access summary
   */
  static async getUserAccessSummary() {
    try {
      const response = await apiClient.get('/rbac/user/access-summary');
      return response.data || response;
    } catch (error) {
      console.error('Failed to get user access summary:', error);
      return this.getFallbackAccessSummary();
    }
  }

  /**
   * Checks if user has access to a specific screen
   * @param {string} screenRoute - Screen route to check
   * @returns {Promise<boolean>} True if user has access
   */
  static async hasScreenAccess(screenRoute) {
    try {
      const response = await apiClient.get(`/rbac/screen-access?route=${encodeURIComponent(screenRoute)}`);
      return response.data?.hasAccess || response.hasAccess || false;
    } catch (error) {
      console.warn('Backend screen access check failed, using fallback:', error);
      return this.getFallbackScreenAccess(screenRoute);
    }
  }

  /**
   * Gets list of accessible screens from backend
   * @returns {Promise<Array>} Array of accessible screen routes
   */
  static async getAccessibleScreens() {
    try {
      const response = await apiClient.get('/rbac/accessible-screens');
      return response.data?.screens || response.screens || response || [];
    } catch (error) {
      console.warn('Backend accessible screens check failed, using fallback:', error);
      return this.getFallbackAccessibleScreens();
    }
  }

  /**
   * Checks if user has access to specific data type
   * @param {string} dataType - Data type to check
   * @param {Object} context - Additional context (e.g., plantCode)
   * @returns {Promise<boolean>} True if user has access
   */
  static async hasDataAccess(dataType, context = {}) {
    try {
      const response = await apiClient.post('/rbac/data-access', {
        dataType,
        context
      });
      return response.data?.hasAccess || response.hasAccess || false;
    } catch (error) {
      console.warn('Backend data access check failed, using fallback:', error);
      return this.getFallbackDataAccess(dataType, context);
    }
  }

  /**
   * Checks if user has access to plant-specific data
   * @param {string} dataType - Data type to check
   * @param {string} plantCode - Plant code to check
   * @param {Object} context - Additional context
   * @returns {Promise<boolean>} True if user has access
   */
  static async hasPlantDataAccess(dataType, plantCode, context = {}) {
    try {
      const response = await apiClient.post('/rbac/plant-data-access', {
        dataType,
        plantCode,
        context
      });
      return response.data?.hasAccess || response.hasAccess || false;
    } catch (error) {
      console.warn('Backend plant data access check failed, using fallback:', error);
      return this.getFallbackPlantDataAccess(dataType, plantCode, context);
    }
  }

  /**
   * Makes an access decision for a resource
   * @param {string} resourceType - Type of resource
   * @param {string} resourceId - Resource identifier
   * @param {string} action - Action to perform
   * @param {Object} context - Additional context
   * @returns {Promise<Object>} Access decision object
   */
  static async makeAccessDecision(resourceType, resourceId, action, context = {}) {
    try {
      const response = await apiClient.post('/rbac/access-decision', {
        resourceType,
        resourceId,
        action,
        context
      });
      return response.data || response;
    } catch (error) {
      console.warn('Backend access decision failed, using fallback:', error);
      return this.getFallbackAccessDecision(resourceType, resourceId, action, context);
    }
  }

  /**
   * Filters data based on user's plant access
   * @param {Array} data - Data array to filter
   * @param {Function} plantExtractor - Function to extract plant code from data item
   * @returns {Array} Filtered data array
   */
  static filterDataByPlantAccess(data, plantExtractor) {
    if (!Array.isArray(data)) {
      return [];
    }

    if (isAdmin()) {
      return data; // Admin sees all data
    }

    if (!isPlantUser()) {
      return data; // Non-plant users see all data (no plant filtering)
    }

    const userPlants = getUserPlantCodes();
    if (userPlants.length === 0) {
      return []; // No plant access
    }

    return data.filter(item => {
      const plantCode = plantExtractor(item);
      return plantCode && userPlants.includes(plantCode);
    });
  }

  /**
   * Gets user's role-based navigation items
   * @returns {Array} Array of navigation items user can access
   */
  static getNavigationItems() {
    const baseItems = [
      { key: '/qrmfg', icon: 'HomeOutlined', label: 'Home', path: '/qrmfg' },
      { key: '/qrmfg/dashboard', icon: 'DashboardOutlined', label: 'Dashboard', path: '/qrmfg/dashboard' }
    ];

    const roleBasedItems = [];

    // Admin gets all items
    if (isAdmin()) {
      roleBasedItems.push(
        { key: '/qrmfg/admin', icon: 'UserOutlined', label: 'Admin Panel', path: '/qrmfg/admin' },
        { key: '/qrmfg/users', icon: 'UserOutlined', label: 'Users', path: '/qrmfg/users' },
        { key: '/qrmfg/roles', icon: 'KeyOutlined', label: 'Roles', path: '/qrmfg/roles' },
        { key: '/qrmfg/sessions', icon: 'ClockCircleOutlined', label: 'Sessions', path: '/qrmfg/sessions' },
        { key: '/qrmfg/user-role-management', icon: 'UsergroupAddOutlined', label: 'User Role Management', path: '/qrmfg/user-role-management' },
        { key: '/qrmfg/auditlogs', icon: 'AuditOutlined', label: 'Audit Logs', path: '/qrmfg/auditlogs' },
        { key: '/qrmfg/api-test', icon: 'ApiOutlined', label: 'API Test', path: '/qrmfg/api-test' }
      );
    }

    // Role-specific items
    if (isJvcUser() || isAdmin()) {
      roleBasedItems.push({ key: '/qrmfg/jvc', icon: 'AppstoreOutlined', label: 'JVC', path: '/qrmfg/jvc' });
    }

    if (isCqsUser() || isAdmin()) {
      roleBasedItems.push({ key: '/qrmfg/cqs', icon: 'SafetyOutlined', label: 'CQS', path: '/qrmfg/cqs' });
    }

    if (isTechUser() || isAdmin()) {
      roleBasedItems.push(
        { key: '/qrmfg/tech', icon: 'TeamOutlined', label: 'TECH', path: '/qrmfg/tech' },
        { key: '/qrmfg/workflow-monitoring', icon: 'MonitorOutlined', label: 'Workflow Monitoring', path: '/qrmfg/workflow-monitoring' }
      );
    }

    if (isPlantUser() || isAdmin()) {
      roleBasedItems.push({ key: '/qrmfg/plant', icon: 'BankOutlined', label: 'PLANT', path: '/qrmfg/plant' });
    }

    // Shared items based on role
    if ((isJvcUser() || isCqsUser() || isTechUser() || isPlantUser()) || isAdmin()) {
      roleBasedItems.push({ key: '/qrmfg/workflows', icon: 'AppstoreOutlined', label: 'Workflows', path: '/qrmfg/workflows' });
    }

    if (!isViewer() || isAdmin()) {
      roleBasedItems.push({ key: '/qrmfg/reports', icon: 'FileSearchOutlined', label: 'Reports', path: '/qrmfg/reports' });
    }

    // Settings accessible to all
    const settingsItems = [
      { key: '/qrmfg/settings', icon: 'SettingOutlined', label: 'Settings', path: '/qrmfg/settings' }
    ];

    return [...baseItems, ...roleBasedItems, ...settingsItems];
  }

  // Fallback methods for when backend is unavailable

  static getFallbackAccessSummary() {
    const currentUser = getCurrentUser();
    const primaryRole = getPrimaryRoleType();
    const plantCodes = getUserPlantCodes();
    const primaryPlant = getPrimaryPlantCode();

    return {
      username: currentUser,
      primaryRole,
      allRoles: [primaryRole].filter(Boolean),
      isAdmin: isAdmin(),
      isPlantUser: isPlantUser(),
      assignedPlants: plantCodes,
      primaryPlant,
      accessibleScreens: this.getFallbackAccessibleScreens(),
      restrictions: {
        plantFiltering: isPlantUser(),
        screenRestrictions: !isAdmin(),
        dataRestrictions: !isAdmin()
      }
    };
  }

  static getFallbackScreenAccess(screenRoute) {
    if (isAdmin()) return true;

    const accessMap = {
      '/': true,
      '/dashboard': true,
      '/settings': true,
      '/admin': isAdmin(),
      '/users': isAdmin(),
      '/roles': isAdmin(),
      '/sessions': isAdmin(),
      '/user-role-management': isAdmin(),
      '/auditlogs': isAdmin() || isTechUser(),
      '/api-test': isAdmin() || isTechUser(),
      '/jvc': isJvcUser() || isAdmin(),
      '/cqs': isCqsUser() || isAdmin(),
      '/tech': isTechUser() || isAdmin(),
      '/plant': isPlantUser() || isAdmin(),
      '/workflows': isJvcUser() || isCqsUser() || isTechUser() || isPlantUser() || isAdmin(),
      '/workflow-monitoring': isTechUser() || isAdmin(),
      '/reports': !isViewer() || isAdmin()
    };

    return accessMap[screenRoute] || false;
  }

  static getFallbackAccessibleScreens() {
    const screens = ['/', '/dashboard', '/settings'];

    if (isAdmin()) {
      return [
        ...screens,
        '/admin',
        '/users',
        '/roles',
        '/sessions',
        '/user-role-management',
        '/auditlogs',
        '/api-test',
        '/jvc',
        '/cqs',
        '/tech',
        '/plant',
        '/workflows',
        '/workflow-monitoring',
        '/reports'
      ];
    }

    if (isJvcUser()) screens.push('/jvc', '/workflows');
    if (isCqsUser()) screens.push('/cqs', '/workflows');
    if (isTechUser()) screens.push('/tech', '/workflows', '/workflow-monitoring', '/auditlogs', '/api-test');
    if (isPlantUser()) screens.push('/plant', '/workflows');
    if (!isViewer()) screens.push('/reports');

    return [...new Set(screens)];
  }

  static getFallbackDataAccess(dataType, context) {
    if (isAdmin()) return true;

    // Basic data access rules
    const dataAccessMap = {
      'workflow': isJvcUser() || isCqsUser() || isTechUser() || isPlantUser(),
      'query': isCqsUser() || isTechUser() || isPlantUser(),
      'document': isJvcUser() || isCqsUser() || isTechUser() || isPlantUser(),
      'user': isAdmin(),
      'role': isAdmin(),
      'audit': isAdmin() || isTechUser(),
      'report': !isViewer()
    };

    return dataAccessMap[dataType] || false;
  }

  static getFallbackPlantDataAccess(dataType, plantCode, context) {
    if (isAdmin()) return true;
    if (!isPlantUser()) return this.getFallbackDataAccess(dataType, context);

    const userPlants = getUserPlantCodes();
    return userPlants.includes(plantCode);
  }

  static getFallbackAccessDecision(resourceType, resourceId, action, context) {
    const hasAccess = this.getFallbackDataAccess(resourceType, context);
    
    return {
      granted: hasAccess,
      message: hasAccess ? 'Access granted based on role' : 'Access denied based on role',
      details: {
        userRole: getPrimaryRoleType(),
        resourceType,
        resourceId,
        action,
        fallback: true
      }
    };
  }
}

export default RBACService;