package com.example.proyec_herramientas.model;

import java.math.BigDecimal;

/**
 * Fila del historial de ventas del panel de administracion.
 *
 * <p>Solo expone lo que necesita la tabla: comprobante, fecha, cliente, total
 * y estado; los detalles y los datos fiscales completos quedan en la venta y
 * se consultan desde el comprobante.</p>
 */
public record VentaResumenDTO(
        String idVenta,
        String numeroDocumento,
        String serie,
        int correlativo,
        String fechaEmision,
        String cliente,
        String documentoCliente,
        BigDecimal total,
        String estado) {
}
