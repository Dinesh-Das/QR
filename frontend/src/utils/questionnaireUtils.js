/**
 * Utility functions for questionnaire field calculations
 * Handles CQS auto-populated field exclusions and progress calculations
 */

/**
 * Get the default questionnaire template with CQS field markings
 * This should match the template structure in PlantQuestionnaire.js
 */
export const getDefaultQuestionnaireTemplate = () => [
  {
    title: 'General',
    fields: [
      { name: 'msds_available', isCqsAutoPopulated: false },
      { name: 'missing_info', isCqsAutoPopulated: false },
      { name: 'sourcing_asked', isCqsAutoPopulated: false },
      { name: 'cas_available', isCqsAutoPopulated: false },
      { name: 'mixture_ingredients', isCqsAutoPopulated: false },
      { name: 'composition_percentage', isCqsAutoPopulated: false },
      { name: 'total_percentage_1', isCqsAutoPopulated: false },
      { name: 'total_percentage', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'Physical',
    fields: [
      { name: 'is_corrosive', isCqsAutoPopulated: true },
      { name: 'corrosive_storage', isCqsAutoPopulated: false },
      { name: 'highly_toxic', isCqsAutoPopulated: true },
      { name: 'toxic_powder_handling', isCqsAutoPopulated: false },
      { name: 'crushing_facilities', isCqsAutoPopulated: false },
      { name: 'heating_facilities', isCqsAutoPopulated: false },
      { name: 'paste_preparation', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'Flammability and Explosivity',
    fields: [
      { name: 'flash_point_65', isCqsAutoPopulated: true },
      { name: 'petroleum_class', isCqsAutoPopulated: true },
      { name: 'storage_license', isCqsAutoPopulated: false },
      { name: 'ccoe_license', isCqsAutoPopulated: false },
      { name: 'flash_point_21', isCqsAutoPopulated: true },
      { name: 'flammable_infrastructure', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'Toxicity',
    fields: [
      { name: 'ld50_oral', isCqsAutoPopulated: true },
      { name: 'ld50_dermal', isCqsAutoPopulated: true },
      { name: 'lc50_inhalation', isCqsAutoPopulated: true },
      { name: 'exposure_minimization', isCqsAutoPopulated: false },
      { name: 'carcinogenic', isCqsAutoPopulated: true },
      { name: 'carcinogenic_control', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'Storage and Handling',
    fields: [
      { name: 'storage_conditions_stores', isCqsAutoPopulated: false },
      { name: 'storage_conditions_floor', isCqsAutoPopulated: false },
      { name: 'closed_loop_required', isCqsAutoPopulated: false },
      { name: 'work_permit_available', isCqsAutoPopulated: false },
      { name: 'procedures_details', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'PPE',
    fields: [
      { name: 'recommended_ppe', isCqsAutoPopulated: true },
      { name: 'ppe_in_use', isCqsAutoPopulated: false },
      { name: 'ppe_procurement_date', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'First Aid',
    fields: [
      { name: 'is_poisonous', isCqsAutoPopulated: true },
      { name: 'antidote_specified', isCqsAutoPopulated: true },
      { name: 'antidote_available', isCqsAutoPopulated: false },
      { name: 'antidote_source', isCqsAutoPopulated: false },
      { name: 'first_aid_capability', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'Statutory',
    fields: [
      { name: 'cmvr_listed', isCqsAutoPopulated: true },
      { name: 'msihc_listed', isCqsAutoPopulated: true },
      { name: 'factories_act_listed', isCqsAutoPopulated: true },
      { name: 'permissible_concentration', isCqsAutoPopulated: false },
      { name: 'monitoring_details', isCqsAutoPopulated: false }
    ]
  },
  {
    title: 'Others',
    fields: [
      { name: 'plant_inputs_required', isCqsAutoPopulated: false },
      { name: 'gaps_identified', isCqsAutoPopulated: false },
      { name: 'additional_input_1', isCqsAutoPopulated: false },
      { name: 'additional_input_2', isCqsAutoPopulated: false }
    ]
  }
];

/**
 * Calculate correct field counts excluding CQS auto-populated fields
 * @param {Object} plantInputs - The plant input data
 * @param {Array} template - Optional template, uses default if not provided
 * @returns {Object} - { totalUserEditableFields, completedUserEditableFields, completionPercentage }
 */
export const calculateCorrectFieldCounts = (plantInputs = {}, template = null) => {
  const questionnaireTemplate = template || getDefaultQuestionnaireTemplate();
  
  let totalUserEditableFields = 0;
  let completedUserEditableFields = 0;

  questionnaireTemplate.forEach(step => {
    const stepFields = step.fields || [];
    
    // Filter out CQS auto-populated fields
    const userEditableFields = stepFields.filter(field => !field.isCqsAutoPopulated && !field.disabled);
    totalUserEditableFields += userEditableFields.length;

    // Count completed user-editable fields
    const completedFields = userEditableFields.filter(field => {
      const value = plantInputs[field.name];
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return value && value !== '' && value !== null && value !== undefined;
    });

    completedUserEditableFields += completedFields.length;
  });

  const completionPercentage = totalUserEditableFields > 0 
    ? Math.round((completedUserEditableFields / totalUserEditableFields) * 100) 
    : 0;

  return {
    totalUserEditableFields,
    completedUserEditableFields,
    completionPercentage
  };
};

/**
 * Get total field counts for the questionnaire template
 * @param {Array} template - Optional template, uses default if not provided
 * @returns {Object} - { totalFields, totalUserEditableFields, totalCqsFields }
 */
export const getTemplateFieldCounts = (template = null) => {
  const questionnaireTemplate = template || getDefaultQuestionnaireTemplate();
  
  let totalFields = 0;
  let totalUserEditableFields = 0;
  let totalCqsFields = 0;

  questionnaireTemplate.forEach(step => {
    const stepFields = step.fields || [];
    totalFields += stepFields.length;
    
    stepFields.forEach(field => {
      if (field.isCqsAutoPopulated || field.disabled) {
        totalCqsFields++;
      } else {
        totalUserEditableFields++;
      }
    });
  });

  return {
    totalFields,
    totalUserEditableFields,
    totalCqsFields
  };
};

/**
 * Recalculate workflow progress with correct field counts
 * @param {Object} workflow - The workflow object from backend
 * @param {Object} plantInputs - The plant input data (optional)
 * @returns {Object} - Updated workflow with correct field counts
 */
export const recalculateWorkflowProgress = (workflow, plantInputs = null) => {
  // If we have plant inputs, calculate based on actual data
  if (plantInputs) {
    const { totalUserEditableFields, completedUserEditableFields, completionPercentage } = 
      calculateCorrectFieldCounts(plantInputs);
    
    return {
      ...workflow,
      totalFields: totalUserEditableFields,
      completedFields: completedUserEditableFields,
      completionPercentage
    };
  }

  // Otherwise, just fix the total field count and recalculate percentage
  const { totalUserEditableFields } = getTemplateFieldCounts();
  
  // Assume the backend's completed count is correct but total is wrong
  const backendCompletedFields = workflow.completedFields || 0;
  const correctedCompletionPercentage = totalUserEditableFields > 0 
    ? Math.round((backendCompletedFields / totalUserEditableFields) * 100) 
    : 0;

  return {
    ...workflow,
    totalFields: totalUserEditableFields,
    completionPercentage: correctedCompletionPercentage
  };
};