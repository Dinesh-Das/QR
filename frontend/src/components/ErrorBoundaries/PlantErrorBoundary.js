import React from 'react';

import { RouteErrorBoundary } from './RouteErrorBoundary';

/**
 * Plant-specific error boundary for handling errors within plant workflow management
 * Extends RouteErrorBoundary with plant-specific context
 */
export const PlantErrorBoundary = ({ children, plantCode }) => (
  <RouteErrorBoundary 
    routeName="Plant Dashboard" 
    errorContext={{ 
      plantCode,
      feature: 'plant-workflow-management',
      level: 'plant'
    }}
  >
    {children}
  </RouteErrorBoundary>
);

export default PlantErrorBoundary;