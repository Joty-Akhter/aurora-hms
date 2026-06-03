import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Send as SendIcon,
  CheckCircle as AcceptedIcon,
  HourglassEmpty as PendingIcon,
  Cancel as CancelledIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import organizationService from '@/services/organizationService';
import { Invitation } from '@/types/organization';

interface InvitationsTabProps {
  organizationId: string;
}

const InvitationsTab: React.FC<InvitationsTabProps> = ({ organizationId }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    role: 'MEMBER',
  });

  useEffect(() => {
    fetchInvitations();
  }, [organizationId]);

  const fetchInvitations = async () => {
    try {
      const data = await organizationService.getInvitations(organizationId);
      setInvitations(data);
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to fetch invitations', {
        variant: 'error',
      });
    }
  };

  const handleOpenDialog = () => {
    setFormData({
      email: '',
      role: 'MEMBER',
    });
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
  };

  const handleSubmit = async () => {
    try {
      await organizationService.sendInvitation(organizationId, formData.email, formData.role);
      enqueueSnackbar('Invitation sent successfully', { variant: 'success' });
      handleCloseDialog();
      fetchInvitations();
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to send invitation', {
        variant: 'error',
      });
    }
  };

  const handleCancel = async (id: string) => {
    if (window.confirm('Are you sure you want to cancel this invitation?')) {
      try {
        await organizationService.cancelInvitation(id);
        enqueueSnackbar('Invitation cancelled successfully', { variant: 'success' });
        fetchInvitations();
      } catch (error: any) {
        enqueueSnackbar(error.response?.data?.message || 'Cancel failed', { variant: 'error' });
      }
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACCEPTED':
        return <AcceptedIcon />;
      case 'PENDING':
        return <PendingIcon />;
      case 'CANCELLED':
        return <CancelledIcon />;
      default:
        return <PendingIcon />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACCEPTED':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'CANCELLED':
      case 'EXPIRED':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6">Invitations</Typography>
        <Button variant="contained" startIcon={<SendIcon />} onClick={handleOpenDialog}>
          Send Invitation
        </Button>
      </Box>

      {invitations.length > 0 ? (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Sent</TableCell>
                <TableCell>Expires</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {invitations.map((invitation) => (
                <TableRow key={invitation.id} hover>
                  <TableCell>{invitation.email}</TableCell>
                  <TableCell>
                    <Chip label={invitation.role} size="small" />
                  </TableCell>
                  <TableCell>
                    <Chip
                      icon={getStatusIcon(invitation.status)}
                      label={invitation.status}
                      size="small"
                      color={getStatusColor(invitation.status) as any}
                    />
                  </TableCell>
                  <TableCell>
                    {new Date(invitation.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    {new Date(invitation.expiresAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell align="right">
                    {invitation.status === 'PENDING' && (
                      <IconButton
                        size="small"
                        onClick={() => handleCancel(invitation.id)}
                        color="error"
                      >
                        <DeleteIcon />
                      </IconButton>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      ) : (
        <Card>
          <CardContent>
            <Typography variant="body2" color="textSecondary" align="center">
              No invitations found. Send one to invite users to your organization!
            </Typography>
          </CardContent>
        </Card>
      )}

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Send Invitation</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Email *"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                helperText="The user will receive an invitation email"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                select
                label="Role *"
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
              >
                <MenuItem value="OWNER">Owner</MenuItem>
                <MenuItem value="ADMIN">Admin</MenuItem>
                <MenuItem value="MANAGER">Manager</MenuItem>
                <MenuItem value="MEMBER">Member</MenuItem>
                <MenuItem value="GUEST">Guest</MenuItem>
              </TextField>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={handleSubmit}
            variant="contained"
            disabled={!formData.email}
            startIcon={<SendIcon />}
          >
            Send Invitation
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InvitationsTab;

