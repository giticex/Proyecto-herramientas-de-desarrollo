-- Ejecutar una vez sobre el esquema existente, antes de iniciar el backend
-- actualizado. El cliente mysql debe conectarse a la base que se quiere migrar.
-- Para instalaciones nuevas, script_mysql.sql ya incorpora los SKU.
ALTER TABLE producto ADD COLUMN sku VARCHAR(40) NULL AFTER categoria;

UPDATE producto
SET sku = CONCAT('SKU-', id_producto)
WHERE sku IS NULL;

ALTER TABLE producto
    MODIFY COLUMN sku VARCHAR(40) NOT NULL,
    ADD CONSTRAINT uq_producto_tienda_sku UNIQUE (id_tienda, sku);
