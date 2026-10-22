USE proyecto;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS rol (
    id_rol      VARCHAR(15)  NOT NULL,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO rol (id_rol, nombre, descripcion) VALUES
('ADMIN', 'Administrador', 'Gestiona productos, historial general, clientes y alertas de stock'),
('CLIENTE', 'Cliente', 'Compra en la tienda, ve su carrito y sus compras')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

SET @existe_usuario := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'id_rol');
SET @sql := IF(@existe_usuario = 0,
    'ALTER TABLE usuario ADD COLUMN id_rol VARCHAR(15) NOT NULL DEFAULT ''CLIENTE''', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE usuario SET id_rol = 'ADMIN' WHERE usuario = 'admin';
UPDATE usuario SET id_rol = 'CLIENTE' WHERE id_rol IS NULL OR id_rol = '';

SET @fk_usuario := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuario' AND CONSTRAINT_NAME = 'fk_usuario_rol');
SET @sql := IF(@fk_usuario = 0,
    'ALTER TABLE usuario ADD CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @existe_cliente := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cliente' AND COLUMN_NAME = 'id_rol');
SET @sql := IF(@existe_cliente = 0,
    'ALTER TABLE cliente ADD COLUMN id_rol VARCHAR(15) NOT NULL DEFAULT ''CLIENTE''', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE cliente SET id_rol = 'CLIENTE' WHERE id_rol IS NULL OR id_rol = '';

SET @fk_cliente := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cliente' AND CONSTRAINT_NAME = 'fk_cliente_rol');
SET @sql := IF(@fk_cliente = 0,
    'ALTER TABLE cliente ADD CONSTRAINT fk_cliente_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
