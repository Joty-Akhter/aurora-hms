import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  CircularProgress,
  FormControl,
  FormHelperText,
  InputLabel,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  InputAdornment
} from '@mui/material';
import { Business as BusinessIcon, Map as MapIcon, Place as PlaceIcon, LocationOn as TerritoryIcon } from '@mui/icons-material';
import { SimpleTreeView, TreeItem } from '@mui/x-tree-view';
import { useAuth } from '../../contexts/AuthContext';
import pharmaService, { Division, Region, Area, Territory } from '../../services/pharmaService';

const PREFIX = {
  DIVISION: 'div-',
  REGION: 'reg-',
  AREA: 'area-',
  TERRITORY: 'ter-',
};

export interface TerritoryTreeSelectorProps {
  value?: string;
  onChange: (territoryId: string) => void;
  disabled?: boolean;
  error?: boolean;
  helperText?: string;
  required?: boolean;
  label?: string;
  /** When true, shows a "Clear selection" option to allow deselecting */
  allowClear?: boolean;
  /** 'popup' = click to open modal with tree (default), 'inline' = tree shown directly in form */
  variant?: 'popup' | 'inline';
}

const TerritoryTreeSelector: React.FC<TerritoryTreeSelectorProps> = ({
  value = '',
  onChange,
  disabled = false,
  error = false,
  helperText,
  required = false,
  label = 'Territory',
  allowClear = false,
  variant = 'popup',
}) => {
  const { currentOrganizationId } = useAuth();
  const [divisions, setDivisions] = useState<Division[]>([]);
  const [regionsByDivision, setRegionsByDivision] = useState<Record<string, Region[]>>({});
  const [areasByRegion, setAreasByRegion] = useState<Record<string, Area[]>>({});
  const [territoriesByArea, setTerritoriesByArea] = useState<Record<string, Territory[]>>({});
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState<string[]>([]);
  const [selected, setSelected] = useState<string>(value ? `${PREFIX.TERRITORY}${value}` : '');
  const [popupOpen, setPopupOpen] = useState(false);
  const [selectedTerritoryName, setSelectedTerritoryName] = useState<string>('');

  useEffect(() => {
    if (currentOrganizationId) {
      loadDivisions();
    }
  }, [currentOrganizationId]);

  useEffect(() => {
    if (value) {
      setSelected(`${PREFIX.TERRITORY}${value}`);
      pharmaService.getTerritoryById(value).then((t) => setSelectedTerritoryName(t.name || '')).catch(() => setSelectedTerritoryName(''));
    } else {
      setSelected(allowClear ? 'clear' : '');
      setSelectedTerritoryName('');
    }
  }, [value, allowClear]);

  // In edit mode: when value (territoryId) is set, fetch territory and expand path to show selected territory
  useEffect(() => {
    if (!value || divisions.length === 0) return;
    const expandToSelectedTerritory = async () => {
      try {
        const territory = await pharmaService.getTerritoryById(value);
        const { divisionId, regionId, areaId } = territory;
        if (!divisionId || !regionId || !areaId) return;
        // Load hierarchy for the path
        const [regionsData, areasData, territoriesData] = await Promise.all([
          pharmaService.getRegionsByDivision(divisionId),
          pharmaService.getAreasByRegion(regionId),
          pharmaService.getTerritoriesByArea(areaId)
        ]);
        setRegionsByDivision((prev) => ({ ...prev, [divisionId]: regionsData }));
        setAreasByRegion((prev) => ({ ...prev, [regionId]: areasData }));
        setTerritoriesByArea((prev) => ({ ...prev, [areaId]: territoriesData }));
        setExpanded((prev) => {
          const toAdd = [`${PREFIX.DIVISION}${divisionId}`, `${PREFIX.REGION}${regionId}`, `${PREFIX.AREA}${areaId}`];
          const combined = new Set([...prev, ...toAdd]);
          return Array.from(combined);
        });
      } catch (err) {
        console.error('Failed to expand to selected territory:', err);
      }
    };
    expandToSelectedTerritory();
  }, [value, divisions.length]);

  const loadDivisions = async (retryCount = 0) => {
    if (!currentOrganizationId) return;
    const maxRetries = 2;
    try {
      setLoading(true);
      const data = await pharmaService.getActiveDivisions(currentOrganizationId);
      setDivisions(data);
    } catch (err: any) {
      const is503 = err?.response?.status === 503;
      if (is503 && retryCount < maxRetries) {
        setTimeout(() => loadDivisions(retryCount + 1), 1500);
      } else {
        console.error('Failed to load divisions:', err);
      }
    } finally {
      setLoading(false);
    }
  };

  const loadRegions = async (divisionId: string) => {
    if (divisionId in regionsByDivision) return;
    try {
      const data = await pharmaService.getRegionsByDivision(divisionId);
      setRegionsByDivision((prev) => ({ ...prev, [divisionId]: data }));
    } catch (err) {
      console.error('Failed to load regions:', err);
    }
  };

  const loadAreas = async (regionId: string) => {
    if (regionId in areasByRegion) return;
    try {
      const data = await pharmaService.getAreasByRegion(regionId);
      setAreasByRegion((prev) => ({ ...prev, [regionId]: data }));
    } catch (err) {
      console.error('Failed to load areas:', err);
    }
  };

  const loadTerritories = async (areaId: string) => {
    if (areaId in territoriesByArea) return;
    try {
      const data = await pharmaService.getTerritoriesByArea(areaId);
      setTerritoriesByArea((prev) => ({ ...prev, [areaId]: data }));
    } catch (err) {
      console.error('Failed to load territories:', err);
    }
  };

  const handleExpandedItemsChange = (_event: React.SyntheticEvent | null, itemIds: string[]) => {
    setExpanded(itemIds.filter((id) => id !== 'clear' && !id.startsWith('ph-') && !id.startsWith('empty-')));
    itemIds.forEach((itemId) => {
      if (itemId === 'clear' || itemId.startsWith('ph-') || itemId.startsWith('empty-')) return;
      if (itemId.startsWith(PREFIX.DIVISION)) {
        const divisionId = itemId.replace(PREFIX.DIVISION, '');
        loadRegions(divisionId);
      } else if (itemId.startsWith(PREFIX.REGION)) {
        const regionId = itemId.replace(PREFIX.REGION, '');
        loadAreas(regionId);
      } else if (itemId.startsWith(PREFIX.AREA)) {
        const areaId = itemId.replace(PREFIX.AREA, '');
        loadTerritories(areaId);
      }
    });
  };

  const handleSelectedItemsChange = (_event: React.SyntheticEvent | null, itemId: string | null) => {
    if (itemId == null) return;
    if (itemId.startsWith(PREFIX.TERRITORY)) {
      const territoryId = itemId.replace(PREFIX.TERRITORY, '');
      const territory = [...Object.values(territoriesByArea).flat()].find((t) => t.id === territoryId);
      setSelected(itemId);
      setSelectedTerritoryName(territory?.name || '');
      onChange(territoryId);
      if (variant === 'popup') setPopupOpen(false);
    } else if (allowClear && itemId === 'clear') {
      setSelected('');
      setSelectedTerritoryName('');
      onChange('');
      if (variant === 'popup') setPopupOpen(false);
    }
  };

  const renderTreeItemLabel = (icon: React.ReactNode, text: string, isLeaf?: boolean) => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}>
      {icon}
      <Typography variant="body2" sx={{ fontWeight: isLeaf ? 600 : 500 }}>
        {text}
      </Typography>
    </Box>
  );

  const renderTreeContent = (isInline = false) => (
    <Paper
      variant="outlined"
      sx={{
        p: 1.5,
        mt: isInline ? 1.5 : 0,
        maxHeight: 360,
        overflow: 'auto',
        borderColor: error ? 'error.main' : 'divider',
        ...(isInline && {
          '&:hover': { borderColor: error ? 'error.main' : 'primary.main' },
          cursor: disabled ? 'not-allowed' : 'default',
          opacity: disabled ? 0.6 : 1,
        }),
      }}
    >
      <SimpleTreeView
        aria-label="territory tree"
        expandedItems={expanded}
        selectedItems={selected || null}
        onExpandedItemsChange={handleExpandedItemsChange}
        onSelectedItemsChange={handleSelectedItemsChange}
        sx={{ minHeight: 200 }}
      >
        {allowClear && (
          <TreeItem
            itemId="clear"
            label={renderTreeItemLabel(<TerritoryIcon fontSize="small" color="action" />, '-- None --')}
          />
        )}
        {divisions.map((div) => (
          <TreeItem
            key={div.id}
            itemId={`${PREFIX.DIVISION}${div.id}`}
            label={renderTreeItemLabel(<BusinessIcon fontSize="small" color="action" />, div.name)}
          >
            {div.id in regionsByDivision ? (
              (regionsByDivision[div.id] || []).length > 0 ? (
              (regionsByDivision[div.id] || []).map((reg) => (
                <TreeItem
                  key={reg.id}
                  itemId={`${PREFIX.REGION}${reg.id}`}
                  label={renderTreeItemLabel(<MapIcon fontSize="small" color="action" />, reg.name)}
                >
                  {reg.id in areasByRegion ? (
                    (areasByRegion[reg.id] || []).length > 0 ? (
                    (areasByRegion[reg.id] || []).map((area) => (
                      <TreeItem
                        key={area.id}
                        itemId={`${PREFIX.AREA}${area.id}`}
                        label={renderTreeItemLabel(<PlaceIcon fontSize="small" color="action" />, area.name)}
                      >
                        {area.id in territoriesByArea ? (
                          (territoriesByArea[area.id] || []).length > 0 ? (
                          (territoriesByArea[area.id] || []).map((ter) => (
                            <TreeItem
                              key={ter.id}
                              itemId={`${PREFIX.TERRITORY}${ter.id}`}
                              label={renderTreeItemLabel(<TerritoryIcon fontSize="small" color="primary" />, ter.name, true)}
                            />
                          ))
                          ) : (
                            <TreeItem itemId={`empty-area-${area.id}`} label={<Typography variant="body2" color="text.secondary">No territories</Typography>} />
                          )
                        ) : (
                          <TreeItem itemId={`ph-${PREFIX.AREA}${area.id}`} label={<Typography variant="body2" color="text.secondary">Loading...</Typography>} />
                        )}
                      </TreeItem>
                    ))
                    ) : (
                      <TreeItem itemId={`empty-reg-${reg.id}`} label={<Typography variant="body2" color="text.secondary">No areas</Typography>} />
                    )
                  ) : (
                    <TreeItem itemId={`ph-${PREFIX.REGION}${reg.id}`} label={<Typography variant="body2" color="text.secondary">Loading...</Typography>} />
                  )}
                </TreeItem>
              ))
              ) : (
                <TreeItem itemId={`empty-div-${div.id}`} label={<Typography variant="body2" color="text.secondary">No regions</Typography>} />
              )
            ) : (
              <TreeItem itemId={`ph-${PREFIX.DIVISION}${div.id}`} label={<Typography variant="body2" color="text.secondary">Loading...</Typography>} />
            )}
          </TreeItem>
        ))}
      </SimpleTreeView>
    </Paper>
  );

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 2 }}>
        <CircularProgress size={24} />
        <Typography variant="body2" color="text.secondary">Loading hierarchy...</Typography>
      </Box>
    );
  }

  if (variant === 'popup') {
    return (
      <FormControl fullWidth error={error} disabled={disabled} required={required}>
        {label && (
          <InputLabel shrink sx={{ bgcolor: 'background.paper', px: 1 }}>
            {label}
          </InputLabel>
        )}
        <TextField
          fullWidth
          value={value ? selectedTerritoryName : allowClear ? '-- None --' : ''}
          placeholder={!allowClear ? 'Select Territory' : ''}
          onClick={() => !disabled && setPopupOpen(true)}
          readOnly
          required={required}
          error={error}
          sx={{
            mt: 1.5,
            cursor: disabled ? 'default' : 'pointer',
            '& .MuiInputBase-input': { cursor: disabled ? 'default' : 'pointer' },
          }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <TerritoryIcon fontSize="small" color="action" />
              </InputAdornment>
            ),
          }}
        />
        <Dialog open={popupOpen} onClose={() => setPopupOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Select Territory</DialogTitle>
          <DialogContent>{renderTreeContent()}</DialogContent>
          <DialogActions>
            <Button onClick={() => setPopupOpen(false)}>Cancel</Button>
          </DialogActions>
        </Dialog>
        {helperText && <FormHelperText>{helperText}</FormHelperText>}
      </FormControl>
    );
  }

  return (
    <FormControl fullWidth error={error} disabled={disabled} required={required}>
      {label && (
        <InputLabel shrink sx={{ bgcolor: 'background.paper', px: 1 }}>
          {label}
        </InputLabel>
      )}
      {renderTreeContent(true)}
      {helperText && <FormHelperText>{helperText}</FormHelperText>}
    </FormControl>
  );
};

export default TerritoryTreeSelector;
