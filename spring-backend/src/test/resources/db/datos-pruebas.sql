-- Catalogos que las pruebas no borran. Los clientes, personas y productos los
-- crea cada test en su propio @BeforeEach.
INSERT INTO tipo_documento VALUES
    ('TD001', 'Boleta'),
    ('TD002', 'Factura');

-- Series y parametros del emisor: los usa ComprobanteServiceImpl.
INSERT INTO correlativo (serie, id_tipo_documento, ultimo_numero) VALUES
    ('B001', 'TD001', 0),
    ('F001', 'TD002', 0);

INSERT INTO parametro (id_parametro, valor, descripcion) VALUES
    ('IGV_PORCENTAJE', '18.00', 'Impuesto General a las Ventas en porcentaje'),
    ('MONEDA', 'PEN', 'Codigo de moneda ISO 4217'),
    ('RUC_EMISOR', '20604567891', 'RUC del emisor'),
    ('RAZON_SOCIAL_EMISOR', 'INNOVATION STORE S.A.C.', 'Razon social del emisor'),
    ('NOMBRE_COMERCIAL', 'TechZone', 'Nombre comercial del emisor'),
    ('DOMICILIO_FISCAL_EMISOR', 'Av. Javier Prado Este 4200, Of. 802', 'Domicilio fiscal del emisor'),
    ('UBIGEO_EMISOR', '150131', 'Codigo ubigeo del domicilio fiscal'),
    ('DISTRITO_EMISOR', 'Santiago de Surco', 'Distrito del emisor'),
    ('PROVINCIA_EMISOR', 'Lima', 'Provincia del emisor'),
    ('DEPARTAMENTO_EMISOR', 'Lima', 'Departamento del emisor'),
    ('PAIS_EMISOR', 'PE', 'Pais ISO del emisor'),
    ('EMAIL_EMISOR', 'facturacion@techzone.pe', 'Correo del emisor'),
    ('TELEFONO_EMISOR', '5115551234', 'Telefono del emisor'),
    ('SERIE_BOLETA', 'B001', 'Serie de las boletas de venta'),
    ('SERIE_FACTURA', 'F001', 'Serie de las facturas de venta'),
    ('LIMITE_BOLETA_SIN_RUC', '700.00', 'Monto maximo de boleta sin RUC (SUNAT)'),
    ('URL_REPRESENTACION_IMPRESA', 'https://www.techzone.pe/comprobantes', 'URL de consulta de la representacion impresa'),
    ('CLIENTE_MOSTRADOR', 'CLI001', 'Cliente por defecto de las ventas de mostrador'),
    ('PERSONA_MOSTRADOR', 'PER001', 'Persona por defecto de las ventas de mostrador');

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
