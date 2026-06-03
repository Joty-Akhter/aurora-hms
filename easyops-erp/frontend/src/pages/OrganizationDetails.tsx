import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Breadcrumbs,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Link,
  Paper,
  Tab,
  Tabs,
  Typography,
  IconButton,
  Divider,
} from '@mui/material';
import {
  Business as BusinessIcon,
  ArrowBack as BackIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import organizationService from '@/services/organizationService';
import { Organization } from '@/types/organization';
import { resolveOrganizationLogoUrl } from '@/utils/organizationLogo';
import DepartmentTab from '@/components/Organization/DepartmentTab';
import LocationTab from '@/components/Organization/LocationTab';
import SettingsTab from '@/components/Organization/SettingsTab';
import MasterDataTab from '@/components/Organization/MasterDataTab';
import InvitationsTab from '@/components/Organization/InvitationsTab';
import ThemeTab from '@/components/Organization/ThemeTab';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

const OrganizationDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [organization, setOrganization] = useState<Organization | null>(null);
  const [loading, setLoading] = useState(true);
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    if (id) {
      fetchOrganization();
    }
  }, [id]);

  const fetchOrganization = async () => {
    try {
      setLoading(true);
      const data = await organizationService.getOrganizationById(id!);
      setOrganization(data);
    } catch (error: any) {
      enqueueSnackbar(error.response?.data?.message || 'Failed to fetch organization', {
        variant: 'error',
      });
      navigate('/organizations');
    } finally {
      setLoading(false);
    }
  };

  if (loading || !organization) {
    return <Typography>Loading...</Typography>;
  }

  const headerLogoUrl = resolveOrganizationLogoUrl(organization.logo);

  return (
    <Box>
      {/* Breadcrumbs */}
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link
          underline="hover"
          color="inherit"
          href="/organizations"
          onClick={(e) => {
            e.preventDefault();
            navigate('/organizations');
          }}
        >
          Organizations
        </Link>
        <Typography color="text.primary">{organization.name}</Typography>
      </Breadcrumbs>

      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <IconButton onClick={() => navigate('/organizations')}>
            <BackIcon />
          </IconButton>
          {headerLogoUrl ? (
            <Box
              component="img"
              src={headerLogoUrl}
              alt=""
              sx={{ maxHeight: 48, maxWidth: 200, objectFit: 'contain' }}
            />
          ) : (
            <BusinessIcon sx={{ fontSize: 40, color: 'primary.main' }} />
          )}
          <Box>
            <Typography variant="h4" component="h1">
              {organization.name}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              {organization.code} • {organization.subscriptionPlan}
            </Typography>
          </Box>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Chip
            label={organization.status}
            color={organization.status === 'ACTIVE' ? 'success' : 'error'}
          />
          <Chip
            label={organization.subscriptionStatus}
            color={organization.subscriptionStatus === 'ACTIVE' ? 'success' : 'warning'}
            variant="outlined"
          />
        </Box>
      </Box>

      {/* Organization Info Card */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" color="textSecondary">Legal Name</Typography>
              <Typography variant="body1">{organization.legalName || '-'}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" color="textSecondary">Email</Typography>
              <Typography variant="body1">{organization.email || '-'}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" color="textSecondary">Phone</Typography>
              <Typography variant="body1">{organization.phone || '-'}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" color="textSecondary">Website</Typography>
              <Typography variant="body1">{organization.website || '-'}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" color="textSecondary">Industry</Typography>
              <Typography variant="body1">{organization.industry || '-'}</Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" color="textSecondary">Currency / Timezone</Typography>
              <Typography variant="body1">{organization.currency} / {organization.timezone}</Typography>
            </Grid>
            {organization.description && (
              <Grid item xs={12}>
                <Typography variant="subtitle2" color="textSecondary">Description</Typography>
                <Typography variant="body1">{organization.description}</Typography>
              </Grid>
            )}
            <Grid item xs={12}>
              <Typography variant="subtitle2" color="textSecondary">Address</Typography>
              <Typography variant="body1">
                {[organization.addressLine1, organization.addressLine2, organization.city, organization.state, organization.postalCode, organization.country]
                  .filter(Boolean)
                  .join(', ') || '—'}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Tabs */}
      <Paper>
        <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
          <Tab label="Departments" />
          <Tab label="Locations" />
          <Tab label="Theme & Branding" />
          <Tab label="Settings" />
          <Tab label="Master Data" />
          <Tab label="Invitations" />
        </Tabs>
        <Divider />
        <TabPanel value={tabValue} index={0}>
          <DepartmentTab organizationId={organization.id} />
        </TabPanel>
        <TabPanel value={tabValue} index={1}>
          <LocationTab organizationId={organization.id} />
        </TabPanel>
        <TabPanel value={tabValue} index={2}>
          <ThemeTab organization={organization} onUpdated={fetchOrganization} />
        </TabPanel>
        <TabPanel value={tabValue} index={3}>
          <SettingsTab organizationId={organization.id} />
        </TabPanel>
        <TabPanel value={tabValue} index={4}>
          <MasterDataTab organizationId={organization.id} />
        </TabPanel>
        <TabPanel value={tabValue} index={5}>
          <InvitationsTab organizationId={organization.id} />
        </TabPanel>
      </Paper>
    </Box>
  );
};

export default OrganizationDetails;

