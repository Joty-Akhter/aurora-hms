--liquibase formatted sql

--changeset easyops:401-update-products-with-pack-box-pricing context:test-data
--comment: Update sample products with pack_size, box_size, wholesale_price (TP), and retail_price (MRP) values for Alien Pharma

-- Update Electronics products
UPDATE inventory.products 
SET pack_size = 1, 
    box_size = 10,
    wholesale_price = COALESCE(wholesale_price, cost_price * 1.15),
    retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-ELEC')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Furniture products
UPDATE inventory.products 
SET pack_size = 1, 
    box_size = 1,
    wholesale_price = COALESCE(wholesale_price, cost_price * 1.20),
    retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-FURN')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Supplies products
UPDATE inventory.products 
SET pack_size = CASE 
    WHEN sku LIKE '%PAPER%' THEN 500  -- Paper: 500 sheets per pack
    WHEN sku LIKE '%PEN%' THEN 12     -- Pens: 12 per pack
    WHEN sku LIKE '%FOLDER%' THEN 100 -- Folders: 100 per pack
    WHEN sku LIKE '%TONER%' THEN 1    -- Toner: 1 cartridge per pack
    WHEN sku LIKE '%NOTEBOOK%' THEN 1 -- Notebook: 1 per pack
    WHEN sku LIKE '%STAPLER%' THEN 1  -- Stapler: 1 per pack
    WHEN sku LIKE '%CALCULATOR%' THEN 1 -- Calculator: 1 per pack
    WHEN sku LIKE '%LABEL%' THEN 1    -- Labels: 1 pack per pack
    WHEN sku LIKE '%TSHIRT%' THEN 1   -- T-shirts: 1 per pack
    ELSE 1
  END,
  box_size = CASE 
    WHEN sku LIKE '%PAPER%' THEN 10   -- Paper: 10 packs per box
    WHEN sku LIKE '%PEN%' THEN 12     -- Pens: 12 packs per box
    WHEN sku LIKE '%FOLDER%' THEN 10  -- Folders: 10 packs per box
    WHEN sku LIKE '%TONER%' THEN 4    -- Toner: 4 cartridges per box
    WHEN sku LIKE '%NOTEBOOK%' THEN 12 -- Notebooks: 12 per box
    WHEN sku LIKE '%STAPLER%' THEN 6  -- Staplers: 6 per box
    WHEN sku LIKE '%CALCULATOR%' THEN 12 -- Calculators: 12 per box
    WHEN sku LIKE '%LABEL%' THEN 12   -- Labels: 12 packs per box
    WHEN sku LIKE '%TSHIRT%' THEN 12  -- T-shirts: 12 per box
    ELSE 1
  END,
  wholesale_price = COALESCE(wholesale_price, cost_price * 1.25),
  retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-SUPP')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Raw Materials products
UPDATE inventory.products 
SET pack_size = CASE 
    WHEN sku LIKE '%STEEL%' THEN 1    -- Steel: 1 sheet per pack
    WHEN sku LIKE '%ALUM%' THEN 12    -- Aluminum: 12 feet per pack
    WHEN sku LIKE '%PLASTIC%' THEN 25 -- Plastic: 25 kg per pack
    WHEN sku LIKE '%WOOD%' THEN 1     -- Wood: 1 sheet per pack
    WHEN sku LIKE '%SAND%' THEN 50    -- Sand: 50 kg per pack
    WHEN sku LIKE '%CEMENT%' THEN 1   -- Cement: 1 bag per pack
    WHEN sku LIKE '%GRAVEL%' THEN 1   -- Gravel: 1 ton per pack
    WHEN sku LIKE '%PAINT%' THEN 1    -- Paint: 1 bucket per pack
    ELSE 1
  END,
  box_size = CASE 
    WHEN sku LIKE '%STEEL%' THEN 20   -- Steel: 20 sheets per box
    WHEN sku LIKE '%ALUM%' THEN 10    -- Aluminum: 10 packs per box
    WHEN sku LIKE '%PLASTIC%' THEN 20 -- Plastic: 20 packs per box
    WHEN sku LIKE '%WOOD%' THEN 10    -- Wood: 10 sheets per box
    WHEN sku LIKE '%SAND%' THEN 20    -- Sand: 20 packs per box
    WHEN sku LIKE '%CEMENT%' THEN 50  -- Cement: 50 bags per box
    WHEN sku LIKE '%GRAVEL%' THEN 5   -- Gravel: 5 tons per box
    WHEN sku LIKE '%PAINT%' THEN 8    -- Paint: 8 buckets per box
    ELSE 1
  END,
  wholesale_price = COALESCE(wholesale_price, cost_price * 1.15),
  retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-RAW')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Finished Goods products
