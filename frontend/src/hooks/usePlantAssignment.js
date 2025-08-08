import { message } from 'antd';
import { useState, useCallback, useEffect, useMemo } from 'react';

import { masterDataAPI } from '../services/masterDataAPI';

export const usePlantAssignment = () => {
  const [availablePlants, setAvailablePlants] = useState([]);
  const [plantsLoading, setPlantsLoading] = useState(false);
  const [error, setError] = useState(null);

  // Fetch plants from master data API
  const fetchPlants = useCallback(async (signal) => {
    setPlantsLoading(true);
    setError(null);
    
    try {
      const locations = await masterDataAPI.getAllLocations({ signal });
      if (!signal?.aborted) {
        const plantOptions = locations.map(location => ({
          label: `${location.locationCode} - ${location.description}`,
          value: location.locationCode,
          key: location.locationCode
        }));
        setAvailablePlants(plantOptions);
      }
    } catch (err) {
      if (!signal?.aborted) {
        console.error('Failed to fetch plants:', err);
        setError(err.message);
        message.warning('Failed to fetch plants from location master, using fallback data');

        // Fallback plant data
        const fallbackPlants = [
          { label: '1107 - Manufacturing Unit 1107', value: '1107', key: '1107' },
          { label: '1106 - Manufacturing Unit 1106', value: '1106', key: '1106' },
          { label: '1104 - Manufacturing Unit 1104', value: '1104', key: '1104' },
          { label: '1103 - Manufacturing Unit 1103', value: '1103', key: '1103' },
          { label: '1102 - Manufacturing Unit 1102', value: '1102', key: '1102' }
        ];
        setAvailablePlants(fallbackPlants);
      }
    } finally {
      if (!signal?.aborted) {
        setPlantsLoading(false);
      }
    }
  }, []);

  // Get plant description by code
  const getPlantDescription = useCallback((plantCode) => {
    const plant = availablePlants.find(p => p.value === plantCode);
    return plant ? plant.label : plantCode;
  }, [availablePlants]);

  // Validate plant assignments
  const validatePlantAssignments = useCallback((assignedPlants, primaryPlant) => {
    const errors = [];

    if (!assignedPlants || assignedPlants.length === 0) {
      errors.push('At least one plant must be assigned');
    }

    if (primaryPlant && assignedPlants && !assignedPlants.includes(primaryPlant)) {
      errors.push('Primary plant must be one of the assigned plants');
    }

    return errors;
  }, []);

  // Filter available plants based on search
  const filterPlants = useCallback((input, option) => {
    return option?.label?.toLowerCase().includes(input.toLowerCase());
  }, []);

  // Get plants that are available for assignment (not already assigned)
  const getAvailablePlantsForAssignment = useCallback((currentAssignments = []) => {
    return availablePlants.filter(plant => !currentAssignments.includes(plant.value));
  }, [availablePlants]);

  // Initialize plants on mount
  useEffect(() => {
    const controller = new AbortController();
    fetchPlants(controller.signal);

    return () => {
      controller.abort();
    };
  }, [fetchPlants]);

  // Memoized plant options for primary plant selection
  const primaryPlantOptions = useMemo(() => {
    return availablePlants.map(plant => ({
      ...plant,
      disabled: false // All plants can be primary
    }));
  }, [availablePlants]);

  return {
    availablePlants,
    plantsLoading,
    error,
    fetchPlants,
    getPlantDescription,
    validatePlantAssignments,
    filterPlants,
    getAvailablePlantsForAssignment,
    primaryPlantOptions
  };
};