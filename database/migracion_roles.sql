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
('VENDEDOR', 'Vendedor', 'Opera el punto de venta: consulta catalogo, clientes y registra ventas de su tienda'),
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

-- Cuenta demo del POS (SOLO DESARROLLO): usuario "vendedor", clave 123456,
-- asignada a la primera tienda registrada (0 si la base aun no tiene tiendas).
-- El hash se migra a BCrypt en el primer login, igual que las cuentas de la
-- carga inicial.
SET @HASH_123456 = '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92';
SET @id_tienda_demo = COALESCE((SELECT id_tienda FROM tienda ORDER BY id_tienda LIMIT 1), 0);

INSERT INTO persona (id_persona, nombre, apellido, correo, telefono, usuario)
VALUES ('PERVEN', 'Vendedor', 'Demo', 'vendedor@softplex.com', '999999998', 'vendedor')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), correo = VALUES(correo), usuario = VALUES(usuario);

INSERT INTO usuario (usuario, contrasena, id_persona, id_rol, id_tienda)
VALUES ('vendedor', @HASH_123456, 'PERVEN', 'VENDEDOR', @id_tienda_demo)
ON DUPLICATE KEY UPDATE id_persona = VALUES(id_persona), id_rol = 'VENDEDOR',
    id_tienda = VALUES(id_tienda);
