import {
  SaveOutlined,
  QuestionCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CloudSyncOutlined,
  WifiOutlined,
  DisconnectOutlined,
  MenuOutlined,
  DashboardOutlined,
  FileTextOutlined,
  SafetyOutlined,
  ExperimentOutlined,
  FireOutlined,
  MedicineBoxOutlined,
  BookOutlined,
  SettingOutlined,
  BulbOutlined,
  RocketOutlined
} from '@ant-design/icons';
import {
  Card,
  Form,
  Input,
  Select,
  Radio,
  Checkbox,
  Button,
  Row,
  Col,
  Progress,
  message,
  Spin,
  Alert,
  Space,
  Tooltip,
  Badge,
  notification,
  Typography,
  Tag,
  Modal,
  FloatButton,
  Drawer,
  Timeline,
  Avatar
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import { UI_CONFIG, AUTO_SAVE } from '../constants';
import { queryAPI } from '../services/queryAPI';
import { workflowAPI } from '../services/workflowAPI';

import MaterialContextPanel from './MaterialContextPanel';
import QueryRaisingModal from './QueryRaisingModal';
import './PlantQuestionnaire.css';

// const { Step } = Steps; // Not currently used
const { TextArea } = Input;
const { Option } = Select;
const { Text, Title, Paragraph } = Typography;

// Hook to detect screen size
const useResponsive = () => {
  const [screenSize, setScreenSize] = useState({
    isMobile: window.innerWidth <= UI_CONFIG.MOBILE_BREAKPOINT,
    isTablet:
      window.innerWidth > UI_CONFIG.MOBILE_BREAKPOINT &&
      window.innerWidth <= UI_CONFIG.TABLET_BREAKPOINT,
    isDesktop: window.innerWidth > UI_CONFIG.TABLET_BREAKPOINT
  });

  useEffect(() => {
    const handleResize = () => {
      setScreenSize({
        isMobile: window.innerWidth <= UI_CONFIG.MOBILE_BREAKPOINT,
        isTablet:
          window.innerWidth > UI_CONFIG.MOBILE_BREAKPOINT &&
          window.innerWidth <= UI_CONFIG.TABLET_BREAKPOINT,
        isDesktop: window.innerWidth > UI_CONFIG.TABLET_BREAKPOINT
      });
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return screenSize;
};

const PlantQuestionnaire = ({ workflowId, onComplete, onSaveDraft }) => {
  const [form] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [workflowData, setWorkflowData] = useState(null);
  const [formData, setFormData] = useState({});
  const [completedSteps, setCompletedSteps] = useState(new Set());
  const [queryModalVisible, setQueryModalVisible] = useState(false);
  const [selectedField, setSelectedField] = useState(null);
  const [queries, setQueries] = useState([]);
  const [autoSaveEnabled, setAutoSaveEnabled] = useState(true);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const [pendingChanges, setPendingChanges] = useState(false);
  const [sidebarVisible, setSidebarVisible] = useState(false);
  // const [compactMode, setCompactMode] = useState(false); // Not currently used
  const [_progressUpdateTrigger, _setProgressUpdateTrigger] = useState(0);
  const { isMobile } = useResponsive();

  // Define questionnaire steps (loaded from backend template)
  const [questionnaireSteps, setQuestionnaireSteps] = useState([]);
  const [templateLoading, setTemplateLoading] = useState(true);
  // eslint-disable-next-line no-unused-vars
  const [_cqsData, setCqsData] = useState({}); // Used for CQS auto-population (pending implementation)
  // eslint-disable-next-line no-unused-vars
  const [_plantSpecificData, setPlantSpecificData] = useState({}); // Used for plant-specific data loading

  // Step icons mapping for modern UI
  const stepIcons = {
    General: <FileTextOutlined />,
    Physical: <ExperimentOutlined />,
    'Flammability and Explosivity': <FireOutlined />,
    Toxicity: <SafetyOutlined />,
    'Storage and Handling': <BookOutlined />,
    PPE: <SafetyOutlined />,
    'First Aid': <MedicineBoxOutlined />,
    Statutory: <BookOutlined />,
    Others: <SettingOutlined />,
    Safety: <SafetyOutlined />,
    Environmental: <BulbOutlined />,
    Quality: <RocketOutlined />
  };

  // Load questionnaire template from backend
  const loadQuestionnaireTemplate = useCallback(async () => {
    try {
      setTemplateLoading(true);

      if (!workflowData?.materialCode || !workflowData?.assignedPlant) {
        console.warn('Missing required data for template loading:', {
          materialCode: workflowData?.materialCode,
          assignedPlant: workflowData?.assignedPlant
        });
        setQuestionnaireSteps(getDefaultTemplate());
        return;
      }

      console.log('Loading questionnaire template for:', {
        materialCode: workflowData.materialCode,
        plantCode: workflowData.assignedPlant
      });

      const template = await workflowAPI.getQuestionnaireTemplate({
        materialCode: workflowData.materialCode,
        plantCode: workflowData.assignedPlant,
        templateType: 'PLANT_QUESTIONNAIRE'
      });

      console.log('Received template:', template);

      // Validate template structure
      if (!template || !template.steps || !Array.isArray(template.steps)) {
        console.error('Invalid template structure:', template);
        throw new Error('Invalid template structure received from backend');
      }

      // Process template to include CQS auto-populated fields
      const processedSteps = template.steps.map(step => ({
        ...step,
        title: step.title || step.stepTitle || `Step ${step.stepNumber || 'Unknown'}`,
        description: step.description || '',
        fields: (step.fields || []).map(field => ({
          ...field,
          isCqsAutoPopulated: field.cqsAutoPopulated || field.isCqsAutoPopulated || false,
          cqsValue: field.cqsAutoPopulated || field.isCqsAutoPopulated ? 'Pending IMP' : null,
          disabled: field.cqsAutoPopulated || field.isCqsAutoPopulated || field.disabled || false,
          placeholder:
            field.cqsAutoPopulated || field.isCqsAutoPopulated
              ? 'Auto-populated by CQS (Pending Implementation)'
              : field.placeholder
        }))
      }));

      console.log('Processed steps:', processedSteps);
      setQuestionnaireSteps(processedSteps);

      // Load CQS data if available
      try {
        const cqsResponse = await workflowAPI.getCqsData({
          materialCode: workflowData?.materialCode,
          plantCode: workflowData?.assignedPlant
        });

        setCqsData(cqsResponse.data || {});

        // Update form with CQS data
        const cqsFormData = {};
        Object.entries(cqsResponse.data || {}).forEach(([key, value]) => {
          if (value && value !== 'Pending IMP') {
            cqsFormData[key] = value;
          }
        });

        if (Object.keys(cqsFormData).length > 0) {
          setFormData(prev => ({ ...prev, ...cqsFormData }));
          form.setFieldsValue(cqsFormData);
        }
      } catch (error) {
        console.error('Failed to load CQS data:', error);
        // Don't show error message as CQS data might not be available yet
      }

      // Load plant-specific data
      try {
        const plantData = await workflowAPI.getOrCreatePlantSpecificData({
          plantCode: workflowData?.assignedPlant,
          materialCode: workflowData?.materialCode,
          workflowId
        });

        setPlantSpecificData(plantData || {});

        // If plant data exists, populate form with existing plant inputs
        if (plantData?.plantInputs) {
          setFormData(prev => ({ ...prev, ...plantData.plantInputs }));
          form.setFieldsValue(plantData.plantInputs);
        }
      } catch (error) {
        console.error('Failed to load plant-specific data:', error);
      }
    } catch (error) {
      console.error('Failed to load questionnaire template:', error);
      message.error(`Failed to load questionnaire template: ${error.message}`);
      // Fallback to default template if backend fails
      console.log('Using fallback template');
      setQuestionnaireSteps(getDefaultTemplate());
    } finally {
      setTemplateLoading(false);
    }
  }, [workflowData, workflowId, form]);


  // Save plant-specific data with composite key
  // const savePlantSpecificData = async data => { // Not currently used
  //   try {
  //     const plantSpecificPayload = {
  //       plantCode: workflowData?.assignedPlant,
  //       materialCode: workflowData?.materialCode,

  //       workflowId: workflowId,
  //       cqsInputs: cqsData,
  //       plantInputs: data,
  //       totalFields: Object.keys(data).length,
  //       completedFields: Object.values(data).filter(
  //         value => value !== null && value !== undefined && value !== ''
  //       ).length
  //     };

  //     await workflowAPI.savePlantSpecificData(plantSpecificPayload, 'current_user');
  //   } catch (error) {
  //     console.error('Failed to save plant-specific data:', error);
  //     throw error;
  //   }
  // };

  // Default template fallback - Updated to match actual 87 questions
  const getDefaultTemplate = () => [
    {
      title: 'General',
      description: 'General information about MSDS availability and completeness',
      fields: [
        {
          name: 'msds_available',
          label: 'Is 16 Section MSDS of the raw material available?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'missing_info',
          label: 'Which information in any one of the 16 sections is not available in full?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe missing information'
        },
        {
          name: 'sourcing_asked',
          label:
            'Has the identified missing / more information required from the supplier asked thru Sourcing?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'cas_available',
          label: 'Is CAS number of the raw material based on the pure substance available?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'mixture_ingredients',
          label: 'For mixtures, are ingredients of mixture available?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'composition_percentage',
          label: 'Is % age composition substances in the mixture available?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'total_percentage_1',
          label:
            'Is the total %age of all substances in the mixture equal to 100?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'total_percentage',
          label:
            'If not what is the % of substances not available?',
          type: 'textarea',
          required: false,
          placeholder: 'Provide details about percentage composition'
        }
      ]
    },
    {
      title: 'Physical',
      description: 'Physical properties and handling requirements',
      fields: [
        {
          name: 'is_corrosive',
          label: 'Is the material corrosive?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'corrosive_storage',
          label:
            'Does the plant have acid and alkali proof storage facilities to store a corrosive raw material?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'highly_toxic',
          label: 'Is the material highly toxic?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'toxic_powder_handling',
          label:
            'Does the plant have facilities to handle fine powder of highly toxic raw material?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'crushing_facilities',
          label: 'Does the plant have facilities to crush the stone like solid raw material?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'heating_facilities',
          label:
            'Does the plant have facilities to heat/melt the raw material if required for charging the same in a batch?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'paste_preparation',
          label:
            'Does the plant have facilities to prepare paste of raw material if required for charging the same in a batch?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        }
      ]
    },
    {
      title: 'Flammability and Explosivity',
      description: 'Flammability, explosivity and fire safety measures',
      fields: [
        {
          name: 'flash_point_65',
          label: 'Is Flash point of the raw material given and less than or equal to 65 degree C?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'petroleum_class',
          label:
            'Is the raw material is to be catgorised as ClassC / Class B / Class A substance as per Petroleum Act / Rules?',
          type: 'select',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'class_a', label: 'Class A' },
            { value: 'class_b', label: 'Class B' },
            { value: 'class_c', label: 'Class C' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'storage_license',
          label: 'Does all the plants have the capacity and license to store the raw material?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'ccoe_license',
          label:
            'If no, has the plant applied for CCoE license and by when expected to receive the license?',
          type: 'textarea',
          required: false,
          placeholder: 'Provide details about CCoE license application'
        },
        {
          name: 'flash_point_21',
          label: 'Is Flash point of the raw material given is less than 21 degree C?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'flammable_infrastructure',
          label:
            "If yes, does plant have infrastructure to comply State Factories Rule for handling 'Flammable liquids'?",
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        }
      ]
    },
    {
      title: 'Toxicity',
      description: 'Toxicity assessment and exposure control',
      fields: [
        {
          name: 'ld50_oral',
          label:
            'Is LD 50 (oral) value available and higher than the threshold limit of 200 mg/Kg BW?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'ld50_dermal',
          label: 'Is LD 50 (Dermal) value available and higher than 1000 mg/Kg BW?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'lc50_inhalation',
          label: 'Is LC50 Inhalation value available and higher than 10 mg/L?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'exposure_minimization',
          label:
            'If no, in any of the above three cases (where avaialble) then does the plant have facilities and /or procedure to minmise the exposure of workman?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe exposure minimization procedures'
        },
        {
          name: 'carcinogenic',
          label: 'Is the RM a suspect Carcinogenic?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'carcinogenic_control',
          label:
            'If yes, plant has adequate facilities and /or procedure to minimse the exposure of workman?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe carcinogenic exposure control measures'
        }
      ]
    },
    {
      title: 'Storage and Handling',
      description: 'Storage and handling procedures',
      fields: [
        {
          name: 'storage_conditions_stores',
          label: 'Are any storage conditions required and available in the plant stores?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe storage conditions in plant stores'
        },
        {
          name: 'storage_conditions_floor',
          label: 'Are any storage conditions required and available in the shop floor?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe storage conditions on shop floor'
        },
        {
          name: 'closed_loop_required',
          label: 'Does it require closed loop handling system during charging?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'work_permit_available',
          label:
            'Does the plant have required Work permit and /or WI/SOP to handle the raw material adequately?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'procedures_details',
          label: 'If, yes specify the procedures',
          type: 'textarea',
          required: false,
          placeholder: 'Specify the procedures and work permits'
        }
      ]
    },
    {
      title: 'PPE',
      description: 'Personal protective equipment requirements',
      fields: [
        {
          name: 'recommended_ppe',
          label: 'Recommended specific PPEs based on MSDS',
          type: 'textarea',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        },
        {
          name: 'ppe_in_use',
          label:
            'Are recommended PPE as per MSDS to handle the RM is already in use at the plants?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'partial', label: 'Partially' }
          ]
        },
        {
          name: 'ppe_procurement_date',
          label: 'If no, by when the plant can procure the require PPE?',
          type: 'input',
          required: false,
          placeholder: 'Enter expected procurement date'
        }
      ]
    },
    {
      title: 'First Aid',
      description: 'First aid measures and emergency response',
      fields: [
        {
          name: 'is_poisonous',
          label: 'Is the raw material poisonous as per the MSDS?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'antidote_specified',
          label:
            'Is the name of antidote required to counter the impact of the material given in the MSDS?',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'antidote_available',
          label: 'Is the above specified antidote available in the plants?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'antidote_source',
          label:
            'If the specified antidote is not available then what is source and who will obtain the antidote in the plant?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe antidote source and procurement plan'
        },
        {
          name: 'first_aid_capability',
          label:
            'Does the plant has capability to provide the first aid mentioned in the MSDS with the existing control measures?',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        }
      ]
    },
    {
      title: 'Statutory',
      description: 'Statutory compliance and regulatory requirements',
      fields: [
        {
          name: 'cmvr_listed',
          label: 'Is the RM or any of its ingredient listed in Table 3 of Rule 137 (CMVR)',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'msihc_listed',
          label: 'Is the RM or any of its ingredient listed in part II of Schedule I of MSIHC Rule',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'factories_act_listed',
          label: 'Is the RM or any of its ingredients listed in Schedule II of Factories Act',
          type: 'radio',
          required: false,
          isCqsAutoPopulated: true,
          disabled: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)',
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' }
          ]
        },
        {
          name: 'permissible_concentration',
          label:
            'With the current infrastructure, is the concentration of RM / ingredients listed in Schedule II of Factories Act within permissible concentrations as per Factories Act in the work area.',
          type: 'radio',
          required: false,
          options: [
            { value: 'yes', label: 'Yes' },
            { value: 'no', label: 'No' },
            { value: 'na', label: 'N/A' }
          ]
        },
        {
          name: 'monitoring_details',
          label:
            'Mention details of work area monitoring results and describe infrastructure used for handling',
          type: 'textarea',
          required: false,
          placeholder: 'Provide monitoring details and infrastructure description'
        }
      ]
    },
    {
      title: 'Others',
      description: 'Additional inputs and gap analysis',
      fields: [
        {
          name: 'plant_inputs_required',
          label: 'Inputs required from plants based on the above assessment?',
          type: 'textarea',
          required: false,
          placeholder: 'Describe inputs required from plants'
        },
        {
          name: 'gaps_identified',
          label: 'Gaps identified vis-à-vis existing controls / protocols',
          type: 'textarea',
          required: false,
          placeholder: 'Identify gaps in existing controls and protocols'
        },
        {
          name: 'additional_input_1',
          label: 'Additional Input 1',
          type: 'textarea',
          required: false,
          placeholder: 'Additional input field 1'
        },
        {
          name: 'additional_input_2',
          label: 'Additional Input 2',
          type: 'textarea',
          required: false,
          placeholder: 'Additional input field 2'
        }
      ]
    }
  ];

  // Function definitions (moved here to avoid hoisting issues)
  const handleNext = useCallback(() => {
    if (currentStep < questionnaireSteps.length - 1) {
      setCurrentStep(currentStep + 1);
    }
  }, [currentStep, questionnaireSteps.length]);

  const handlePrevious = useCallback(() => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  }, [currentStep]);

  const handleStepChange = useCallback(
    step => {
      if (step >= 0 && step < questionnaireSteps.length) {
        setCurrentStep(step);
      }
    },
    [questionnaireSteps.length]
  );

  // Helper function definitions
  // const getStepForField = fieldName => { // Not currently used
  //   for (let i = 0; i < questionnaireSteps.length; i++) {
  //     if (questionnaireSteps[i].fields.some(field => field.name === fieldName)) {
  //       return i;
  //     }
  //   }
  //   return 0;
  // };

  const getOverallCompletionPercentage = useCallback(() => {
    if (!questionnaireSteps || questionnaireSteps.length === 0 || !form) {
      return 0;
    }

    let totalFields = 0;
    let completedFields = 0;

    // Get current form values including any unsaved changes
    try {
      const currentFormValues = form.getFieldsValue();
      const currentData = { ...formData, ...currentFormValues };

      questionnaireSteps.forEach((step, _index) => {
        const stepFields = step.fields || [];
        totalFields += stepFields.length;

        const completedStepFields = stepFields.filter(field => {
          const value = currentData[field.name];
          if (Array.isArray(value)) {
            return value.length > 0;
          }
          return value && value !== '' && value !== null && value !== undefined;
        });

        completedFields += completedStepFields.length;
      });

      const percentage = totalFields > 0 ? Math.round((completedFields / totalFields) * 100) : 0;

      // Overall completion calculated

      return percentage;
    } catch (error) {
      console.error('Error calculating overall completion:', error);
      return 0;
    }
  }, [questionnaireSteps, formData, form]);

  const getTotalFieldsPopulated = useCallback(() => {
    if (!questionnaireSteps || questionnaireSteps.length === 0 || !form) {
      return { total: 0, populated: 0 };
    }

    let totalFields = 0;
    let populatedFields = 0;

    try {
      // Get current form values including any unsaved changes
      const currentFormValues = form.getFieldsValue();
      const currentData = { ...formData, ...currentFormValues };

      questionnaireSteps.forEach(step => {
        const stepFields = step.fields || [];
        totalFields += stepFields.length;

        const populatedStepFields = stepFields.filter(field => {
          const value = currentData[field.name];
          if (Array.isArray(value)) {
            return value.length > 0;
          }
          return value && value !== '' && value !== null && value !== undefined;
        });

        populatedFields += populatedStepFields.length;
      });

      // Total fields populated calculated

      return { total: totalFields, populated: populatedFields };
    } catch (error) {
      console.error('Error calculating total fields populated:', error);
      return { total: 0, populated: 0 };
    }
  }, [questionnaireSteps, formData, form]);

  const handleSaveDraft = useCallback(
    async (silent = false) => {
      try {
        setSaving(true);
        const currentValues = form.getFieldsValue();
        const updatedFormData = { ...formData, ...currentValues };

        // Enhanced validation before saving
        const validatedFormData = {};
        Object.entries(updatedFormData).forEach(([key, value]) => {
          if (value !== null && value !== undefined && value !== '') {
            validatedFormData[key] = value;
          }
        });

        // Save to local storage as backup with enhanced metadata
        const draftKey = `plant_questionnaire_draft_${workflowId}`;
        const draftData = {
          formData: validatedFormData,
          currentStep,
          timestamp: Date.now(),
          completedSteps: Array.from(completedSteps),
          version: '2.0',
          materialCode: workflowData?.materialCode,
          materialName: workflowData?.materialName,
          assignedPlant: workflowData?.assignedPlant,
          lastSyncAttempt: Date.now(),
          syncStatus: isOffline ? 'pending' : 'synced',
          totalFields: Object.keys(validatedFormData).length,
          completionPercentage: getOverallCompletionPercentage(),
          sessionId: Date.now()
        };

        try {
          localStorage.setItem(draftKey, JSON.stringify(draftData));
        } catch (localStorageError) {
          console.warn('Failed to save draft to local storage:', localStorageError);
        }

        // Save to server if online
        if (!isOffline) {
          try {
            const draftData = {
              plantCode: workflowData?.assignedPlant,
              materialCode: workflowData?.materialCode,

              responses: updatedFormData,
              currentStep,
              completedSteps: Array.from(completedSteps),
              modifiedBy: 'current_user'
            };

            await workflowAPI.saveDraftPlantResponses(workflowId, draftData);

            if (!silent) {
              message.success('Draft saved successfully');
            }
          } catch (serverError) {
            console.error('Failed to save draft to server:', serverError);
            setPendingChanges(true);

            if (!silent) {
              message.warning('Draft saved locally. Will sync when connection is restored.');
            }
          }
        } else {
          setPendingChanges(true);
          if (!silent) {
            message.info('Draft saved locally. Will sync when online.');
          }
        }

        setFormData(updatedFormData);

        if (onSaveDraft) {
          onSaveDraft(updatedFormData);
        }
      } catch (error) {
        console.error('Failed to save draft:', error);
        if (!silent) {
          message.error('Failed to save draft. Please try again.');
        }
      } finally {
        setSaving(false);
      }
    },
    [form, formData, workflowId, onSaveDraft, currentStep, completedSteps, isOffline, workflowData, getOverallCompletionPercentage]
  );

  // Network status monitoring with enhanced offline handling
  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      notification.success({
        message: 'Connection Restored',
        description: 'You are back online. Syncing your changes...',
        icon: <WifiOutlined style={{ color: '#52c41a' }} />,
        duration: 3
      });

      if (pendingChanges) {
        handleSaveDraft(true); // Auto-sync when back online
        setPendingChanges(false);
      }
    };

    const handleOffline = () => {
      setIsOffline(true);
      notification.warning({
        message: 'Connection Lost',
        description:
          'You are offline. Changes will be saved locally and synced when connection is restored.',
        icon: <DisconnectOutlined style={{ color: '#fa8c16' }} />,
        duration: 5
      });
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [pendingChanges, handleSaveDraft]);

  // Enhanced keyboard navigation
  useEffect(() => {
    const handleKeyDown = event => {
      // Ctrl/Cmd + S to save draft
      if ((event.ctrlKey || event.metaKey) && event.key === 's') {
        event.preventDefault();
        handleSaveDraft();
      }

      // Ctrl/Cmd + Right Arrow to go to next step
      if ((event.ctrlKey || event.metaKey) && event.key === 'ArrowRight') {
        event.preventDefault();
        if (currentStep < questionnaireSteps.length - 1) {
          handleNext();
        }
      }

      // Ctrl/Cmd + Left Arrow to go to previous step
      if ((event.ctrlKey || event.metaKey) && event.key === 'ArrowLeft') {
        event.preventDefault();
        if (currentStep > 0) {
          handlePrevious();
        }
      }

      // F1 to show help/shortcuts
      if (event.key === 'F1') {
        event.preventDefault();
        Modal.info({
          title: 'Keyboard Shortcuts',
          content: (
            <div>
              <p>
                <strong>Ctrl/Cmd + S:</strong> Save draft
              </p>
              <p>
                <strong>Ctrl/Cmd + →:</strong> Next step
              </p>
              <p>
                <strong>Ctrl/Cmd + ←:</strong> Previous step
              </p>
              <p>
                <strong>Tab:</strong> Navigate between fields
              </p>
              <p>
                <strong>Enter:</strong> Submit form or proceed
              </p>
              <p>
                <strong>Esc:</strong> Close modals
              </p>
            </div>
          )
        });
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [currentStep, questionnaireSteps.length, handleNext, handlePrevious, handleSaveDraft]);

  // Define functions before useEffect hooks that depend on them
  const loadWorkflowData = useCallback(async () => {
    try {
      setLoading(true);
      const workflow = await workflowAPI.getWorkflow(workflowId);
      setWorkflowData(workflow);

      // Pre-populate material name from workflow data (from ProjectItemMaster)
      const initialData = {};
      if (workflow.materialName) {
        initialData.materialName = workflow.materialName;
      }

      // Load existing responses if any
      if (workflow.responses && workflow.responses.length > 0) {
        const existingData = { ...initialData };
        const completed = new Set();

        workflow.responses.forEach(response => {
          existingData[response.fieldName] = response.fieldValue;
          completed.add(response.stepNumber);
        });

        setFormData(existingData);
        setCompletedSteps(completed);
        form.setFieldsValue(existingData);
      } else {
        // Set initial data even if no responses exist
        setFormData(initialData);
        form.setFieldsValue(initialData);
      }
    } catch (error) {
      console.error('Failed to load workflow data:', error);
      message.error('Failed to load workflow data');
    } finally {
      setLoading(false);
    }
  }, [workflowId, form]);

  const loadQueries = useCallback(async () => {
    try {
      const workflowQueries = await queryAPI.getQueriesByWorkflow(workflowId);
      setQueries(workflowQueries);
    } catch (error) {
      console.error('Failed to load queries:', error);
    }
  }, [workflowId]);

  // Load workflow data and existing responses
  useEffect(() => {
    if (workflowId) {
      loadWorkflowData();
      loadQueries();
    }
  }, [workflowId, loadWorkflowData, loadQueries]);

  // Load questionnaire template when workflow data is available
  useEffect(() => {
    if (workflowData && workflowData.materialCode && workflowData.assignedPlant) {
      loadQuestionnaireTemplate();
    }
  }, [workflowData, loadQuestionnaireTemplate]);

  // Auto-save functionality with recovery
  useEffect(() => {
    if (autoSaveEnabled && Object.keys(formData).length > 0) {
      const autoSaveTimer = setTimeout(() => {
        handleSaveDraft(true); // Silent save
      }, AUTO_SAVE.INTERVAL); // Auto-save every 30 seconds

      return () => clearTimeout(autoSaveTimer);
    }
  }, [formData, autoSaveEnabled, handleSaveDraft]);

  // Track form data changes and update completed steps
  useEffect(() => {
    if (questionnaireSteps.length > 0) {
      const newCompletedSteps = new Set();

      // Get current form values including any unsaved changes
      const currentFormValues = form.getFieldsValue();
      const currentData = { ...formData, ...currentFormValues };

      questionnaireSteps.forEach((step, index) => {
        const stepFields = step.fields || [];
        const requiredFields = stepFields.filter(field => field.required);

        const completedRequiredFields = requiredFields.filter(field => {
          const value = currentData[field.name];
          if (Array.isArray(value)) {
            return value.length > 0;
          }
          return value && value !== '' && value !== null && value !== undefined;
        });

        // Mark step as complete based on field completion
        if (requiredFields.length > 0) {
          // If there are required fields, all must be completed
          if (completedRequiredFields.length === requiredFields.length) {
            newCompletedSteps.add(index);
          }
        } else {
          // If no required fields, mark complete if at least 50% of fields are filled
          const completedOptionalFields = stepFields.filter(field => {
            const value = currentData[field.name];
            if (Array.isArray(value)) {
              return value.length > 0;
            }
            return value && value !== '' && value !== null && value !== undefined;
          });

          const completionPercentage =
            stepFields.length > 0 ? (completedOptionalFields.length / stepFields.length) * 100 : 0;

          if (completionPercentage >= 50) {
            newCompletedSteps.add(index);
          }
        }

        // Step completion calculated
      });

      // Step completion updated

      setCompletedSteps(newCompletedSteps);
    }
  }, [formData, questionnaireSteps, form]);

  // Enhanced form validation with field-specific rules
  const getFieldValidationRules = field => {
    const rules = [];

    if (field.required) {
      rules.push({
        required: true,
        message: `${field.label} is required for MSDS completion`
      });
    }

    // Add specific validation based on field type and name
    switch (field.name) {
      case 'casNumber':
        rules.push({
          pattern: /^\d{1,7}-\d{2}-\d$/,
          message:
            'Please enter a valid CAS number format (e.g., 64-17-5). If unknown, raise a query to the Technical team.'
        });
        break;
      case 'boilingPoint':
      case 'meltingPoint':
        rules.push({
          pattern: /^-?\d+(\.\d+)?$/,
          message: 'Please enter a valid temperature in Celsius (e.g., 100.5 or -10)'
        });
        break;
      case 'materialName':
        rules.push({
          min: 2,
          message: 'Material name must be at least 2 characters'
        });
        rules.push({
          max: 200,
          message: 'Material name cannot exceed 200 characters'
        });
        break;
      case 'supplierName':
        rules.push({
          min: 2,
          message: 'Supplier name must be at least 2 characters'
        });
        rules.push({
          max: 100,
          message: 'Supplier name cannot exceed 100 characters'
        });
        break;
      case 'missing_info':
      case 'exposure_minimization':
      case 'carcinogenic_control':
      case 'storage_conditions_stores':
      case 'storage_conditions_floor':
      case 'procedures_details':
      case 'antidote_source':
      case 'monitoring_details':
      case 'plant_inputs_required':
      case 'gaps_identified':
      case 'additional_input_1':
      case 'additional_input_2':
        rules.push({
          min: 10,
          message: `${field.label} must be at least 10 characters for regulatory compliance`
        });
        rules.push({
          max: 2000,
          message: `${field.label} cannot exceed 2000 characters`
        });
        break;
      default:
        break;
    }

    return rules;
  };

  // Get contextual help text for fields
  const getFieldHelpText = field => {
    const helpTexts = {
      msds_available:
        'Material Safety Data Sheet with all 16 sections as per regulatory requirements',
      cas_available:
        'Chemical Abstracts Service number - unique identifier for chemical substances',
      is_corrosive: 'Corrosive materials can cause damage to skin, eyes, and respiratory system',
      highly_toxic: 'Materials with high toxicity require special handling and safety measures',
      flash_point_65:
        'Flash point indicates fire hazard - materials with flash point ≤65°C are flammable',
      petroleum_class:
        'Classification under Petroleum Act determines storage and handling requirements',
      ld50_oral: 'Lethal Dose 50 (oral) - dose that kills 50% of test animals when ingested',
      ld50_dermal:
        'Lethal Dose 50 (dermal) - dose that kills 50% of test animals through skin contact',
      lc50_inhalation:
        'Lethal Concentration 50 (inhalation) - concentration that kills 50% through inhalation',
      carcinogenic: 'Materials suspected to cause cancer require enhanced safety protocols',
      recommended_ppe: 'Personal Protective Equipment recommendations based on material hazards',
      is_poisonous: 'Poisonous materials require specific antidotes and first aid procedures',
      cmvr_listed:
        'Chemical Manufacture and Verification Rules listing affects regulatory compliance',
      msihc_listed: 'Manufacture, Storage and Import of Hazardous Chemical Rules listing',
      factories_act_listed: 'Factories Act Schedule II listing requires workplace monitoring'
    };

    return helpTexts[field.name] || field.help;
  };

  // Enhanced auto-recovery on component mount with improved error handling
  useEffect(() => {
    const recoverDraftData = () => {
      try {
        const draftKey = `plant_questionnaire_draft_${workflowId}`;
        const savedDraft = localStorage.getItem(draftKey);

        if (savedDraft) {
          const draftData = JSON.parse(savedDraft);
          const draftTimestamp = draftData.timestamp;
          const currentTime = Date.now();

          // Only recover if draft is less than 7 days old (extended from 24 hours)
          if (currentTime - draftTimestamp < 7 * 24 * 60 * 60 * 1000) {
            // Enhanced validation of draft data integrity
            if (draftData.formData && typeof draftData.formData === 'object') {
              // Validate each field value before setting
              const validatedFormData = {};
              Object.entries(draftData.formData).forEach(([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                  validatedFormData[key] = value;
                }
              });

              setFormData(prev => ({ ...prev, ...validatedFormData }));
              form.setFieldsValue(validatedFormData);

              if (
                typeof draftData.currentStep === 'number' &&
                draftData.currentStep >= 0 &&
                draftData.currentStep < questionnaireSteps.length
              ) {
                setCurrentStep(draftData.currentStep);
              }

              if (Array.isArray(draftData.completedSteps)) {
                setCompletedSteps(new Set(draftData.completedSteps));
              }

              // Check if there are pending changes to sync
              if (draftData.syncStatus === 'pending') {
                setPendingChanges(true);
              }

              const recoveredFields = Object.keys(validatedFormData).length;
              const draftAge = Math.round((currentTime - draftTimestamp) / (1000 * 60 * 60));

              notification.success({
                message: 'Draft Recovered',
                description: `${recoveredFields} fields restored from ${draftAge} hours ago. Your progress has been preserved.`,
                duration: 6,
                placement: 'topRight'
              });
            } else {
              // Remove corrupted draft
              localStorage.removeItem(draftKey);
              notification.warning({
                message: 'Draft Recovery Failed',
                description: 'Previous draft data was corrupted and has been cleared.',
                duration: 4
              });
            }
          } else {
            // Remove old draft
            localStorage.removeItem(draftKey);
            const draftAge = Math.round((currentTime - draftTimestamp) / (1000 * 60 * 60 * 24));
            notification.info({
              message: 'Old Draft Cleared',
              description: `Draft from ${draftAge} days ago was automatically removed.`,
              duration: 3
            });
          }
        }
      } catch (error) {
        console.error('Failed to recover draft data:', error);
        // Remove corrupted draft
        try {
          localStorage.removeItem(`plant_questionnaire_draft_${workflowId}`);
          notification.error({
            message: 'Draft Recovery Error',
            description: 'Failed to recover previous draft. Starting fresh.',
            duration: 4
          });
        } catch (removeError) {
          console.error('Failed to remove corrupted draft:', removeError);
        }
      }
    };

    if (workflowId && !workflowData) {
      recoverDraftData();
    }
  }, [workflowId, workflowData, form, questionnaireSteps.length]);

  // Enhanced step completion tracking with validation
  const getStepCompletionStatus = useCallback(stepIndex => {
    if (!questionnaireSteps[stepIndex] || !questionnaireSteps[stepIndex].fields) {
      return {
        total: 0,
        required: 0,
        optional: 0,
        completed: 0,
        requiredCompleted: 0,
        optionalCompleted: 0,
        isComplete: false,
        hasOpenQueries: false,
        hasResolvedQueries: false,
        openQueriesCount: 0,
        resolvedQueriesCount: 0,
        completionPercentage: 0,
        requiredCompletionPercentage: 0
      };
    }

    const stepFields = questionnaireSteps[stepIndex].fields;
    const requiredFields = stepFields.filter(field => field.required);
    const optionalFields = stepFields.filter(field => !field.required);

    // Get current form values including any unsaved changes
    const currentFormValues = form.getFieldsValue();
    const currentData = { ...formData, ...currentFormValues };

    const completedRequiredFields = requiredFields.filter(field => {
      const value = currentData[field.name];
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return value && value !== '' && value !== null && value !== undefined;
    });

    const completedOptionalFields = optionalFields.filter(field => {
      const value = currentData[field.name];
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return value && value !== '' && value !== null && value !== undefined;
    });

    const stepQueries = queries.filter(q => q.stepNumber === stepIndex);
    const openQueries = stepQueries.filter(q => q.status === 'OPEN');
    const resolvedQueries = stepQueries.filter(q => q.status === 'RESOLVED');

    return {
      total: stepFields.length,
      required: requiredFields.length,
      optional: optionalFields.length,
      completed: completedRequiredFields.length + completedOptionalFields.length,
      requiredCompleted: completedRequiredFields.length,
      optionalCompleted: completedOptionalFields.length,
      isComplete:
        requiredFields.length > 0
          ? completedRequiredFields.length === requiredFields.length
          : stepFields.length > 0 &&
          (completedRequiredFields.length + completedOptionalFields.length) / stepFields.length >=
          0.5,
      hasOpenQueries: openQueries.length > 0,
      hasResolvedQueries: resolvedQueries.length > 0,
      openQueriesCount: openQueries.length,
      resolvedQueriesCount: resolvedQueries.length,
      completionPercentage:
        stepFields.length > 0
          ? Math.round(
            ((completedRequiredFields.length + completedOptionalFields.length) /
              stepFields.length) *
            100
          )
          : 100,
      requiredCompletionPercentage:
        requiredFields.length > 0
          ? Math.round((completedRequiredFields.length / requiredFields.length) * 100)
          : 100
    };
  }, [questionnaireSteps, formData, form, queries]);

  const handleRaiseQuery = fieldName => {
    console.log('handleRaiseQuery called with fieldName:', fieldName);
    const field = questionnaireSteps[currentStep].fields.find(f => f.name === fieldName);
    const currentValue = formData[fieldName] || form.getFieldValue(fieldName);

    console.log('Found field:', field);
    console.log('Current value:', currentValue);

    setSelectedField({
      ...field,
      stepNumber: currentStep,
      stepTitle: questionnaireSteps[currentStep].title,
      currentValue,
      materialContext: {
        materialCode: workflowData?.materialCode,
        materialName: workflowData?.materialName,
        materialType: formData.materialType || workflowData?.materialType,
        supplierName: formData.supplierName || workflowData?.supplierName
      }
    });

    console.log('Setting queryModalVisible to true');
    setQueryModalVisible(true);
  };

  const handleQueryCreated = _queryData => {
    setQueryModalVisible(false);
    setSelectedField(null);
    loadQueries(); // Reload queries
    message.success('Query raised successfully');
  };

  // Auto-scroll to field with resolved query
  const scrollToResolvedQuery = useCallback(fieldName => {
    setTimeout(() => {
      const fieldElement = document.querySelector(`[data-field-name="${fieldName}"]`);
      if (fieldElement) {
        fieldElement.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
          inline: 'nearest'
        });

        // Highlight the field briefly
        fieldElement.style.transition = 'background-color 0.3s ease';
        fieldElement.style.backgroundColor = '#f6ffed';
        setTimeout(() => {
          fieldElement.style.backgroundColor = '';
        }, 2000);
      }
    }, 100);
  }, []);

  // Check for newly resolved queries and auto-scroll
  useEffect(() => {
    if (queries.length > 0) {
      const resolvedQueriesInCurrentStep = queries.filter(
        q => q.stepNumber === currentStep && q.status === 'RESOLVED' && !q.hasBeenViewed // Add this flag to track if user has seen the resolution
      );

      if (resolvedQueriesInCurrentStep.length > 0) {
        const latestResolvedQuery = resolvedQueriesInCurrentStep.sort(
          (a, b) => new Date(b.resolvedAt) - new Date(a.resolvedAt)
        )[0];

        scrollToResolvedQuery(latestResolvedQuery.fieldName);

        // Show notification about resolved query
        notification.success({
          message: 'Query Resolved',
          description: `Your query about "${latestResolvedQuery.fieldName}" has been resolved. Check the field for the response.`,
          duration: 5,
          placement: 'topRight'
        });
      }
    }
  }, [queries, currentStep, scrollToResolvedQuery]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);

      // Check for open queries
      const openQueries = queries.filter(q => q.status === 'OPEN');
      if (openQueries.length > 0) {
        Modal.confirm({
          title: 'Open Queries Detected',
          content: `You have ${openQueries.length} open queries. Are you sure you want to submit the questionnaire? It's recommended to resolve all queries before submission.`,
          okText: 'Submit Anyway',
          cancelText: 'Cancel',
          onOk: () => proceedWithSubmission()
        });
        return;
      }

      await proceedWithSubmission();
    } catch (error) {
      console.error('Failed to submit questionnaire:', error);
      if (error.status === 400) {
        message.error('Please complete all required fields before submitting');
      } else if (error.status === 401) {
        message.error('Session expired. Please log in again.');
      } else {
        message.error('Failed to submit questionnaire. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const proceedWithSubmission = async () => {
    // Validate all required fields
    const allRequiredFields = questionnaireSteps.flatMap(step =>
      step.fields.filter(field => field.required).map(field => field.name)
    );

    await form.validateFields(allRequiredFields);

    const finalData = form.getFieldsValue();

    // Check completion percentage
    const completionPercentage = getOverallCompletionPercentage();
    if (completionPercentage < 80) {
      const proceed = await new Promise(resolve => {
        Modal.confirm({
          title: 'Incomplete Questionnaire',
          content: `Your questionnaire is only ${completionPercentage}% complete. Are you sure you want to submit?`,
          okText: 'Submit',
          cancelText: 'Continue Editing',
          onOk: () => resolve(true),
          onCancel: () => resolve(false)
        });
      });

      if (!proceed) {
        return;
      }
    }

    const submissionData = {
      plantCode: workflowData?.assignedPlant,
      materialCode: workflowData?.materialCode,
      responses: finalData,
      completionPercentage,
      submittedBy: 'current_user',
      totalQueries: queries.length,
      openQueries: queries.filter(q => q.status === 'OPEN').length
    };

    await workflowAPI.submitPlantQuestionnaire(workflowId, submissionData);

    // Clear draft data after successful submission
    try {
      localStorage.removeItem(`plant_questionnaire_draft_${workflowId}`);
    } catch (error) {
      console.warn('Failed to clear draft data:', error);
    }

    message.success('Questionnaire submitted successfully');

    if (onComplete) {
      onComplete(finalData);
    }
  };

  const renderField = field => {
    const fieldQueries = queries.filter(
      q => q.fieldName === field.name && q.stepNumber === currentStep
    );

    const hasOpenQuery = fieldQueries.some(q => q.status === 'OPEN');
    const hasResolvedQuery = fieldQueries.some(q => q.status === 'RESOLVED');
    const resolvedQuery = fieldQueries.find(q => q.status === 'RESOLVED');

    const isFieldCompleted = formData[field.name] && formData[field.name] !== '';

    const fieldLabel = (
      <div className="modern-field-label">
        <Space align="center">
          {field.label}
          {field.required && <span style={{ color: '#ef4444' }}>*</span>}
          {field.isCqsAutoPopulated && <span className="cqs-badge">CQS</span>}
          {isFieldCompleted && (
            <Tooltip title="Field completed">
              <CheckCircleOutlined style={{ color: '#10b981', fontSize: '14px' }} />
            </Tooltip>
          )}
        </Space>
        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
          <Button
            type="text"
            size="small"
            icon={<QuestionCircleOutlined />}
            onClick={() => handleRaiseQuery(field.name)}
            style={{
              color: '#667eea',
              padding: '2px 6px',
              height: 'auto',
              fontSize: '12px'
            }}
          >
            Query
          </Button>
          {hasOpenQuery && (
            <Tag color="red" size="small">
              Query Open
            </Tag>
          )}
          {hasResolvedQuery && !hasOpenQuery && (
            <Tag color="green" size="small">
              Query Resolved
            </Tag>
          )}
        </div>
      </div>
    );

    // Enhanced validation rules
    const validationRules = getFieldValidationRules(field);

    const helpContent = resolvedQuery ? (
      <div
        style={{
          marginTop: 8,
          padding: '12px 16px',
          background: 'linear-gradient(135deg, #f0fdf4, #dcfce7)',
          border: '1px solid #bbf7d0',
          borderRadius: '8px',
          fontSize: '12px'
        }}
      >
        <div style={{ marginBottom: 6 }}>
          <Text strong style={{ color: '#059669' }}>
            Query Response:
          </Text>
        </div>
        <div style={{ marginBottom: 6, color: '#374151' }}>{resolvedQuery.response}</div>
        <div style={{ fontSize: '10px', color: '#6b7280' }}>
          Resolved by {resolvedQuery.resolvedBy} on{' '}
          {new Date(resolvedQuery.resolvedAt).toLocaleDateString()}
        </div>
      </div>
    ) : (
      getFieldHelpText(field)
    );

    const commonProps = {
      name: field.name,
      label: fieldLabel,
      rules: validationRules,
      help: helpContent,
      'data-field-name': field.name
    };

    const inputProps = {
      className: 'modern-input',
      disabled: field.disabled || field.isCqsAutoPopulated,
      placeholder: field.placeholder || `Enter ${field.label.toLowerCase()}`
    };

    switch (field.type) {
      case 'input':
        return (
          <Form.Item {...commonProps}>
            <Input {...inputProps} />
          </Form.Item>
        );

      case 'textarea':
        return (
          <Form.Item {...commonProps}>
            <TextArea {...inputProps} rows={4} autoSize={{ minRows: 3, maxRows: 6 }} />
          </Form.Item>
        );

      case 'select':
        return (
          <Form.Item {...commonProps}>
            <Select
              {...inputProps}
              placeholder={`Select ${field.label.toLowerCase()}`}
              showSearch
              optionFilterProp="children"
            >
              {field.options?.map(option => (
                <Option key={option.value} value={option.value}>
                  {option.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
        );

      case 'radio':
        return (
          <Form.Item {...commonProps}>
            <Radio.Group
              className="modern-radio-group"
              disabled={field.disabled || field.isCqsAutoPopulated}
            >
              <Space direction="vertical" size="small">
                {field.options?.map(option => (
                  <Radio key={option.value} value={option.value}>
                    {option.label}
                  </Radio>
                ))}
              </Space>
            </Radio.Group>
          </Form.Item>
        );

      case 'checkbox':
        return (
          <Form.Item {...commonProps} valuePropName="checked">
            <Checkbox.Group
              className="modern-checkbox-group"
              disabled={field.disabled || field.isCqsAutoPopulated}
            >
              <Space direction="vertical" size="small">
                {field.options?.map(option => (
                  <Checkbox key={option.value} value={option.value}>
                    {option.label}
                  </Checkbox>
                ))}
              </Space>
            </Checkbox.Group>
          </Form.Item>
        );

      default:
        return (
          <Form.Item {...commonProps}>
            <Input {...inputProps} />
          </Form.Item>
        );
    }
  };

  if (loading) {
    return (
      <div
        className="plant-questionnaire-container"
        style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
          gap: '24px'
        }}
      >
        <div
          style={{
            background: 'white',
            padding: '48px',
            borderRadius: '16px',
            boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
            textAlign: 'center',
            maxWidth: '400px'
          }}
        >
          <div style={{ marginBottom: '24px' }}>
            <div
              style={{
                width: '60px',
                height: '60px',
                margin: '0 auto 16px',
                background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <FileTextOutlined style={{ fontSize: '24px', color: 'white' }} />
            </div>
            <Title level={3} style={{ margin: 0, color: '#1e293b' }}>
              Loading Questionnaire
            </Title>
            <Text type="secondary" style={{ fontSize: '14px' }}>
              Preparing your workflow data...
            </Text>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <Spin size="large" />
          </div>

          <div
            style={{
              height: '4px',
              background: '#e2e8f0',
              borderRadius: '2px',
              overflow: 'hidden'
            }}
          >
            <div
              style={{
                height: '100%',
                width: '30%',
                background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                borderRadius: '2px',
                animation: 'slideInFromLeft 2s ease-in-out infinite'
              }}
            />
          </div>
        </div>
      </div>
    );
  }

  if (!workflowData) {
    return (
      <Alert
        message="Workflow Not Found"
        description="The requested workflow could not be loaded."
        type="error"
        showIcon
      />
    );
  }

  // Add safety checks for questionnaire steps
  if (templateLoading) {
    return (
      <div
        className="plant-questionnaire-container"
        style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
          gap: '24px'
        }}
      >
        <div
          style={{
            background: 'white',
            padding: '48px',
            borderRadius: '16px',
            boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
            textAlign: 'center',
            maxWidth: '400px'
          }}
        >
          <div style={{ marginBottom: '24px' }}>
            <div
              style={{
                width: '60px',
                height: '60px',
                margin: '0 auto 16px',
                background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <ExperimentOutlined style={{ fontSize: '24px', color: 'white' }} />
            </div>
            <Title level={3} style={{ margin: 0, color: '#1e293b' }}>
              Loading Template
            </Title>
            <Text type="secondary" style={{ fontSize: '14px' }}>
              Preparing your plant-specific questionnaire template...
            </Text>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <Spin size="large" />
          </div>

          <div
            style={{
              height: '4px',
              background: '#e2e8f0',
              borderRadius: '2px',
              overflow: 'hidden'
            }}
          >
            <div
              style={{
                height: '100%',
                width: '60%',
                background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                borderRadius: '2px',
                animation: 'slideInFromLeft 2s ease-in-out infinite'
              }}
            />
          </div>
        </div>
      </div>
    );
  }

  if (!questionnaireSteps || questionnaireSteps.length === 0) {
    return (
      <Alert
        message="Template Not Available"
        description="The questionnaire template could not be loaded. Please try refreshing the page."
        type="error"
        showIcon
        action={
          <Button size="small" onClick={() => window.location.reload()}>
            Refresh
          </Button>
        }
      />
    );
  }

  // const progress = Math.round(((currentStep + 1) / questionnaireSteps.length) * 100); // Not currently used
  const currentStepData = questionnaireSteps[currentStep] || {
    title: 'Loading...',
    description: '',
    fields: []
  };

  return (
    <div className="plant-questionnaire-container">
      {/* Modern Header */}
      <div className="plant-questionnaire-header">
        <div className="plant-questionnaire-header-content">
          <div className="plant-questionnaire-title">
            <Avatar size={40} style={{ background: 'linear-gradient(135deg, #667eea, #764ba2)' }}>
              <DashboardOutlined />
            </Avatar>
            <div>
              <Title level={3} style={{ margin: 0, color: '#1e293b' }}>
                Plant Questionnaire
              </Title>
              <Text type="secondary" style={{ fontSize: '14px' }}>
                {workflowData?.materialCode} • {workflowData?.assignedPlant}
              </Text>
            </div>
          </div>

          <div className="plant-questionnaire-stats">
            <div className="modern-stats-card">
              <div
                className="modern-progress-ring"
                style={{ '--progress': getOverallCompletionPercentage() }}
              >
                <svg>
                  <defs>
                    <linearGradient id="progressGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stopColor="#4f46e5" />
                      <stop offset="100%" stopColor="#7c3aed" />
                    </linearGradient>
                  </defs>
                  <circle className="progress-circle progress-background" cx="30" cy="30" r="26" />
                  <circle className="progress-circle progress-foreground" cx="30" cy="30" r="26" />
                </svg>
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px',
                    fontWeight: '600',
                    color: '#1e293b'
                  }}
                >
                  {getOverallCompletionPercentage()}%
                </div>
              </div>
              <div style={{ textAlign: 'center', marginTop: 8 }}>
                <Text style={{ fontSize: '11px', color: '#64748b' }}>Overall Progress</Text>
              </div>
            </div>

            <div className="modern-stats-card">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 8
                }}
              >
                <Badge
                  count={queries.filter(q => q.status === 'OPEN').length}
                  style={{
                    backgroundColor:
                      queries.filter(q => q.status === 'OPEN').length > 0 ? '#ef4444' : '#10b981'
                  }}
                >
                  <Avatar
                    icon={<QuestionCircleOutlined />}
                    style={{
                      backgroundColor:
                        queries.filter(q => q.status === 'OPEN').length > 0 ? '#fef2f2' : '#f0fdf4',
                      color:
                        queries.filter(q => q.status === 'OPEN').length > 0 ? '#ef4444' : '#10b981'
                    }}
                  />
                </Badge>
              </div>
              <Text
                style={{
                  fontSize: '11px',
                  color: '#64748b',
                  display: 'block',
                  textAlign: 'center'
                }}
              >
                Open Queries
              </Text>
            </div>

            <div className="modern-stats-card">
              <div style={{ textAlign: 'center' }}>
                <div
                  className={`modern-completion-badge ${getOverallCompletionPercentage() >= 80
                    ? 'high'
                    : getOverallCompletionPercentage() >= 50
                      ? 'medium'
                      : 'low'
                    }`}
                >
                  Step {currentStep + 1}/{questionnaireSteps.length}
                </div>
                <Text
                  style={{ fontSize: '11px', color: '#64748b', display: 'block', marginTop: 4 }}
                >
                  Current Step
                </Text>
              </div>
            </div>

            {/* Header Actions */}
            <Space>
              <Button
                className="modern-btn modern-btn-secondary"
                icon={<SaveOutlined />}
                onClick={() => handleSaveDraft()}
                loading={saving}
              >
                Save Draft
              </Button>
              {isMobile && (
                <Button
                  className="modern-btn modern-btn-secondary"
                  icon={<MenuOutlined />}
                  onClick={() => setSidebarVisible(true)}
                >
                  Steps
                </Button>
              )}
            </Space>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="plant-questionnaire-main">
        {/* Sidebar - Steps Navigation */}
        {!isMobile && (
          <div className="plant-questionnaire-sidebar">
            <Card className="modern-steps-card">
              <div className="modern-steps-header">
                <Title level={4} style={{ margin: 0, color: '#1e293b' }}>
                  Questionnaire Steps
                </Title>
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  {completedSteps.size} of {questionnaireSteps.length} steps completed
                </Text>
                <Text type="secondary" style={{ fontSize: '11px', display: 'block', marginTop: 2 }}>
                  {(() => {
                    const fieldStats = getTotalFieldsPopulated();
                    return `${fieldStats.populated} of ${fieldStats.total} fields populated`;
                  })()}
                </Text>
              </div>

              <div className="modern-steps-list">
                {questionnaireSteps.map((step, index) => {
                  const stepStatus = getStepCompletionStatus(index);
                  const hasOpenQueries = queries.some(
                    q => q.stepNumber === index && q.status === 'OPEN'
                  );
                  const isActive = index === currentStep;
                  const isCompleted = stepStatus.isComplete;

                  return (
                    <div
                      key={index}
                      className={`modern-step-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}
                      onClick={() => handleStepChange(index)}
                    >
                      <div
                        className={`step-completion-ring ${isCompleted ? 'completed' : ''}`}
                        style={{
                          '--completion-angle': `${(stepStatus.requiredCompleted / Math.max(stepStatus.required, 1)) * 360}deg`,
                          background: isActive ? '#4f46e5' : isCompleted ? '#10b981' : '#f1f5f9',
                          color: isActive || isCompleted ? 'white' : '#64748b'
                        }}
                      >
                        {isCompleted ? (
                          <CheckCircleOutlined />
                        ) : hasOpenQueries ? (
                          <ExclamationCircleOutlined />
                        ) : (
                          stepIcons[step.title] || <FileTextOutlined />
                        )}
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: isActive ? 600 : 400, color: '#1e293b' }}>
                          {step.title}
                        </div>
                        <div style={{ fontSize: '12px', color: '#64748b', marginTop: 2 }}>
                          {stepStatus.completed}/{stepStatus.total} fields (
                          {stepStatus.requiredCompleted}/{stepStatus.required} required)
                          {hasOpenQueries && (
                            <Tag color="red" size="small" style={{ marginLeft: 4 }}>
                              {
                                queries.filter(q => q.stepNumber === index && q.status === 'OPEN')
                                  .length
                              }{' '}
                              queries
                            </Tag>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </Card>

            {/* Material Context Panel */}
            <div style={{ marginTop: 16 }}>
              <MaterialContextPanel workflowData={workflowData} />
            </div>
          </div>
        )}

        {/* Main Form Content */}
        <div className="plant-questionnaire-content">
          <Card className="modern-form-card">
            <div className="modern-form-header">
              <div>
                <Title level={2} style={{ margin: 0, color: 'white' }}>
                  {currentStepData.title}
                </Title>
                <Paragraph style={{ margin: '8px 0 0 0', color: 'rgba(255, 255, 255, 0.8)' }}>
                  {currentStepData.description}
                </Paragraph>
              </div>

              <div className="modern-progress-container">
                <Progress
                  className="modern-progress"
                  percent={(() => {
                    const status = getStepCompletionStatus(currentStep);
                    return status.required > 0
                      ? Math.round((status.requiredCompleted / status.required) * 100)
                      : 100;
                  })()}
                  showInfo={false}
                  strokeColor="rgba(255, 255, 255, 0.9)"
                  trailColor="rgba(255, 255, 255, 0.2)"
                />
                <Text style={{ color: 'rgba(255, 255, 255, 0.8)', fontSize: '12px' }}>
                  Step Progress:{' '}
                  {(() => {
                    const status = getStepCompletionStatus(currentStep);
                    return `${status.requiredCompleted}/${status.required} required fields`;
                  })()}
                </Text>
              </div>
            </div>

            {/* Offline Indicator */}
            {isOffline && (
              <Alert
                className="modern-alert modern-alert-warning"
                message="Offline Mode"
                description="You are currently offline. Changes will be saved locally and synced when connection is restored."
                type="warning"
                showIcon
                style={{ margin: '16px 32px' }}
                closable
              />
            )}

            <div
              className={`modern-form-content ${currentStep % 2 === 0 ? 'slide-in-from-right' : 'slide-in-from-left'}`}
            >
              <Form
                form={form}
                layout="vertical"
                onValuesChange={(changedValues, allValues) => {
                  setFormData(prev => ({ ...prev, ...allValues }));
                  setPendingChanges(true);
                  _setProgressUpdateTrigger(prev => prev + 1);

                  // Trigger completion status update
                  setTimeout(() => {
                    if (questionnaireSteps.length > 0) {
                      const newCompletedSteps = new Set();

                      questionnaireSteps.forEach((step, index) => {
                        const stepFields = step.fields || [];
                        const requiredFields = stepFields.filter(field => field.required);

                        const completedRequiredFields = requiredFields.filter(field => {
                          const value = allValues[field.name];
                          if (Array.isArray(value)) {
                            return value.length > 0;
                          }
                          return value && value !== '' && value !== null && value !== undefined;
                        });

                        // Mark step as complete based on field completion
                        if (requiredFields.length > 0) {
                          // If there are required fields, all must be completed
                          if (completedRequiredFields.length === requiredFields.length) {
                            newCompletedSteps.add(index);
                          }
                        } else {
                          // If no required fields, mark complete if at least 50% of fields are filled
                          const completedOptionalFields = stepFields.filter(field => {
                            const value = allValues[field.name];
                            if (Array.isArray(value)) {
                              return value.length > 0;
                            }
                            return value && value !== '' && value !== null && value !== undefined;
                          });

                          const completionPercentage =
                            stepFields.length > 0
                              ? (completedOptionalFields.length / stepFields.length) * 100
                              : 0;

                          if (completionPercentage >= 50) {
                            newCompletedSteps.add(index);
                          }
                        }
                      });

                      // Step completion updated on form change

                      setCompletedSteps(newCompletedSteps);
                    }
                  }, 100); // Small delay to ensure form state is updated
                }}
              >
                <Row gutter={[24, 24]}>
                  {(currentStepData.fields || []).map((field, index) => (
                    <Col
                      key={field.name}
                      xs={24}
                      sm={field.type === 'textarea' ? 24 : 12}
                      md={field.type === 'textarea' ? 24 : 12}
                    >
                      <div
                        className={`modern-field-group ${field.required ? 'required' : ''} ${field.isCqsAutoPopulated ? 'cqs-populated' : ''} fade-in-up`}
                        style={{ animationDelay: `${index * 0.1}s` }}
                      >
                        <div className="modern-field-header">
                          <div style={{ flex: 1 }}>{renderField(field)}</div>
                          <div className="modern-field-actions">
                            <Tooltip title="Raise Query">
                              <button
                                className="modern-query-button"
                                onClick={() => handleRaiseQuery(field.name)}
                              >
                                <QuestionCircleOutlined />
                              </button>
                            </Tooltip>
                            {field.isCqsAutoPopulated && (
                              <Tooltip title="CQS Auto-populated">
                                <div
                                  style={{
                                    background: 'linear-gradient(135deg, #10b981, #059669)',
                                    color: 'white',
                                    padding: '2px 6px',
                                    borderRadius: '4px',
                                    fontSize: '10px',
                                    fontWeight: '500'
                                  }}
                                >
                                  CQS
                                </div>
                              </Tooltip>
                            )}
                          </div>
                        </div>
                      </div>
                    </Col>
                  ))}
                </Row>
              </Form>
            </div>

            {/* Navigation */}
            <div className="modern-navigation">
              <Button
                className="modern-btn modern-btn-secondary"
                icon={<ArrowLeftOutlined />}
                onClick={handlePrevious}
                disabled={currentStep === 0}
                style={{
                  opacity: currentStep === 0 ? 0.5 : 1,
                  cursor: currentStep === 0 ? 'not-allowed' : 'pointer'
                }}
              >
                Previous
              </Button>

              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div style={{ textAlign: 'center' }}>
                  <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>
                    Step Progress
                  </Text>
                  <Text style={{ fontSize: '14px', fontWeight: '600', color: '#1e293b' }}>
                    {currentStep + 1} of {questionnaireSteps.length}
                  </Text>
                </div>

                {pendingChanges && (
                  <div
                    style={{
                      padding: '4px 8px',
                      background: '#fef3c7',
                      border: '1px solid #fde68a',
                      borderRadius: '6px',
                      fontSize: '11px',
                      color: '#92400e'
                    }}
                  >
                    Unsaved changes
                  </div>
                )}

                {currentStep === questionnaireSteps.length - 1 ? (
                  <Button
                    className={`modern-btn modern-btn-primary ${getOverallCompletionPercentage() === 100 ? 'pulse-glow' : ''}`}
                    onClick={handleSubmit}
                    loading={submitting}
                    size="large"
                    style={{ minWidth: '160px' }}
                  >
                    {submitting ? 'Submitting...' : 'Submit Questionnaire'}
                  </Button>
                ) : (
                  <Button
                    className="modern-btn modern-btn-primary"
                    icon={<ArrowRightOutlined />}
                    onClick={handleNext}
                    style={{ minWidth: '120px' }}
                  >
                    Next Step
                  </Button>
                )}
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Mobile Sidebar Drawer */}
      <Drawer
        className="modern-drawer"
        title="Questionnaire Steps"
        placement="left"
        onClose={() => setSidebarVisible(false)}
        open={sidebarVisible}
        width={320}
      >
        <div className="modern-timeline">
          <Timeline>
            {questionnaireSteps.map((step, index) => {
              const stepStatus = getStepCompletionStatus(index);
              const hasOpenQueries = queries.some(
                q => q.stepNumber === index && q.status === 'OPEN'
              );
              const isActive = index === currentStep;
              const isCompleted = stepStatus.isComplete;

              return (
                <Timeline.Item
                  key={index}
                  color={isCompleted ? '#10b981' : isActive ? '#667eea' : '#cbd5e1'}
                  dot={
                    isCompleted ? (
                      <CheckCircleOutlined style={{ color: '#10b981' }} />
                    ) : hasOpenQueries ? (
                      <ExclamationCircleOutlined style={{ color: '#ef4444' }} />
                    ) : (
                      stepIcons[step.title] || <FileTextOutlined />
                    )
                  }
                >
                  <div
                    onClick={() => {
                      handleStepChange(index);
                      setSidebarVisible(false);
                    }}
                    style={{ cursor: 'pointer', padding: '8px 0' }}
                  >
                    <Text strong={isActive} style={{ color: isActive ? '#667eea' : '#1e293b' }}>
                      {step.title}
                    </Text>
                    <div style={{ fontSize: '12px', color: '#64748b', marginTop: 4 }}>
                      {stepStatus.completed}/{stepStatus.total} fields completed (
                      {stepStatus.requiredCompleted}/{stepStatus.required} required)
                      {hasOpenQueries && (
                        <Tag color="red" size="small" style={{ marginLeft: 4 }}>
                          {
                            queries.filter(q => q.stepNumber === index && q.status === 'OPEN')
                              .length
                          }{' '}
                          queries
                        </Tag>
                      )}
                    </div>
                  </div>
                </Timeline.Item>
              );
            })}
          </Timeline>
        </div>
      </Drawer>

      {/* Modern Floating Completion Indicator */}
      {!isMobile && (
        <div className="modern-floating-indicator">
          <div style={{ textAlign: 'center', marginBottom: 12 }}>
            <Text strong style={{ color: '#1e293b', fontSize: '14px' }}>
              Questionnaire Progress
            </Text>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <div
              className="modern-progress-ring"
              style={{
                '--progress': getOverallCompletionPercentage(),
                width: '40px',
                height: '40px'
              }}
            >
              <svg>
                <circle className="progress-circle progress-background" cx="20" cy="20" r="16" />
                <circle className="progress-circle progress-foreground" cx="20" cy="20" r="16" />
              </svg>
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '10px',
                  fontWeight: '600',
                  color: '#1e293b'
                }}
              >
                {getOverallCompletionPercentage()}%
              </div>
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#1e293b', fontWeight: '500' }}>
                {completedSteps.size} of {questionnaireSteps.length} steps
              </div>
              <div style={{ fontSize: '11px', color: '#64748b' }}>
                {(() => {
                  const fieldStats = getTotalFieldsPopulated();
                  return `${fieldStats.populated}/${fieldStats.total} fields`;
                })()}
              </div>
              <div style={{ fontSize: '11px', color: '#64748b' }}>
                {queries.filter(q => q.status === 'OPEN').length} open queries
              </div>
            </div>
          </div>
          <div
            style={{
              height: '2px',
              background: '#e2e8f0',
              borderRadius: '1px',
              overflow: 'hidden'
            }}
          >
            <div
              style={{
                height: '100%',
                width: `${getOverallCompletionPercentage()}%`,
                background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                transition: 'width 0.5s ease'
              }}
            />
          </div>
        </div>
      )}

      {/* Floating Action Buttons */}
      <div className="modern-floating-actions">
        <FloatButton.Group
          trigger="hover"
          type="primary"
          style={{ right: 24 }}
          icon={<SettingOutlined />}
        >
          <FloatButton
            icon={<SaveOutlined />}
            tooltip="Save Draft"
            onClick={() => handleSaveDraft()}
          />
          <FloatButton
            icon={<DashboardOutlined />}
            tooltip="Summary"
            onClick={() => {
              const summaryData = questionnaireSteps.map((step, index) => {
                const status = getStepCompletionStatus(index);
                const stepQueries = queries.filter(q => q.stepNumber === index);
                return {
                  step: index + 1,
                  title: step.title,
                  completed: status.requiredCompleted,
                  required: status.required,
                  percentage:
                    status.required > 0
                      ? Math.round((status.requiredCompleted / status.required) * 100)
                      : 100,
                  openQueries: stepQueries.filter(q => q.status === 'OPEN').length,
                  resolvedQueries: stepQueries.filter(q => q.status === 'RESOLVED').length
                };
              });

              Modal.info({
                title: 'Questionnaire Summary',
                width: 600,
                content: (
                  <div>
                    <div style={{ marginBottom: 16 }}>
                      <Text strong>Overall Progress: {getOverallCompletionPercentage()}%</Text>
                    </div>
                    {summaryData.map(step => (
                      <div
                        key={step.step}
                        style={{
                          marginBottom: 12,
                          padding: '12px 16px',
                          background:
                            step.percentage === 100
                              ? 'linear-gradient(135deg, #f0fdf4, #dcfce7)'
                              : 'linear-gradient(135deg, #fffbeb, #fef3c7)',
                          border: `1px solid ${step.percentage === 100 ? '#bbf7d0' : '#fde68a'}`,
                          borderRadius: '8px'
                        }}
                      >
                        <div
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                          }}
                        >
                          <Text strong>
                            Step {step.step}: {step.title}
                          </Text>
                          <Tag color={step.percentage === 100 ? 'green' : 'orange'}>
                            {step.percentage}%
                          </Tag>
                        </div>
                        <div style={{ fontSize: '12px', color: '#666', marginTop: 4 }}>
                          {step.completed}/{step.required} required fields completed
                          {step.openQueries > 0 && (
                            <span style={{ color: '#ef4444', marginLeft: 8 }}>
                              • {step.openQueries} open queries
                            </span>
                          )}
                          {step.resolvedQueries > 0 && (
                            <span style={{ color: '#10b981', marginLeft: 8 }}>
                              • {step.resolvedQueries} resolved queries
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )
              });
            }}
          />
          <FloatButton
            icon={autoSaveEnabled ? <CloudSyncOutlined /> : <DisconnectOutlined />}
            tooltip={autoSaveEnabled ? 'Auto-save ON' : 'Auto-save OFF'}
            onClick={() => setAutoSaveEnabled(!autoSaveEnabled)}
          />
        </FloatButton.Group>
      </div>

      {/* Query Raising Modal */}
      <QueryRaisingModal
        open={queryModalVisible}
        onCancel={() => {
          setQueryModalVisible(false);
          setSelectedField(null);
        }}
        onSubmit={handleQueryCreated}
        workflowId={workflowId}
        fieldContext={selectedField}
      />
    </div>
  );
};

export default PlantQuestionnaire;
