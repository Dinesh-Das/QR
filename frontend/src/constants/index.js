// Application Constants

// API Configuration
export const API_CONFIG = {
  TIMEOUT: 30000, // 30 seconds
  BASE_URL: process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1',
  RETRY_ATTEMPTS: 3
};

// Authentication
export const AUTH_CONFIG = {
  TOKEN_KEY: 'qrmfg_secure_token',
  LOGIN_PATH: '/qrmfg/login',
  DEFAULT_REDIRECT: '/qrmfg'
};

// UI Constants
export const UI_CONFIG = {
  MOBILE_BREAKPOINT: 768,
  TABLET_BREAKPOINT: 1024,
  PAGE_SIZE: 10,
  DEBOUNCE_DELAY: 300
};

// Workflow States
export const WORKFLOW_STATES = {
  INITIATED: 'INITIATED',
  IN_PROGRESS: 'IN_PROGRESS',
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  COMPLETED: 'COMPLETED'
};

// Query States
export const QUERY_STATES = {
  OPEN: 'OPEN',
  IN_PROGRESS: 'IN_PROGRESS',
  RESOLVED: 'RESOLVED',
  CLOSED: 'CLOSED'
};

// Priority Levels
export const PRIORITY_LEVELS = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  URGENT: 'URGENT',
  CRITICAL: 'CRITICAL'
};

// Teams
export const TEAMS = {
  CQS: 'CQS',
  TECH: 'TECH',
  JVC: 'JVC'
};

// Error Severity
export const ERROR_SEVERITY = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL'
};

// Error Categories
export const ERROR_CATEGORY = {
  APPLICATION: 'APPLICATION',
  COMPONENT: 'COMPONENT',
  API: 'API',
  ROUTE: 'ROUTE',
  ASYNC: 'ASYNC',
  AUTHENTICATION: 'AUTHENTICATION'
};

// Status Colors
export const STATUS_COLORS = {
  [WORKFLOW_STATES.INITIATED]: 'blue',
  [WORKFLOW_STATES.IN_PROGRESS]: 'orange',
  [WORKFLOW_STATES.PENDING_REVIEW]: 'purple',
  [WORKFLOW_STATES.APPROVED]: 'green',
  [WORKFLOW_STATES.REJECTED]: 'red',
  [WORKFLOW_STATES.COMPLETED]: 'green'
};

// Priority Colors
export const PRIORITY_COLORS = {
  [PRIORITY_LEVELS.LOW]: 'green',
  [PRIORITY_LEVELS.MEDIUM]: 'blue',
  [PRIORITY_LEVELS.HIGH]: 'orange',
  [PRIORITY_LEVELS.URGENT]: 'red',
  [PRIORITY_LEVELS.CRITICAL]: 'red'
};

// Form Validation
export const VALIDATION = {
  MIN_PASSWORD_LENGTH: 8,
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_FILE_TYPES: ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.jpg', '.jpeg', '.png'],
  MAX_COMMENT_LENGTH: 1000,
  MAX_DESCRIPTION_LENGTH: 500
};

// Date Formats
export const DATE_FORMATS = {
  DISPLAY: 'DD/MM/YYYY',
  DISPLAY_WITH_TIME: 'DD/MM/YYYY HH:mm',
  API: 'YYYY-MM-DD',
  ISO: 'YYYY-MM-DDTHH:mm:ss.SSSZ'
};

// Local Storage Keys
export const STORAGE_KEYS = {
  USER_PREFERENCES: 'qrmfg_user_preferences',
  THEME: 'qrmfg_theme',
  LANGUAGE: 'qrmfg_language'
};

// Animation Delays
export const ANIMATION = {
  FADE_IN_DELAY: 0.1, // seconds
  STAGGER_DELAY: 0.05, // seconds
  TRANSITION_DURATION: 0.3 // seconds
};

// Notification Types
export const NOTIFICATION_TYPES = {
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info'
};

// Component Names (for error boundaries)
export const COMPONENT_NAMES = {
  USER_TABLE: 'UserTable',
  USER_MODAL: 'UserModal',
  PLANT_ASSIGNMENT_FORM: 'PlantAssignmentForm',
  PLANT_DASHBOARD: 'PlantDashboard',
  WORKFLOW_TABLE: 'WorkflowTable',
  FILTER_PANEL: 'FilterPanel'
};

// File size constants
export const FILE_SIZE = {
  MAX_UPLOAD_SIZE: 25 * 1024 * 1024, // 25MB
  BYTES_PER_KB: 1024,
  BYTES_PER_MB: 1024 * 1024,
  BYTES_PER_GB: 1024 * 1024 * 1024
};

// Workflow specific states
export const WORKFLOW_SPECIFIC_STATES = {
  JVC_PENDING: 'JVC_PENDING',
  PLANT_PENDING: 'PLANT_PENDING',
  CQS_PENDING: 'CQS_PENDING',
  TECH_PENDING: 'TECH_PENDING',
  DRAFT: 'DRAFT',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED'
};

// Query status
export const QUERY_STATUS = {
  OPEN: 'OPEN',
  RESOLVED: 'RESOLVED',
  CLOSED: 'CLOSED'
};

// Query status groups for filtering
export const QUERY_STATUS_GROUPS = {
  ACTIVE: ['OPEN'],
  INACTIVE: ['RESOLVED', 'CLOSED']
};

// Team names
export const TEAM_NAMES = {
  CQS: 'CQS',
  TECH: 'TECH',
  JVC: 'JVC',
  PLANT: 'PLANT'
};

// Auto-save intervals
export const AUTO_SAVE = {
  INTERVAL: 30000, // 30 seconds
  DEBOUNCE_DELAY: 2000 // 2 seconds
};

// Pagination defaults
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  SMALL_PAGE_SIZE: 5,
  LARGE_PAGE_SIZE: 15,
  SHOW_SIZE_CHANGER: true,
  SHOW_QUICK_JUMPER: true
};

// Completion filter options
export const COMPLETION_FILTERS = {
  ALL: 'all',
  COMPLETED: 'completed',
  IN_PROGRESS: 'in-progress',
  NOT_STARTED: 'not-started'
};

// Tab keys
export const TAB_KEYS = {
  INITIATE: 'initiate',
  PENDING: 'pending',
  COMPLETED: 'completed',
  QUERIES: 'queries',
  HISTORY: 'history'
};

// Re-export role constants
export * from './roles';
