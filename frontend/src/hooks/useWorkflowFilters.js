import { useState, useEffect, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

import { COMPLETION_FILTERS } from '../constants';

/**
 * Custom hook for managing workflow filters with URL synchronization
 * Handles filter state, application, and persistence
 */
export const useWorkflowFilters = (workflows = []) => {
  const [searchParams, setSearchParams] = useSearchParams();
  
  // Filter state
  const [searchText, setSearchText] = useState(searchParams.get('search') || '');
  const [statusFilter, setStatusFilter] = useState(searchParams.get('status') || 'all');
  const [completionFilter, setCompletionFilter] = useState(searchParams.get('completion') || 'all');

  /**
   * Apply all filters to the workflows data
   */
  const filteredWorkflows = useMemo(() => {
    let filtered = [...workflows];

    // Apply search filter
    if (searchText) {
      const searchLower = searchText.toLowerCase();
      filtered = filtered.filter(
        workflow =>
          workflow.materialCode?.toLowerCase().includes(searchLower) ||
          workflow.plantCode?.toLowerCase().includes(searchLower) ||
          workflow.materialName?.toLowerCase().includes(searchLower) ||
          workflow.itemDescription?.toLowerCase().includes(searchLower)
      );
    }

    // Apply status filter
    if (statusFilter !== 'all') {
      filtered = filtered.filter(workflow => workflow.completionStatus === statusFilter);
    }

    // Apply completion filter
    if (completionFilter !== 'all') {
      if (completionFilter === COMPLETION_FILTERS.COMPLETED) {
        filtered = filtered.filter(workflow => workflow.completionPercentage === 100);
      } else if (completionFilter === COMPLETION_FILTERS.IN_PROGRESS) {
        filtered = filtered.filter(
          workflow => workflow.completionPercentage > 0 && workflow.completionPercentage < 100
        );
      } else if (completionFilter === COMPLETION_FILTERS.NOT_STARTED) {
        filtered = filtered.filter(workflow => workflow.completionPercentage === 0);
      }
    }

    return filtered;
  }, [workflows, searchText, statusFilter, completionFilter]);

  /**
   * Update search text filter
   */
  const updateSearchText = useCallback((value) => {
    setSearchText(value);
  }, []);

  /**
   * Update status filter
   */
  const updateStatusFilter = useCallback((value) => {
    setStatusFilter(value);
  }, []);

  /**
   * Update completion filter
   */
  const updateCompletionFilter = useCallback((value) => {
    setCompletionFilter(value);
  }, []);

  /**
   * Clear all filters
   */
  const clearAllFilters = useCallback(() => {
    setSearchText('');
    setStatusFilter('all');
    setCompletionFilter('all');
  }, []);

  /**
   * Apply multiple filters at once
   */
  const applyFilters = useCallback((filters) => {
    if (filters.searchText !== undefined) {
      setSearchText(filters.searchText);
    }
    if (filters.statusFilter !== undefined) {
      setStatusFilter(filters.statusFilter);
    }
    if (filters.completionFilter !== undefined) {
      setCompletionFilter(filters.completionFilter);
    }
  }, []);

  /**
   * Get filter presets
   */
  const filterPresets = useMemo(() => [
    {
      name: 'All Workflows',
      filters: { searchText: '', statusFilter: 'all', completionFilter: 'all' }
    },
    {
      name: 'In Progress',
      filters: { searchText: '', statusFilter: 'IN_PROGRESS', completionFilter: 'in-progress' }
    },
    {
      name: 'Completed',
      filters: { searchText: '', statusFilter: 'COMPLETED', completionFilter: 'completed' }
    },
    {
      name: 'Not Started',
      filters: { searchText: '', statusFilter: 'DRAFT', completionFilter: 'not-started' }
    },
    {
      name: 'Overdue',
      filters: { searchText: '', statusFilter: 'all', completionFilter: 'all' }
    }
  ], []);

  /**
   * Apply a filter preset
   */
  const applyPreset = useCallback((presetName) => {
    const preset = filterPresets.find(p => p.name === presetName);
    if (preset) {
      applyFilters(preset.filters);
    }
  }, [filterPresets, applyFilters]);

  /**
   * Get current filter summary
   */
  const filterSummary = useMemo(() => {
    const activeFilters = [];
    
    if (searchText) {
      activeFilters.push(`Search: "${searchText}"`);
    }
    if (statusFilter !== 'all') {
      activeFilters.push(`Status: ${statusFilter.replace('_', ' ')}`);
    }
    if (completionFilter !== 'all') {
      activeFilters.push(`Completion: ${completionFilter.replace('-', ' ')}`);
    }

    return {
      activeFilters,
      hasActiveFilters: activeFilters.length > 0,
      totalFiltered: filteredWorkflows.length,
      totalOriginal: workflows.length
    };
  }, [searchText, statusFilter, completionFilter, filteredWorkflows.length, workflows.length]);

  /**
   * Save current filters to localStorage
   */
  const saveFilters = useCallback(() => {
    const filters = {
      searchText,
      statusFilter,
      completionFilter
    };
    localStorage.setItem('workflowFilters', JSON.stringify(filters));
  }, [searchText, statusFilter, completionFilter]);

  /**
   * Load filters from localStorage
   */
  const loadSavedFilters = useCallback(() => {
    try {
      const saved = localStorage.getItem('workflowFilters');
      if (saved) {
        const filters = JSON.parse(saved);
        applyFilters(filters);
      }
    } catch (error) {
      console.warn('Failed to load saved filters:', error);
    }
  }, [applyFilters]);

  // Sync filters with URL parameters
  useEffect(() => {
    const params = new URLSearchParams();
    
    if (searchText) {
      params.set('search', searchText);
    }
    if (statusFilter !== 'all') {
      params.set('status', statusFilter);
    }
    if (completionFilter !== 'all') {
      params.set('completion', completionFilter);
    }

    // Update URL without triggering navigation
    setSearchParams(params, { replace: true });
  }, [searchText, statusFilter, completionFilter, setSearchParams]);

  return {
    // Filter state
    searchText,
    statusFilter,
    completionFilter,
    
    // Filtered data
    filteredWorkflows,
    
    // Filter actions
    updateSearchText,
    updateStatusFilter,
    updateCompletionFilter,
    clearAllFilters,
    applyFilters,
    
    // Presets
    filterPresets,
    applyPreset,
    
    // Persistence
    saveFilters,
    loadSavedFilters,
    
    // Summary
    filterSummary
  };
};