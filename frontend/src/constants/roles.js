/**
 * Role constants that match the backend RoleConstants.java
 */
export const ROLES = {
  ADMIN: 'ADMIN',
  JVC_USER: 'JVC_USER',
  PLANT_USER: 'PLANT_USER',
  CQS_USER: 'CQS_USER',
  TECH_USER: 'TECH_USER',
  VIEWER: 'VIEWER'
};

/**
 * Role types for backend compatibility
 */
export const ROLE_TYPES = {
  ADMIN: 'ADMIN',
  JVC_ROLE: 'JVC_ROLE',
  PLANT_ROLE: 'PLANT_ROLE',
  CQS_ROLE: 'CQS_ROLE',
  TECH_ROLE: 'TECH_ROLE',
  VIEWER_ROLE: 'VIEWER_ROLE'
};

/**
 * Role display names
 */
export const ROLE_DISPLAY_NAMES = {
  [ROLES.ADMIN]: 'Administrator',
  [ROLES.JVC_USER]: 'JVC User',
  [ROLES.PLANT_USER]: 'Plant User',
  [ROLES.CQS_USER]: 'CQS User',
  [ROLES.TECH_USER]: 'Technical User',
  [ROLES.VIEWER]: 'Viewer'
};

/**
 * Role descriptions
 */
export const ROLE_DESCRIPTIONS = {
  [ROLES.ADMIN]: 'Full system access with administrative privileges',
  [ROLES.JVC_USER]: 'Joint Venture Company user with workflow initiation rights',
  [ROLES.PLANT_USER]: 'Plant-specific user with questionnaire completion rights',
  [ROLES.CQS_USER]: 'Corporate Quality Services user with query management rights',
  [ROLES.TECH_USER]: 'Technical user with system monitoring and audit access',
  [ROLES.VIEWER]: 'Read-only access to assigned content'
};

/**
 * Role hierarchy (higher number = more privileges)
 */
export const ROLE_HIERARCHY = {
  [ROLES.VIEWER]: 1,
  [ROLES.PLANT_USER]: 2,
  [ROLES.CQS_USER]: 3,
  [ROLES.JVC_USER]: 3,
  [ROLES.TECH_USER]: 4,
  [ROLES.ADMIN]: 5
};

/**
 * Screen access mapping by role
 */
export const SCREEN_ACCESS = {
  [ROLES.ADMIN]: [
    '/qrmfg',
    '/qrmfg/dashboard',
    '/qrmfg/admin',
    '/qrmfg/jvc',
    '/qrmfg/cqs',
    '/qrmfg/tech',
    '/qrmfg/plant',
    '/qrmfg/workflows',
    '/qrmfg/workflow-monitoring',
    '/qrmfg/reports',
    '/qrmfg/users',
    '/qrmfg/roles',
    '/qrmfg/sessions',
    '/qrmfg/user-role-management',
    '/qrmfg/auditlogs',
    '/qrmfg/settings'
  ],
  [ROLES.JVC_USER]: [
    '/qrmfg',
    '/qrmfg/dashboard',
    '/qrmfg/jvc',
    '/qrmfg/workflows',
    '/qrmfg/reports',
    '/qrmfg/settings'
  ],
  [ROLES.CQS_USER]: [
    '/qrmfg',
    '/qrmfg/dashboard',
    '/qrmfg/cqs',
    '/qrmfg/workflows',
    '/qrmfg/reports',
    '/qrmfg/settings'
  ],
  [ROLES.TECH_USER]: [
    '/qrmfg',
    '/qrmfg/dashboard',
    '/qrmfg/tech',
    '/qrmfg/workflows',
    '/qrmfg/workflow-monitoring',
    '/qrmfg/reports',
    '/qrmfg/auditlogs',
    '/qrmfg/settings'
  ],
  [ROLES.PLANT_USER]: [
    '/qrmfg',
    '/qrmfg/dashboard',
    '/qrmfg/plant',
    '/qrmfg/workflows',
    '/qrmfg/reports',
    '/qrmfg/settings'
  ],
  [ROLES.VIEWER]: [
    '/qrmfg',
    '/qrmfg/dashboard',
    '/qrmfg/settings'
  ]
};

/**
 * Data access permissions by role
 */
export const DATA_ACCESS = {
  [ROLES.ADMIN]: ['*'], // All data
  [ROLES.JVC_USER]: ['workflow', 'document', 'query', 'jvc'],
  [ROLES.CQS_USER]: ['workflow', 'document', 'query', 'cqs'],
  [ROLES.TECH_USER]: ['workflow', 'document', 'query', 'audit', 'system', 'tech'],
  [ROLES.PLANT_USER]: ['workflow', 'document', 'query', 'plant'],
  [ROLES.VIEWER]: ['workflow:read', 'document:read']
};

/**
 * Action permissions by role
 */
export const ACTION_PERMISSIONS = {
  [ROLES.ADMIN]: ['*'], // All actions
  [ROLES.JVC_USER]: ['create', 'read', 'update', 'initiate'],
  [ROLES.CQS_USER]: ['create', 'read', 'update', 'respond'],
  [ROLES.TECH_USER]: ['create', 'read', 'update', 'monitor', 'audit'],
  [ROLES.PLANT_USER]: ['read', 'update', 'complete'],
  [ROLES.VIEWER]: ['read']
};

/**
 * Helper functions
 */
export const getRoleDisplayName = (role) => {
  return ROLE_DISPLAY_NAMES[role] || role;
};

export const getRoleDescription = (role) => {
  return ROLE_DESCRIPTIONS[role] || 'No description available';
};

export const getRoleHierarchy = (role) => {
  return ROLE_HIERARCHY[role] || 0;
};

export const hasHigherPrivilege = (role1, role2) => {
  return getRoleHierarchy(role1) > getRoleHierarchy(role2);
};

export const getScreensForRole = (role) => {
  return SCREEN_ACCESS[role] || [];
};

export const getDataAccessForRole = (role) => {
  return DATA_ACCESS[role] || [];
};

export const getActionPermissionsForRole = (role) => {
  return ACTION_PERMISSIONS[role] || [];
};

export const canAccessScreen = (role, screen) => {
  const screens = getScreensForRole(role);
  return screens.includes('*') || screens.includes(screen);
};

export const canAccessData = (role, dataType) => {
  const dataAccess = getDataAccessForRole(role);
  return dataAccess.includes('*') || 
         dataAccess.includes(dataType) || 
         dataAccess.some(access => access.startsWith(`${dataType}:`));
};

export const canPerformAction = (role, action) => {
  const actions = getActionPermissionsForRole(role);
  return actions.includes('*') || actions.includes(action);
};