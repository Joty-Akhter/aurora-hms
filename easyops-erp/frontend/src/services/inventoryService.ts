import api from './api';

export interface Product {
  id: string;
  organizationId: string;
  sku: string;
  barcode?: string;
  name: string;
  description?: string;
  shortDescription?: string;
  categoryId?: string;
  brand?: string;
  manufacturer?: string;
  productType: string;
  itemType: string;
  costPrice?: number;       // kept optional; mapped from TP
  sellingPrice?: number;    // kept optional; mapped from MRP
  wholesalePrice?: number; // TP - Trade Price
  retailPrice?: number;    // MRP - Maximum Retail Price
  currency: string;
  uom: string;
  // Alien Pharma specific fields
  packSize?: number; // Number of units per pack
  reorderLevel: number;
  minStockLevel: number;
  maxStockLevel?: number;
  safetyStock: number;
  trackInventory: boolean;
  trackBatch: boolean;
  trackSerial: boolean;
  isActive: boolean;
  status: string;
  imageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductCategory {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  description?: string;
  parentCategoryId?: string;
  imageUrl?: string;
  isActive: boolean;
  displayOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Warehouse {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  warehouseType: string;
  addressLine1?: string;
  city?: string;
  state?: string;
  country?: string;
  phone?: string;
  email?: string;
  isActive: boolean;
  status: string;
}

export interface Stock {
  id: string;
  organizationId: string;
  productId: string;
  warehouseId: string;
  quantityOnHand: number;
  quantityAllocated: number;
  quantityAvailable: number;
  quantityOnOrder: number;
  unitCost: number;
  totalCost: number;
  status: string;
  batchNumber?: string;
  expiryDate?: string;
}

export interface StockMovement {
  id: string;
  transactionNumber: string;
  transactionDate: string;
  transactionType: string;
  productId: string;
  warehouseId: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  sourceType?: string;
  status: string;
}

const inventoryService = {
  // Products
  async getProducts(organizationId: string, activeOnly?: boolean): Promise<Product[]> {
    const response = await api.get('/api/inventory/products', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getProductById(id: string): Promise<Product> {
    const response = await api.get(`/api/inventory/products/${id}`);
    return response.data;
  },

  async getProductBySku(organizationId: string, sku: string): Promise<Product> {
    const response = await api.get(`/api/inventory/products/sku/${sku}`, {
      params: { organizationId }
    });
    return response.data;
  },

  async searchProducts(organizationId: string, keyword: string): Promise<Product[]> {
    const response = await api.get('/api/inventory/products/search', {
      params: { organizationId, keyword }
    });
    return response.data;
  },

  async createProduct(product: Partial<Product>): Promise<Product> {
    const response = await api.post('/api/inventory/products', product);
    return response.data;
  },

  async updateProduct(id: string, product: Partial<Product>): Promise<Product> {
    const response = await api.put(`/api/inventory/products/${id}`, product);
    return response.data;
  },

  async deleteProduct(id: string): Promise<void> {
    await api.delete(`/api/inventory/products/${id}`);
  },

  // Product Categories
  async getCategories(organizationId: string, activeOnly?: boolean): Promise<ProductCategory[]> {
    const response = await api.get('/api/inventory/categories', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getCategoryById(id: string): Promise<ProductCategory> {
    const response = await api.get(`/api/inventory/categories/${id}`);
    return response.data;
  },

  async createCategory(category: Partial<ProductCategory>): Promise<ProductCategory> {
    const response = await api.post('/api/inventory/categories', category);
    return response.data;
  },

  async updateCategory(id: string, category: Partial<ProductCategory>): Promise<ProductCategory> {
    const response = await api.put(`/api/inventory/categories/${id}`, category);
    return response.data;
  },

  async deleteCategory(id: string): Promise<void> {
    await api.delete(`/api/inventory/categories/${id}`);
  },

  // Warehouses
  async getWarehouses(organizationId: string, activeOnly?: boolean): Promise<Warehouse[]> {
    const response = await api.get('/api/inventory/warehouses', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getWarehouseById(id: string): Promise<Warehouse> {
    const response = await api.get(`/api/inventory/warehouses/${id}`);
    return response.data;
  },

  async createWarehouse(warehouse: Partial<Warehouse>): Promise<Warehouse> {
    const response = await api.post('/api/inventory/warehouses', warehouse);
    return response.data;
  },

  async updateWarehouse(id: string, warehouse: Partial<Warehouse>): Promise<Warehouse> {
    const response = await api.put(`/api/inventory/warehouses/${id}`, warehouse);
    return response.data;
  },

  // Stock
  async getStock(organizationId: string, productId?: string, warehouseId?: string): Promise<Stock[]> {
    const response = await api.get('/api/inventory/stock', {
      params: { organizationId, productId, warehouseId }
    });
    return response.data;
  },

  async getAvailableQuantity(organizationId: string, productId: string, warehouseId: string): Promise<number> {
    const response = await api.get('/api/inventory/stock/available', {
      params: { organizationId, productId, warehouseId }
    });
    return response.data.availableQuantity;
  },

  async getLowStockItems(organizationId: string): Promise<Stock[]> {
    const response = await api.get('/api/inventory/stock/low-stock', {
      params: { organizationId }
    });
    return response.data;
  },

  async getOutOfStockItems(organizationId: string): Promise<Stock[]> {
    const response = await api.get('/api/inventory/stock/out-of-stock', {
      params: { organizationId }
    });
    return response.data;
  },

  async receiveStock(request: {
    organizationId: string;
    productId: string;
    warehouseId: string;
    quantity: number;
    unitCost: number;
    sourceType?: string;
    sourceId?: string;
    createdBy?: string;
  }): Promise<Stock> {
    const response = await api.post('/api/inventory/stock/receive', request);
    return response.data;
  },

  async issueStock(request: {
    organizationId: string;
    productId: string;
    warehouseId: string;
    quantity: number;
    sourceType?: string;
    sourceId?: string;
    createdBy?: string;
  }): Promise<Stock> {
    const response = await api.post('/api/inventory/stock/issue', request);
    return response.data;
  },

  /**
   * Stock adjustment: send either `newQuantity` (absolute on-hand after adjustment) or `quantityDelta` (add/subtract from current on-hand).
   */
  async adjustStock(request: {
    organizationId: string;
    productId: string;
    warehouseId: string;
    reason: string;
    createdBy?: string;
    newQuantity?: number;
    quantityDelta?: number;
  }): Promise<Stock> {
    const n = request.newQuantity;
    const d = request.quantityDelta;
    // NaN/Infinity are not valid JSON numbers (they become null), so the server would see both fields missing → 400.
    const badNum = (v: unknown) => typeof v === 'number' && !Number.isFinite(v);
    const hasNew = n !== undefined && n !== null && !badNum(n);
    const hasDelta = d !== undefined && d !== null && !badNum(d);
    if (hasDelta === hasNew) {
      throw new Error('Provide exactly one valid newQuantity or quantityDelta (must be a finite number)');
    }
    const body: Record<string, unknown> = {
      organizationId: request.organizationId,
      productId: request.productId,
      warehouseId: request.warehouseId,
      reason: request.reason,
    };
    if (request.createdBy) body.createdBy = request.createdBy;
    if (hasDelta) body.quantityDelta = d;
    else body.newQuantity = n;
    const response = await api.post('/api/inventory/stock/adjust', body);
    return response.data;
  },

  async bulkReceiveStock(request: {
    organizationId: string;
    date: string;
    warehouseId: string;
    items: Array<{
      productId: string;
      quantity: number;
      packSize: number;
      tradePrice: number;
      mrp: number;
      expiryDate?: string;
      notes?: string;
    }>;
    notes?: string;
    createdBy?: string;
  }): Promise<{
    success: boolean;
    receivedCount: number;
    totalQuantity: number;
    totalAmount: number;
    stocks: Stock[];
    errors?: string[];
  }> {
    const response = await api.post('/api/inventory/stock/receive/bulk', request);
    return response.data;
  }
};

export default inventoryService;

