-- Catalogos que las pruebas no borran. Los clientes, personas, usuarios y
-- productos los crea cada test en su propio @BeforeEach.
INSERT INTO tipo_documento VALUES
    ('TD001', 'Boleta'),
    ('TD002', 'Factura');

-- Roles del sistema. Las pruebas de seguridad usan estas cuentas para
-- comprobar que ADMIN entra al panel y CLIENTE no.
INSERT INTO rol (id_rol, nombre, descripcion) VALUES
    ('ADMIN', 'Administrador', 'Gestiona productos, historial general, clientes y alertas de stock'),
    ('CLIENTE', 'Cliente', 'Compra en la tienda, ve su carrito y sus compras');

INSERT INTO medio_pago VALUES
    ('MP001', 'Efectivo'),
    ('MP002', 'Tarjeta de crédito'),
    ('MP003', 'Tarjeta de débito'),
    ('MP004', 'Yape'),
    ('MP005', 'Plin'),
    ('MP006', 'Transferencia'),
    ('MP007', 'Pago contra entrega'),
    ('MP008', 'Billetera digital'),
    ('MP009', 'Visa'),
    ('MP010', 'Mastercard'),
    ('MP011', 'American Express'),
    ('MP012', 'Depósito'),
    ('MP013', 'Crédito empresarial'),
    ('MP014', 'Pago mixto'),
    ('MP015', 'PayPal');
