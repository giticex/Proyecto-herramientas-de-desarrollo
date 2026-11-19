# Facturación por tienda

## Activación

1. Haz una copia verificable de MySQL y detén las aplicaciones que escriban en el esquema mientras lo actualizas.
2. Si la base ya tiene datos, aplica `database/migracion_venta_tienda.sql` y después `database/migracion_facturacion_por_tienda.sql` en el esquema seleccionado. El script inicial ya incluye las tablas y columnas para una instalación nueva.
3. Inicia el backend de esta rama con las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` de desarrollo. Hibernate valida el esquema al iniciar.
4. En el panel de la tienda, abre **Facturación** y guarda el emisor, su domicilio fiscal y sus series de boleta y factura. Cada administrador configura su propia tienda.

La compra en una tienda que todavía carece de configuración fiscal queda pendiente de activación. Los datos históricos permanecen en MySQL. Para los comprobantes nuevos, se guarda el emisor y el XML original junto con la venta.

## Prueba funcional

- Configura dos tiendas con RUC distintos y la misma serie. Cada emisor debe administrar su propia numeración. Las facturas usan su serie y contador separados de las boletas.
- Realiza una compra desde cada catálogo y verifica el RUC del emisor en el comprobante. Cambia la configuración de una tienda y vuelve a descargar el XML anterior. Ambos archivos deben coincidir.
- Envía dos veces la misma solicitud autenticada con el mismo encabezado `Idempotency-Key` de tipo UUID. Ambas respuestas deben identificar la misma venta y el stock debe descontarse una sola vez. Reusar esa clave con otros datos debe devolver HTTP 409.
- Intenta consultar el comprobante desde un usuario de otra tienda. La API debe devolver HTTP 404.
- La confirmación de compra recupera el comprobante desde el servidor. El medio de pago seleccionado se registra con el pedido. El estado de emisión electrónica disponible en este flujo es `GENERADO`.

Las pruebas principales de esta fase están en `ComprobanteTiendasTest.java`.

