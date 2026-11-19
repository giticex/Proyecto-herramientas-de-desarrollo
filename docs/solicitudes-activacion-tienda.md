# Solicitud y activación de una tienda

La aplicación ofrece una demo interactiva en `/demo`. La demo usa estado temporal del navegador y no registra ventas, cambia inventario real ni genera comprobantes fiscales.

## Preparación en MySQL 8

1. Haz un respaldo verificable. Para una base existente aplica, en este orden, las migraciones de ventas por tienda, facturación por tienda y solicitudes. Prueba primero en una copia.
2. Para instalaciones nuevas usa la carga inicial del proyecto. Evita ejecutarla sobre bases existentes, ya que incluye limpieza de tablas.
3. Arranca Spring Boot con la conexión a la copia y `spring.jpa.hibernate.ddl-auto=validate`. Mantén la inicialización SQL automática desactivada.
4. Para crear la primera cuenta maestra pasa `APP_MAESTRO_CORREO` y `APP_MAESTRO_CLAVE` mediante el entorno privado. Usa un correo dedicado y una contraseña de al menos 12 caracteres. Spring guarda BCrypt al primer arranque y conserva una cuenta maestra existente. Quita esas variables tras el aprovisionamiento. Nunca configures estas variables en React.
5. Arranca `frontend-react` con `npm ci` y `npm run dev`. Usa una ventana de incógnito o un perfil de navegador distinto para cada una de las dos cuentas.

## Demostración

1. Visita la página pública, abre la demo e intenta agregar y vender un producto.
2. En **Crear mi tienda** completa los datos ficticios, indica dos celulares de prueba distintos y confirma la tarjeta ficticia incorporada. No se introducen datos bancarios ni se realiza un cobro.
3. El solicitante ve su estado en `/solicitud-tienda`. El maestro entra por el login común y revisa la bandeja en `/maestro/solicitudes`.
4. Si el maestro solicita información, el solicitante puede responder en la misma pantalla. Una aprobación crea la tienda privada y actualiza el rol de la cuenta existente.
5. El solicitante pulsa **Actualizar** y, tras la aprobación, **Configurar mi tienda**. Inicia sesión con el mismo correo y contraseña. Puede preparar su marca e inventario y publicar voluntariamente el catálogo.
6. Comprueba que la URL pública muestra una tienda inexistente hasta publicar. Tiendas que ya existían quedan publicadas tras migrar.

La bandeja se actualiza desde el botón **Actualizar**. El sistema conserva la referencia simulada, dos teléfonos de contacto y los últimos cuatro dígitos del DNI. El DNI no se contrasta con un proveedor de identidad, los teléfonos no actúan como factor de autenticación y no se envían correos.
