-- ============================================================================
--  MIGRACION: FACTURACION ELECTRONICA SUNAT (UBL 2.1)
--  Para bases que YA existen. En una instalacion nueva basta con ejecutar
--  database/script_mysql.sql, que ya incluye todo este esquema.
--
--  Objetivo:
--    1. Tabla `correlativo`: numeracion correlativa por serie (SUNAT exige
--       series correlativas, sin saltos ni duplicados).
--    2. Columnas fiscales en `venta` (cabecera del comprobante).
--    3. Columnas fiscales en `detalle_venta` (items del comprobante).
--    4. Columnas de catalogo en `producto` (codigo, unidad, afectacion IGV).
--    5. Parametros del emisor.
--
--  MySQL 8.0 no admite "ADD COLUMN IF NOT EXISTS" ni "CREATE INDEX IF NOT
--  EXISTS": por eso las columnas se agregan con un procedimiento auxiliar que
--  consulta information_schema antes de cada ALTER.
--
--  Uso:
--    mysql -u root -p proyecto < database/migracion_facturacion_sunat.sql
-- ============================================================================

USE proyecto;

SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
--  Procedimiento auxiliar: agregar una columna solo si no existe
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS agregar_columna_si_no_existe;
DROP PROCEDURE IF EXISTS agregar_indice_si_no_existe;

