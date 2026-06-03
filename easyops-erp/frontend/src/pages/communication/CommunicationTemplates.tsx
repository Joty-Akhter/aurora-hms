import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add, Delete, Edit, Preview } from '@mui/icons-material';
import communicationService, {
  CommunicationTemplate,
  CommunicationTemplateCreateRequest,
  TemplatePreviewResponse,
  TemplateTestSendResponse,
} from '@services/communicationService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';

const DEFAULT_VARIABLES = '{\n  "patientName": "John Doe",\n  "appointmentDate": "2026-05-01"\n}';
const DEFAULT_VARIABLES_SCHEMA = '{\n  "patientName": "string",\n  "appointmentDate": "string"\n}';

const emptyForm: CommunicationTemplateCreateRequest = {
  templateKey: '',
  channel: 'SMS',
  locale: 'en',
  version: 1,
  status: 'DRAFT',
  subjectTemplate: '',
  bodyTemplate: '',
  variablesSchema: DEFAULT_VARIABLES_SCHEMA,
};

const statusChipColor = (status: string): 'default' | 'success' | 'warning' => {
  if (status === 'ACTIVE') return 'success';
  if (status === 'DRAFT') return 'warning';
  return 'default';
};

const CommunicationTemplates: React.FC = () => {
  const [templates, setTemplates] = useState<CommunicationTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [editorOpen, setEditorOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<CommunicationTemplateCreateRequest>(emptyForm);

  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewTemplateId, setPreviewTemplateId] = useState('');
  const [recipient, setRecipient] = useState('');
  const [variablesJson, setVariablesJson] = useState(DEFAULT_VARIABLES);
  const [preview, setPreview] = useState<TemplatePreviewResponse | null>(null);
  const [testSendResult, setTestSendResult] = useState<TemplateTestSendResponse | null>(null);

  const loadTemplates = useCallback(async () => {
    try {
      setLoading(true);
      const page = await communicationService.listTemplates();
      setTemplates(page.content);
      setError(null);
    } catch {
      setError('Failed to load message templates.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTemplates();
  }, [loadTemplates]);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setEditorOpen(true);
    setActionError(null);
  };

  const openEdit = (template: CommunicationTemplate) => {
    setEditingId(template.id);
    setForm({
      templateKey: template.templateKey,
      channel: template.channel,
      locale: template.locale,
      version: template.version,
      status: template.status,
      subjectTemplate: template.subjectTemplate ?? '',
      bodyTemplate: template.bodyTemplate,
      variablesSchema: template.variablesSchema,
    });
    setEditorOpen(true);
    setActionError(null);
  };

  const openPreview = (template: CommunicationTemplate) => {
    setPreviewTemplateId(template.id);
    setPreview(null);
    setTestSendResult(null);
    setPreviewOpen(true);
    setActionError(null);
  };

  const handleSave = async () => {
    try {
      setActionError(null);
      if (editingId) {
        await communicationService.updateTemplate(editingId, form);
      } else {
        await communicationService.createTemplate(form);
      }
      setEditorOpen(false);
      await loadTemplates();
    } catch (err: unknown) {
      setActionError(ehrApiErrorMessage(err, 'Failed to save template.'));
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this template?')) return;
    try {
      setActionError(null);
      await communicationService.deleteTemplate(id);
      await loadTemplates();
    } catch (err: unknown) {
      setActionError(ehrApiErrorMessage(err, 'Failed to delete template.'));
    }
  };

  const parseVariables = () => {
    try {
      return JSON.parse(variablesJson) as Record<string, unknown>;
    } catch {
      throw new Error('Variables must be valid JSON.');
    }
  };

  const handlePreview = async () => {
    try {
      setActionError(null);
      setPreview(null);
      const data = await communicationService.previewTemplate({
        templateId: previewTemplateId,
        variables: parseVariables(),
      });
      setPreview(data);
    } catch (err: unknown) {
      setActionError(ehrApiErrorMessage(err, 'Preview failed.'));
    }
  };

  const handleTestSend = async () => {
    try {
      setActionError(null);
      setTestSendResult(null);
      const data = await communicationService.testSendTemplate({
        templateId: previewTemplateId,
        recipient,
        variables: parseVariables(),
      });
      setTestSendResult(data);
    } catch (err: unknown) {
      setActionError(ehrApiErrorMessage(err, 'Test send failed.'));
    }
  };

  const selectedPreviewTemplate = useMemo(
    () => templates.find((t) => t.id === previewTemplateId) ?? null,
    [previewTemplateId, templates],
  );

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 240 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">Message Templates</Typography>
          <Typography variant="body2" color="text.secondary">
            Manage SMS and email templates used for appointment and billing notifications.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={openCreate} sx={{ mt: { xs: 2, sm: 0 } }}>
          New Template
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {actionError && <Alert severity="error" sx={{ mb: 2 }}>{actionError}</Alert>}

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Template Key</TableCell>
              <TableCell>Channel</TableCell>
              <TableCell>Locale</TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Updated</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {templates.map((template) => (
              <TableRow key={template.id} hover>
                <TableCell>{template.templateKey}</TableCell>
                <TableCell>{template.channel}</TableCell>
                <TableCell>{template.locale}</TableCell>
                <TableCell>v{template.version}</TableCell>
                <TableCell>
                  <Chip label={template.status} size="small" color={statusChipColor(template.status)} />
                </TableCell>
                <TableCell>{new Date(template.updatedAt).toLocaleString()}</TableCell>
                <TableCell align="right">
                  <IconButton size="small" title="Preview & test" onClick={() => openPreview(template)}>
                    <Preview fontSize="small" />
                  </IconButton>
                  <IconButton size="small" title="Edit" onClick={() => openEdit(template)}>
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" title="Delete" onClick={() => void handleDelete(template.id)}>
                    <Delete fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {!templates.length && (
              <TableRow>
                <TableCell colSpan={7}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                    No templates yet. Create one to start sending notifications.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={editorOpen} onClose={() => setEditorOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editingId ? 'Edit Template' : 'New Template'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Template Key"
              value={form.templateKey}
              onChange={(e) => setForm({ ...form, templateKey: e.target.value })}
              helperText="e.g. appointment.confirmed.sms"
              fullWidth
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                select
                label="Channel"
                value={form.channel}
                onChange={(e) => setForm({ ...form, channel: e.target.value as 'SMS' | 'EMAIL' })}
                fullWidth
              >
                <MenuItem value="SMS">SMS</MenuItem>
                <MenuItem value="EMAIL">EMAIL</MenuItem>
              </TextField>
              <TextField
                label="Locale"
                value={form.locale}
                onChange={(e) => setForm({ ...form, locale: e.target.value })}
                fullWidth
              />
              <TextField
                label="Version"
                type="number"
                value={form.version}
                onChange={(e) => setForm({ ...form, version: Number(e.target.value) })}
                fullWidth
              />
              <TextField
                select
                label="Status"
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value as 'DRAFT' | 'ACTIVE' | 'ARCHIVED' })}
                fullWidth
              >
                <MenuItem value="DRAFT">DRAFT</MenuItem>
                <MenuItem value="ACTIVE">ACTIVE</MenuItem>
                <MenuItem value="ARCHIVED">ARCHIVED</MenuItem>
              </TextField>
            </Stack>
            {form.channel === 'EMAIL' && (
              <TextField
                label="Subject Template"
                value={form.subjectTemplate ?? ''}
                onChange={(e) => setForm({ ...form, subjectTemplate: e.target.value })}
                fullWidth
              />
            )}
            <TextField
              label="Body Template"
              value={form.bodyTemplate}
              onChange={(e) => setForm({ ...form, bodyTemplate: e.target.value })}
              multiline
              minRows={6}
              fullWidth
            />
            <TextField
              label="Variables Schema (JSON)"
              value={form.variablesSchema}
              onChange={(e) => setForm({ ...form, variablesSchema: e.target.value })}
              multiline
              minRows={4}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditorOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void handleSave()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Preview & Test Send</DialogTitle>
        <DialogContent>
          {selectedPreviewTemplate && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {selectedPreviewTemplate.templateKey} · {selectedPreviewTemplate.channel} · v{selectedPreviewTemplate.version}
            </Typography>
          )}
          <Stack spacing={2}>
            <TextField
              label="Recipient (phone or email)"
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
              fullWidth
            />
            <TextField
              label="Variables JSON"
              value={variablesJson}
              onChange={(e) => setVariablesJson(e.target.value)}
              multiline
              minRows={6}
              fullWidth
            />
            <Stack direction="row" spacing={1}>
              <Button variant="outlined" onClick={() => void handlePreview()}>
                Preview
              </Button>
              <Button variant="contained" onClick={() => void handleTestSend()} disabled={!recipient.trim()}>
                Test Send
              </Button>
            </Stack>
            {preview && (
              <Box sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
                <Typography variant="subtitle2">Subject</Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>{preview.renderedSubject ?? '—'}</Typography>
                <Typography variant="subtitle2">Body</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{preview.renderedBody}</Typography>
              </Box>
            )}
            {testSendResult && (
              <Stack direction="row" spacing={1} flexWrap="wrap">
                <Chip label={`Channel: ${testSendResult.channel}`} />
                <Chip label={`Provider: ${testSendResult.provider}`} />
                <Chip color="success" label={`Status: ${testSendResult.status}`} />
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreviewOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CommunicationTemplates;
