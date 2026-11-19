-- Aplicar con la aplicación detenida y sobre el esquema seleccionado.
-- Una venta histórica debe tener productos de exactamente una tienda.
DELIMITER $$

DROP PROCEDURE IF EXISTS migrar_venta_tienda $$
CREATE PROCEDURE migrar_venta_tienda()
BEGIN
    IF EXISTS (
        SELECT v.id_venta
        FROM venta v
        LEFT JOIN detalle_venta d ON d.id_venta = v.id_venta
        LEFT JOIN producto p ON p.id_producto = d.id_producto
        GROUP BY v.id_venta
        HAVING COUNT(DISTINCT p.id_tienda) <> 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Hay ventas sin tienda inequívoca. Revisar sus detalles antes de migrar.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND COLUMN_NAME = 'id_tienda'
    ) THEN
        ALTER TABLE venta ADD COLUMN id_tienda INT NULL AFTER id_venta;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM venta v
        JOIN detalle_venta d ON d.id_venta = v.id_venta
        JOIN producto p ON p.id_producto = d.id_producto
        WHERE v.id_tienda IS NOT NULL AND v.id_tienda <> p.id_tienda
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Una venta ya tiene una tienda diferente a la de sus productos.';
    END IF;

    UPDATE venta v
    JOIN (
        SELECT d.id_venta, MIN(p.id_tienda) AS id_tienda
        FROM detalle_venta d
        JOIN producto p ON p.id_producto = d.id_producto
        GROUP BY d.id_venta
    ) origen ON origen.id_venta = v.id_venta
    SET v.id_tienda = origen.id_tienda
    WHERE v.id_tienda IS NULL;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND INDEX_NAME = 'idx_venta_tienda_fecha'
    ) THEN
        ALTER TABLE venta ADD KEY idx_venta_tienda_fecha (id_tienda, fecha_emision);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'venta' AND CONSTRAINT_NAME = 'fk_venta_tienda'
    ) THEN
        ALTER TABLE venta ADD CONSTRAINT fk_venta_tienda FOREIGN KEY (id_tienda) REFERENCES tienda (id_tienda);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'venta'
          AND COLUMN_NAME = 'id_tienda' AND IS_NULLABLE = 'YES'
    ) THEN
        ALTER TABLE venta MODIFY COLUMN id_tienda INT NOT NULL;
    END IF;
END $$

CALL migrar_venta_tienda() $$
DROP PROCEDURE migrar_venta_tienda $$

DELIMITER ;
