import { notification } from 'antd';

// Simple error handler
export const handleError = (error, options = {}) => {
  const { showNotification = true, customMessage = null, context = 'general' } = options;

  let errorMessage = 'An unexpected error occurred';

  // Handle different error types
  if (error?.response) {
    // HTTP error response
    const { data, status } = error.response;
    errorMessage = data?.message || `HTTP ${status} Error`;
  } else if (error?.request) {
    // Network error
    errorMessage = 'Unable to connect to server. Please check your internet connection.';
  } else if (error?.message) {
    // Generic error with message
    errorMessage = error.message;
  }

  // Use custom message if provided
  if (customMessage) {
    errorMessage = customMessage;
  }

  // Log error for debugging
  console.error(`[${context.toUpperCase()}] Error:`, error);

  // Show notification
  if (showNotification) {
    notification.error({
      message: 'Error',
      description: errorMessage,
      duration: 4.5,
      placement: 'topRight'
    });
  }

  return { message: errorMessage };
};

export const handleApiError = (error, options = {}) => {
  return handleError(error, { ...options, context: 'api' });
};

export default {
  handleError,
  handleApiError
};
