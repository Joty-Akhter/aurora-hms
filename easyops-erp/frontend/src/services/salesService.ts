import api from './api';
import {
  Customer,
  Product,
  Quotation,
  QuotationRequest,
  SalesOrder,
  SalesOrderRequest
} from '../types/sales';

const salesService = {
  // Customer APIs - Now using CRM Service
  // Customers are managed in CRM Service as Accounts, not in Sales Service
  async getCustomers(organizationId: string, activeOnly: boolean = false): Promise<Customer[]> {
    const response = await api.get('/api/crm/accounts', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getCustomerById(id: string): Promise<Customer> {
    const response = await api.get(`/api/crm/accounts/${id}`);
    return response.data;
  },

  async createCustomer(data: any): Promise<Customer> {
    const response = await api.post('/api/crm/accounts', data);
    return response.data;
  },

  async updateCustomer(id: string, data: any): Promise<Customer> {
    const response = await api.put(`/api/crm/accounts/${id}`, data);
    return response.data;
  },

  async deleteCustomer(id: string): Promise<void> {
    await api.delete(`/api/crm/accounts/${id}`);
  },

  async deactivateCustomer(id: string): Promise<Customer> {
    // CRM uses different status update endpoint
    const response = await api.put(`/api/crm/accounts/${id}`, { isActive: false });
    return response.data;
  },

  // Product APIs - Now using Inventory Service
  // Products are managed in Inventory Service, not Sales Service
  async getProducts(organizationId: string, activeOnly: boolean = false): Promise<Product[]> {
    const response = await api.get('/api/inventory/products', {
      params: { organizationId, activeOnly }
    });
    return response.data;
  },

  async getProductById(id: string): Promise<Product> {
    const response = await api.get(`/api/inventory/products/${id}`);
    return response.data;
  },

  async createProduct(data: any): Promise<Product> {
    const response = await api.post('/api/inventory/products', data);
    return response.data;
  },

  async updateProduct(id: string, data: any): Promise<Product> {
    const response = await api.put(`/api/inventory/products/${id}`, data);
    return response.data;
  },

  async deleteProduct(id: string): Promise<void> {
    await api.delete(`/api/inventory/products/${id}`);
  },

  async deactivateProduct(id: string): Promise<Product> {
    const response = await api.post(`/api/inventory/products/${id}/deactivate`);
    return response.data;
  },

  // Quotation APIs
  async getQuotations(organizationId: string, status?: string): Promise<Quotation[]> {
    const response = await api.get('/api/sales/quotations', {
      params: { organizationId, status }
    });
    return response.data;
  },

  async getQuotationById(id: string): Promise<Quotation> {
    const response = await api.get(`/api/sales/quotations/${id}`);
    return response.data;
  },

  async getQuotationsByCustomer(organizationId: string, customerId: string): Promise<Quotation[]> {
    const response = await api.get(`/api/sales/quotations/customer/${customerId}`, {
      params: { organizationId }
    });
    return response.data;
  },

  async createQuotation(data: QuotationRequest): Promise<Quotation> {
    const response = await api.post('/api/sales/quotations', data);
    return response.data;
  },

  async updateQuotation(id: string, data: QuotationRequest): Promise<Quotation> {
    const response = await api.put(`/api/sales/quotations/${id}`, data);
    return response.data;
  },

  async sendQuotation(id: string): Promise<Quotation> {
    const response = await api.post(`/api/sales/quotations/${id}/send`);
    return response.data;
  },

  async acceptQuotation(id: string): Promise<Quotation> {
    const response = await api.post(`/api/sales/quotations/${id}/accept`);
    return response.data;
  },

  async rejectQuotation(id: string): Promise<Quotation> {
    const response = await api.post(`/api/sales/quotations/${id}/reject`);
    return response.data;
  },

  async deleteQuotation(id: string): Promise<void> {
    await api.delete(`/api/sales/quotations/${id}`);
  },

  // Sales Order APIs
  async getSalesOrders(organizationId: string, status?: string): Promise<SalesOrder[]> {
    const response = await api.get('/api/sales/orders', {
      params: { organizationId, status }
    });
    return response.data;
  },

  async getSalesOrderById(id: string): Promise<SalesOrder> {
    const response = await api.get(`/api/sales/orders/${id}`);
    return response.data;
  },

  async getSalesOrdersByCustomer(organizationId: string, customerId: string): Promise<SalesOrder[]> {
    const response = await api.get(`/api/sales/orders/customer/${customerId}`, {
      params: { organizationId }
    });
    return response.data;
  },

  async getOrdersReadyForInvoicing(organizationId: string): Promise<SalesOrder[]> {
    const response = await api.get('/api/sales/orders/ready-for-invoicing', {
      params: { organizationId }
    });
    return response.data;
  },

  async createSalesOrder(data: SalesOrderRequest): Promise<SalesOrder> {
    const response = await api.post('/api/sales/orders', data);
    return response.data;
  },

  async createOrderFromQuotation(quotationId: string): Promise<SalesOrder> {
    const response = await api.post(`/api/sales/orders/from-quotation/${quotationId}`);
    return response.data;
  },

  async updateSalesOrder(id: string, data: SalesOrderRequest): Promise<SalesOrder> {
    const response = await api.put(`/api/sales/orders/${id}`, data);
    return response.data;
  },

  async confirmOrder(id: string): Promise<SalesOrder> {
    const response = await api.post(`/api/sales/orders/${id}/confirm`);
    return response.data;
  },

  async approveOrder(id: string, approvedBy: string): Promise<SalesOrder> {
    const response = await api.post(`/api/sales/orders/${id}/approve`, null, {
      params: { approvedBy }
    });
    return response.data;
  },

  async startProcessing(id: string): Promise<SalesOrder> {
    const response = await api.post(`/api/sales/orders/${id}/start-processing`);
    return response.data;
  },

  async completeOrder(id: string): Promise<SalesOrder> {
    const response = await api.post(`/api/sales/orders/${id}/complete`);
    return response.data;
  },

  async cancelOrder(id: string): Promise<SalesOrder> {
    const response = await api.post(`/api/sales/orders/${id}/cancel`);
    return response.data as SalesOrder;
  },

  async convertToInvoice(id: string): Promise<{ invoiceId: string; message: string }> {
    const response = await api.post(`/api/sales/orders/${id}/convert-to-invoice`);
    return response.data as { invoiceId: string; message: string };
  },

  async deleteSalesOrder(id: string): Promise<void> {
    await api.delete(`/api/sales/orders/${id}`);
  },
};

export default salesService;

