import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';

const MaintenanceList: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Equipment Maintenance</Typography>
        <Button variant="contained" startIcon={<AddIcon />}>
          Schedule Maintenance
        </Button>
      </Box>
      <Typography color="textSecondary">Maintenance scheduling will be displayed here.</Typography>
    </Box>
  );
};

export default MaintenanceList;

