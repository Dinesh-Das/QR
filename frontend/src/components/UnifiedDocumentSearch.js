import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Input,
  Tabs,
  List,
  Button,
  Space,
  Typography,
  Tag,
  Select,
  DatePicker,
  Row,
  Col,
  Empty,
  message,
  Tooltip,
  Badge,
  Spin,
  Alert
} from 'antd';
import {
  SearchOutlined,
  DownloadOutlined,
  FileTextOutlined,
  FolderOutlined,
  QuestionCircleOutlined,
  MessageOutlined,
  FilterOutlined,
  ClearOutlined,
  CalendarOutlined,
  UserOutlined,
  FileOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined
} from '@ant-design/icons';

import { documentAPI } from '../services/documentAPI';
import { queryAPI } from '../services/queryAPI';
import { UI_CONFIG } from '../constants';

const { Search } = Input;
const { Text, Title } = Typography;
const { TabPane } = Tabs;
const { Option } = Select;
const { RangePicker } = DatePicker;

/**
 * UnifiedDocumentSearch Component
 * 
 * Provides unified search across both workflow and query documents with:
 * - Tabbed interface for different document types
 * - Advanced filtering and sorting capabilities
 * - Integration with existing document management patterns
 * - Real-time search with debouncing
 * 
 * @param {Object} props
 * @param {string} props.projectCode - Project code to filter by (optional)
 * @param {string} props.materialCode - Material code to filter by (optional)
 * @param {boolean} props.showFilters - Whether to show advanced filters (default: true)
 * @param {Function} props.onDocumentSelect - Callback when document is selected
 * @param {string} props.defaultTab - Default active tab ('all', 'workflow', 'query', 'response')
 */
