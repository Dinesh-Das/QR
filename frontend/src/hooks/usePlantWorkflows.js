import { message } from 'antd';
import { useState, useEffect, useCallback } from 'react';

import { WORKFLOW_SPECIFIC_STATES, WORKFLOW_STATES } from '../constants';
import { getCurrentUser } from '../services/auth';
import { workflowAPI } from '../services/workflowAPI';
import { recalculateWorkflowProgress, getTemplateFieldCounts, calculateCorrectFieldCounts } from '../utils/questionnaireUtils';

/**
 * Custom hook for managing plant workflow data and operations
 * Handles workflow fetching, state management, and dashboard statistics
 */
export const usePlantWorkflows = (currentPlant, userPlantData) => {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dashboardStats, setDashboardStats] = useState({
    totalWorkflows: 0,
    completedCount: 0,
    inProgressCount: 0,
    draftCount: 0,
    averageCompletion: 0,
    completedToday: 0
  });

    /**
   * Get current plant from localStorage or default
   */
    const getCurrentPlant = useCallback(() => {
        const storedPlant = localStorage.getItem('userPlant');
        const storedUser = localStorage.getItem('username');
    
        console.log('Plant detection:', {
          storedPlant,
          storedUser,
          localStorage: Object.keys(localStorage)
        });
    
        return storedPlant || '1102';
      }, []);
    

  /**
   * Load plant workflows with proper error handling and AbortController cleanup
   */
  const loadPlantWorkflows = useCallback(async (signal) => {
    try {
      setLoading(true);
      setError(null);
      
      const plantCode = currentPlant || getCurrentPlant();

      if (!plantCode) {
        console.warn('No plant code available for loading workflows');
        setWorkflows([]);
        setDashboardStats({
          totalWorkflows: 0,
          completedCount: 0,
          inProgressCount: 0,
          draftCount: 0,
          averageCompletion: 0,
          completedToday: 0
        });
        return;
      }

      console.log('Loading workflows for plant:', plantCode);

      let dashboardData;
      let workflowsWithProgress = [];

      try {
        // Try to get plant dashboard data with progress information
        dashboardData = await workflowAPI.getPlantDashboardData(plantCode, { signal });
        console.log('Received dashboard data:', dashboardData);

        // Transform the data to match the expected format and filter by current plant
        workflowsWithProgress = dashboardData.workflows
          .filter(workflow => workflow.plantCode === plantCode) // Only show workflows for current plant
          .map(workflow => {
            // Recalculate progress with correct field counts (excluding CQS auto-populated fields)
            const correctedWorkflow = recalculateWorkflowProgress(workflow, workflow.plantInputs);
            
            // Determine the correct completion status based on actual progress
            let completionStatus;
            if (workflow.isSubmitted) {
              completionStatus = 'SUBMITTED';
            } else if (correctedWorkflow.completionPercentage === 100) {
              completionStatus = 'COMPLETED';
            } else if (correctedWorkflow.completionPercentage > 0) {
              completionStatus = 'IN_PROGRESS';
            } else {
              completionStatus = 'DRAFT';
            }

            return {
              id: workflow.workflowId,
              materialCode: workflow.materialCode,
              plantCode: workflow.plantCode,
              currentState: workflow.isSubmitted
                ? WORKFLOW_STATES.COMPLETED
                : WORKFLOW_SPECIFIC_STATES.PLANT_PENDING,
              completionStatus,
              completionPercentage: correctedWorkflow.completionPercentage,
              totalFields: correctedWorkflow.totalFields,
              completedFields: correctedWorkflow.completedFields,
              requiredFields: workflow.requiredFields || 0,
              completedRequiredFields: workflow.completedRequiredFields || 0,
              lastModified: workflow.lastModified,
              submittedAt: workflow.submittedAt,
              submittedBy: workflow.submittedBy,
              isSubmitted: workflow.isSubmitted,
              isCompleted: workflow.isCompleted,
              openQueries: workflow.openQueries || 0,
              assignedPlant: workflow.plantCode,
              materialName: workflow.materialName,
              itemDescription: workflow.itemDescription
            };
          });
      } catch (plantDataError) {
        console.warn(
          'Failed to load plant-specific data, falling back to regular workflows:',
          plantDataError
        );

        // Fallback: Load workflows by plant directly
        try {
          const plantWorkflows = await workflowAPI.getWorkflowsByPlant(plantCode, { signal });
          console.log('Fallback: Loaded plant workflows:', plantWorkflows);

          // Add mock progress data to plant workflows and filter by current plant
          const { totalUserEditableFields } = getTemplateFieldCounts();
          
          workflowsWithProgress = plantWorkflows
            .filter(workflow => {
              const workflowPlant = workflow.plantCode || workflow.assignedPlant;
              return workflowPlant === plantCode; // Only show workflows for current plant
            })
            .map(workflow => {
              // Calculate progress if plant inputs are available
              const plantInputs = workflow.plantInputs || workflow.plantData?.plantInputs || {};
              const { completionPercentage, completedFields } = calculateCorrectFieldCounts(plantInputs);
              
              // Determine the correct completion status based on actual progress
              let completionStatus;
              const isSubmitted = workflow.isSubmitted || workflow.submittedAt;
              if (isSubmitted) {
                completionStatus = 'SUBMITTED';
              } else if (completionPercentage === 100) {
                completionStatus = 'COMPLETED';
              } else if (completionPercentage > 0) {
                completionStatus = 'IN_PROGRESS';
              } else {
                completionStatus = 'DRAFT';
              }

              return {
                id: workflow.id,
                materialCode: workflow.materialCode,
                plantCode: workflow.plantCode || workflow.assignedPlant,
                currentState: workflow.currentState || workflow.state,
                completionStatus,
                completionPercentage,
                totalFields: totalUserEditableFields, // Correct total user-editable fields
                completedFields,
                requiredFields: 50, // Default required
                completedRequiredFields: 0, // Default completed required
                lastModified: workflow.lastModified,
                submittedAt: workflow.submittedAt || null,
                submittedBy: workflow.submittedBy || null,
                isSubmitted,
                isCompleted: workflow.isCompleted || false,
                openQueries: workflow.openQueries || 0,
                assignedPlant: workflow.plantCode || workflow.assignedPlant,
                materialName: workflow.materialName,
                itemDescription: workflow.itemDescription
              };
            });

          console.log('Fallback: Processed workflows:', workflowsWithProgress);
        } catch (fallbackError) {
          console.error('Fallback also failed:', fallbackError);
          workflowsWithProgress = [];
        }

        // Set default dashboard data for fallback
        dashboardData = {
          totalWorkflows: workflowsWithProgress.length,
          completedCount: 0,
          inProgressCount: 0,
          draftCount: workflowsWithProgress.length,
          averageCompletion: 0
        };
      }

      setWorkflows(workflowsWithProgress);

      // Set dashboard stats
      setDashboardStats({
        totalWorkflows: dashboardData.totalWorkflows || 0,
        completedCount: dashboardData.completedCount || 0,
        inProgressCount: dashboardData.inProgressCount || 0,
        draftCount: dashboardData.draftCount || 0,
        averageCompletion: dashboardData.averageCompletion || 0,
        completedToday: 0 // TODO: Calculate from submittedAt dates
      });
    } catch (error) {
      console.error('Failed to load plant workflows:', error);
      setError(error.message);
      message.error('Failed to load workflows');
      setWorkflows([]);
      setDashboardStats({
        totalWorkflows: 0,
        completedCount: 0,
        inProgressCount: 0,
        draftCount: 0,
        averageCompletion: 0,
        completedToday: 0
      });
    } finally {
      setLoading(false);
    }
  }, [currentPlant, getCurrentPlant]);



  /**
   * Refresh workflows data
   */
  const refreshWorkflows = useCallback(async () => {
    const controller = new AbortController();
    await loadPlantWorkflows(controller.signal);
  }, [loadPlantWorkflows]);


  // Load workflows when plant or user data changes
  useEffect(() => {
    const controller = new AbortController();

    const fetchWorkflows = async () => {
      if (userPlantData && currentPlant) {
        try {
          await loadPlantWorkflows(controller.signal);
        } catch (error) {
          if (!controller.signal.aborted) {
            console.error('Error loading plant workflows:', error);
          }
        }
      } else {
        // Clear workflows if no plant is selected
        setWorkflows([]);
        setDashboardStats({
          totalWorkflows: 0,
          completedCount: 0,
          inProgressCount: 0,
          draftCount: 0,
          averageCompletion: 0,
          completedToday: 0
        });
        setLoading(false);
      }
    };

    fetchWorkflows();

    return () => {
      controller.abort();
    };
  }, [userPlantData, currentPlant, loadPlantWorkflows]);

  return {
    workflows,
    loading,
    error,
    dashboardStats,
    loadPlantWorkflows,
    refreshWorkflows
  };
};