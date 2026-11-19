-- Ejecutar en la base seleccionada, después de las migraciones de ventas y facturación.
ALTER TABLE usuario MODIFY COLUMN id_tienda INT NULL;
SET @solicitudes_add_publicada = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tienda' AND COLUMN_NAME = 'publicada'),
    'SELECT 1',
    'ALTER TABLE tienda ADD COLUMN publicada BOOLEAN NOT NULL DEFAULT TRUE'
);
PREPARE solicitudes_stmt FROM @solicitudes_add_publicada;
EXECUTE solicitudes_stmt;
DEALLOCATE PREPARE solicitudes_stmt;

INSERT IGNORE INTO rol (id_rol, nombre, descripcion) VALUES
('SOLICITANTE', 'Solicitante', 'Consulta el estado de su solicitud de tienda'),
('SUPER_ADMIN', 'Administrador de plataforma', 'Revisa y activa solicitudes de negocio');

CREATE TABLE IF NOT EXISTS solicitud_tienda (
    id_solicitud VARCHAR(36) NOT NULL,
    id_usuario INT NOT NULL,
    negocio VARCHAR(100) NOT NULL,
    rubro VARCHAR(80) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    dni_ultimos4 CHAR(4) NOT NULL,
    telefono_alternativo VARCHAR(9) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    referencia_demo VARCHAR(41) NOT NULL,
    mensaje VARCHAR(500),
    respuesta VARCHAR(500),
    creada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revisada TIMESTAMP NULL,
    revisor VARCHAR(50),
    PRIMARY KEY (id_solicitud),
    UNIQUE KEY uk_solicitud_cuenta (id_usuario),
    UNIQUE KEY uk_solicitud_slug (slug),
    KEY idx_solicitud_colas (estado, creada),
    CONSTRAINT fk_solicitud_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('PENDIENTE','REQUIERE_DATOS','APROBADA','RECHAZADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