UPDATE inventory.products 
SET pack_size = 1, 
    box_size = 10,
    wholesale_price = COALESCE(wholesale_price, cost_price * 1.20),
    retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-FIN')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Food & Beverage products
UPDATE inventory.products 
SET pack_size = CASE 
    WHEN sku LIKE '%COFFEE%' THEN 1   -- Coffee: 1 kg per pack
    WHEN sku LIKE '%MILK%' THEN 6     -- Milk: 6 liters per pack
    WHEN sku LIKE '%JUICE%' THEN 6    -- Juice: 6 liters per pack
    WHEN sku LIKE '%BREAD%' THEN 12   -- Bread: 12 loaves per pack
    WHEN sku LIKE '%CHEESE%' THEN 2   -- Cheese: 2 units per pack
    ELSE 1
  END,
  box_size = CASE 
    WHEN sku LIKE '%COFFEE%' THEN 20  -- Coffee: 20 packs per box
    WHEN sku LIKE '%MILK%' THEN 4     -- Milk: 4 packs per box
    WHEN sku LIKE '%JUICE%' THEN 4    -- Juice: 4 packs per box
    WHEN sku LIKE '%BREAD%' THEN 5    -- Bread: 5 packs per box
    WHEN sku LIKE '%CHEESE%' THEN 12  -- Cheese: 12 packs per box
    ELSE 1
  END,
  wholesale_price = COALESCE(wholesale_price, cost_price * 1.20),
  retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-FOOD')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Chemicals products
UPDATE inventory.products 
SET pack_size = CASE 
    WHEN sku LIKE '%CHEM%' THEN 1     -- Chemicals: 1 container per pack
    WHEN sku LIKE '%LUBR%' THEN 1     -- Lubricants: 1 drum per pack
    ELSE 1
  END,
  box_size = CASE 
    WHEN sku LIKE '%CHEM%' THEN 4     -- Chemicals: 4 containers per box
    WHEN sku LIKE '%LUBR%' THEN 4     -- Lubricants: 4 drums per box
    ELSE 1
  END,
  wholesale_price = COALESCE(wholesale_price, cost_price * 1.18),
  retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-CHEM')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Update Packaging products
UPDATE inventory.products 
SET pack_size = CASE 
    WHEN sku LIKE '%BOX%' THEN 50     -- Boxes: 50 per pack
    WHEN sku LIKE '%TAPE%' THEN 12    -- Tape: 12 rolls per pack
    WHEN sku LIKE '%BUBBLE%' THEN 10  -- Bubble wrap: 10 rolls per pack
    WHEN sku LIKE '%PALLET%' THEN 4   -- Pallets: 4 per pack
    WHEN sku LIKE '%STRETCH%' THEN 12 -- Stretch wrap: 12 rolls per pack
    ELSE 1
  END,
  box_size = CASE 
    WHEN sku LIKE '%BOX%' THEN 10     -- Boxes: 10 packs per box
    WHEN sku LIKE '%TAPE%' THEN 5     -- Tape: 5 packs per box
    WHEN sku LIKE '%BUBBLE%' THEN 5   -- Bubble wrap: 5 packs per box
    WHEN sku LIKE '%PALLET%' THEN 5   -- Pallets: 5 packs per box
    WHEN sku LIKE '%STRETCH%' THEN 4  -- Stretch wrap: 4 packs per box
    ELSE 1
  END,
  wholesale_price = COALESCE(wholesale_price, cost_price * 1.30),
  retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND category_id = (SELECT id FROM inventory.product_categories WHERE code = 'CAT-PACK')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0);

-- Ensure all remaining products have default values if still null
UPDATE inventory.products 
SET pack_size = COALESCE(pack_size, 1),
    box_size = COALESCE(box_size, 1),
    wholesale_price = COALESCE(wholesale_price, cost_price * 1.15),
    retail_price = COALESCE(retail_price, selling_price)
WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'DEMO_ORG')
  AND (pack_size IS NULL OR pack_size = 0 OR box_size IS NULL OR box_size = 0 
       OR wholesale_price IS NULL OR retail_price IS NULL);

