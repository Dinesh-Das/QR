import {
  QuestionCircleOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
  PaperClipOutlined
} from '@ant-design/icons';
import {
  Modal,
  Form,
  Input,
  Select,
  Radio,
  Button,
  Space,
  Alert,
  Divider,
  Tag,
  message,
  Collapse
} from 'antd';
import React, { useState } from 'react';

import { queryAPI } from '../services/queryAPI';

import QueryDocumentUpload from './QueryDocumentUpload';
import SecureForm, { SecureInput, SecureTextArea, SecureFormItem } from './SecureForm';

const { TextArea } = Input;
const { Option } = Select;

const QueryRaisingModal = ({ open, visible, onCancel, onSubmit, workflowId, fieldContext }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [uploadedDocuments, setUploadedDocuments] = useState([]);
  const [createdQueryId, setCreatedQueryId] = useState(null);

  console.log('QueryRaisingModal props:', { open, visible, workflowId, fieldContext });

  const handleSubmit = async values => {
    try {
      setLoading(true);

      const queryData = {
        question: values.question,
        assignedTeam: values.assignedTeam, // This should be 'CQS', 'TECH', or 'JVC'
        priorityLevel: values.priority || 'MEDIUM',
        fieldName: fieldContext?.name,
        stepNumber: fieldContext?.stepNumber,
        queryCategory: 'GENERAL' // Default category
      };

      const createdQuery = await queryAPI.createQuery(workflowId, queryData);
      setCreatedQueryId(createdQuery.id);

      message.success('Query created successfully! You can now attach documents if needed.');
      
      // Don't close the modal immediately - allow user to upload documents
      // The modal will close when they click "Done" or "Cancel"
      
    } catch (error) {
      console.error('Failed to create query:', error);
      message.error('Failed to raise query. Please try again.');
      setLoading(false);
    }
  };

  // const getCurrentUser = () => { // Not currently used
  //   return localStorage.getItem('username') || 'current_user';
  // };

  const handleCancel = () => {
    form.resetFields();
    setUploadedDocuments([]);
    setCreatedQueryId(null);
    onCancel();
  };

  // Handle document upload completion
  const handleDocumentUpload = (documents) => {
    setUploadedDocuments(prev => [...prev, ...documents]);
    message.success(`${documents.length} document(s) uploaded successfully`);
  };

  // const getTeamDescription = team => { // Not currently used
  //   const descriptions = {
  //     CQS: 'Chemical Quality & Safety team - for safety data, hazard classifications, and regulatory compliance questions',
  //     TECH: 'Technical team - for technical specifications, physical properties, and process-related questions',
  //     JVC: 'JVC team - for material identification, supplier information, and general material questions'
  //   };
  //   return descriptions[team] || '';
  // };

  const getPriorityColor = priority => {
    const colors = {
      LOW: 'green',
      MEDIUM: 'blue',
      HIGH: 'orange',
      URGENT: 'red'
    };
    return colors[priority] || 'default';
  };

  const getRecommendedTeam = fieldContext => {
    if (!fieldContext || !fieldContext.name) {
      return null;
    }

    const fieldName = fieldContext.name.toLowerCase();
    const stepTitle = fieldContext.stepTitle?.toLowerCase() || '';
    const fieldLabel = fieldContext.label?.toLowerCase() || '';

    // Enhanced smart team recommendation with confidence scoring
    // const recommendations = []; // Not currently used

    // CQS Team scoring
    let cqsScore = 0;
    const cqsKeywords = [
      'hazard',
      'safety',
      'precautionary',
      'environmental',
      'toxic',
      'corrosive',
      'flammable',
      'classification',
      'ghs',
      'signal',
      'statement'
    ];
    cqsKeywords.forEach(keyword => {
      if (
        fieldName.includes(keyword) ||
        stepTitle.includes(keyword) ||
        fieldLabel.includes(keyword)
      ) {
        cqsScore += 1;
      }
    });

    // Technical Team scoring
    let techScore = 0;
    const techKeywords = [
      'physical',
      'boiling',
      'melting',
      'technical',
      'properties',
      'temperature',
      'state',
      'color',
      'odor',
      'specification'
    ];
    techKeywords.forEach(keyword => {
      if (
        fieldName.includes(keyword) ||
        stepTitle.includes(keyword) ||
        fieldLabel.includes(keyword)
      ) {
        techScore += 1;
      }
    });

    // JVC Team scoring
    let jvcScore = 0;
    const jvcKeywords = [
      'material',
      'supplier',
      'cas',
      'basic',
      'information',
      'name',
      'identification',
      'type'
    ];
    jvcKeywords.forEach(keyword => {
      if (
        fieldName.includes(keyword) ||
        stepTitle.includes(keyword) ||
        fieldLabel.includes(keyword)
      ) {
        jvcScore += 1;
      }
    });

    // Determine primary recommendation
    const maxScore = Math.max(cqsScore, techScore, jvcScore);
    let primaryTeam = 'CQS'; // Default to CQS for safety
    let confidence = 'Medium';

    if (maxScore === 0) {
      confidence = 'Low';
    } else if (maxScore >= 3) {
      confidence = 'High';
    }

    if (cqsScore === maxScore) {
      primaryTeam = 'CQS';
    } else if (techScore === maxScore) {
      primaryTeam = 'TECH';
    } else if (jvcScore === maxScore) {
      primaryTeam = 'JVC';
    }

    const teamDescriptions = {
      CQS: 'Chemical Quality & Safety team - handles safety data, hazard classifications, and regulatory compliance',
      TECH: 'Technical team - handles technical specifications, physical properties, and process-related questions',
      JVC: 'JVC team - handles material identification, supplier information, and general material questions'
    };

    return (
      <div>
        <div style={{ marginBottom: 8 }}>
          <strong
            style={{
              color:
                confidence === 'High' ? '#52c41a' : confidence === 'Medium' ? '#1890ff' : '#faad14'
            }}
          >
            {primaryTeam} Team
          </strong>{' '}
          is recommended ({confidence} confidence)
        </div>
        <div style={{ fontSize: '12px', color: '#666', marginBottom: 8 }}>
          {teamDescriptions[primaryTeam]}
        </div>
        <div style={{ fontSize: '11px', color: '#999' }}>
          Based on field: "<em>{fieldContext.label}</em>" in step: "
          <em>{fieldContext.stepTitle}</em>"
        </div>
        {confidence === 'Low' && (
          <div style={{ fontSize: '11px', color: '#fa8c16', marginTop: 4 }}>
            💡 Consider the nature of your question when selecting the team
          </div>
        )}
      </div>
    );
  };

  return (
    <Modal
      title={
        <Space>
          <QuestionCircleOutlined />
          Raise Query
        </Space>
      }
      open={open || visible}
      onCancel={handleCancel}
      footer={
        createdQueryId ? [
          <Button key="done" type="primary" onClick={() => {
            const documentMessage = uploadedDocuments.length > 0 
              ? ` with ${uploadedDocuments.length} document(s) attached`
              : '';
            message.success(`Query completed${documentMessage}`);
            
            form.resetFields();
            setUploadedDocuments([]);
            const queryToReturn = { id: createdQueryId, documents: uploadedDocuments };
            setCreatedQueryId(null);
            setLoading(false);
            
            if (onSubmit) {
              onSubmit(queryToReturn);
            }
          }}>
            Done
          </Button>
        ] : [
          <Button key="cancel" onClick={handleCancel}>
            Cancel
          </Button>,
          <Button key="submit" type="primary" loading={loading} onClick={() => form.submit()}>
            Create Query
          </Button>
        ]
      }
      width={600}
      destroyOnClose
    >
      {/* Field Context Information */}
      {fieldContext && (
        <Alert
          message="Field Context"
          description={
            <div>
              <p>
                <strong>Step:</strong> {fieldContext.stepTitle}
              </p>
              <p>
                <strong>Field:</strong> {fieldContext.label}
              </p>
              {fieldContext.placeholder && (
                <p>
                  <strong>Expected:</strong> {fieldContext.placeholder}
                </p>
              )}
            </div>
          }
          type="info"
          icon={<InfoCircleOutlined />}
          style={{ marginBottom: 16 }}
        />
      )}

      <SecureForm
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          priority: 'MEDIUM',
          fieldContext: fieldContext?.label
        }}
        componentName="QueryRaisingModal"
        enableSecurityLogging={true}
        disabled={createdQueryId !== null}
      >
        {/* Smart Team Recommendation */}
        {fieldContext && (
          <Alert
            message="Recommended Team"
            description={getRecommendedTeam(fieldContext)}
            type="info"
            style={{ marginBottom: 16 }}
            showIcon
          />
        )}

        {/* Team Selection with Enhanced Context */}
        <Form.Item
          name="assignedTeam"
          label="Assign to Team"
          rules={[{ required: true, message: 'Please select a team to assign this query to' }]}
          help="Choose the most appropriate team based on your question type. Consider the field context and question nature."
        >
          <Select
            placeholder="Select team to handle this query"
            size="large"
            showSearch
            optionFilterProp="children"
            filterOption={(input, option) =>
              option.children.props.children[0].props.children
                .toLowerCase()
                .indexOf(input.toLowerCase()) >= 0
            }
          >
            <Option value="CQS">
              <div>
                <div style={{ fontWeight: 'bold', color: '#722ed1' }}>CQS Team</div>
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '4px' }}>
                  Safety data, hazard classifications, regulatory compliance
                </div>
                <div style={{ fontSize: '11px', color: '#999' }}>
                  Best for: Safety measures, hazard statements, environmental impact
                </div>
              </div>
            </Option>
            <Option value="TECH">
              <div>
                <div style={{ fontWeight: 'bold', color: '#13c2c2' }}>Technical Team</div>
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '4px' }}>
                  Technical specs, physical properties, processes
                </div>
                <div style={{ fontSize: '11px', color: '#999' }}>
                  Best for: Physical properties, technical specifications, processes
                </div>
              </div>
            </Option>
            <Option value="JVC">
              <div>
                <div style={{ fontWeight: 'bold', color: '#1890ff' }}>JVC Team</div>
                <div style={{ fontSize: '12px', color: '#666', marginBottom: '4px' }}>
                  Material identification, supplier info, general questions
                </div>
                <div style={{ fontSize: '11px', color: '#999' }}>
                  Best for: Material details, supplier information, general clarifications
                </div>
              </div>
            </Option>
          </Select>
        </Form.Item>

        {/* Field Context (editable) */}
        <SecureFormItem
          name="fieldContext"
          label="Field Context"
          help="Specify which field or section this query relates to"
          validationType="text"
        >
          <SecureInput
            placeholder="e.g., Material Name, Safety Classification, Storage Conditions"
            prefix={<InfoCircleOutlined />}
            validationType="text"
            componentName="QueryRaisingModal"
            fieldName="fieldContext"
          />
        </SecureFormItem>

        {/* Priority Selection */}
        <Form.Item
          name="priority"
          label="Priority Level"
          help="Select the urgency level for this query"
        >
          <Radio.Group>
            <Space direction="vertical">
              <Radio value="LOW">
                <Tag color={getPriorityColor('LOW')}>LOW</Tag>
                <span style={{ marginLeft: 8 }}>General information, non-blocking</span>
              </Radio>
              <Radio value="MEDIUM">
                <Tag color={getPriorityColor('MEDIUM')}>MEDIUM</Tag>
                <span style={{ marginLeft: 8 }}>Standard clarification needed</span>
              </Radio>
              <Radio value="HIGH">
                <Tag color={getPriorityColor('HIGH')}>HIGH</Tag>
                <span style={{ marginLeft: 8 }}>Important for completion</span>
              </Radio>
              <Radio value="URGENT">
                <Tag color={getPriorityColor('URGENT')}>URGENT</Tag>
                <span style={{ marginLeft: 8 }}>Blocking progress, needs immediate attention</span>
              </Radio>
            </Space>
          </Radio.Group>
        </Form.Item>

        <Divider />

        {/* Question Text */}
        <SecureFormItem
          name="question"
          label="Your Question"
          validationType="richtext"
          help="Be specific and provide context to help the assigned team understand your question"
        >
          <SecureTextArea
            rows={6}
            placeholder="Describe your question in detail. Include:
