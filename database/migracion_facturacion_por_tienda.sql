-- Aplicar después de migracion_venta_tienda.sql y con la aplicación detenida.
-- El historial previo conserva sus filas y la numeración anterior.
CREATE TABLE IF NOT EXISTS emisor_tienda (
    id_tienda   INT        NOT NULL,
    datos       JSON       NOT NULL,
    serie_boleta VARCHAR(4) NOT NULL,
    serie_factura VARCHAR(4) NOT NULL,
    PRIMARY KEY (id_tienda),
    CONSTRAINT fk_emisor_tienda FOREIGN KEY (id_tienda) REFERENCES tienda (id_tienda)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS correlativo_emisor (
    ruc_emisor VARCHAR(11) NOT NULL,
    id_tipo_documento VARCHAR(25) NOT NULL,
    serie VARCHAR(4) NOT NULL,
    ultimo_numero INT NOT NULL,
    PRIMARY KEY (ruc_emisor, id_tipo_documento, serie),
    CONSTRAINT fk_correlativo_emisor_tipo FOREIGN KEY (id_tipo_documento)
        REFERENCES tipo_documento (id_documento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrar_facturacion_tienda $$
CREATE PROCEDURE migrar_facturacion_tienda()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND COLUMN_NAME = 'ruc_emisor') THEN
        ALTER TABLE venta ADD COLUMN ruc_emisor VARCHAR(11) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND COLUMN_NAME = 'emisor_snapshot') THEN
        ALTER TABLE venta ADD COLUMN emisor_snapshot JSON NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND COLUMN_NAME = 'xml_emitido') THEN
        ALTER TABLE venta ADD COLUMN xml_emitido LONGTEXT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND COLUMN_NAME = 'serie_legacy') THEN
        ALTER TABLE venta ADD COLUMN serie_legacy VARCHAR(4)
            GENERATED ALWAYS AS (CASE WHEN ruc_emisor IS NULL THEN serie ELSE NULL END) VIRTUAL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND COLUMN_NAME = 'huella_operacion') THEN
        ALTER TABLE venta ADD COLUMN huella_operacion VARCHAR(64) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND INDEX_NAME = 'uk_venta_legacy') THEN
        ALTER TABLE venta ADD UNIQUE KEY uk_venta_legacy (serie_legacy, correlativo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND INDEX_NAME = 'uk_venta_emisor_documento') THEN
        ALTER TABLE venta ADD UNIQUE KEY uk_venta_emisor_documento (ruc_emisor, id_tipo_documento, serie, correlativo);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND INDEX_NAME = 'uk_venta_serie_correlativo') THEN
        ALTER TABLE venta DROP INDEX uk_venta_serie_correlativo;
    END IF;
END $$
CALL migrar_facturacion_tienda() $$
DROP PROCEDURE migrar_facturacion_tienda $$
DELIMITER ;