DELIMITER $$
CREATE PROCEDURE agregar_columna_si_no_existe(
    IN p_tabla     VARCHAR(64),
    IN p_columna   VARCHAR(64),
    IN p_definicion TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_tabla
          AND COLUMN_NAME = p_columna
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN `', p_columna, '` ', p_definicion);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE agregar_indice_si_no_existe(
    IN p_tabla VARCHAR(64),
    IN p_indice VARCHAR(64),
    IN p_columnas VARCHAR(255),
    IN p_unico BOOLEAN
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_tabla
          AND INDEX_NAME = p_indice
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_tabla, '` ADD ',
                          IF(p_unico, 'UNIQUE ', ''),
                          'INDEX `', p_indice, '` (', p_columnas, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 1. Correlativo por serie
--    El numero de correlativo NO se puede calcular con COUNT(): dos ventas
--    simultaneas lo repetirian. Se bloquea la fila con SELECT ... FOR UPDATE.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS correlativo (
    serie             VARCHAR(4)  NOT NULL,
    id_tipo_documento VARCHAR(25) NOT NULL,
    ultimo_numero     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (serie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. Cabecera del comprobante (tabla venta)
-- ----------------------------------------------------------------------------
CALL agregar_columna_si_no_existe('venta', 'serie',                'VARCHAR(4)   NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'correlativo',          'INT          NOT NULL DEFAULT 0');
CALL agregar_columna_si_no_existe('venta', 'moneda',               'VARCHAR(3)   NOT NULL DEFAULT ''PEN''');
CALL agregar_columna_si_no_existe('venta', 'condicion_operacion',  'VARCHAR(2)   NOT NULL DEFAULT ''01''');
CALL agregar_columna_si_no_existe('venta', 'forma_pago',           'VARCHAR(2)   NOT NULL DEFAULT ''01''');
CALL agregar_columna_si_no_existe('venta', 'total_venta',          'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'total_gravada',        'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'total_inafecta',       'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'total_exonerada',      'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'total_gratuita',       'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'total_igv',            'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'total_descuentos',     'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'monto_total_credito',  'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('venta', 'receptor_tipo_documento', 'VARCHAR(1)   NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_num_documento',  'VARCHAR(15)  NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_razon_social',   'VARCHAR(200) NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_direccion',      'VARCHAR(200) NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_ubigeo',         'VARCHAR(6)   NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_distrito',       'VARCHAR(80)  NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_provincia',      'VARCHAR(80)  NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_departamento',   'VARCHAR(80)  NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'receptor_pais',           'VARCHAR(2)   NOT NULL DEFAULT ''PE''');
CALL agregar_columna_si_no_existe('venta', 'entrega_direccion',       'VARCHAR(200) NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'entrega_ubigeo',          'VARCHAR(6)   NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'email_contacto',          'VARCHAR(100) NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('venta', 'hash_comprobante',        'VARCHAR(64)  NULL');
CALL agregar_columna_si_no_existe('venta', 'codigo_qr',               'VARCHAR(500) NULL');
CALL agregar_columna_si_no_existe('venta', 'xml_name',                'VARCHAR(120) NULL');
CALL agregar_columna_si_no_existe('venta', 'estado',                  'VARCHAR(20)  NOT NULL DEFAULT ''EMITIDO''');
CALL agregar_columna_si_no_existe('venta', 'estado_sunat',            'VARCHAR(20)  NULL');
CALL agregar_columna_si_no_existe('venta', 'cdR_summary',             'VARCHAR(500) NULL');
CALL agregar_columna_si_no_existe('venta', 'leyendas',                'TEXT         NULL');
CALL agregar_columna_si_no_existe('venta', 'observacion',             'VARCHAR(500) NOT NULL DEFAULT ''''');

-- Las ventas historicas solo tienen `numero_documento` ("B001-000001"): de ahi
-- se deduce la serie y el correlativo que exige SUNAT.
UPDATE venta
SET serie = UPPER(SUBSTRING_INDEX(numero_documento, '-', 1)),
    correlativo = CAST(SUBSTRING_INDEX(numero_documento, '-', -1) AS UNSIGNED)
WHERE numero_documento LIKE '%-%'
  AND (serie = '' OR correlativo = 0);

-- Las ventas que no tenian numeracion (numero_documento sin serie-correlativo)
-- se agrupan en la serie MIG para no chocar con las series de SUNAT ni con el
-- indice unico (serie, correlativo).
SET @migrado := (SELECT COALESCE(MAX(correlativo), 0) FROM venta WHERE serie = 'MIG');
UPDATE venta
SET correlativo = @migrado := @migrado + 1,
    serie = 'MIG'
WHERE serie = '' OR correlativo = 0 OR serie IS NULL;

-- Las ventas migradas no tienen XML firmado ni hash: no son comprobantes
-- electronicos validos, solo historicos.
UPDATE venta SET estado = 'MIGRADO' WHERE hash_comprobante IS NULL AND estado = 'EMITIDO';

CALL agregar_indice_si_no_existe('venta', 'uk_venta_serie_correlativo', '`serie`, `correlativo`', TRUE);

-- ----------------------------------------------------------------------------
-- 3. Items del comprobante (tabla detalle_venta)
--    precio_unitario : Precio de venta unitario CON IGV (catálogo del producto)
--    valor_unitario  : Valor unitario SIN IGV (base imponible del item)
--    subtotal        : Monto total del item (base + IGV del item)
-- ----------------------------------------------------------------------------
CALL agregar_columna_si_no_existe('detalle_venta', 'codigo_item',    'VARCHAR(50)  NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('detalle_venta', 'descripcion',    'VARCHAR(500) NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('detalle_venta', 'unidad_medida',  'VARCHAR(5)   NOT NULL DEFAULT ''NIU''');
CALL agregar_columna_si_no_existe('detalle_venta', 'valor_unitario', 'DECIMAL(12,6) NOT NULL DEFAULT 0.000000');
CALL agregar_columna_si_no_existe('detalle_venta', 'afectacion_igv', 'VARCHAR(2)   NOT NULL DEFAULT ''01''');
CALL agregar_columna_si_no_existe('detalle_venta', 'igv_item',       'DECIMAL(12,2) NOT NULL DEFAULT 0.00');
CALL agregar_columna_si_no_existe('detalle_venta', 'descuento',      'DECIMAL(12,2) NOT NULL DEFAULT 0.00');

-- ----------------------------------------------------------------------------
-- 4. Catalogo de productos (tabla producto)
--    afectacion_igv: 01=GRAVADA, 02=INAFECTA, 03=EXONERADA, 04=GRATUITA
-- ----------------------------------------------------------------------------
CALL agregar_columna_si_no_existe('producto', 'codigo',         'VARCHAR(50) NOT NULL DEFAULT ''''');
CALL agregar_columna_si_no_existe('producto', 'unidad_medida',  'VARCHAR(5)  NOT NULL DEFAULT ''NIU''');
CALL agregar_columna_si_no_existe('producto', 'afectacion_igv', 'VARCHAR(2)  NOT NULL DEFAULT ''01''');
-- Atributos dinamicos del catalogo (talla, color, conexion...).
CALL agregar_columna_si_no_existe('producto', 'atributos',      'JSON        NULL');

-- ----------------------------------------------------------------------------
-- 5. Parametros del emisor y series
-- ----------------------------------------------------------------------------
INSERT INTO parametro (id_parametro, valor, descripcion) VALUES
('RUC_EMISOR',              '20604567891', 'RUC del emisor de los comprobantes'),
('RAZON_SOCIAL_EMISOR',     'INNOVATION STORE S.A.C.', 'Razon social del emisor'),
('NOMBRE_COMERCIAL',        'TechZone', 'Nombre comercial del emisor'),
('DOMICILIO_FISCAL_EMISOR', 'Av. Javier Prado Este 4200, Of. 802', 'Domicilio fiscal del emisor'),
('UBIGEO_EMISOR',           '150131', 'Codigo ubigeo del domicilio fiscal del emisor'),
('DISTRITO_EMISOR',         'Santiago de Surco', 'Distrito del emisor'),
('PROVINCIA_EMISOR',        'Lima', 'Provincia del emisor'),
('DEPARTAMENTO_EMISOR',      'Lima', 'Departamento del emisor'),
('PAIS_EMISOR',             'PE', 'Pais ISO del emisor'),
('EMAIL_EMISOR',            'facturacion@techzone.pe', 'Correo para el envio del comprobante'),
('TELEFONO_EMISOR',         '5115551234', 'Telefono del emisor'),
('SERIE_BOLETA',            'B001', 'Serie de los comprobantes de venta (boleta)'),
('SERIE_FACTURA',           'F001', 'Serie de los comprobantes de venta (factura)'),
('LIMITE_BOLETA_SIN_RUC',   '700.00', 'Monto maximo de boleta sin RUC (SUNAT)'),
('URL_REPRESENTACION_IMPRESA', 'https://www.techzone.pe/comprobantes', 'URL donde el adquirente consulta la representacion impresa'),
('CLIENTE_MOSTRADOR',       'CLI001', 'Cliente por defecto de las ventas de mostrador'),
('PERSONA_MOSTRADOR',       'PER001', 'Persona por defecto de las ventas de mostrador')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- Correlativos iniciales alineados con las ventas ya migradas: se toma el
-- MAXIMO del correlativo historico de cada serie (no el COUNT).
INSERT INTO correlativo (serie, id_tipo_documento, ultimo_numero)
SELECT 'B001', 'TD001', COALESCE(MAX(correlativo), 0) FROM venta WHERE id_tipo_documento = 'TD001'
ON DUPLICATE KEY UPDATE ultimo_numero = GREATEST(ultimo_numero, VALUES(ultimo_numero));

INSERT INTO correlativo (serie, id_tipo_documento, ultimo_numero)
SELECT 'F001', 'TD002', COALESCE(MAX(correlativo), 0) FROM venta WHERE id_tipo_documento = 'TD002'
ON DUPLICATE KEY UPDATE ultimo_numero = GREATEST(ultimo_numero, VALUES(ultimo_numero));

-- Codigos de producto (SUNAT no los exige, pero mejora la trazabilidad del CDR).
UPDATE producto SET codigo = CONCAT('P', LPAD(id_producto, 3, '0')) WHERE codigo = '';

-- Los productos sin afectacion explicita se consideran gravados.
UPDATE producto SET afectacion_igv = '01' WHERE afectacion_igv = '';

-- ----------------------------------------------------------------------------
--  Fin: se eliminan los procedimientos auxiliares
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS agregar_columna_si_no_existe;
DROP PROCEDURE IF EXISTS agregar_indice_si_no_existe;
