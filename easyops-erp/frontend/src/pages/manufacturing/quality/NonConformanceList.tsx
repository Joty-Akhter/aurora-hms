import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';

const NonConformanceList: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Non-Conformances</Typography>
        <Button variant="contained" startIcon={<AddIcon />}>
          Create NC
        </Button>
      </Box>
      <Typography color="textSecondary">Non-conformance tracking will be displayed here.</Typography>
    </Box>
  );
};

export default NonConformanceList;

