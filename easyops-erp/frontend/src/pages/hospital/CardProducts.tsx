import React, { useCallback, useEffect, useState } from "react";
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Edit as EditIcon,
  Visibility as ViewIcon,
} from "@mui/icons-material";
import { useSnackbar } from "notistack";
import hospitalCardManagementService, {
  CardProductResponse,
  CreateCardProductRequest,
  LimitProfileResponse,
  PagedResponse,
  UpdateCardProductRequest,
} from "../../services/hospitalCardManagementService";
import "./Hospital.css";

const MEDIUM_TYPES = ["RFID", "QR", "PHYSICAL", "VIRTUAL"];
const STATUS_OPTIONS = ["ACTIVE", "INACTIVE"];
const USAGE_DOMAIN_OPTIONS = ["HOSPITAL", "CANTEEN", "BOTH", "OTHERS"];

const CardProductsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  const [loading, setLoading] = useState<boolean>(false);
  const [products, setProducts] = useState<CardProductResponse[]>([]);
  const [limitProfiles, setLimitProfiles] = useState<LimitProfileResponse[]>(
    [],
  );
  const [page, setPage] = useState<number>(0);
  const [size] = useState<number>(20);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [codeFilter, setCodeFilter] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<string>("");

  const [createOpen, setCreateOpen] = useState<boolean>(false);
  const [viewOpen, setViewOpen] = useState<boolean>(false);
  const [editOpen, setEditOpen] = useState<boolean>(false);
  const [selectedProduct, setSelectedProduct] =
    useState<CardProductResponse | null>(null);

  const [newProduct, setNewProduct] = useState<CreateCardProductRequest>({
    code: "",
    name: "",
    description: "",
    mediumType: "PHYSICAL",
    usageDomains: "",
    status: "ACTIVE",
  });
  const [editProduct, setEditProduct] = useState<UpdateCardProductRequest>({});

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      const params: {
        page: number;
        size: number;
        code?: string;
        status?: string;
      } = {
        page,
        size,
      };
      if (codeFilter.trim()) params.code = codeFilter.trim();
      if (statusFilter) params.status = statusFilter;
      const response: PagedResponse<CardProductResponse> =
        await hospitalCardManagementService.getCardProducts(params);
      setProducts(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error("Failed to load card products", err);
      enqueueSnackbar("Failed to load card products", { variant: "error" });
    } finally {
      setLoading(false);
    }
  }, [page, size, codeFilter, statusFilter, enqueueSnackbar]);

  const loadLimitProfiles = useCallback(async () => {
    try {
      const response = await hospitalCardManagementService.getLimitProfiles({
        page: 0,
        size: 200,
      });
      setLimitProfiles(response.content);
    } catch (err) {
      console.error("Failed to load limit profiles", err);
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  useEffect(() => {
    loadLimitProfiles();
  }, [loadLimitProfiles]);

  const handleRefresh = () => {
    setPage(0);
    loadProducts();
  };

  const handleApplyFilters = () => {
    setPage(0);
    loadProducts();
  };

  const handleCreateOpen = () => {
    setNewProduct({
      code: "",
      name: "",
      description: "",
      mediumType: "PHYSICAL",
      usageDomains: "",
      status: "ACTIVE",
    });
    setCreateOpen(true);
  };

  const handleCreateSubmit = async () => {
    if (!newProduct.code?.trim() || !newProduct.name?.trim()) {
      enqueueSnackbar("Code and name are required", { variant: "warning" });
      return;
    }
    try {
      setLoading(true);
      await hospitalCardManagementService.createCardProduct(newProduct);
      enqueueSnackbar("Card product created", { variant: "success" });
      setCreateOpen(false);
      loadProducts();
    } catch (err: unknown) {
      console.error("Failed to create card product", err);
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to create card product";
      enqueueSnackbar(String(msg), { variant: "error" });
    } finally {
      setLoading(false);
    }
  };

  const handleView = (row: CardProductResponse) => {
    setSelectedProduct(row);
    setViewOpen(true);
  };

  const handleEditOpen = (row: CardProductResponse) => {
    setSelectedProduct(row);
    setEditProduct({
      name: row.name,
      description: row.description ?? "",
      mediumType: row.mediumType,
      usageDomains: row.usageDomains ?? "",
      defaultLimitProfileId: row.defaultLimitProfileId ?? undefined,
      validityStartDate: row.validityStartDate ?? undefined,
      validityEndDate: row.validityEndDate ?? undefined,
      status: row.status,
    });
    setEditOpen(true);
  };

  const handleEditSubmit = async () => {
    if (!selectedProduct) return;
    try {
      setLoading(true);
      await hospitalCardManagementService.updateCardProduct(
        selectedProduct.id,
        editProduct,
      );
      enqueueSnackbar("Card product updated", { variant: "success" });
      setEditOpen(false);
      setSelectedProduct(null);
      loadProducts();
    } catch (err: unknown) {
      console.error("Failed to update card product", err);
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to update card product";
      enqueueSnackbar(String(msg), { variant: "error" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box className="hospital-page">
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mb={3}
      >
        <Typography variant="h5" className="hospital-page-title">
          Cards – Products
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={loading}
            sx={{ mr: 1 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateOpen}
            disabled={loading}
          >
            Create card product
          </Button>
        </Box>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" gutterBottom>
            Filters
          </Typography>
          <Box display="flex" flexWrap="wrap" gap={2} alignItems="center">
            <TextField
              label="Code"
              size="small"
              value={codeFilter}
              onChange={(e) => setCodeFilter(e.target.value)}
              sx={{ minWidth: 160 }}
            />
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="">
                  <em>Any</em>
                </MenuItem>
                {STATUS_OPTIONS.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button
              variant="outlined"
              size="medium"
              onClick={handleApplyFilters}
            >
              Apply
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {loading && products.length === 0 ? (
            <Box display="flex" justifyContent="center" py={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Code</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Medium</TableCell>
                    <TableCell>Usage domains</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {products.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        No card products found.
                      </TableCell>
                    </TableRow>
                  ) : (
                    products.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.code}</TableCell>
                        <TableCell>{row.name}</TableCell>
                        <TableCell>{row.mediumType ?? "—"}</TableCell>
                        <TableCell>{row.usageDomains ?? "—"}</TableCell>
                        <TableCell>{row.status}</TableCell>
                        <TableCell align="right">
                          <Button
                            size="small"
                            startIcon={<ViewIcon />}
                            onClick={() => handleView(row)}
                          >
                            View
                          </Button>
                          <Button
                            size="small"
                            startIcon={<EditIcon />}
                            onClick={() => handleEditOpen(row)}
                          >
                            Edit
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {totalElements > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Total: {totalElements}
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Create dialog */}
      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create card product</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Code"
              required
              size="small"
              value={newProduct.code}
              onChange={(e) =>
                setNewProduct({ ...newProduct, code: e.target.value })
              }
            />
            <TextField
              label="Name"
              required
              size="small"
              value={newProduct.name}
              onChange={(e) =>
                setNewProduct({ ...newProduct, name: e.target.value })
              }
            />
            <TextField
              label="Description"
              size="small"
              multiline
              value={newProduct.description ?? ""}
              onChange={(e) =>
                setNewProduct({ ...newProduct, description: e.target.value })
              }
            />
            <FormControl size="small" fullWidth>
              <InputLabel>Medium type</InputLabel>
              <Select
                label="Medium type"
                value={newProduct.mediumType}
                onChange={(e) =>
                  setNewProduct({ ...newProduct, mediumType: e.target.value })
                }
              >
                {MEDIUM_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel>Usage Domain</InputLabel>
              <Select
                label="Usage Domain"
                value={newProduct.usageDomains ?? ""}
                onChange={(e) =>
                  setNewProduct({ ...newProduct, usageDomains: e.target.value })
                }
              >
                <MenuItem value="">
                  <em>Select usage domain</em>
                </MenuItem>
                {USAGE_DOMAIN_OPTIONS.map((domain) => (
                  <MenuItem key={domain} value={domain}>
                    {domain}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel>Default limit profile</InputLabel>
              <Select
                label="Default limit profile"
                value={newProduct.defaultLimitProfileId ?? ""}
                onChange={(e) =>
                  setNewProduct({
                    ...newProduct,
                    defaultLimitProfileId: e.target.value || undefined,
                  })
                }
              >
                <MenuItem value="">
                  <em>None</em>
                </MenuItem>
                {limitProfiles.map((lp) => (
                  <MenuItem key={lp.id} value={lp.id}>
                    {lp.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Validity start (date)"
              type="date"
              size="small"
              value={newProduct.validityStartDate ?? ""}
              onChange={(e) =>
                setNewProduct({
                  ...newProduct,
                  validityStartDate: e.target.value || undefined,
                })
              }
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Validity end (date)"
              type="date"
              size="small"
              value={newProduct.validityEndDate ?? ""}
              onChange={(e) =>
                setNewProduct({
                  ...newProduct,
                  validityEndDate: e.target.value || undefined,
                })
              }
              InputLabelProps={{ shrink: true }}
            />
            <FormControl size="small" fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={newProduct.status ?? "ACTIVE"}
                onChange={(e) =>
                  setNewProduct({ ...newProduct, status: e.target.value })
                }
              >
                {STATUS_OPTIONS.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateSubmit}
            disabled={loading}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* View dialog */}
      <Dialog
        open={viewOpen}
        onClose={() => setViewOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Card product</DialogTitle>
        <DialogContent>
          {selectedProduct && (
            <Box display="flex" flexDirection="column" gap={1} pt={1}>
              <Typography>
                <strong>Code:</strong> {selectedProduct.code}
              </Typography>
              <Typography>
                <strong>Name:</strong> {selectedProduct.name}
              </Typography>
              <Typography>
                <strong>Description:</strong>{" "}
                {selectedProduct.description ?? "—"}
              </Typography>
              <Typography>
                <strong>Medium:</strong> {selectedProduct.mediumType ?? "—"}
              </Typography>
              <Typography>
                <strong>Usage domains:</strong>{" "}
                {selectedProduct.usageDomains ?? "—"}
              </Typography>
              <Typography>
                <strong>Status:</strong> {selectedProduct.status}
              </Typography>
              <Typography>
                <strong>Limit profile:</strong>{" "}
                {limitProfiles.find(
                  (lp) => lp.id === selectedProduct.defaultLimitProfileId,
                )?.name ??
                  selectedProduct.defaultLimitProfileId ??
                  "—"}
              </Typography>
              <Typography>
                <strong>Validity:</strong>{" "}
                {selectedProduct.validityStartDate ?? "—"} to{" "}
                {selectedProduct.validityEndDate ?? "—"}
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewOpen(false)}>Close</Button>
          <Button
            variant="outlined"
            onClick={() =>
              selectedProduct &&
              (setViewOpen(false), handleEditOpen(selectedProduct))
            }
          >
            Edit
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit dialog */}
      <Dialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Edit card product</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Name"
              size="small"
              value={editProduct.name ?? ""}
              onChange={(e) =>
                setEditProduct({ ...editProduct, name: e.target.value })
              }
            />
            <TextField
              label="Description"
              size="small"
              multiline
              value={editProduct.description ?? ""}
              onChange={(e) =>
                setEditProduct({ ...editProduct, description: e.target.value })
              }
            />
            <FormControl size="small" fullWidth>
              <InputLabel>Medium type</InputLabel>
              <Select
                label="Medium type"
                value={editProduct.mediumType ?? ""}
                onChange={(e) =>
                  setEditProduct({ ...editProduct, mediumType: e.target.value })
                }
              >
                {MEDIUM_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel>Usage Domain</InputLabel>
              <Select
                label="Usage Domain"
                value={editProduct.usageDomains ?? ""}
                onChange={(e) =>
                  setEditProduct({ ...editProduct, usageDomains: e.target.value })
                }
              >
                <MenuItem value="">
                  <em>Select usage domain</em>
                </MenuItem>
                {USAGE_DOMAIN_OPTIONS.map((domain) => (
                  <MenuItem key={domain} value={domain}>
                    {domain}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel>Default limit profile</InputLabel>
              <Select
                label="Default limit profile"
                value={editProduct.defaultLimitProfileId ?? ""}
                onChange={(e) =>
                  setEditProduct({
                    ...editProduct,
                    defaultLimitProfileId: e.target.value || undefined,
                  })
                }
              >
                <MenuItem value="">
                  <em>None</em>
                </MenuItem>
                {limitProfiles.map((lp) => (
                  <MenuItem key={lp.id} value={lp.id}>
                    {lp.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Validity start (date)"
              type="date"
              size="small"
              value={editProduct.validityStartDate ?? ""}
              onChange={(e) =>
                setEditProduct({
                  ...editProduct,
                  validityStartDate: e.target.value || undefined,
                })
              }
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Validity end (date)"
              type="date"
              size="small"
              value={editProduct.validityEndDate ?? ""}
              onChange={(e) =>
                setEditProduct({
                  ...editProduct,
                  validityEndDate: e.target.value || undefined,
                })
              }
              InputLabelProps={{ shrink: true }}
            />
            <FormControl size="small" fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={editProduct.status ?? ""}
                onChange={(e) =>
                  setEditProduct({ ...editProduct, status: e.target.value })
                }
              >
                {STATUS_OPTIONS.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleEditSubmit}
            disabled={loading}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CardProductsPage;