const UnifiedDocumentSearch = ({
  projectCode,
  materialCode,
  showFilters = true,
  onDocumentSelect,
  defaultTab = 'all'
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState(defaultTab);
  const [searchResults, setSearchResults] = useState({
    all: [],
    workflow: [],
    query: [],
    response: []
  });
  const [loading, setLoading] = useState(false);
  const [downloadingIds, setDownloadingIds] = useState(new Set());
  
  // Filter states
  const [filters, setFilters] = useState({
    fileType: null,
    uploadedBy: null,
    dateRange: null,
    sortBy: 'uploadedAt',
    sortOrder: 'desc'
  });
  const [selectedSources, setSelectedSources] = useState(['WORKFLOW', 'QUERY', 'RESPONSE']);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  // Debounced search
  const [searchTimeout, setSearchTimeout] = useState(null);

  // Perform search with current parameters
  const performSearch = useCallback(async (term = searchTerm, currentFilters = filters) => {
    try {
      setLoading(true);
      
      const searchFilters = {
        ...currentFilters,
        dateFrom: currentFilters.dateRange?.[0]?.format('YYYY-MM-DD'),
        dateTo: currentFilters.dateRange?.[1]?.format('YYYY-MM-DD')
      };

      // Use the enhanced unified search API if available, fallback to regular search
      let groupedResults;
      
      try {
        // Try the new unified search API first
        const unifiedResult = await documentAPI.searchAllDocumentsUnified(
          term,
          projectCode,
          materialCode,
          selectedSources.length > 0 ? selectedSources : ['WORKFLOW', 'QUERY', 'RESPONSE']
        );
        
        // Use the structured response from unified search
        groupedResults = {
          all: unifiedResult.allDocuments || [],
          workflow: unifiedResult.documentsBySource?.WORKFLOW || [],
          query: unifiedResult.documentsBySource?.QUERY || [],
          response: unifiedResult.documentsBySource?.RESPONSE || []
        };
        
        // Log search metadata for debugging
        if (unifiedResult.searchMetadata) {
          console.log('Search completed:', unifiedResult.searchMetadata);
        }
      } catch (unifiedError) {
        console.warn('Unified search failed, falling back to regular search:', unifiedError);
        
        // Fallback to regular search
        const results = await documentAPI.searchAllDocuments(
          term,
          projectCode,
          materialCode,
          searchFilters
        );

        // Group results by document source
        groupedResults = {
          all: results,
          workflow: results.filter(doc => doc.documentSource === 'WORKFLOW'),
          query: results.filter(doc => doc.documentSource === 'QUERY'),
          response: results.filter(doc => doc.documentSource === 'RESPONSE')
        };
      }

      setSearchResults(groupedResults);
    } catch (error) {
      console.error('Error searching documents:', error);
      message.error('Failed to search documents. Please try again.');
      setSearchResults({ all: [], workflow: [], query: [], response: [] });
    } finally {
      setLoading(false);
    }
  }, [searchTerm, filters, projectCode, materialCode]);

  // Handle search input change with debouncing
  const handleSearchChange = (value) => {
    setSearchTerm(value);
    
    // Clear existing timeout
    if (searchTimeout) {
      clearTimeout(searchTimeout);
    }
    
    // Set new timeout for debounced search
    const timeout = setTimeout(() => {
      performSearch(value);
    }, UI_CONFIG.DEBOUNCE_DELAY);
    
    setSearchTimeout(timeout);
  };

  // Handle filter changes
  const handleFilterChange = (filterKey, value) => {
    const newFilters = { ...filters, [filterKey]: value };
    setFilters(newFilters);
    performSearch(searchTerm, newFilters);
  };

  // Clear all filters
  const clearFilters = () => {
    const clearedFilters = {
      fileType: null,
      uploadedBy: null,
      dateRange: null,
      sortBy: 'uploadedAt',
      sortOrder: 'desc'
    };
    setFilters(clearedFilters);
    performSearch(searchTerm, clearedFilters);
  };

  // Format file size for display
  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // Format date for display
  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    try {
      return new Date(dateString).toLocaleDateString('en-GB', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      return 'Invalid Date';
    }
  };

  // Get file type icon
  const getFileIcon = (fileName) => {
    if (!fileName) return <FileOutlined />;
    
    const extension = fileName.toLowerCase().split('.').pop();
    const iconStyle = { fontSize: '16px' };
    
    switch (extension) {
      case 'pdf':
        return <FileTextOutlined style={{ ...iconStyle, color: '#ff4d4f' }} />;
      case 'doc':
      case 'docx':
        return <FileTextOutlined style={{ ...iconStyle, color: '#1890ff' }} />;
      case 'xls':
      case 'xlsx':
        return <FileTextOutlined style={{ ...iconStyle, color: '#52c41a' }} />;
      case 'jpg':
      case 'jpeg':
      case 'png':
        return <FileOutlined style={{ ...iconStyle, color: '#722ed1' }} />;
      default:
        return <FileOutlined style={iconStyle} />;
    }
  };

  // Get document source icon and color
  const getSourceInfo = (documentSource) => {
    switch (documentSource) {
      case 'WORKFLOW':
        return { icon: <FolderOutlined />, color: 'blue', label: 'Workflow' };
      case 'QUERY':
        return { icon: <QuestionCircleOutlined />, color: 'orange', label: 'Query' };
      case 'RESPONSE':
        return { icon: <MessageOutlined />, color: 'green', label: 'Response' };
      default:
        return { icon: <FileOutlined />, color: 'default', label: 'Unknown' };
    }
  };

  // Handle document download
  const handleDownload = async (document) => {
    try {
      setDownloadingIds(prev => new Set([...prev, document.id]));
      
      let blob;
      if (document.documentSource === 'WORKFLOW') {
        blob = await documentAPI.downloadDocument(document.id, document.workflowId);
      } else {
        blob = await queryAPI.downloadQueryDocument(document.queryId, document.id);
      }
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = document.originalFileName || document.fileName;
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);
      
      message.success(`Downloaded ${document.originalFileName || document.fileName}`);
    } catch (error) {
      console.error('Error downloading document:', error);
      message.error('Failed to download document. Please try again.');
    } finally {
      setDownloadingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(document.id);
        return newSet;
      });
    }
  };

  // Render document list
  const renderDocumentList = (documents) => {
    if (documents.length === 0) {
      return (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            <Space direction="vertical">
              <Text type="secondary">
                {searchTerm ? 'No documents found matching your search' : 'No documents found'}
              </Text>
              {searchTerm && (
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  Try adjusting your search terms or filters
                </Text>
              )}
            </Space>
          }
        />
      );
    }

    return (
      <List
        dataSource={documents}
        renderItem={document => {
          const sourceInfo = getSourceInfo(document.documentSource);
          
          return (
            <List.Item
              actions={[
                <Tooltip title="Download document" key="download">
                  <Button
                    type="text"
                    icon={<DownloadOutlined />}
                    onClick={() => handleDownload(document)}
                    loading={downloadingIds.has(document.id)}
                    size="small"
                  />
                </Tooltip>
              ]}
              onClick={() => onDocumentSelect && onDocumentSelect(document)}
              style={{ cursor: onDocumentSelect ? 'pointer' : 'default' }}
            >
              <List.Item.Meta
                avatar={getFileIcon(document.originalFileName)}
                title={
                  <Space>
                    <Text strong>{document.originalFileName || document.fileName}</Text>
                    <Tag color={sourceInfo.color} icon={sourceInfo.icon} size="small">
                      {sourceInfo.label}
                    </Tag>
                    {document.isReused && (
                      <Tag color="purple" size="small">Reused</Tag>
                    )}
                  </Space>
                }
                description={
                  <Space direction="vertical" size="small">
                    <Space size="large">
                      <Text type="secondary">
                        <FileOutlined style={{ marginRight: '4px' }} />
                        {formatFileSize(document.fileSize)}
                      </Text>
                      <Text type="secondary">
                        <CalendarOutlined style={{ marginRight: '4px' }} />
                        {formatDate(document.uploadedAt)}
                      </Text>
                      <Text type="secondary">
                        <UserOutlined style={{ marginRight: '4px' }} />
                        {document.uploadedBy || 'Unknown'}
                      </Text>
                    </Space>
                    {(document.projectCode || document.materialCode) && (
                      <Space>
                        {document.projectCode && (
                          <Text type="secondary" style={{ fontSize: '12px' }}>
                            Project: {document.projectCode}
                          </Text>
                        )}
                        {document.materialCode && (
                          <Text type="secondary" style={{ fontSize: '12px' }}>
                            Material: {document.materialCode}
                          </Text>
                        )}
                      </Space>
                    )}
                  </Space>
                }
              />
            </List.Item>
          );
        }}
      />
    );
  };

  // Initial search on component mount
  useEffect(() => {
    performSearch('');
  }, []);

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (searchTimeout) {
        clearTimeout(searchTimeout);
      }
    };
  }, [searchTimeout]);

  return (
    <Card
      title={
        <Space>
          <SearchOutlined />
          <span>Document Search</span>
          {(projectCode || materialCode) && (
            <Text type="secondary" style={{ fontSize: '12px' }}>
              ({projectCode && `Project: ${projectCode}`}
              {projectCode && materialCode && ', '}
              {materialCode && `Material: ${materialCode}`})
            </Text>
          )}
        </Space>
      }
      extra={
        showFilters && (
          <Button
            icon={<FilterOutlined />}
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
            size="small"
            type={showAdvancedFilters ? 'primary' : 'default'}
          >
            Filters
          </Button>
        )
      }
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        {/* Search Input */}
        <Search
          placeholder="Search documents by name, content, or uploader..."
          value={searchTerm}
          onChange={(e) => handleSearchChange(e.target.value)}
          onSearch={(value) => performSearch(value)}
          loading={loading}
          size="large"
          allowClear
        />

        {/* Advanced Filters */}
        {showFilters && showAdvancedFilters && (
          <Card size="small" title="Advanced Filters">
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={6}>
                <Text strong>File Type</Text>
                <Select
                  placeholder="All types"
                  value={filters.fileType}
                  onChange={(value) => handleFilterChange('fileType', value)}
                  style={{ width: '100%' }}
                  allowClear
                >
                  <Option value="pdf">PDF</Option>
                  <Option value="doc">Word</Option>
                  <Option value="docx">Word (New)</Option>
                  <Option value="xls">Excel</Option>
                  <Option value="xlsx">Excel (New)</Option>
                  <Option value="jpg">JPEG</Option>
                  <Option value="png">PNG</Option>
                </Select>
              </Col>
              
              <Col xs={24} sm={12} md={6}>
                <Text strong>Uploaded By</Text>
                <Input
                  placeholder="Username"
                  value={filters.uploadedBy}
                  onChange={(e) => handleFilterChange('uploadedBy', e.target.value)}
                  allowClear
                />
              </Col>
              
              <Col xs={24} sm={12} md={8}>
                <Text strong>Date Range</Text>
                <RangePicker
                  value={filters.dateRange}
                  onChange={(dates) => handleFilterChange('dateRange', dates)}
                  style={{ width: '100%' }}
                />
              </Col>
              
              <Col xs={24} sm={12} md={4}>
                <Text strong>Sort</Text>
                <Space.Compact style={{ width: '100%' }}>
                  <Select
                    value={filters.sortBy}
                    onChange={(value) => handleFilterChange('sortBy', value)}
                    style={{ width: '70%' }}
                  >
                    <Option value="uploadedAt">Date</Option>
                    <Option value="fileName">Name</Option>
                    <Option value="fileSize">Size</Option>
                    <Option value="uploadedBy">Uploader</Option>
                  </Select>
                  <Button
                    icon={filters.sortOrder === 'asc' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
                    onClick={() => handleFilterChange('sortOrder', filters.sortOrder === 'asc' ? 'desc' : 'asc')}
                    style={{ width: '30%' }}
                  />
                </Space.Compact>
              </Col>
            </Row>
            
            <div style={{ marginTop: '16px', textAlign: 'right' }}>
              <Button
                icon={<ClearOutlined />}
                onClick={clearFilters}
                size="small"
              >
                Clear Filters
              </Button>
            </div>
          </Card>
        )}

        {/* Results Summary */}
        {!loading && (
          <Alert
            message={`Found ${searchResults.all.length} document(s)`}
            description={
              <Space>
                <Text type="secondary">
                  {searchResults.workflow.length} workflow,
                  {' '}{searchResults.query.length} query,
                  {' '}{searchResults.response.length} response documents
                </Text>
              </Space>
            }
            type="info"
            showIcon
            size="small"
          />
        )}

        {/* Tabbed Results */}
        <Spin spinning={loading}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            type="card"
            size="small"
          >
            <TabPane
              tab={
                <Badge count={searchResults.all.length} size="small">
                  <Space>
                    <SearchOutlined />
                    All Documents
                  </Space>
                </Badge>
              }
              key="all"
            >
              {renderDocumentList(searchResults.all)}
            </TabPane>
            
            <TabPane
              tab={
                <Badge count={searchResults.workflow.length} size="small">
                  <Space>
                    <FolderOutlined />
                    Workflow
                  </Space>
                </Badge>
              }
              key="workflow"
            >
              {renderDocumentList(searchResults.workflow)}
            </TabPane>
            
            <TabPane
              tab={
                <Badge count={searchResults.query.length} size="small">
                  <Space>
                    <QuestionCircleOutlined />
                    Query
                  </Space>
                </Badge>
              }
              key="query"
            >
              {renderDocumentList(searchResults.query)}
            </TabPane>
            
            <TabPane
              tab={
                <Badge count={searchResults.response.length} size="small">
                  <Space>
                    <MessageOutlined />
                    Response
                  </Space>
                </Badge>
              }
              key="response"
            >
              {renderDocumentList(searchResults.response)}
            </TabPane>
          </Tabs>
        </Spin>
      </Space>
    </Card>
  );
};

export default UnifiedDocumentSearch;