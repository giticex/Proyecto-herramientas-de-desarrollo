-- Esquema minimo para las pruebas: solo las tablas que mapea spring-backend.
-- Espejo de database/script_mysql.sql. Se recrea en cada arranque de contexto.
DROP VIEW IF EXISTS historial_compras;
DROP TABLE IF EXISTS credito;
DROP TABLE IF EXISTS devolucion_detalle;
DROP TABLE IF EXISTS devolucion;
DROP TABLE IF EXISTS detalle_venta;
DROP TABLE IF EXISTS venta;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS persona;
DROP TABLE IF EXISTS tienda;
DROP TABLE IF EXISTS medio_pago;
DROP TABLE IF EXISTS tipo_documento;
DROP TABLE IF EXISTS parametro;
DROP TABLE IF EXISTS correlativo;
DROP TABLE IF EXISTS rol;

CREATE TABLE parametro (
    id_parametro VARCHAR(30)  NOT NULL,
    valor        VARCHAR(50)  NOT NULL,
    descripcion  VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_parametro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

-- Catalogo de roles: ADMIN para el panel, VENDEDOR para el POS y CLIENTE
-- para el comprador.
CREATE TABLE rol (
    id_rol      VARCHAR(15)  NOT NULL,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tienda (
    id_tienda        INT          NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100) NOT NULL,
    rubro            VARCHAR(80)  NOT NULL,
    slug             VARCHAR(80)  NOT NULL,
    color_primario   VARCHAR(7)   NOT NULL DEFAULT '#0071e3',
    color_secundario VARCHAR(7)   NOT NULL DEFAULT '#1d1d1f',
    logo_url         VARCHAR(255),
    PRIMARY KEY (id_tienda),
    UNIQUE KEY uk_tienda_slug (slug)
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
    id_tienda  INT          NOT NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuario_usuario (usuario),
    KEY idx_usuario_id_persona (id_persona),
    KEY idx_usuario_id_rol (id_rol),
    KEY idx_usuario_id_tienda (id_tienda),
    CONSTRAINT fk_usuario_persona FOREIGN KEY (id_persona)
        REFERENCES persona (id_persona),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol),
    CONSTRAINT fk_usuario_tienda FOREIGN KEY (id_tienda)
        REFERENCES tienda (id_tienda)
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
    id_producto    INT           NOT NULL,
    nombre         VARCHAR(100)  NOT NULL,
    stock          INT           NOT NULL,
    precio         DECIMAL(10, 2) NOT NULL,
    categoria      VARCHAR(80)   NOT NULL,
    codigo         VARCHAR(50)   NOT NULL DEFAULT '',
    unidad_medida  VARCHAR(5)    NOT NULL DEFAULT 'NIU',
    afectacion_igv VARCHAR(2)    NOT NULL DEFAULT '01',
    atributos      JSON          NULL,
    id_tienda      INT           NOT NULL,
    PRIMARY KEY (id_producto),
    KEY idx_producto_id_tienda (id_tienda),
    CONSTRAINT fk_producto_tienda FOREIGN KEY (id_tienda)
        REFERENCES tienda (id_tienda)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cabecera del comprobante de pago electronico.
CREATE TABLE venta (
    id_venta          VARCHAR(36) NOT NULL,
    id_cliente        VARCHAR(20) NOT NULL,
    id_persona        VARCHAR(25) NOT NULL,
    id_tipo_documento VARCHAR(25) NOT NULL,
    numero_documento  VARCHAR(36) NOT NULL,
    id_medio_pago     VARCHAR(10) NOT NULL,
    fecha_emision     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    serie             VARCHAR(4)   NOT NULL DEFAULT '',
    correlativo       INT          NOT NULL DEFAULT 0,
    moneda            VARCHAR(3)   NOT NULL DEFAULT 'PEN',
    condicion_operacion VARCHAR(2) NOT NULL DEFAULT '01',
    forma_pago        VARCHAR(2)   NOT NULL DEFAULT '01',
    total_venta            DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_gravada          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_inafecta         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_exonerada        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_gratuita         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_igv              DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_descuentos       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    monto_total_credito    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    receptor_tipo_documento VARCHAR(1)   NOT NULL DEFAULT '',
    receptor_num_documento  VARCHAR(15)  NOT NULL DEFAULT '',
    receptor_razon_social   VARCHAR(200) NOT NULL DEFAULT '',
    receptor_direccion      VARCHAR(200) NOT NULL DEFAULT '',
    receptor_ubigeo         VARCHAR(6)   NOT NULL DEFAULT '',
    receptor_distrito       VARCHAR(80)  NOT NULL DEFAULT '',
    receptor_provincia      VARCHAR(80)  NOT NULL DEFAULT '',
    receptor_departamento   VARCHAR(80)  NOT NULL DEFAULT '',
    receptor_pais           VARCHAR(2)   NOT NULL DEFAULT 'PE',
    entrega_direccion       VARCHAR(200) NOT NULL DEFAULT '',
    entrega_ubigeo          VARCHAR(6)   NOT NULL DEFAULT '',
    email_contacto          VARCHAR(100) NOT NULL DEFAULT '',
    hash_comprobante        VARCHAR(64)  NULL,
    codigo_qr               VARCHAR(500) NULL,
    xml_name                VARCHAR(120) NULL,
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'EMITIDO',
    estado_sunat            VARCHAR(20)  NULL,
    cdR_summary             VARCHAR(500) NULL,
    leyendas                TEXT         NULL,
    observacion             VARCHAR(500) NOT NULL DEFAULT '',
    PRIMARY KEY (id_venta),
    UNIQUE KEY uk_venta_serie_correlativo (serie, correlativo),
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

-- Items del comprobante.
--   precio_unitario : precio de venta unitario CON IGV
--   valor_unitario  : valor unitario SIN IGV (base imponible del item)
--   subtotal        : monto total del item (base + IGV del item)
CREATE TABLE detalle_venta (
    id_detalle      INT           NOT NULL AUTO_INCREMENT,
    id_venta        VARCHAR(36)   NOT NULL,
    id_producto     INT           NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    subtotal        DECIMAL(12, 2) NOT NULL,
    valor_unitario  DECIMAL(12, 6) NOT NULL DEFAULT 0.000000,
    igv_item        DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    descuento       DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    codigo_item     VARCHAR(50)   NOT NULL DEFAULT '',
    descripcion     VARCHAR(500)  NOT NULL DEFAULT '',
    unidad_medida   VARCHAR(5)    NOT NULL DEFAULT 'NIU',
    afectacion_igv  VARCHAR(2)    NOT NULL DEFAULT '01',
    PRIMARY KEY (id_detalle),
    KEY idx_detalle_id_venta (id_venta),
    KEY idx_detalle_id_producto (id_producto),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (id_venta)
        REFERENCES venta (id_venta),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Devolucion sobre una venta ya emitida. Guarda el usuario que la registro
-- y el total devuelto; las lineas van en devolucion_detalle.
CREATE TABLE devolucion (
    id_devolucion VARCHAR(36)  NOT NULL,
    id_venta      VARCHAR(36)  NOT NULL,
    fecha         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo        VARCHAR(300) NOT NULL DEFAULT '',
    total         DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    usuario       VARCHAR(50)  NOT NULL DEFAULT '',
    id_credito    VARCHAR(36)  NULL,
    PRIMARY KEY (id_devolucion),
    KEY idx_devolucion_id_venta (id_venta),
    CONSTRAINT fk_devolucion_venta FOREIGN KEY (id_venta)
        REFERENCES venta (id_venta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lineas devueltas: precio y descripcion congelados del detalle original.
CREATE TABLE devolucion_detalle (
    id_detalle      INT            NOT NULL AUTO_INCREMENT,
    id_devolucion   VARCHAR(36)    NOT NULL,
    id_producto     INT            NOT NULL,
    cantidad        INT            NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    subtotal        DECIMAL(12, 2) NOT NULL,
    descripcion     VARCHAR(500)   NOT NULL DEFAULT '',
    PRIMARY KEY (id_detalle),
    KEY idx_devolucion_detalle_devolucion (id_devolucion),
    KEY idx_devolucion_detalle_producto (id_producto),
    CONSTRAINT fk_devolucion_detalle_devolucion FOREIGN KEY (id_devolucion)
        REFERENCES devolucion (id_devolucion),
    CONSTRAINT fk_devolucion_detalle_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Credito a favor generado por una devolucion. Referencia la venta de origen
-- sin FK dura: la venta original jamas se modifica ni se elimina.
CREATE TABLE credito (
    id_credito              VARCHAR(36)  NOT NULL,
    codigo                  VARCHAR(20)  NOT NULL,
    id_venta_origen         VARCHAR(36)  NOT NULL,
    numero_documento_origen VARCHAR(36)  NOT NULL,
    cliente_nombre          VARCHAR(200) NOT NULL DEFAULT '',
    dni_cliente             VARCHAR(15)  NOT NULL DEFAULT '',
    monto_total             DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    monto_disponible        DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'Vigente',
    fecha                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_credito),
    UNIQUE KEY uk_credito_codigo (codigo),
    KEY idx_credito_venta_origen (id_venta_origen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Numeracion correlativa por serie. El correlativo no puede calcularse con
-- COUNT(): dos ventas simultaneas lo repetirian. Se bloquea la fila con
-- SELECT ... FOR UPDATE.
CREATE TABLE correlativo (
    serie             VARCHAR(4)  NOT NULL,
    id_tipo_documento VARCHAR(25) NOT NULL,
    ultimo_numero     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (serie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
