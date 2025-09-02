import { SearchOutlined, ClearOutlined, SaveOutlined, FilterOutlined } from '@ant-design/icons';
import { Card, Row, Col, Input, Select, Button, Space, Tag, Divider } from 'antd';
import PropTypes from 'prop-types';
import React, { useCallback } from 'react';

const { Search } = Input;
const { Option } = Select;

/**
 * FilterPanel component provides filtering controls for workflow data
 * Optimized with React.memo and useCallback for performance
 */
const FilterPanel = React.memo(({ 
  searchText,
  statusFilter,
  completionFilter,
  onSearchTextChange,
  onStatusFilterChange,
  onCompletionFilterChange,
  onClearFilters,
  onApplyPreset,
  onSaveFilters,
  filterPresets,
  filterSummary
}) => {
  /**
   * Handle search text change
   */
  const handleSearchChange = useCallback((value) => {
    if (onSearchTextChange) {
      onSearchTextChange(value);
    }
  }, [onSearchTextChange]);

  /**
   * Handle status filter change
   */
  const handleStatusChange = useCallback((value) => {
    if (onStatusFilterChange) {
      onStatusFilterChange(value);
    }
  }, [onStatusFilterChange]);

  /**
   * Handle completion filter change
   */
  const handleCompletionChange = useCallback((value) => {
    if (onCompletionFilterChange) {
      onCompletionFilterChange(value);
    }
  }, [onCompletionFilterChange]);

  /**
   * Handle clear all filters
   */
  const handleClearFilters = useCallback(() => {
    if (onClearFilters) {
      onClearFilters();
    }
  }, [onClearFilters]);

  /**
   * Handle preset application
   */
  const handlePresetClick = useCallback((presetName) => {
    if (onApplyPreset) {
      onApplyPreset(presetName);
    }
  }, [onApplyPreset]);

  /**
   * Handle save filters
   */
  const handleSaveFilters = useCallback(() => {
    if (onSaveFilters) {
      onSaveFilters();
    }
  }, [onSaveFilters]);

  return (
    <Card 
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <FilterOutlined />
          <span>Filters</span>
          {filterSummary?.hasActiveFilters && (
            <Tag color="blue" style={{ marginLeft: 8 }}>
              {filterSummary.activeFilters.length} active
            </Tag>
          )}
        </div>
      }
      size="small"
      style={{ marginBottom: 16 }}
      extra={
        <Space>
          {onSaveFilters && (
            <Button 
              size="small" 
              icon={<SaveOutlined />}
              onClick={handleSaveFilters}
              disabled={!filterSummary?.hasActiveFilters}
            >
              Save
            </Button>
          )}
          <Button 
            size="small" 
            icon={<ClearOutlined />}
            onClick={handleClearFilters}
            disabled={!filterSummary?.hasActiveFilters}
          >
            Clear All
          </Button>
        </Space>
      }
    >
      {/* Main Filter Controls */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={8} md={8} lg={8}>
          <div style={{ marginBottom: 4, fontSize: '12px', fontWeight: '500', color: '#666' }}>
            Search Materials
          </div>
          <Search
            placeholder="Search by Material Code or Plant"
            value={searchText}
            onChange={(e) => handleSearchChange(e.target.value)}
            style={{ width: '100%' }}
            allowClear
            enterButton={<SearchOutlined />}
          />
        </Col>
        <Col xs={12} sm={8} md={8} lg={8}>
          <div style={{ marginBottom: 4, fontSize: '12px', fontWeight: '500', color: '#666' }}>
            Status Filter
          </div>
          <Select
            placeholder="Filter by Status"
            value={statusFilter}
            onChange={handleStatusChange}
            style={{ width: '100%' }}
            allowClear
          >
            <Option value="all">All Statuses</Option>
            <Option value="DRAFT">Draft</Option>
            <Option value="IN_PROGRESS">In Progress</Option>
            <Option value="COMPLETED">Completed</Option>
          </Select>
        </Col>
        <Col xs={12} sm={8} md={8} lg={8}>
          <div style={{ marginBottom: 4, fontSize: '12px', fontWeight: '500', color: '#666' }}>
            Completion Filter
          </div>
          <Select
            placeholder="Filter by Completion"
            value={completionFilter}
            onChange={handleCompletionChange}
            style={{ width: '100%' }}
            allowClear
          >
            <Option value="all">All Progress</Option>
            <Option value="not-started">Not Started (0%)</Option>
            <Option value="in-progress">In Progress (1-99%)</Option>
            <Option value="completed">Completed (100%)</Option>
          </Select>
        </Col>
      </Row>

      {/* Filter Presets */}
      {filterPresets && filterPresets.length > 0 && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          <div style={{ marginBottom: 8 }}>
            <span style={{ fontSize: '12px', fontWeight: '500', color: '#666' }}>
              Quick Filters:
            </span>
          </div>
          <Space wrap>
            {filterPresets.map((preset) => (
              <Button
                key={preset.name}
                size="small"
                type="default"
                onClick={() => handlePresetClick(preset.name)}
                style={{ fontSize: '12px' }}
              >
                {preset.name}
              </Button>
            ))}
          </Space>
        </>
      )}

      {/* Active Filters Summary */}
      {filterSummary?.hasActiveFilters && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          <div style={{ marginBottom: 8 }}>
            <span style={{ fontSize: '12px', fontWeight: '500', color: '#666' }}>
              Active Filters:
            </span>
          </div>
          <Space wrap>
            {filterSummary.activeFilters.map((filter, index) => (
              <Tag key={index} color="blue" style={{ fontSize: '11px' }}>
                {filter}
              </Tag>
            ))}
          </Space>
          <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
            Showing {filterSummary.totalFiltered} of {filterSummary.totalOriginal} workflows
          </div>
        </>
      )}

      {/* No Active Filters Message */}
      {!filterSummary?.hasActiveFilters && (
        <>
          <Divider style={{ margin: '12px 0' }} />
          <div style={{ fontSize: '12px', color: '#999', textAlign: 'center' }}>
            No filters applied - showing all workflows
          </div>
        </>
      )}
    </Card>
  );
});

FilterPanel.displayName = 'FilterPanel';

FilterPanel.propTypes = {
  searchText: PropTypes.string,
  statusFilter: PropTypes.string,
  completionFilter: PropTypes.string,
  onSearchTextChange: PropTypes.func,
  onStatusFilterChange: PropTypes.func,
  onCompletionFilterChange: PropTypes.func,
  onClearFilters: PropTypes.func,
  onApplyPreset: PropTypes.func,
  onSaveFilters: PropTypes.func,
  filterPresets: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    filters: PropTypes.object.isRequired
  })),
  filterSummary: PropTypes.shape({
    activeFilters: PropTypes.arrayOf(PropTypes.string),
    hasActiveFilters: PropTypes.bool,
    totalFiltered: PropTypes.number,
    totalOriginal: PropTypes.number
  })
};

FilterPanel.defaultProps = {
  searchText: '',
  statusFilter: 'all',
  completionFilter: 'all',
  onSearchTextChange: null,
  onStatusFilterChange: null,
  onCompletionFilterChange: null,
  onClearFilters: null,
  onApplyPreset: null,
  onSaveFilters: null,
  filterPresets: [],
  filterSummary: null
};

export default FilterPanel;