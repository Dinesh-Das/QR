import apiClient from '../api/client';

/**
 * Project API service providing project and master data functionality
 * Migrated to use unified APIClient with standardized error handling
 *
 * @namespace projectAPI
 */
export const projectAPI = {
  // Project dropdown data
  /**
   * Get all projects
   * @returns {Promise<Array>} Array of project objects
   */
  getProjects: async () => {
    try {
      const projectCodes = await apiClient.get('/master-data/project-codes');
      // Transform project codes into project objects with proper structure
      return projectCodes.map(code => ({
        value: code,
        label: code,
         code,
        name: code
      }));
    } catch (error) {
      console.error('Error fetching projects:', error);
      return [];
    }
  },

  /**
   * Get materials for a specific project
   * @param {string} projectCode - Project code
   * @returns {Promise<Array>} Array of materials for the project
   */
  getMaterialsByProject: async projectCode => {
    try {
      const itemCodes = await apiClient.get(`/master-data/project-codes/${encodeURIComponent(projectCode)}/items`);
      // Transform item codes into material objects with proper structure
      return itemCodes.map(code => ({
        value: code,
        label: code,
         code,
        name: code
      }));
    } catch (error) {
      console.error('Error fetching materials for project:', projectCode, error);
      return [];
    }
  },

  /**
   * Get all plants
   * @returns {Promise<Array>} Array of plant objects
   */
  getPlants: async () => {
    try {
      // Try the projects endpoint first (which has proper structure)
      const plants = await apiClient.get('/projects/plants');
      if (plants && plants.length > 0) {
        return plants.map(plant => ({
          value: plant.code || plant.id,
          label: `${plant.code || plant.id} - ${plant.name}`,
          code: plant.code || plant.id,
          name: plant.name,
          id: plant.id || plant.code
        }));
      }
      
      // Fallback to master-data endpoint if projects endpoint is empty
      const locations = await apiClient.get('/master-data/locations');
      return locations.map(location => ({
        value: location.locationCode,
        label: `${location.locationCode} - ${location.description}`,
        code: location.locationCode,
        name: location.description,
        id: location.locationCode
      }));
    } catch (error) {
      console.error('Error fetching plants:', error);
      return [];
    }
  },

  // Validation endpoints
  /**
   * Validate project code
   * @param {string} projectCode - Project code to validate
   * @returns {Promise<boolean>} True if project code is valid
   */
  validateProjectCode: projectCode =>
    apiClient.get(`/projects/${encodeURIComponent(projectCode)}/validate`),

  /**
   * Validate material code for a project
   * @param {string} projectCode - Project code
   * @param {string} materialCode - Material code to validate
   * @returns {Promise<boolean>} True if material code is valid for the project
   */
  validateMaterialCode: (projectCode, materialCode) =>
    apiClient.get(
      `/projects/${encodeURIComponent(projectCode)}/materials/${encodeURIComponent(materialCode)}/validate`
    ),

  /**
   * Validate plant code
   * @param {string} plantCode - Plant code to validate
   * @returns {Promise<boolean>} True if plant code is valid
   */
  validatePlantCode: plantCode =>
    apiClient.get(`/projects/plants/${encodeURIComponent(plantCode)}/validate`),



  // Search endpoints
  /**
   * Search projects by term
   * @param {string} searchTerm - Search term
   * @returns {Promise<Array>} Array of matching projects
   */
  searchProjects: searchTerm =>
    apiClient.get(`/projects/search?searchTerm=${encodeURIComponent(searchTerm)}`),

  /**
   * Search materials within a project
   * @param {string} projectCode - Project code
   * @param {string} searchTerm - Search term
   * @returns {Promise<Array>} Array of matching materials
   */
  searchMaterials: (projectCode, searchTerm) =>
    apiClient.get(
      `/projects/${encodeURIComponent(projectCode)}/materials/search?searchTerm=${encodeURIComponent(searchTerm)}`
    ),

  /**
   * Search plants by term
   * @param {string} searchTerm - Search term
   * @returns {Promise<Array>} Array of matching plants
   */
  searchPlants: searchTerm =>
    apiClient.get(`/projects/plants/search?searchTerm=${encodeURIComponent(searchTerm)}`),



  // Enhanced data endpoints
  /**
   * Get projects with material count
   * @returns {Promise<Array>} Array of projects with material counts
   */
  getProjectsWithMaterialCount: () => apiClient.get('/projects/with-material-count'),



  // Questionnaire templates
  /**
   * Get all questionnaire templates
   * @returns {Promise<Array>} Array of questionnaire templates
   */
  getQuestionnaireTemplates: () => apiClient.get('/projects/questionnaire/templates'),

  /**
   * Get questionnaire steps
   * @returns {Promise<Array>} Array of questionnaire steps
   */
  getQuestionnaireSteps: () => apiClient.get('/projects/questionnaire/steps'),

  /**
   * Get questionnaire templates by step
   * @param {number} stepNumber - Step number
   * @returns {Promise<Array>} Array of templates for the step
   */
  getQuestionnaireTemplatesByStep: stepNumber =>
    apiClient.get(`/projects/questionnaire/steps/${stepNumber}`),

  /**
   * Get specific questionnaire template
   * @param {string} templateId - Template ID
   * @returns {Promise<Object>} Questionnaire template object
   */
  getQuestionnaireTemplate: templateId =>
    apiClient.get(`/projects/questionnaire/questions/${templateId}`)
};
