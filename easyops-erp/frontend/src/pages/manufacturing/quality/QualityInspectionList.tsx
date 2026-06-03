import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';

const QualityInspectionList: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Quality Inspections</Typography>
        <Button variant="contained" startIcon={<AddIcon />}>
          Create Inspection
        </Button>
      </Box>
      <Typography color="textSecondary">Quality inspection list will be displayed here.</Typography>
    </Box>
  );
};

export default QualityInspectionList;

