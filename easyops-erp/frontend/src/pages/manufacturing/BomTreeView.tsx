import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material';
import {
  ArrowBack,
  Edit,
  Delete,
  Add,
  Calculate,
} from '@mui/icons-material';
import { SimpleTreeView, TreeItem } from '@mui/x-tree-view';
import { bomApi, bomLineApi, bomExplosionApi, BomHeader, BomLine } from '@services/manufacturingService';
import { useAuth } from '@contexts/AuthContext';

const BomTreeView: React.FC = () => {
  const { bomId } = useParams<{ bomId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [bom, setBom] = useState<BomHeader | null>(null);
  const [bomLines, setBomLines] = useState<BomLine[]>([]);
  const [explosion, setExplosion] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [addLineDialog, setAddLineDialog] = useState(false);

  useEffect(() => {
    if (bomId) {
      loadBomData();
    }
  }, [bomId]);

  const loadBomData = async () => {
    try {
      const [bomResponse, linesResponse, explosionResponse] = await Promise.all([
        bomApi.getBomById(bomId!),
        bomLineApi.getBomLines(bomId!),
        bomApi.bomExplosionApi.explodeBom(bomId!, 1),
      ]);
      setBom(bomResponse.data);
      setBomLines(linesResponse.data);
      setExplosion(explosionResponse.data);
    } catch (error) {
      console.error('Failed to load BOM:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRecalculateCosts = async () => {
    try {
      await bomApi.recalculateCosts(bomId!);
      loadBomData();
    } catch (error) {
      console.error('Failed to recalculate costs:', error);
    }
  };

  const handleApproveBom = async () => {
    try {
      await bomApi.approveBom(bomId!, user!.id);
      loadBomData();
    } catch (error) {
      console.error('Failed to approve BOM:', error);
    }
  };

  const renderTreeItems = (lines: BomLine[], parentId: string | null = null, level: number = 0) => {
    const children = lines.filter(line => 
      (parentId === null && !line.parentLine) || 
      (line.parentLine && line.parentLine.bomLineId === parentId)
    );

    return children.map((line) => (
      <TreeItem
        key={line.bomLineId}
        itemId={line.bomLineId!}
        label={
          <Box sx={{ display: 'flex', alignItems: 'center', py: 1 }}>
            <Typography variant="body2" sx={{ fontWeight: 'medium', mr: 2 }}>
              {line.componentCode} - {line.componentName}
            </Typography>
            <Chip label={`Qty: ${line.quantityPerUnit}`} size="small" sx={{ mr: 1 }} />
            <Chip label={line.componentType} size="small" color="primary" sx={{ mr: 1 }} />
            {line.unitCost && (
              <Chip label={`$${line.unitCost.toFixed(2)}`} size="small" color="success" />
            )}
          </Box>
        }
      >
        {renderTreeItems(lines, line.bomLineId!, level + 1)}
      </TreeItem>
    ));
  };

  if (loading) return <Typography>Loading...</Typography>;
  if (!bom) return <Typography>BOM not found</Typography>;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <IconButton onClick={() => navigate('/manufacturing/boms')} sx={{ mr: 2 }}>
            <ArrowBack />
          </IconButton>
          <Box>
            <Typography variant="h4">{bom.bomNumber}</Typography>
            <Typography variant="subtitle1" color="textSecondary">
              {bom.productCode} - {bom.productName}
            </Typography>
          </Box>
        </Box>
        <Box>
          <Chip label={bom.status} color={bom.status === 'ACTIVE' ? 'success' : 'default'} sx={{ mr: 1 }} />
          <Chip label={`Version ${bom.versionNumber}`} sx={{ mr: 1 }} />
          <Button onClick={handleRecalculateCosts} startIcon={<Calculate />} sx={{ mr: 1 }}>
            Recalculate Costs
          </Button>
          {bom.status === 'DRAFT' && (
            <Button variant="contained" onClick={handleApproveBom}>
              Approve BOM
            </Button>
          )}
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* BOM Info Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>BOM Information</Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">BOM Type</Typography>
                <Typography variant="body1">{bom.bomType}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Base Quantity</Typography>
                <Typography variant="body1">{bom.baseQuantity} {bom.uom}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Total Components</Typography>
                <Typography variant="body1">{bomLines.length}</Typography>
              </Box>
            </CardContent>
          </Card>

          {/* Cost Breakdown Card */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Cost Breakdown</Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Material Cost</Typography>
                <Typography variant="h6">${bom.materialCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Labor Cost</Typography>
                <Typography variant="h6">${bom.laborCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="textSecondary">Overhead Cost</Typography>
                <Typography variant="h6">${bom.overheadCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
              <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: 'divider' }}>
                <Typography variant="body2" color="textSecondary">Total Cost</Typography>
                <Typography variant="h5" color="primary">${bom.totalCost?.toFixed(2) || '0.00'}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* BOM Tree View */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">BOM Structure</Typography>
              <Button startIcon={<Add />} variant="outlined" onClick={() => setAddLineDialog(true)}>
                Add Component
              </Button>
            </Box>
            <SimpleTreeView defaultExpandedItems={bomLines.filter(l => !l.parentLine).map(l => l.bomLineId!)}>
              {renderTreeItems(bomLines)}
            </SimpleTreeView>
          </Paper>

          {/* Explosion View */}
          {explosion && (
            <Paper sx={{ p: 2, mt: 2 }}>
              <Typography variant="h6" gutterBottom>Material Requirements (Explosion)</Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Level</TableCell>
                      <TableCell>Component</TableCell>
                      <TableCell align="right">Qty Per Unit</TableCell>
                      <TableCell align="right">Required Qty</TableCell>
                      <TableCell>UOM</TableCell>
                      <TableCell align="right">Unit Cost</TableCell>
                      <TableCell align="right">Extended Cost</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {explosion.components?.map((comp: any, index: number) => (
                      <TableRow key={index}>
                        <TableCell>{comp.level}</TableCell>
                        <TableCell>
                          {'  '.repeat(comp.level - 1)}
                          {comp.componentCode} - {comp.componentName}
                        </TableCell>
                        <TableCell align="right">{comp.quantityPerUnit?.toFixed(4)}</TableCell>
                        <TableCell align="right">{comp.requiredQuantity?.toFixed(4)}</TableCell>
                        <TableCell>{comp.uom}</TableCell>
                        <TableCell align="right">${comp.unitCost?.toFixed(2) || '0.00'}</TableCell>
                        <TableCell align="right">${comp.extendedCost?.toFixed(2) || '0.00'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Box sx={{ mt: 2, textAlign: 'right' }}>
                <Typography variant="h6">
                  Total Material Cost: ${explosion.components?.reduce((sum: number, c: any) => 
                    sum + (c.extendedCost || 0), 0).toFixed(2) || '0.00'}
                </Typography>
              </Box>
            </Paper>
          )}
        </Grid>
      </Grid>
    </Box>
  );
};

export default BomTreeView;

