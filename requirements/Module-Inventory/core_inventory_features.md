# Core Inventory Features – Detailed Specification

## Table of Contents
- 1. Purpose & Scope
- 2. Product Catalog
- 3. Stock Management
- 4. Multi-Warehouse & Locations
- 5. Real-Time Stock Updates
- 6. Batch & Lot Tracking
- 7. Serial Number Tracking
- 8. Reorder & Alerts
- 9. Stock Transfers
- 10. Inventory Valuation
- 11. Reporting & KPIs

---

## 1. Purpose & Scope
- Maintain accurate product master and stock across warehouses, enabling purchasing, sales, and accounting integrations

## 2. Product Catalog
### 2.1 Basic Product Information
- Product ID (Auto-generated unique ID)
- SKU (unique), Barcode/QR, Name, Descriptions, Category/Sub-Category, Brand/Manufacturer, Variant/Model, Images, Status
### 2.2 Pricing & Costing
- Purchase/Selling/Wholesale/Special prices, effective dates, currency, tax rate/type, price history
### 2.3 Stock & Inventory Details
- Stock UoM, current qty, min/max/safety levels, warehouse/location, batch/lot, serials, expiry/mfg, storage conditions
### 2.4 Supplier Information
- Primary/alternate suppliers, lead time, supplier codes, pricing agreements
### 2.5 Variants & Attributes
- Variant types/values, variant SKU/price/stock, attribute sets (length/width/weight)
### 2.6 Logistics & Shipping
- Dimensions, weight, packaging, HS code, origin, restrictions
### 2.7 Accounting & Tax
- GL mapping, cost center, asset/expense, tax category
### 2.8 Customer-Facing & Compliance
- Marketing info, warranty/returns, compliance docs, lifecycle metadata

## 3. Stock Management
### 3.1 Add / Receive
- Transaction ID, product, qty, warehouse/location, batch/serial, PO reference, unit cost/total, dates, notes, user
### 3.2 Adjust
- Previous vs new qty, adjustment type/reason, approvals, timestamps
### 3.3 Remove / Issue
- SO/Invoice reference, removal reason, cost impact, approvals
### 3.4 System Features
- Audit trail, approvals, real-time updates, multi-warehouse transfers, notifications, integrations

## 4. Multi-Warehouse & Locations
- Master data for warehouses/zones/racks/bins; stock by warehouse; transfers; per-location adjustments; access control; integrations

## 5. Real-Time Stock Updates
- Triggers from PO/GRN, SO/Shipment, adjustments; propagation across locations; notifications; audit

## 6. Batch & Lot Tracking
- Batch master, per-batch stock, expiry alerts, movements, reports, integrations

## 7. Serial Number Tracking
- Serial master, per-serial status, movement, warranty/service, reports, integrations

## 8. Reorder & Alerts
- Reorder levels per product/warehouse; monitoring; alert configuration; automated PO triggers; reports; integrations

## 9. Stock Transfers
- Transfer master/items, approvals, movement tracking, alerts, reports, integrations

## 10. Inventory Valuation
- Methods: FIFO/LIFO/Weighted Average; per-warehouse policy; effective dating
- Costing events: receipts, issues, returns, adjustments; COGS integration to Accounting
- Reports: valuation summary, movement cost analysis

## 11. Reporting & KPIs
- Stock summary/movement, low/critical stock, expiry, dead stock, capacity utilization, transfer analysis
- KPIs: inventory turnover, fill rate, days of inventory, shrinkage rate

