--liquibase formatted sql

--changeset easyops:045-create-manufacturing-bom-routing-views splitStatements:false

-- =====================================================
-- View: BOM Explosion (Multi-Level BOM)
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_bom_explosion AS
WITH RECURSIVE bom_explosion AS (
    -- Base level (Level 1 components)
    SELECT 
        bl.bom_id,
        bh.product_id AS finished_product_id,
        bh.product_code AS finished_product_code,
        bl.bom_line_id,
        bl.component_id,
        bl.component_code,
        bl.component_name,
        bl.quantity_per_unit::NUMERIC,
        bl.uom,
        bl.level_number,
        bl.component_type,
        bl.scrap_percentage,
        bl.unit_cost::NUMERIC,
        COALESCE(bl.extended_cost, 0)::NUMERIC,
        bl.sequence_number,
        ARRAY[bl.component_id] AS path,
        1 AS bom_level
    FROM manufacturing.bom_lines bl
    JOIN manufacturing.bom_headers bh ON bl.bom_id = bh.bom_id
    WHERE bl.parent_line_id IS NULL
    
    UNION ALL
    
    -- Recursive part (sub-levels)
    SELECT 
        be.bom_id,
        be.finished_product_id,
        be.finished_product_code,
        bl.bom_line_id,
        bl.component_id,
        bl.component_code,
        bl.component_name,
        (be.quantity_per_unit * bl.quantity_per_unit)::NUMERIC AS quantity_per_unit,
        bl.uom,
        bl.level_number,
        bl.component_type,
        bl.scrap_percentage,
        COALESCE(bl.unit_cost, 0)::NUMERIC AS unit_cost,
        (be.quantity_per_unit * bl.quantity_per_unit * COALESCE(bl.unit_cost, 0))::NUMERIC AS extended_cost,
        bl.sequence_number,
        be.path || bl.component_id,
        be.bom_level + 1
    FROM bom_explosion be
    JOIN manufacturing.bom_lines bl ON bl.parent_line_id = be.bom_line_id
    WHERE bl.component_id <> ALL(be.path) -- Prevent circular references
)
SELECT * FROM bom_explosion;

-- =====================================================
-- View: BOM Summary with Costs
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_bom_summary AS
SELECT 
    bh.bom_id,
    bh.organization_id,
    bh.bom_number,
    bh.product_id,
    bh.product_code,
    bh.product_name,
    bh.bom_type,
    bh.version_number,
    bh.status,
    bh.base_quantity,
    bh.material_cost,
    bh.labor_cost,
    bh.overhead_cost,
    bh.total_cost,
    
    -- Component Counts
    (SELECT COUNT(*) FROM manufacturing.bom_lines WHERE bom_id = bh.bom_id) AS component_count,
    (SELECT COUNT(DISTINCT component_type) FROM manufacturing.bom_lines WHERE bom_id = bh.bom_id) AS component_types,
    
    -- Cost Calculations
    (SELECT SUM(extended_cost) FROM manufacturing.bom_lines WHERE bom_id = bh.bom_id) AS total_material_cost,
    
    -- Routing Info
    (SELECT COUNT(*) FROM manufacturing.product_routings WHERE product_id = bh.product_id) AS operation_count,
    (SELECT SUM(setup_time + run_time_per_unit * bh.base_quantity) 
     FROM manufacturing.product_routings WHERE product_id = bh.product_id) AS total_production_time,
    
    bh.effective_from,
    bh.effective_to,
    bh.approved_by,
    bh.approved_date,
    bh.created_at,
    bh.updated_at
FROM manufacturing.bom_headers bh;

-- =====================================================
-- View: Product Routing Summary
-- =====================================================
CREATE OR REPLACE VIEW manufacturing.v_product_routing_summary AS
SELECT 
    pr.product_id,
    pr.organization_id,
    COUNT(*) AS total_operations,
    SUM(pr.setup_time) AS total_setup_time,
    AVG(pr.run_time_per_unit) AS avg_run_time,
    SUM(pr.setup_cost) AS total_setup_cost,
    MAX(pr.operation_sequence) AS max_sequence
FROM manufacturing.product_routings pr
WHERE pr.is_active = true
GROUP BY pr.product_id, pr.organization_id;

