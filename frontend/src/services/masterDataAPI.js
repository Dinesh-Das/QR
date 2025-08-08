import apiClient from '../api/client';

export const masterDataAPI = {
  // Location Master (Plants) endpoints
  getAllLocations: async () => {
    try {
      return await apiClient.get('/master-data/locations');
    } catch (error) {
      console.warn('Main locations endpoint failed, trying fallback:', error);
      try {
        return await apiClient.get('/master-data/locations/fallback');
      } catch (fallbackError) {
        console.warn('Fallback endpoint failed, trying simple endpoint:', fallbackError);
        try {
          return await apiClient.get('/simple-locations');
        } catch (simpleError) {
          console.error('All locations endpoints failed:', simpleError);
          throw simpleError;
        }
      }
    }
  },

  getLocationByCode: locationCode =>
    apiClient.get(`/master-data/locations/${encodeURIComponent(locationCode)}`),

  searchLocations: searchTerm =>
    apiClient.get(`/master-data/locations/search?term=${encodeURIComponent(searchTerm)}`),

  createLocation: locationData => apiClient.post('/master-data/locations', locationData),

  updateLocation: (locationCode, locationData) =>
    apiClient.put(`/master-data/locations/${encodeURIComponent(locationCode)}`, locationData),

  deleteLocation: locationCode =>
    apiClient.delete(`/master-data/locations/${encodeURIComponent(locationCode)}`),



  // Project Item Master endpoints
  getAllProjectItems: () => apiClient.get('/master-data/project-items'),

  getItemsByProject: projectCode =>
    apiClient.get(`/master-data/project-items/projects/${encodeURIComponent(projectCode)}`),

  getProjectsByItem: itemCode =>
    apiClient.get(`/master-data/project-items/items/${encodeURIComponent(itemCode)}`),

  getAllProjectCodes: () => apiClient.get('/master-data/project-codes'),

  getAllItemCodes: () => apiClient.get('/master-data/item-codes'),

  getItemCodesByProject: projectCode =>
    apiClient.get(`/master-data/project-codes/${encodeURIComponent(projectCode)}/items`),

  // Test endpoints
  testLocationMaster: () => apiClient.get('/master-data/locations/test'),

  testSimpleLocation: () => apiClient.get('/simple-locations/test'),

  diagnosticLocationMaster: () => apiClient.get('/master-data/locations/diagnostic'),

  getSimpleLocationCount: () => apiClient.get('/simple-locations/count')
};