• What specific information you need
• Why you need this information
• Any relevant context or background
• What you've already tried or researched"
            showCount
            maxLength={1000}
            validationType="richtext"
            componentName="QueryRaisingModal"
            fieldName="question"
          />
        </SecureFormItem>

        {/* Document Upload Section */}
        {createdQueryId ? (
          <div style={{ marginTop: 16 }}>
            <Alert
              message="Query Created Successfully!"
              description="Your query has been created. You can now attach supporting documents if needed, or click 'Done' to finish."
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <QueryDocumentUpload
              queryId={createdQueryId}
              context="query"
              onUploadComplete={handleDocumentUpload}
              maxFiles={5}
              disabled={false}
            />
          </div>
        ) : (
          <Collapse
            size="small"
            ghost
            items={[
              {
                key: 'documents',
                label: (
                  <Space>
                    <PaperClipOutlined />
                    <span>Supporting Documents</span>
                    <Tag color="blue">Available after query creation</Tag>
                  </Space>
                ),
                children: (
                  <Alert
                    message="Document Attachment"
                    description="After creating your query, you'll be able to attach supporting documents to provide additional context. Documents help the assigned team better understand your question."
                    type="info"
                    showIcon
                  />
                )
              }
            ]}
          />
        )}

        <Divider />

        {/* Guidelines */}
        <Alert
          message="Query Guidelines"
          description={
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>Be specific and clear in your question</li>
              <li>Include relevant context and background information</li>
              <li>Mention any documentation or resources you've already checked</li>
              <li>For urgent queries, explain why immediate attention is needed</li>
              <li>Use appropriate technical terminology when relevant</li>
            </ul>
          }
          type="info"
          showIcon
          icon={<ExclamationCircleOutlined />}
        />
      </SecureForm>
    </Modal>
  );
};

export default QueryRaisingModal;
