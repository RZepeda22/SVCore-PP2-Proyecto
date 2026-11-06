-- Datos de demostración de Adventure Works (los mismos que muestra el frontend).

-- Roles del sistema (código de 4 letras, igual que en DemoConvencionServer)
INSERT INTO rol (id, name) VALUES
    ('ADMN', 'Administrador'),
    ('VEND', 'Vendedor'),
    ('INVE', 'Encargado de inventario');

-- Categorías: el código coincide con las pestañas del POS
INSERT INTO product_category (id, code, name) VALUES
    (UUID(), 'bikes',       'Bicicletas'),
    (UUID(), 'components',  'Componentes'),
    (UUID(), 'clothing',    'Ropa'),
    (UUID(), 'accessories', 'Accesorios');

INSERT INTO product (id, code, name, price, stock, min_stock, category_id, active, created_at)
SELECT UUID(), p.code, p.name, p.price, p.stock, p.min_stock, c.id, b'1', NOW(6)
FROM (
    SELECT 'BK-M68B-42' AS code, 'Mountain-200 Black' AS name, 649.00 AS price, 42 AS stock, 8 AS min_stock, 'bikes' AS category
    UNION ALL SELECT 'BK-M68S-42', 'Mountain-200 Silver',         649.00, 18,  8, 'bikes'
    UNION ALL SELECT 'RF-R100',    'Road Bike Frame',            1249.00,  3,  2, 'bikes'
    UNION ALL SELECT 'CH-0234',    'Cadena HL Chain',              20.24,  7, 10, 'components'
    UNION ALL SELECT 'CB-3456',    'Chainring Bolts',               8.99, 24, 10, 'components'
    UNION ALL SELECT 'BR-2310',    'Front Brake Set',              106.50,  4, 12, 'components'
    UNION ALL SELECT 'GL-F110-M',  'Gloves Full-Finger M',         24.49, 63, 15, 'clothing'
    UNION ALL SELECT 'SJ-C100-M',  'Short-Sleeve Classic Jersey',  52.99, 31, 10, 'clothing'
    UNION ALL SELECT 'HL-U509-R',  'Sport-100 Helmet',             34.99, 12, 20, 'accessories'
    UNION ALL SELECT 'BC-0501',    'Bike Computer Pro',            89.00, 15, 10, 'accessories'
    UNION ALL SELECT 'WB-H098',    'Water Bottle 30oz',             4.99,  8, 25, 'accessories'
    UNION ALL SELECT 'FS-M500',    'Fender Set - Mountain',        21.98,  5,  5, 'accessories'
    UNION ALL SELECT 'HM-001',     'Helmet Mount',                  9.99, 45, 10, 'accessories'
    UNION ALL SELECT 'LT-9087',    'LED Headlight Pro',            39.99,  0,  8, 'accessories'
) AS p
JOIN product_category c ON c.code = p.category;

-- Clientes contribuyentes de demostración (los mismos del POS).
-- "Comercial San Jorge" tiene un NRC que la simulación de Hacienda rechaza (código 003).
INSERT INTO customer (id, name, taxpayer_type, nit, nrc, email, address, created_at) VALUES
    (UUID(), 'Almacenes MORENA, S.A.', 'CONTRIBUYENTE', '0614-280590-102-3', '220145-3', 'facturacion@morena.com.sv', 'San Salvador, San Salvador', NOW(6)),
    (UUID(), 'Tech Solutions S.A.',    'CONTRIBUYENTE', NULL,                '118934-7', NULL, NULL, NOW(6)),
    (UUID(), 'Distribuidora El Roble', 'CONTRIBUYENTE', NULL,                '187654-2', NULL, NULL, NOW(6)),
    (UUID(), 'Comercial San Jorge',    'CONTRIBUYENTE', NULL,                '990000-0', NULL, NULL, NOW(6));
