import React, { useState, useEffect } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  FormControlLabel,
  Grid,
  Radio,
  RadioGroup,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { Palette as PaletteIcon, Preview as PreviewIcon, Restore as RestoreIcon, Save as SaveIcon } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import organizationService from '@/services/organizationService';
import { Organization } from '@/types/organization';
import { useOrgTheme } from '@contexts/ThemeContext';
import { OrgTheme, DEFAULT_THEME } from '@services/authService';

interface ThemeTabProps {
  organization: Organization;
  onUpdated: () => void;
}

const COLOR_FIELDS: { key: keyof OrgTheme; label: string; description: string }[] = [
  { key: 'primaryColor', label: 'Primary Color', description: 'Main brand color used in buttons, links, and highlights' },
  { key: 'secondaryColor', label: 'Secondary Color', description: 'Supporting color for secondary actions and accents' },
  { key: 'accentColor', label: 'Accent Color', description: 'Highlight color for active states and special elements' },
  { key: 'sidebarColor', label: 'Sidebar Background', description: 'Navigation sidebar background color' },
  { key: 'sidebarTextColor', label: 'Sidebar Text / Icons', description: 'Navigation sidebar text and icon color' },
];

const ColorSwatch: React.FC<{ color: string; onChange: (color: string) => void }> = ({
  color,
  onChange,
}) => (
  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
    <Tooltip title="Click to pick a color">
      <Box
        component="label"
        sx={{
          width: 40,
          height: 40,
          borderRadius: 2,
          bgcolor: color,
          border: '2px solid',
          borderColor: 'divider',
          cursor: 'pointer',
          flexShrink: 0,
          overflow: 'hidden',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <input
          type="color"
          value={color}
          onChange={(e) => onChange(e.target.value)}
          style={{ opacity: 0, position: 'absolute', width: 0, height: 0 }}
        />
      </Box>
    </Tooltip>
    <Typography variant="body2" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>
      {color.toUpperCase()}
    </Typography>
  </Box>
);

const ThemeTab: React.FC<ThemeTabProps> = ({ organization, onUpdated }) => {
  const { enqueueSnackbar } = useSnackbar();
  const { applyTheme } = useOrgTheme();
  const [saving, setSaving] = useState(false);

  const [draft, setDraft] = useState<OrgTheme>({
    mode: (organization.themeMode as 'light' | 'dark') || DEFAULT_THEME.mode,
    primaryColor: organization.themePrimaryColor || DEFAULT_THEME.primaryColor,
    secondaryColor: organization.themeSecondaryColor || DEFAULT_THEME.secondaryColor,
    accentColor: organization.themeAccentColor || DEFAULT_THEME.accentColor,
    sidebarColor: organization.themeSidebarColor || DEFAULT_THEME.sidebarColor,
    sidebarTextColor: organization.themeSidebarTextColor || DEFAULT_THEME.sidebarTextColor,
  });

  useEffect(() => {
    setDraft({
      mode: (organization.themeMode as 'light' | 'dark') || DEFAULT_THEME.mode,
      primaryColor: organization.themePrimaryColor || DEFAULT_THEME.primaryColor,
      secondaryColor: organization.themeSecondaryColor || DEFAULT_THEME.secondaryColor,
      accentColor: organization.themeAccentColor || DEFAULT_THEME.accentColor,
      sidebarColor: organization.themeSidebarColor || DEFAULT_THEME.sidebarColor,
      sidebarTextColor: organization.themeSidebarTextColor || DEFAULT_THEME.sidebarTextColor,
    });
  }, [organization]);

  const setColor = (key: keyof OrgTheme, value: string) => setDraft((d) => ({ ...d, [key]: value }));

  const handlePreview = () => {
    applyTheme(draft);
    enqueueSnackbar('Theme preview applied — save to persist', { variant: 'info' });
  };

  const handleReset = () => {
    setDraft(DEFAULT_THEME);
    applyTheme(DEFAULT_THEME);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await organizationService.updateTheme(organization.id, {
        themeMode: draft.mode,
        themePrimaryColor: draft.primaryColor,
        themeSecondaryColor: draft.secondaryColor,
        themeAccentColor: draft.accentColor,
        themeSidebarColor: draft.sidebarColor,
        themeSidebarTextColor: draft.sidebarTextColor,
      });
      applyTheme(draft);
      // Update localStorage so the theme persists across refreshes for the current session
      localStorage.setItem('currentOrganizationTheme', JSON.stringify(draft));
      enqueueSnackbar('Theme saved successfully', { variant: 'success' });
      onUpdated();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to save theme', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <PaletteIcon color="primary" />
        <Typography variant="h6">Theme & Branding</Typography>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        Theme colors are applied when users log in or switch to this organization. Use <strong>Preview</strong> to see changes instantly without saving.
      </Alert>

      <Grid container spacing={3}>
        {/* Mode */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>Color Mode</Typography>
              <RadioGroup row value={draft.mode} onChange={(e) => setDraft((d) => ({ ...d, mode: e.target.value as 'light' | 'dark' }))}>
                <FormControlLabel value="light" control={<Radio />} label="Light" />
                <FormControlLabel value="dark" control={<Radio />} label="Dark" />
              </RadioGroup>
            </CardContent>
          </Card>
        </Grid>

        {/* Live preview strip */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>Preview Strip</Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {COLOR_FIELDS.map(({ key, label }) => (
                  <Tooltip key={key} title={label}>
                    <Box sx={{ width: 36, height: 36, borderRadius: 1, bgcolor: draft[key], border: '1px solid', borderColor: 'divider' }} />
                  </Tooltip>
                ))}
              </Box>
              <Box sx={{ mt: 2, p: 1.5, borderRadius: 2, bgcolor: draft.sidebarColor, display: 'flex', alignItems: 'center', gap: 1 }}>
                <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: draft.primaryColor }} />
                <Typography variant="caption" sx={{ color: draft.sidebarTextColor }}>Sidebar preview</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Color pickers */}
        {COLOR_FIELDS.map(({ key, label, description }) => (
          <Grid item xs={12} sm={6} key={key}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="subtitle2" fontWeight={600}>{label}</Typography>
                <Typography variant="caption" color="text.secondary" display="block" mb={2}>{description}</Typography>
                <ColorSwatch
                  color={draft[key] as string}
                  onChange={(v) => setColor(key, v)}
                />
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Divider sx={{ my: 3 }} />

      <Stack direction="row" spacing={2}>
        <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave} disabled={saving}>
          {saving ? 'Saving…' : 'Save Theme'}
        </Button>
        <Button variant="outlined" startIcon={<PreviewIcon />} onClick={handlePreview}>
          Preview
        </Button>
        <Button variant="text" startIcon={<RestoreIcon />} onClick={handleReset} color="warning">
          Reset to Defaults
        </Button>
      </Stack>
    </Box>
  );
};

export default ThemeTab;
