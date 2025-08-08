import {
  UploadOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  ProjectOutlined,
  ExperimentOutlined,
  HomeOutlined,
  FileProtectOutlined,
  RocketOutlined,
  ExclamationCircleOutlined,
  DownloadOutlined
} from '@ant-design/icons';
import {
  Form,
  Select,
  Upload,
  Button,
  Card,
  Row,
  Col,
  message,
  Space,
  Typography,
  Alert,
  List,
  Checkbox,
  Badge,
  Descriptions,
  Modal,
  Result,
  Statistic,
  Tag
} from 'antd';
import React, { useState, useEffect } from 'react';

import { documentAPI } from '../services/documentAPI';
import { projectAPI } from '../services/projectAPI';

import SecureFileUpload from './SecureFileUpload';

const { Option } = Select;
const { Title, Text } = Typography;

const MaterialExtensionFormSimple = ({ onSubmit, loading }) => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [plants, setPlants] = useState([]);
  const [fileList, setFileList] = useState([]);
  const [reusableDocuments, setReusableDocuments] = useState([]);
  const [selectedReusableDocuments, setSelectedReusableDocuments] = useState([]);
  const [showReusableDocuments, setShowReusableDocuments] = useState(false);
  const [loadingStates, setLoadingStates] = useState({
    projects: false,
    materials: false,
    plants: false,
    reusableDocuments: false
  });

  useEffect(() => {
    const controller = new AbortController();

    const loadInitialData = async () => {
      try {
        await Promise.all([
          loadProjectsWithAbort(controller.signal),
          loadPlantsWithAbort(controller.signal)
        ]);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error loading initial data:', error);
        }
      }
    };

    const loadProjectsWithAbort = async signal => {
      try {
        setLoadingState('projects', true);
        const projectData = await projectAPI.getProjects({ signal });
        if (!signal?.aborted) {
          setProjects(projectData || []);
        }
      } catch (error) {
        if (!signal?.aborted) {
          console.error('Error loading projects:', error);
          message.error('Failed to load projects');
        }
      } finally {
        if (!signal?.aborted) {
          setLoadingState('projects', false);
        }
      }
    };

    const loadPlantsWithAbort = async signal => {
      try {
        setLoadingState('plants', true);
        const plantData = await projectAPI.getPlants({ signal });
        if (!signal?.aborted) {
          setPlants(plantData || []);
        }
      } catch (error) {
        if (!signal?.aborted) {
          console.error('Error loading plants:', error);
          message.error('Failed to load plants');
        }
      } finally {
        if (!signal?.aborted) {
          setLoadingState('plants', false);
        }
      }
    };

    loadInitialData();

    return () => {
      controller.abort();
    };
  }, []); // Remove dependencies

  const setLoadingState = (key, value) => {
    setLoadingStates(prev => ({ ...prev, [key]: value }));
  };



  const handleProjectChange = async projectCode => {
    try {
      setLoadingState('materials', true);
      setMaterials([]);

      if (projectCode) {
        const materialData = await projectAPI.getMaterialsByProject(projectCode);
        setMaterials(materialData || []);

        // Materials loaded successfully
      }

      // Clear reusable documents when project changes
      setReusableDocuments([]);
      setSelectedReusableDocuments([]);
      setShowReusableDocuments(false);
    } catch (error) {
      console.error('Error loading materials:', error);
      message.error('Failed to load materials. Please try again.');
    } finally {
      setLoadingState('materials', false);
    }
  };

  const handleMaterialChange = async materialCode => {
    const projectCode = form.getFieldValue('projectCode');

    if (projectCode && materialCode) {
      await checkForReusableDocuments(projectCode, materialCode);
    }
  };

  const checkForReusableDocuments = async (projectCode, materialCode) => {
    try {
      setLoadingState('reusableDocuments', true);
      const reusableDocs = await documentAPI.getReusableDocuments(projectCode, materialCode, true);

      if (reusableDocs && reusableDocs.length > 0) {
        setReusableDocuments(reusableDocs);
        setShowReusableDocuments(true);
        setSelectedReusableDocuments(reusableDocs.map(doc => doc.id));

        // Reusable documents found
      } else {
        setReusableDocuments([]);
        setShowReusableDocuments(false);
        setSelectedReusableDocuments([]);
      }
    } catch (error) {
      console.error('Error checking for reusable documents:', error);
      message.warning(
        'Unable to check for reusable documents. You can still upload new documents.'
      );
    } finally {
      setLoadingState('reusableDocuments', false);
    }
  };

  const handleFileChange = ({ fileList: newFileList }) => {
    const validatedFileList = newFileList.map(file => {
      if (file.originFileObj) {
        const validation = documentAPI.validateFile(file.originFileObj);
        if (!validation.isValidType) {
          file.status = 'error';
          file.response = 'Invalid file type. Only PDF, Word, and Excel files are allowed.';
        } else if (!validation.isValidSize) {
          file.status = 'error';
          file.response = `File size exceeds 25MB limit (${(file.originFileObj.size / 1024 / 1024).toFixed(2)}MB).`;
        } else {
          file.status = 'done';
          file.percent = 100;
        }

        file.size = file.originFileObj.size;
        file.type = file.originFileObj.type;
        file.lastModified = file.originFileObj.lastModified;
      }
      return file;
    });

    setFileList(validatedFileList);

    const errorFiles = validatedFileList.filter(f => f.status === 'error').length;

    if (errorFiles > 0) {
      message.warning(
        `${errorFiles} file(s) have validation errors. Please check file types and sizes.`
      );
    }
  };

  const handleSubmit = async values => {
    try {
      // Basic validation
      const totalDocs =
        fileList.filter(f => f.status === 'done').length + selectedReusableDocuments.length;

      if (!values.projectCode || !values.materialCode || !values.plantCodes || values.plantCodes.length === 0) {
        message.error('Please fill in all required fields');
        return;
      }

      if (totalDocs === 0) {
        message.error('Please upload at least one document or select reusable documents');
        return;
      }

      // Validation variables for form submission
      // const selectedProject = projects.find(p => p.value === values.projectCode);
      // const selectedMaterial = materials.find(m => m.value === values.materialCode);
      // const selectedPlant = plants.find(p => p.value === values.plantCode);

      Modal.confirm({
        title: (
          <Space>
            <RocketOutlined style={{ color: '#1890ff' }} />
            <span>Confirm Material Extension Submission</span>
          </Space>
        ),
        width: 800,
        style: { top: 80 },
        bodyStyle: {
          minHeight: '450px',
          padding: '24px'
        },
        content: (
          <div>
            <Alert
              message="Ready to Submit Material Extension"
              description="Please review the details below before creating the workflow."
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />

            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Project Code" span={1}>
                <Space>
                  <ProjectOutlined style={{ color: '#1890ff' }} />
                  <Text strong>{values.projectCode}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Material Code" span={1}>
                <Space>
                  <ExperimentOutlined style={{ color: '#52c41a' }} />
                  <Text code>{values.materialCode}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Plant Codes" span={1}>
                <Space wrap>
                  <HomeOutlined style={{ color: '#fa8c16' }} />
                  {values.plantCodes.map(plantCode => (
                    <Tag key={plantCode} color="orange">{plantCode}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>

              <Descriptions.Item label="Documents Summary" span={2}>
                <Space>
                  <FileProtectOutlined style={{ color: '#13c2c2' }} />
                  <Badge
                    count={totalDocs}
                    style={{ backgroundColor: totalDocs > 0 ? '#52c41a' : '#d9d9d9' }}
                  />
                  <Text style={{ marginLeft: 8 }}>
                    {totalDocs} file{totalDocs !== 1 ? 's' : ''} (
                    {fileList.filter(f => f.status === 'done').length} new,{' '}
                    {selectedReusableDocuments.length} reused)
                  </Text>
                </Space>
              </Descriptions.Item>
            </Descriptions>

            {/* Document Details Section */}
            {totalDocs > 0 && (
              <div style={{ marginTop: 20 }}>
                {fileList.filter(f => f.status === 'done').length > 0 && (
                  <div style={{ marginBottom: 16 }}>
                    <Text strong style={{ color: '#1890ff', marginBottom: 8, display: 'block' }}>
                      📎 New Documents ({fileList.filter(f => f.status === 'done').length})
                    </Text>
                    <List
                      size="small"
                      bordered
                      dataSource={fileList.filter(f => f.status === 'done')}
                      renderItem={file => (
                        <List.Item
                          actions={[
                            <Button
                              key="download"
                              type="link"
                              size="small"
                              icon={<DownloadOutlined />}
                              onClick={() => {
                                // Create a temporary URL for downloading the file
                                const url = URL.createObjectURL(file.originFileObj || file);
                                const link = document.createElement('a');
                                link.href = url;
                                link.download = file.name;
                                document.body.appendChild(link);
                                link.click();
                                document.body.removeChild(link);
                                URL.revokeObjectURL(url);
                              }}
                            >
                              Download
                            </Button>
                          ]}
                        >
                          <List.Item.Meta
                            avatar={<FileTextOutlined style={{ color: '#52c41a' }} />}
                            title={file.name}
                            description={`Size: ${(file.size / 1024 / 1024).toFixed(2)} MB`}
                          />
                        </List.Item>
                      )}
                    />
                  </div>
                )}

                {selectedReusableDocuments.length > 0 && (
                  <div>
                    <Text strong style={{ color: '#fa8c16', marginBottom: 8, display: 'block' }}>
                      🔄 Reused Documents ({selectedReusableDocuments.length})
                    </Text>
                    <List
                      size="small"
                      bordered
                      dataSource={reusableDocuments.filter(doc =>
                        selectedReusableDocuments.includes(doc.id)
                      )}
                      renderItem={doc => (
                        <List.Item
                          actions={[
                            <Button
                              key="download"
                              type="link"
                              size="small"
                              icon={<DownloadOutlined />}
                              onClick={async () => {
                                try {
                                  const blob = await documentAPI.downloadDocument(doc.id);
                                  const url = window.URL.createObjectURL(blob);
                                  const link = document.createElement('a');
                                  link.href = url;
                                  link.download = doc.originalFileName || `document_${doc.id}`;
                                  document.body.appendChild(link);
                                  link.click();
                                  document.body.removeChild(link);
                                  window.URL.revokeObjectURL(url);
                                  message.success(`Downloaded ${doc.originalFileName}`);
                                } catch (error) {
                                  console.error('Error downloading document:', error);
                                  message.error('Failed to download document');
                                }
                              }}
                            >
                              Download
                            </Button>
                          ]}
                        >
                          <List.Item.Meta
                            avatar={<FileTextOutlined style={{ color: '#fa8c16' }} />}
                            title={doc.originalFileName}
                            description={
                              <Space split={<span style={{ color: '#d9d9d9' }}>|</span>}>
                                <Text type="secondary">
                                  Size: {(doc.fileSize / 1024 / 1024).toFixed(2)} MB
                                </Text>
                                <Text type="secondary">
                                  From: {doc.projectCode}/{doc.materialCode}
                                </Text>
                                <Tag color="orange" size="small">
                                  Reused
                                </Tag>
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </div>
                )}
              </div>
            )}
          </div>
        ),
        onOk: () => {
          return new Promise(async (resolve, reject) => {
            const submissionData = {
              ...values,
              uploadedFiles: fileList.filter(file => file.status === 'done'),
              reusedDocuments: selectedReusableDocuments,
              metadata: {
                totalDocuments: totalDocs,
                newDocuments: fileList.filter(f => f.status === 'done').length,
                reusedDocuments: selectedReusableDocuments.length,
                submittedAt: new Date().toISOString(),
                formVersion: '3.0-simplified'
              }
            };

            try {
              const result = await onSubmit(submissionData);

              // Handle smart extension result
              if (result && result.isSmartExtension) {
                // Show detailed smart extension result modal
                Modal.info({
                  title: (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <RocketOutlined style={{ color: '#1890ff', fontSize: '20px' }} />
                      <span>Smart Plant Extension Results</span>
                    </div>
                  ),
                  width: 900,
                  style: { top: 50 },
                  bodyStyle: {
                    maxHeight: '70vh',
                    overflowY: 'auto',
                    padding: '24px'
                  },
                  content: (
                    <div style={{ padding: '16px 0' }}>
                      <Alert
                        message={result.message}
                        type={result.success ? 'success' : 'warning'}
                        showIcon
                        style={{ marginBottom: 20 }}
                      />

                      {/* Summary Statistics */}
                      <Card title="Extension Summary" size="small" style={{ marginBottom: 20 }}>
                        <Row gutter={16}>
                          <Col span={6}>
                            <Statistic title="Total Requested" value={result.summary.totalRequested} />
                          </Col>
                          <Col span={6}>
                            <Statistic title="Newly Created" value={result.summary.created} valueStyle={{ color: '#3f8600' }} />
                          </Col>
                          <Col span={6}>
                            <Statistic title="Duplicates Skipped" value={result.summary.skipped} valueStyle={{ color: '#faad14' }} />
                          </Col>
                          <Col span={6}>
                            <Statistic title="Failed" value={result.summary.failed} valueStyle={{ color: '#cf1322' }} />
                          </Col>
                        </Row>
                      </Card>

                      {/* Newly Created Workflows */}
                      {result.createdWorkflows && result.createdWorkflows.length > 0 && (
                        <Card title="✅ Newly Created Workflows" size="small" style={{ marginBottom: 20 }}>
                          <List
                            dataSource={result.createdWorkflows}
                            renderItem={workflow => (
                              <List.Item>
                                <List.Item.Meta
                                  title={
                                    <Space>
                                      <Tag color="green">Plant: {workflow.plantCode}</Tag>
                                      <Tag color="blue">ID: {workflow.id}</Tag>
                                      <Tag color="orange">State: {workflow.state}</Tag>
                                    </Space>
                                  }
                                  description={`Created: ${new Date(workflow.createdAt).toLocaleString()}`}
                                />
                              </List.Item>
                            )}
                          />
                        </Card>
                      )}

                      {/* Duplicate Workflows */}
                      {result.duplicateWorkflows && result.duplicateWorkflows.length > 0 && (
                        <Card title="⚠️ Duplicate Workflows (Already Exist)" size="small" style={{ marginBottom: 20 }}>
                          <Alert
                            message="These workflows were not created because they already exist"
                            type="warning"
                            showIcon
                            style={{ marginBottom: 16 }}
                          />
                          <List
                            dataSource={result.duplicateWorkflows}
                            renderItem={workflow => (
                              <List.Item>
                                <List.Item.Meta
                                  title={
                                    <Space>
                                      <Tag color="orange">Plant: {workflow.plantCode}</Tag>
                                      <Tag color="blue">ID: {workflow.id}</Tag>
                                      <Tag color="purple">State: {workflow.state}</Tag>
                                    </Space>
                                  }
                                  description={`Existing workflow created: ${new Date(workflow.createdAt).toLocaleString()}`}
                                />
                              </List.Item>
                            )}
                          />
                        </Card>
                      )}

                      {/* Document Reuse Information */}
                      {result.details && result.details.documentReuse && result.details.documentReuse.totalReusedDocuments > 0 && (
                        <Card 
                          title={
                            <Space>
                              <FileProtectOutlined style={{ color: '#13c2c2' }} />
                              <span>📎 Document Reuse Information</span>
                              <Badge 
                                count={result.details.documentReuse.totalReusedDocuments} 
                                style={{ backgroundColor: '#52c41a' }} 
                              />
                            </Space>
                          } 
                          size="small" 
                          style={{ marginBottom: 20 }}
                        >
                          <Alert
                            message={`${result.details.documentReuse.totalReusedDocuments} documents automatically reused across ${result.summary.created} workflow(s)`}
                            description={
                              <div>
                                <div style={{ marginBottom: 8 }}>
                                  <strong>Strategy:</strong> {result.details.documentReuse.reuseStrategy || 'Automatic'}
                                </div>
                                {result.details.documentReuse.sourceDescription && (
                                  <div style={{ marginBottom: 8 }}>
                                    <strong>Sources:</strong> {result.details.documentReuse.sourceDescription}
                                  </div>
                                )}
                                {result.details.documentReuse.statistics && (
                                  <div>
                                    <strong>Document Sources:</strong>{' '}
                                    {result.details.documentReuse.statistics.workflowDocuments > 0 && 
                                      `${result.details.documentReuse.statistics.workflowDocuments} from workflows`}
                                    {result.details.documentReuse.statistics.queryDocuments > 0 && 
                                      `${result.details.documentReuse.statistics.workflowDocuments > 0 ? ', ' : ''}${result.details.documentReuse.statistics.queryDocuments} from queries`}
                                    {result.details.documentReuse.statistics.responseDocuments > 0 && 
                                      `${(result.details.documentReuse.statistics.workflowDocuments > 0 || result.details.documentReuse.statistics.queryDocuments > 0) ? ', ' : ''}${result.details.documentReuse.statistics.responseDocuments} from responses`}
                                  </div>
                                )}
                              </div>
                            }
                            type="success"
                            showIcon
                            style={{ marginBottom: 16 }}
                          />
                          
                          <List
                            dataSource={result.details.documentReuse.reusedDocuments || []}
                            renderItem={doc => (
                              <List.Item
                                actions={[
                                  <Button
                                    key="download"
                                    type="link"
                                    size="small"
                                    icon={<DownloadOutlined />}
                                    onClick={async () => {
                                      try {
                                        const blob = await documentAPI.downloadDocument(doc.id);
                                        const url = window.URL.createObjectURL(blob);
                                        const link = document.createElement('a');
                                        link.href = url;
                                        link.download = doc.originalFileName || `document_${doc.id}`;
                                        document.body.appendChild(link);
                                        link.click();
                                        document.body.removeChild(link);
                                        window.URL.revokeObjectURL(url);
                                        message.success(`Downloaded ${doc.originalFileName}`);
                                      } catch (error) {
                                        console.error('Error downloading document:', error);
                                        message.error('Failed to download document');
                                      }
                                    }}
                                  >
                                    Download
                                  </Button>
                                ]}
                              >
                                <List.Item.Meta
                                  avatar={
                                    <div style={{ position: 'relative' }}>
                                      <FileTextOutlined 
                                        style={{ 
                                          color: doc.documentSource === 'WORKFLOW' ? '#1890ff' : 
                                                 doc.documentSource === 'QUERY' ? '#fa8c16' : '#722ed1',
                                          fontSize: '16px'
                                        }} 
                                      />
                                      {doc.isReused && (
                                        <Badge 
                                          count="R" 
                                          style={{ 
                                            backgroundColor: '#52c41a',
                                            fontSize: '10px',
                                            height: '16px',
                                            minWidth: '16px',
                                            lineHeight: '16px',
                                            position: 'absolute',
                                            top: '-8px',
                                            right: '-8px'
                                          }} 
                                        />
                                      )}
                                    </div>
                                  }
                                  title={
                                    <Space>
                                      <span>{doc.originalFileName}</span>
                                      {doc.documentSource && (
                                        <Tag 
                                          color={
                                            doc.documentSource === 'WORKFLOW' ? 'blue' : 
                                            doc.documentSource === 'QUERY' ? 'orange' : 'purple'
                                          }
                                          size="small"
                                        >
                                          {doc.documentSource === 'WORKFLOW' ? 'Workflow' : 
                                           doc.documentSource === 'QUERY' ? 'Query' : 'Response'}
                                        </Tag>
                                      )}
                                      {doc.isReused && (
                                        <Tag color="green" size="small">
                                          Reused {doc.reuseCount > 1 ? `(${doc.reuseCount}x)` : ''}
                                        </Tag>
                                      )}
                                    </Space>
                                  }
                                  description={
                                    <Space split={<span style={{ color: '#d9d9d9' }}>|</span>}>
                                      <Text type="secondary">
                                        Size: {((doc.fileSize || 0) / 1024 / 1024).toFixed(2)} MB
                                      </Text>
                                      {doc.projectCode && doc.materialCode && (
                                        <Text type="secondary">
                                          From: {doc.projectCode}/{doc.materialCode}
                                        </Text>
                                      )}
                                      {doc.sourceDescription && (
                                        <Text type="secondary">
                                          {doc.sourceDescription}
                                        </Text>
                                      )}
                                      {doc.uploadedBy && (
                                        <Text type="secondary">
                                          By: {doc.uploadedBy}
                                        </Text>
                                      )}
                                    </Space>
                                  }
                                />
                              </List.Item>
                            )}
                          />
                          
                          {result.details.documentReuse.statistics && (
                            <div style={{ marginTop: 16, padding: '12px', backgroundColor: '#f6ffed', borderRadius: '6px' }}>
                              <Row gutter={16}>
                                <Col span={6}>
                                  <Statistic 
                                    title="Unique Documents" 
                                    value={result.details.documentReuse.statistics.totalUniqueDocuments || result.details.documentReuse.totalReusedDocuments}
                                    valueStyle={{ fontSize: '16px' }}
                                  />
                                </Col>
                                <Col span={6}>
                                  <Statistic 
                                    title="Per Workflow" 
                                    value={result.details.documentReuse.documentsPerWorkflow || 0}
                                    valueStyle={{ fontSize: '16px' }}
                                  />
                                </Col>
                                <Col span={6}>
                                  <Statistic 
                                    title="Workflows with Docs" 
                                    value={result.details.documentReuse.statistics.workflowsWithReusedDocuments || result.summary.created}
                                    valueStyle={{ fontSize: '16px' }}
                                  />
                                </Col>
                                <Col span={6}>
                                  <Statistic 
                                    title="Total Attachments" 
                                    value={result.details.documentReuse.totalReusedDocuments}
                                    valueStyle={{ fontSize: '16px', color: '#52c41a' }}
                                  />
                                </Col>
                              </Row>
                            </div>
                          )}
                        </Card>
                      )}

                      {/* Failed Plants */}
                      {result.failedPlants && result.failedPlants.length > 0 && (
                        <Card title="❌ Failed Extensions" size="small" style={{ marginBottom: 20 }}>
                          <Alert
                            message="These plant extensions failed due to errors"
                            type="error"
                            showIcon
                            style={{ marginBottom: 16 }}
                          />
                          <List
                            dataSource={result.failedPlants}
                            renderItem={plantCode => (
                              <List.Item>
                                <Tag color="red">Plant: {plantCode}</Tag>
                              </List.Item>
                            )}
                          />
                        </Card>
                      )}

                      <Descriptions
                        title="Request Parameters"
                        bordered
                        size="small"
                        column={2}
                        style={{ marginTop: 20 }}
                      >
                        <Descriptions.Item label="Project Code" span={1}>
                          <Tag color="blue">{values.projectCode}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Material Code" span={1}>
                          <Tag color="green">{values.materialCode}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Plant Codes" span={2}>
                          <Space wrap>
                            {values.plantCodes.map(plantCode => (
                              <Tag key={plantCode} color="orange">{plantCode}</Tag>
                            ))}
                          </Space>
                        </Descriptions.Item>

                      </Descriptions>

                      {result.duplicateWorkflows && result.duplicateWorkflows.length > 0 && result.duplicateWorkflows[0] && (
                        <Descriptions
                          title="Existing Workflow Details"
                          bordered
                          size="small"
                          column={2}
                          style={{ marginBottom: 20 }}
                        >
                          <Descriptions.Item label="Workflow ID" span={1}>
                            <Tag color="red">#{result.duplicateWorkflows[0].id}</Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="Current State" span={1}>
                            <Tag color="processing">
                              {result.duplicateWorkflows[0].state?.replace('_', ' ') || 'PENDING'}
                            </Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="Created Date" span={1}>
                            {result.duplicateWorkflows[0].createdAt
                              ? new Date(result.duplicateWorkflows[0].createdAt).toLocaleDateString('en-US', {
                                  year: 'numeric',
                                  month: 'short',
                                  day: 'numeric',
                                  hour: '2-digit',
                                  minute: '2-digit'
                                })
                              : 'N/A'}
                          </Descriptions.Item>
                          <Descriptions.Item label="Initiated By" span={1}>
                            {result.duplicateWorkflows[0].initiatedBy || 'Unknown'}
                          </Descriptions.Item>
                          {result.duplicateWorkflows[0].documentCount > 0 && (
                            <Descriptions.Item label="Documents" span={2}>
                              <Badge
                                count={result.duplicateWorkflows[0].documentCount}
                                style={{ backgroundColor: '#52c41a' }}
                              />
                              <span style={{ marginLeft: 8 }}>files attached</span>
                            </Descriptions.Item>
                          )}
                        </Descriptions>
                      )}

                      <Alert
                        message="Next Steps"
                        description={
                          <div>
                            <p style={{ margin: '8px 0' }}>
                              • Check the <strong>"Pending Extensions"</strong> tab to view the
                              existing workflow{result.duplicateWorkflows && result.duplicateWorkflows.length > 1 ? 's' : ''}
                            </p>
                            <p style={{ margin: '8px 0' }}>
                              • Use different parameters if you need to create a new workflow
                            </p>
                            <p style={{ margin: '8px 0' }}>
                              • Contact the workflow initiator if you need to modify the existing
                              workflow{result.duplicateWorkflows && result.duplicateWorkflows.length > 1 ? 's' : ''}
                            </p>
                          </div>
                        }
                        type="info"
                        showIcon
                      />
                    </div>
                  ),
                  okText: 'Got It',
                  okButtonProps: {
                    size: 'large',
                    type: 'primary'
                  },
                  onOk: () => {
                    // Reset form after user acknowledges
                    resetForm();
                  }
                });
                // Close the confirmation modal
                resolve();
                return;
              }

              // Success - just reset form, success message is handled by parent
              resetForm();
              resolve();
            } catch (error) {
              // Don't show success modal if onSubmit failed
              console.error('Form submission failed:', error);
              // Error message is handled by parent component
              reject(error);
            }
          });
        }
      });
    } catch (error) {
      console.error('Error submitting form:', error);
      message.error('Failed to create material extension. Please try again.');
    }
  };

  const resetForm = () => {
    form.resetFields();
    setFileList([]);
    setReusableDocuments([]);
    setSelectedReusableDocuments([]);
    setShowReusableDocuments(false);
    setMaterials([]);
  };

  return (
    <Card>
      <div style={{ marginBottom: 24 }}>
        <Space>
          <SafetyCertificateOutlined />
          <Title level={4} style={{ margin: 0 }}>
            Material Extension Form
          </Title>
        </Space>
      </div>

      <Form form={form} layout="vertical" onFinish={handleSubmit} size="large">
        <Row gutter={[16, 16]}>
          {/* Project Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Project Code"
              name="projectCode"
              rules={[{ required: true, message: 'Please select a project code' }]}
            >
              <Select
                placeholder="Select project code"
                showSearch
                loading={loadingStates.projects}
                onChange={handleProjectChange}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {projects.map(project => (
                  <Option key={project.value} value={project.value}>
                    {project.label || project.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          {/* Material Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Material Code"
              name="materialCode"
              rules={[{ required: true, message: 'Please select a material code' }]}
            >
              <Select
                placeholder="Select material code"
                showSearch
                loading={loadingStates.materials}
                onChange={handleMaterialChange}
                disabled={materials.length === 0}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {materials.map(material => (
                  <Option key={material.value} value={material.value}>
                    {material.label || material.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          {/* Plant Selection - Dropdown with Multiple Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Plant Codes"
              name="plantCodes"
              rules={[{ required: true, message: 'Please select at least one plant code' }]}
            >
              <Select
                mode="multiple"
                placeholder="Select plant codes"
                showSearch
                loading={loadingStates.plants}
                allowClear
                maxTagCount="responsive"
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
                style={{ width: '100%' }}
              >
                {plants.map(plant => (
                  <Option key={plant.value} value={plant.value}>
                    {plant.label || plant.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>


        </Row>

        {/* Reusable Documents Section */}
        {showReusableDocuments && (
          <Card
            title={
              <Space>
                <FileTextOutlined style={{ color: '#1890ff' }} />
                <span>Reusable Documents Found</span>
                <Badge count={reusableDocuments.length} style={{ backgroundColor: '#52c41a' }} />
              </Space>
            }
            style={{ marginBottom: 16 }}
            size="small"
          >
            <Alert
              message={`Found ${reusableDocuments.length} reusable document(s)`}
              description="These documents are automatically selected. Uncheck any you don't want to reuse."
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Checkbox.Group
              value={selectedReusableDocuments}
              onChange={setSelectedReusableDocuments}
              style={{ width: '100%' }}
            >
              <List
                size="small"
                dataSource={reusableDocuments}
                renderItem={doc => (
                  <List.Item>
                    <Checkbox value={doc.id}>
                      <Space>
                        <FileTextOutlined style={{ color: '#1890ff' }} />
                        <span>{doc.originalFileName}</span>
                        <Text type="secondary">({(doc.fileSize / 1024 / 1024).toFixed(2)}MB)</Text>
                      </Space>
                    </Checkbox>
                  </List.Item>
                )}
              />
            </Checkbox.Group>
          </Card>
        )}

        {/* File Upload Section */}
        <Form.Item
          label="Upload Documents"
          help="Upload PDF, Word, or Excel files (max 25MB each). At least one document is required."
        >
          <SecureFileUpload
            fileList={fileList}
            onChange={handleFileChange}
            maxFiles={10}
            maxSizeMB={25}
            componentName="MaterialExtensionForm"
            enableMalwareScan={true}
            showSecurityInfo={true}
            allowedTypes={[
              'application/pdf',
              'application/msword',
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
              'application/vnd.ms-excel',
              'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
            ]}
            allowedExtensions={['.pdf', '.doc', '.docx', '.xls', '.xlsx']}
          />
        </Form.Item>

        {/* Submit Button */}
        <Form.Item style={{ marginTop: 24 }}>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              size="large"
              icon={<RocketOutlined />}
            >
              Create Material Extension
            </Button>
            <Button onClick={resetForm} size="large">
              Reset Form
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default MaterialExtensionFormSimple;
