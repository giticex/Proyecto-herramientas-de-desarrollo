-- ============================================================================
--  MIGRACION: rol global SUPER_ADMIN y su cuenta inicial
--  Idempotente: se puede ejecutar varias veces sin duplicar filas.
--
--  Cuenta:   superadm@gmail.com (usuario "superadm")
--  Clave:    Sup3rAdm!n2026#   (solo se guarda el hash BCrypt de abajo)
--
--  usuario.id_tienda es NOT NULL en el esquema historico. Para esta cuenta
--  global la columna solo cumple la restriccion: el login del SUPER_ADMIN
--  omite la tienda del token y de la respuesta, asi que ninguna operacion
--  queda acotada a esa tienda.
--
--  Si el correo superadm@gmail.com ya existiera en persona con otro
--  id_persona, el INSERT de usuario fallaria por la FK: revisa antes con
--  SELECT id_persona FROM persona WHERE correo = 'superadm@gmail.com';
-- ============================================================================

INSERT INTO rol (id_rol, nombre, descripcion) VALUES
('SUPER_ADMIN', 'Super Administrador', 'Supervisa todas las empresas de la plataforma')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT INTO persona (id_persona, nombre, apellido, fecha_nacimiento, correo, telefono, usuario) VALUES
('PERSUP', 'Super', 'Administrador', NULL, 'superadm@gmail.com', '999999997', 'superadm')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), apellido = VALUES(apellido),
    correo = VALUES(correo), telefono = VALUES(telefono), usuario = VALUES(usuario);

INSERT INTO usuario (usuario, contrasena, id_persona, id_rol, id_tienda) VALUES
('superadm', '$2a$10$hkSAKFQF8rVHGeYhM/mBw.nWrEsXKqKcxx5hxPSZr4B6wJK1OUvYG', 'PERSUP', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE contrasena = VALUES(contrasena), id_persona = VALUES(id_persona),
    id_rol = VALUES(id_rol), id_tienda = VALUES(id_tienda);
