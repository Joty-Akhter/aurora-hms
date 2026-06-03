import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import accountingService from '../../services/accountingService';
import { formatCurrency } from '../../utils/currencyFormatter';

const OutstandingDocuments: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [tab, setTab] = useState(0);
  const [outstandingInvoices, setOutstandingInvoices] = useState<any[]>([]);
  const [overdueInvoices, setOverdueInvoices] = useState<any[]>([]);
  const [outstandingBills, setOutstandingBills] = useState<any[]>([]);
  const [overdueBills, setOverdueBills] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const organizationId = currentOrganizationId || '';

  useEffect(() => {
    if (!organizationId) return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        if (tab === 0) {
          const [outstanding, overdue] = await Promise.all([
            accountingService.getOutstandingInvoices(organizationId),
            accountingService.getOverdueInvoices(organizationId),
          ]);
          setOutstandingInvoices(outstanding);
          setOverdueInvoices(overdue);
        } else {
          const [outstanding, overdue] = await Promise.all([
            accountingService.getOutstandingBills(organizationId),
            accountingService.getOverdueBills(organizationId),
          ]);
          setOutstandingBills(outstanding);
          setOverdueBills(overdue);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load documents');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [organizationId, tab]);

  const renderInvoiceTable = (rows: any[], title: string) => (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>{title}</Typography>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Invoice #</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell align="right">Balance Due</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length === 0 ? (
                <TableRow><TableCell colSpan={5}>None</TableCell></TableRow>
              ) : (
                rows.map((inv) => (
                  <TableRow key={inv.id}>
                    <TableCell>{inv.invoiceNumber}</TableCell>
                    <TableCell>{inv.customer?.customerName ?? '—'}</TableCell>
                    <TableCell>{inv.dueDate}</TableCell>
                    <TableCell align="right">{formatCurrency(inv.balanceDue ?? 0)}</TableCell>
                    <TableCell>{inv.paymentStatus}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>
    </Card>
  );

  const renderBillTable = (rows: any[], title: string) => (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>{title}</Typography>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Bill #</TableCell>
                <TableCell>Vendor</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell align="right">Balance Due</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length === 0 ? (
                <TableRow><TableCell colSpan={5}>None</TableCell></TableRow>
              ) : (
                rows.map((bill) => (
                  <TableRow key={bill.id}>
                    <TableCell>{bill.billNumber}</TableCell>
                    <TableCell>{bill.vendor?.vendorName ?? '—'}</TableCell>
                    <TableCell>{bill.dueDate}</TableCell>
                    <TableCell align="right">{formatCurrency(bill.balanceDue ?? 0)}</TableCell>
                    <TableCell>{bill.paymentStatus}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>
    </Card>
  );

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>Outstanding & Overdue</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {loading && <Alert severity="info" sx={{ mb: 2 }}>Loading...</Alert>}

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Receivables" />
        <Tab label="Payables" />
      </Tabs>

      {tab === 0 ? (
        <>
          {renderInvoiceTable(outstandingInvoices, 'Outstanding Invoices')}
          {renderInvoiceTable(overdueInvoices, 'Overdue Invoices')}
        </>
      ) : (
        <>
          {renderBillTable(outstandingBills, 'Outstanding Bills')}
          {renderBillTable(overdueBills, 'Overdue Bills')}
        </>
      )}
    </Box>
  );
};

export default OutstandingDocuments;
