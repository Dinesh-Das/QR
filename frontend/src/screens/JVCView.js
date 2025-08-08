import {
  PlusOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  MessageOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import {
  Card,
  Button,
  Typography,
  Row,
  Col,
  message,
  Tag,
  Space,
  Divider,
  Tabs,
  Statistic,
  Alert,
  Table
} from 'antd';
import React, { useState, useEffect } from 'react';

import apiClient from '../api/client';
import MaterialExtensionForm from '../components/MaterialExtensionForm';
import PendingExtensionsList from '../components/PendingExtensionsListSimple';
import QueryHistoryTracker from '../components/QueryHistoryTracker';
import QueryInbox from '../components/QueryInbox';
import { JvcOnly } from '../components/RoleBasedComponent';
import { TEAM_NAMES, WORKFLOW_STATES, TAB_KEYS, PAGINATION } from '../constants';
import { documentAPI } from '../services/documentAPI';
import { workflowAPI } from '../services/workflowAPI';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

const JVCView = () => {
  const [loading, setLoading] = useState(false);
  const [completedWorkflows, setCompletedWorkflows] = useState([]);
  // eslint-disable-next-line no-unused-vars
  const [_duplicateWorkflow, setDuplicateWorkflow] = useState(null);
  // eslint-disable-next-line no-unused-vars
  const [_showDuplicateAlert, setShowDuplicateAlert] = useState(false);

  const [activeTab, setActiveTab] = useState('initiate');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [formKey] = useState(0); // setFormKey not currently used
  const [queryStats, setQueryStats] = useState({
    totalQueries: 0,
    openQueries: 0,
    resolvedToday: 0,
    overdueQueries: 0,
    avgResolutionTime: 0,
    highPriorityQueries: 0
  });



  useEffect(() => {
    const controller = new AbortController();



    const fetchData = async () => {
      try {
        await Promise.all([
          loadCompletedWorkflows(controller.signal),
          loadQueryStats(controller.signal)
        ]);
      } catch (error) {
        if (!controller.signal.aborted) {
          console.error('Error loading JVC data:', error);
        }
      }
    };

    fetchData();

    return () => {
      controller.abort();
    };
  }, []);
  const loadCompletedWorkflows = async signal => {
    try {
      setLoading(true);
      // Load completed workflows
      const completed = await workflowAPI.getWorkflowsByState(WORKFLOW_STATES.COMPLETED, {
        signal
      });
      if (!signal?.aborted) {
        setCompletedWorkflows(completed || []);
      }
    } catch (error) {
      if (!signal?.aborted) {
        console.error('Error loading completed workflows:', error);
        message.error('Failed to load completed workflows');
      }
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
      }
    }
  };

  const loadQueryStats = async signal => {
    try {
      const [openCount, resolvedToday, overdueQueries, avgTime, highPriorityQueries] =
        await Promise.all([
          apiClient.get('/queries/stats/count-open/JVC', { signal }).catch(() => 0),
          apiClient.get('/queries/stats/resolved-today', { signal }).catch(() => 0),
          apiClient
            .get('/queries/overdue', { signal })
            .then(data =>
              Array.isArray(data) ? data.filter(q => q.assignedTeam === TEAM_NAMES.JVC).length : 0
            )
            .catch(() => 0),
          apiClient.get('/queries/stats/avg-resolution-time/JVC', { signal }).catch(() => 0),
          apiClient
            .get('/queries/high-priority', { signal })
            .then(data =>
              Array.isArray(data) ? data.filter(q => q.assignedTeam === TEAM_NAMES.JVC).length : 0
            )
            .catch(() => 0)
        ]);

      setQueryStats({
        totalQueries: (openCount || 0) + (resolvedToday || 0),
        openQueries: openCount || 0,
        resolvedToday: resolvedToday || 0,
        overdueQueries: overdueQueries || 0,
        avgResolutionTime: avgTime || 0,
        highPriorityQueries: highPriorityQueries || 0
      });
    } catch (error) {
      console.error('Failed to load query stats:', error);
      // Set default values on error
      setQueryStats({
        totalQueries: 0,
        openQueries: 0,
        resolvedToday: 0,
        overdueQueries: 0,
        avgResolutionTime: 0,
        highPriorityQueries: 0
      });
    }
  };

  const checkForExistingWorkflow = async (projectCode, materialCode, plantCode) => {
    try {
      console.log('Checking for existing workflow:', {
        projectCode,
        materialCode,
        plantCode
      });
      const response = await workflowAPI.checkWorkflowExists(
        projectCode,
        materialCode,
        plantCode
      );
      console.log('Check workflow response:', response);
      return response.exists ? response.workflow : null;
    } catch (error) {
      console.error('Error checking for existing workflow:', error);
      return null;
    }
  };

  const handleInitiateWorkflow = async formData => {
    try {
      setLoading(true);

      const plantCodes = formData.plantCodes || [];

      // Use smart extension API for multiple plants
      if (plantCodes.length > 1) {
        const extensionData = {
          projectCode: formData.projectCode,
          materialCode: formData.materialCode,
          plantCodes
        };

        const smartResult = await workflowAPI.extendToMultiplePlantsSmartly(extensionData);

        // Handle document uploads for newly created workflows
        if (smartResult.details.newlyCreatedWorkflows.length > 0) {
          const documentErrors = [];

          for (const workflow of smartResult.details.newlyCreatedWorkflows) {
            try {
              // Upload new documents if any
              if (formData.uploadedFiles && formData.uploadedFiles.length > 0) {
                const files = formData.uploadedFiles.map(file => {
                  return file.originFileObj || file;
                });

                await documentAPI.uploadDocuments(
                  files,
                  formData.projectCode,
                  formData.materialCode,
                  workflow.id
                );
              }

              // Reuse existing documents if any selected
              if (formData.reusedDocuments && formData.reusedDocuments.length > 0) {
                await documentAPI.reuseDocuments(formData.reusedDocuments, workflow.id);
              }
            } catch (docError) {
              console.error(`Document operation failed for workflow ${workflow.id}:`, docError);
              documentErrors.push({
                workflowId: workflow.id,
                plantCode: workflow.plantCode,
                error: docError.message || 'Document operation failed'
              });
            }
          }

          // Show enhanced document error warnings if any occurred
          if (documentErrors.length > 0) {
            const affectedPlants = documentErrors.map(e => e.plantCode).join(', ');
            const errorMessage = `Document operations failed for ${documentErrors.length} workflow(s) at plant${documentErrors.length !== 1 ? 's' : ''}: ${affectedPlants}. Workflows were created successfully but documents may need to be uploaded manually.`;

            // Log detailed error information for debugging
            console.error('Document operation failures:', documentErrors);

            message.warning({
              content: errorMessage,
              duration: 12,
              style: { marginTop: '20vh' }
            });

            // Also show individual error details if available
            documentErrors.forEach(error => {
              console.error(`Plant ${error.plantCode} (Workflow ${error.workflowId}): ${error.error}`);
            });
          }
        }

        // Enhanced success message with comprehensive document reuse information
        let successMessage = smartResult.message;

        if (smartResult.details.documentReuse && smartResult.details.documentReuse.totalReusedDocuments > 0) {
          const docInfo = smartResult.details.documentReuse;
          const totalDocs = docInfo.totalReusedDocuments;
          const workflowCount = smartResult.details.newlyCreatedWorkflows.length;

          // Add document reuse summary
          successMessage += ` ${totalDocs} document${totalDocs !== 1 ? 's' : ''} ${totalDocs === 1 ? 'was' : 'were'} automatically reused across ${workflowCount} workflow${workflowCount !== 1 ? 's' : ''}.`;

          // Add source breakdown if available
          if (docInfo.statistics) {
            const sources = [];
            if (docInfo.statistics.workflowDocuments > 0) {
              sources.push(`${docInfo.statistics.workflowDocuments} from previous workflows`);
            }
            if (docInfo.statistics.queryDocuments > 0) {
              sources.push(`${docInfo.statistics.queryDocuments} from queries`);
            }
            if (docInfo.statistics.responseDocuments > 0) {
              sources.push(`${docInfo.statistics.responseDocuments} from query responses`);
            }

            if (sources.length > 0) {
              successMessage += ` Document sources: ${sources.join(', ')}.`;
            }
          }

          // Add reuse strategy information
          if (docInfo.reuseStrategy) {
            successMessage += ` Reuse strategy: ${docInfo.reuseStrategy}.`;
          }

          // Add source description if available
          if (docInfo.sourceDescription) {
            successMessage += ` ${docInfo.sourceDescription}`;
          }
        } else {
          // Mention when no documents were available for reuse
          successMessage += ' No existing documents were found for automatic reuse.';
        }

        // Show enhanced success message
        message.success({
          content: successMessage,
          duration: 10,
          style: { marginTop: '20vh' }
        });

        // Show additional notification for document reuse details if significant
        if (smartResult.details.documentReuse && smartResult.details.documentReuse.totalReusedDocuments > 5) {
          const docInfo = smartResult.details.documentReuse;
          setTimeout(() => {
            message.info({
              content: `Document reuse summary: ${docInfo.totalReusedDocuments} documents were automatically attached to ${smartResult.details.newlyCreatedWorkflows.length} workflows, saving time and ensuring consistency across plants.`,
              duration: 6,
              style: { marginTop: '20vh' }
            });
          }, 2000);
        }

        // Trigger data refresh
        setRefreshTrigger(prev => prev + 1);
        setActiveTab(TAB_KEYS.PENDING);

        // Refresh completed workflows and stats
        loadCompletedWorkflows();
        loadQueryStats();

        // Return smart result with detailed duplicate information
        return {
          success: smartResult.success,
          message: successMessage,
          summary: smartResult.summary,
          details: smartResult.details, // Include full details for modal display
          createdWorkflows: smartResult.details.newlyCreatedWorkflows,
          duplicateWorkflows: smartResult.details.duplicateWorkflows.existingWorkflows,
          duplicatePlants: smartResult.details.duplicateWorkflows.plants,
          failedPlants: smartResult.details.failedPlants,
          isSmartExtension: true
        };
      }

      // Single plant workflow creation (fallback to original logic)
      const plantCode = plantCodes[0];
      const existingWorkflow = await checkForExistingWorkflow(
        formData.projectCode,
        formData.materialCode,
        plantCode
      );

      if (existingWorkflow) {
        return {
          isDuplicate: true,
          existingWorkflow,
          formData,
          duplicateWorkflows: [{ plantCode, existingWorkflow }]
        };
      }

      // Create single workflow
      const workflowData = {
        projectCode: formData.projectCode,
        materialCode: formData.materialCode,
        plantCode,
        initiatedBy: 'current-user' // In real app, get from auth context
      };

      const createdWorkflow = await workflowAPI.createWorkflow(workflowData);

      let documentOperationFailed = false;
      let documentErrorMessage = '';

      try {
        // Upload new documents if any
        if (formData.uploadedFiles && formData.uploadedFiles.length > 0) {
          const files = formData.uploadedFiles.map(file => {
            return file.originFileObj || file;
          });

          await documentAPI.uploadDocuments(
            files,
            formData.projectCode,
            formData.materialCode,
            createdWorkflow.id
          );
        }

        // Reuse existing documents if any selected
        if (formData.reusedDocuments && formData.reusedDocuments.length > 0) {
          await documentAPI.reuseDocuments(formData.reusedDocuments, createdWorkflow.id);
        }
      } catch (docError) {
        console.error('Document operation failed for single workflow:', docError);
        documentOperationFailed = true;
        documentErrorMessage = docError.message || 'Document operation failed';

        // Determine what type of document operation failed
        const uploadCount = formData.uploadedFiles?.length || 0;
        const reuseCount = formData.reusedDocuments?.length || 0;
        let operationType = '';

        if (uploadCount > 0 && reuseCount > 0) {
          operationType = 'document upload and reuse operations';
        } else if (uploadCount > 0) {
          operationType = 'document upload operation';
        } else if (reuseCount > 0) {
          operationType = 'document reuse operation';
        } else {
          operationType = 'document operations';
        }

        // Show enhanced document error warning
        message.warning({
          content: `Workflow created successfully for ${formData.projectCode}/${formData.materialCode} at plant ${plantCode}, but ${operationType} failed: ${documentErrorMessage}. Please upload or reuse documents manually from the workflow details page.`,
          duration: 10,
          style: { marginTop: '20vh' }
        });
      }

      // Enhanced success message for single workflow with comprehensive document information
      if (!documentOperationFailed) {
        const totalDocs = (formData.uploadedFiles?.length || 0) + (formData.reusedDocuments?.length || 0);
        let successMessage = `Workflow created successfully for ${formData.projectCode}/${formData.materialCode} at plant ${plantCode}.`;

        if (totalDocs > 0) {
          const newDocs = formData.uploadedFiles?.length || 0;
          const reusedDocs = formData.reusedDocuments?.length || 0;

          successMessage += ` ${totalDocs} document${totalDocs !== 1 ? 's' : ''} ${totalDocs === 1 ? 'was' : 'were'} attached`;

          if (newDocs > 0 && reusedDocs > 0) {
            successMessage += ` (${newDocs} newly uploaded, ${reusedDocs} reused from existing materials)`;
          } else if (reusedDocs > 0) {
            successMessage += ` (all ${reusedDocs} reused from existing materials for this project/material combination)`;
          } else if (newDocs > 0) {
            successMessage += ` (all ${newDocs} newly uploaded)`;
          }
          successMessage += '.';
        } else {
          successMessage += ' No documents were attached to this workflow.';
        }

        // Show enhanced success message only if document operations succeeded
        message.success({
          content: successMessage,
          duration: 8,
          style: { marginTop: '20vh' }
        });
      }

      // Trigger data refresh
      setRefreshTrigger(prev => prev + 1);
      setActiveTab(TAB_KEYS.PENDING);

      // Refresh completed workflows and stats
      loadCompletedWorkflows();
      loadQueryStats();

      return {
        success: true,
        message: documentOperationFailed
          ? `Workflow created successfully but document operations failed: ${documentErrorMessage}. Please upload documents manually.`
          : `Workflow created successfully for ${formData.projectCode}/${formData.materialCode} at plant ${plantCode} with all document operations completed.`,
        createdWorkflows: [createdWorkflow],
        duplicateWorkflows: [],
        duplicatePlants: [],
        failedPlants: [],
        documentOperationFailed,
        documentErrorMessage
      };

    } catch (error) {
      console.error('Error initiating workflows:', error);

      // Provide more specific error messages based on error type
      let errorMessage = 'Failed to initiate workflows';

      if (error.message) {
        if (error.message.includes('document')) {
          errorMessage = `Workflow creation failed due to document processing error: ${error.message}`;
        } else if (error.message.includes('duplicate')) {
          errorMessage = `Workflow creation failed due to duplicate detection: ${error.message}`;
        } else if (error.message.includes('permission') || error.message.includes('access')) {
          errorMessage = `Workflow creation failed due to access permissions: ${error.message}`;
        } else {
          errorMessage = `Failed to initiate workflows: ${error.message}`;
        }
      } else {
        errorMessage = 'Failed to initiate workflows. Please check your connection and try again.';
      }

      message.error({
        content: errorMessage,
        duration: 8,
        style: { marginTop: '20vh' }
      });

      // Return error information for potential handling by calling component
      return {
        success: false,
        error: error.message || 'Unknown error occurred',
        message: errorMessage
      };
    } finally {
      setLoading(false);
    }
  };

  const handleExtendToPlant = async workflow => {
    try {
      setLoading(true);

      await workflowAPI.extendWorkflow(workflow.id, {
        plantCode: workflow.plantCode,
        comment: `Extended to plant ${workflow.plantCode} for questionnaire completion. Project: ${workflow.projectCode}, Material: ${workflow.materialCode}`
      });

      message.success({
        content: `Workflow successfully extended to plant ${workflow.plantCode}. Plant team has been notified to begin questionnaire completion for ${workflow.projectCode}/${workflow.materialCode}.`,
        duration: 6
      });

      setRefreshTrigger(prev => prev + 1);

      // Refresh stats
      loadQueryStats();
    } catch (error) {
      console.error('Error extending workflow:', error);
      message.error({
        content: `Failed to extend workflow for ${workflow.projectCode}/${workflow.materialCode} to plant ${workflow.plantCode}. Please try again.`,
        duration: 5
      });
      throw error; // Re-throw to let PendingExtensionsList handle it
    } finally {
      setLoading(false);
    }
  };

  const refreshData = () => {
    loadCompletedWorkflows();
    loadQueryStats();
    setRefreshTrigger(prev => prev + 1);
  };

  // const handleViewExistingWorkflow = workflow => { // Not currently used
  //   setShowDuplicateAlert(false);
  //   setDuplicateWorkflow(null);
  //   setActiveTab(TAB_KEYS.PENDING);
  //   // Reset the form by changing its key, which forces a re-render
  //   setFormKey(prev => prev + 1);
  //   message.info(`Switched to Pending Extensions tab. Look for workflow #${workflow.id}.`);
  // };

  // const handleCloseDuplicateAlert = () => { // Not currently used
  //   setShowDuplicateAlert(false);
  //   setDuplicateWorkflow(null);
  //   // Reset the form by changing its key, which forces a re-render
  //   setFormKey(prev => prev + 1);
  // };

  const completedColumns = [
    {
      title: 'Project Code',
      dataIndex: 'projectCode',
      key: 'projectCode',
      render: text => <Text strong>{text}</Text>
    },
    {
      title: 'Material Code',
      dataIndex: 'materialCode',
      key: 'materialCode',
      width: 140,
      render: (text, record) => (
        <div>
          <Text code style={{ fontWeight: 'bold' }}>
            {text}
          </Text>
          {record.itemDescription && (
            <div style={{ fontSize: '12px', color: '#666', marginTop: '2px' }}>
              {record.itemDescription}
            </div>
          )}
        </div>
      )
    },
    {
      title: 'Plant Code',
      dataIndex: 'plantCode',
      key: 'plantCode'
    },
    {
      title: 'Completed',
      dataIndex: 'lastModified',
      key: 'lastModified',
      render: date => new Date(date).toLocaleDateString()
    },
    {
      title: 'Status',
      dataIndex: 'state',
      key: 'state',
      render: () => (
        <Tag color="green" icon={<CheckCircleOutlined />}>
          Completed
        </Tag>
      )
    }
  ];

  return (
    <JvcOnly>
      <div style={{ padding: 24 }}>
        <Title level={2}>JVC Dashboard</Title>
        <Text type="secondary">
          Initiate MSDS workflows and manage material safety documentation process
        </Text>

        <Divider />

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          tabBarExtraContent={
            <Space>
              <Button icon={<ReloadOutlined />} onClick={refreshData} size="small">
                Refresh
              </Button>
            </Space>
          }
        >
          <TabPane tab="Initiate Workflow" key="initiate" icon={<PlusOutlined />}>
            <Row gutter={24}>
              <Col span={18}>
                <MaterialExtensionForm
                  key={formKey}
                  onSubmit={handleInitiateWorkflow}
                  loading={loading}
                />
              </Col>

              <Col span={6}>
                <Card title="Quick Stats">
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ marginBottom: 16 }}>
                      <Text type="secondary">Completed This Month</Text>
                      <div style={{ fontSize: 24, fontWeight: 'bold', color: '#52c41a' }}>
                        {completedWorkflows.length}
                      </div>
                    </div>
                  </div>
                </Card>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab="Pending Extensions" key="pending" icon={<ClockCircleOutlined />}>
            <PendingExtensionsList
              onExtendToPlant={handleExtendToPlant}
              refreshTrigger={refreshTrigger}
            />
          </TabPane>

          <TabPane
            tab={`Completed (${completedWorkflows.length})`}
            key="completed"
            icon={<CheckCircleOutlined />}
          >
            <Card title="Completed Workflows">
              <Table
                dataSource={completedWorkflows}
                columns={completedColumns}
                rowKey="id"
                loading={loading}
                pagination={{ pageSize: PAGINATION.DEFAULT_PAGE_SIZE }}
                locale={{
                  emptyText: 'No completed workflows'
                }}
              />
            </Card>
          </TabPane>

          {/* Enhanced Query Management Tabs */}
          <TabPane
            tab={
              <Space>
                <MessageOutlined />
                <span>Query Inbox</span>
                {queryStats.openQueries > 0 && (
                  <span
                    style={{
                      background: '#ff4d4f',
                      color: 'white',
                      borderRadius: '10px',
                      padding: '2px 6px',
                      fontSize: '12px'
                    }}
                  >
                    {queryStats.openQueries}
                  </span>
                )}
              </Space>
            }
            key="queries"
          >
            {/* Query Stats */}
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="Open Queries"
                    value={queryStats.openQueries}
                    prefix={<MessageOutlined />}
                    valueStyle={{ color: '#cf1322' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="Resolved Today"
                    value={queryStats.resolvedToday}
                    prefix={<CheckCircleOutlined />}
                    valueStyle={{ color: '#3f8600' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="Overdue"
                    value={queryStats.overdueQueries}
                    prefix={<ExclamationCircleOutlined />}
                    valueStyle={{ color: queryStats.overdueQueries > 0 ? '#cf1322' : '#3f8600' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="Avg Resolution"
                    value={queryStats.avgResolutionTime}
                    precision={1}
                    suffix="hrs"
                    prefix={<ClockCircleOutlined />}
                  />
                </Card>
              </Col>
            </Row>

            {/* Alerts for urgent items */}
            {queryStats.overdueQueries > 0 && (
              <Alert
                message={`${queryStats.overdueQueries} queries are overdue (>3 days)`}
                description="These queries require immediate attention to maintain SLA compliance."
                type="error"
                showIcon
                style={{ marginBottom: 16 }}
                action={
                  <Button size="small" danger>
                    View Overdue
                  </Button>
                }
              />
            )}

            {queryStats.highPriorityQueries > 0 && (
              <Alert
                message={`${queryStats.highPriorityQueries} high priority queries pending`}
                description="These queries have been marked as high priority and need urgent resolution."
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                action={
                  <Button size="small" type="primary">
                    View High Priority
                  </Button>
                }
              />
            )}

            <QueryInbox team="JVC" userRole="JVC_USER" />
          </TabPane>

          <TabPane
            tab={
              <Space>
                <HistoryOutlined />
                <span>Query History</span>
              </Space>
            }
            key="history"
          >
            <QueryHistoryTracker />
          </TabPane>
        </Tabs>
      </div>
    </JvcOnly>
  );
};

export default JVCView;
