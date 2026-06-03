import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';

const WorkCenterList: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Work Centers</Typography>
        <Button variant="contained" startIcon={<AddIcon />}>
          Create Work Center
        </Button>
      </Box>
      <Typography color="textSecondary">Work center management will be displayed here.</Typography>
    </Box>
  );
};

export default WorkCenterList;

