-- Esquema minimo para las pruebas: solo las tablas que mapea spring-backend.
-- Espejo de database/script_mysql.sql. Se recrea en cada arranque de contexto.
DROP VIEW IF EXISTS historial_compras;
DROP TABLE IF EXISTS detalle_venta;
DROP TABLE IF EXISTS venta;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS persona;
DROP TABLE IF EXISTS medio_pago;
DROP TABLE IF EXISTS tipo_documento;
DROP TABLE IF EXISTS rol;

CREATE TABLE tipo_documento (
    id_documento VARCHAR(25)  NOT NULL,
    nombre      VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id_documento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medio_pago (
    id_medio_pago VARCHAR(10)  NOT NULL,
    descripcion   VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_medio_pago)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalogo de roles: ADMIN para el panel, CLIENTE para el comprador.
CREATE TABLE rol (
    id_rol      VARCHAR(15)  NOT NULL,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE persona (
    id_persona       VARCHAR(25) NOT NULL,
    nombre           VARCHAR(50) NOT NULL,
    apellido         VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE,
    correo           VARCHAR(100),
    telefono         VARCHAR(9),
    usuario          VARCHAR(50),
    PRIMARY KEY (id_persona),
    UNIQUE KEY uk_persona_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuario (
    id_usuario INT          NOT NULL AUTO_INCREMENT,
    usuario    VARCHAR(50)  NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    id_persona VARCHAR(25)  NOT NULL,
    id_rol     VARCHAR(15)  NOT NULL DEFAULT 'ADMIN',
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuario_usuario (usuario),
    KEY idx_usuario_id_persona (id_persona),
    KEY idx_usuario_id_rol (id_rol),
    CONSTRAINT fk_usuario_persona FOREIGN KEY (id_persona)
        REFERENCES persona (id_persona),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cliente (
    id_cliente VARCHAR(20)  NOT NULL,
    nombre     VARCHAR(50)  NOT NULL,
    apellido   VARCHAR(50)  NOT NULL,
    telefono   VARCHAR(15),
    contrasena VARCHAR(255) NOT NULL,
    correo     VARCHAR(100),
    id_rol     VARCHAR(15)  NOT NULL DEFAULT 'CLIENTE',
    UNIQUE KEY uk_cliente_correo (correo),
    PRIMARY KEY (id_cliente),
    KEY idx_cliente_id_rol (id_rol),
    CONSTRAINT fk_cliente_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE producto (
    id_producto INT           NOT NULL,
    nombre      VARCHAR(100)  NOT NULL,
    stock       INT           NOT NULL,
    precio      DECIMAL(10, 2) NOT NULL,
    categoria   VARCHAR(80)   NOT NULL,
    atributos   JSON          NULL,
    PRIMARY KEY (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE venta (
    id_venta          VARCHAR(36) NOT NULL,
    id_cliente        VARCHAR(20) NOT NULL,
    id_persona        VARCHAR(25) NOT NULL,
    id_tipo_documento VARCHAR(25) NOT NULL,
    numero_documento  VARCHAR(36) NOT NULL,
    id_medio_pago     VARCHAR(10) NOT NULL,
    fecha_emision     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_venta),
    KEY idx_venta_id_cliente (id_cliente),
    KEY idx_venta_id_persona (id_persona),
    KEY idx_venta_id_tipo_documento (id_tipo_documento),
    KEY idx_venta_id_medio_pago (id_medio_pago),
    KEY idx_venta_fecha_emision (fecha_emision),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente),
    CONSTRAINT fk_venta_persona FOREIGN KEY (id_persona)
        REFERENCES persona (id_persona),
    CONSTRAINT fk_venta_tipo_documento FOREIGN KEY (id_tipo_documento)
        REFERENCES tipo_documento (id_documento),
    CONSTRAINT fk_venta_medio_pago FOREIGN KEY (id_medio_pago)
        REFERENCES medio_pago (id_medio_pago)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalle_venta (
    id_detalle      INT           NOT NULL AUTO_INCREMENT,
    id_venta        VARCHAR(36)   NOT NULL,
    id_producto     INT           NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    subtotal        DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id_detalle),
    KEY idx_detalle_id_venta (id_venta),
    KEY idx_detalle_id_producto (id_producto),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (id_venta)
        REFERENCES venta (id_venta),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
