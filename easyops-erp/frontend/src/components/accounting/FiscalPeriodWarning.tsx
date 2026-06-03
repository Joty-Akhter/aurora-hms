import React from 'react';
import { Alert, Button } from '@mui/material';
import { Link } from 'react-router-dom';

interface FiscalPeriodWarningProps {
  hasOpenPeriods: boolean;
}

const FiscalPeriodWarning: React.FC<FiscalPeriodWarningProps> = ({ hasOpenPeriods }) => {
  if (hasOpenPeriods) {
    return null;
  }

  return (
    <Alert
      severity="warning"
      sx={{ mb: 2 }}
      action={
        <Button component={Link} to="/accounting/fiscal-year-setup" size="small" color="inherit">
          Set up fiscal year
        </Button>
      }
    >
      No open accounting periods. Set up a fiscal year before creating or posting AR/AP documents.
    </Alert>
  );
};

export default FiscalPeriodWarning;
