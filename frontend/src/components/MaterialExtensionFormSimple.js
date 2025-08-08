import {
  UploadOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  ProjectOutlined,
  ExperimentOutlined,
  HomeOutlined,

  FileProtectOutlined,
  RocketOutlined
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
  Result
} from 'antd';
import React, { useState, useEffect, useCallback } from 'react';

import { FILE_SIZE } from '../constants';
import { documentAPI } from '../services/documentAPI';
import { projectAPI } from '../services/projectAPI';

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
    loadProjects();
    loadPlants();
  }, [loadProjects, loadPlants]);

  const setLoadingState = (key, value) => {
    setLoadingStates(prev => ({ ...prev, [key]: value }));
  };

  const loadProjects = useCallback(async () => {
    try {
      setLoadingState('projects', true);
      const projectData = await projectAPI.getProjects();
      setProjects(projectData || []);
    } catch (error) {
      console.error('Error loading projects:', error);
      message.error('Failed to load projects');
    } finally {
      setLoadingState('projects', false);
    }
  }, []);

  const loadPlants = useCallback(async () => {
    try {
      setLoadingState('plants', true);
      const plantData = await projectAPI.getPlants();
      setPlants(plantData || []);
    } catch (error) {
      console.error('Error loading plants:', error);
      message.error('Failed to load plants');
    } finally {
      setLoadingState('plants', false);
    }
  }, []);

  const handleProjectChange = async projectCode => {
    try {
      setLoadingState('materials', true);
      setMaterials([]);

      if (projectCode) {
        const materialData = await projectAPI.getMaterialsByProject(projectCode);
        setMaterials(materialData || []);

        if (materialData && materialData.length > 0) {
          message.success(`Loaded ${materialData.length} materials for project ${projectCode}`);
        }
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

  const handlePlantChange = async plantCode => {
    // Plant change handler - blocks functionality removed
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

        message.success(
          `Found ${reusableDocs.length} reusable document(s) for ${projectCode}/${materialCode}`
        );
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
          file.response = `File size exceeds 25MB limit (${(file.originFileObj.size / FILE_SIZE.BYTES_PER_MB).toFixed(2)}MB).`;
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

    const validFiles = validatedFileList.filter(f => f.status === 'done').length;
    const errorFiles = validatedFileList.filter(f => f.status === 'error').length;

    if (errorFiles > 0) {
      message.warning(
        `${errorFiles} file(s) have validation errors. Please check file types and sizes.`
      );
    } else if (validFiles > 0) {
      message.success(`${validFiles} file(s) ready for upload.`);
    }
  };

  const handleSubmit = async values => {
    try {
      // Basic validation
      const totalDocs =
        fileList.filter(f => f.status === 'done').length + selectedReusableDocuments.length;

      if (!values.projectCode || !values.materialCode || !values.plantCode ) {
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
        width: 600,
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
              <Descriptions.Item label="Plant Code" span={1}>
                <Space>
                  <HomeOutlined style={{ color: '#fa8c16' }} />
                  <Text strong>{values.plantCode}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Total Documents" span={2}>
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
          </div>
        ),
        onOk: async () => {
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

          await onSubmit(submissionData);

          Modal.success({
            title: 'Material Extension Created Successfully!',
            content: (
              <Result
                status="success"
                title="Workflow Initiated"
                subTitle={`Material extension for ${values.projectCode}/${values.materialCode} has been successfully assigned to plant ${values.plantCode} `}
              />
            )
          });

          resetForm();
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

          {/* Plant Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Plant Code"
              name="plantCode"
              rules={[{ required: true, message: 'Please select a plant code' }]}
            >
              <Select
                placeholder="Select plant code"
                showSearch
                loading={loadingStates.plants}
                onChange={handlePlantChange}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
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
                        <Text type="secondary">
                          ({(doc.fileSize / FILE_SIZE.BYTES_PER_MB).toFixed(2)}MB)
                        </Text>
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
          <Upload.Dragger
            multiple
            fileList={fileList}
            onChange={handleFileChange}
            beforeUpload={() => false} // Prevent auto upload
            accept=".pdf,.doc,.docx,.xls,.xlsx"
          >
            <p className="ant-upload-drag-icon">
              <UploadOutlined />
            </p>
            <p className="ant-upload-text">Click or drag files to this area to upload</p>
            <p className="ant-upload-hint">
              Support for PDF, Word, and Excel files. Maximum file size: 25MB
            </p>
          </Upload.Dragger>
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
