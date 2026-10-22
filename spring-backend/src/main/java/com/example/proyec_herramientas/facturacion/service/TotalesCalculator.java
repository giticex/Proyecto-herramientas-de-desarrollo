package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.DetalleComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.TotalesDTO;
import com.example.proyec_herramientas.facturacion.model.ItemComprobanteDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula los importes fiscales de cada item y los totales del comprobante.
 *
 * <p>Reglas aplicadas (SUNAT UBL 2.1):</p>
 * <ul>
 *   <li>Item gravado: el precio de venta del catalogo incluye IGV, por lo que
 *       el valor unitario (base) es precio / (1 + IGV) y la base de la linea es
 *       valor unitario x cantidad. El IGV del item es la diferencia entre el
 *       importe cobrado (precio x cantidad) y la base imponible.</li>
 *   <li>Item inafecto o exonerado: la base es el precio de venta y el IGV es
 *       cero.</li>
 *   <li>Item gratuito: la base es el valor referencial (precio de venta con
 *       IGV incluido) y el IGV es cero. Esa suma alimenta el total de
 *       "Operaciones Gratuitas" y forma parte del total de la venta.</li>
 * </ul>
 */
@Service
public class TotalesCalculator {

    private static final int ESCALA_IMPORTE = 2;
    private static final int ESCALA_VALOR_UNITARIO = 6;

    /**
     * Calcula base, IGV y monto total de cada item, y devuelve los totales
     * del comprobante.
     */
    public TotalesDTO calcular(List<DetalleComprobanteDTO> detalles, BigDecimal igvPorcentaje) {
        BigDecimal gravada = BigDecimal.ZERO;
        BigDecimal inafecta = BigDecimal.ZERO;
        BigDecimal exonerada = BigDecimal.ZERO;
        BigDecimal gratuita = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal descuentos = BigDecimal.ZERO;

        BigDecimal factor = BigDecimal.ONE.add(igvPorcentaje.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));

        for (DetalleComprobanteDTO detalle : detalles) {
            BigDecimal cantidad = detalle.getCantidad() == null ? BigDecimal.ONE : detalle.getCantidad();
            BigDecimal precio = detalle.getPrecioUnitario() == null ? BigDecimal.ZERO : detalle.getPrecioUnitario();
            BigDecimal descuento = detalle.getDescuento() == null ? BigDecimal.ZERO : detalle.getDescuento();
            // Importe que paga el cliente por la linea (el precio del catalogo
            // ya incluye el IGV).
            BigDecimal importeBruto = precio.multiply(cantidad).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
            BigDecimal importe = importeBruto.subtract(descuento).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);

            BigDecimal base;
            BigDecimal igvItem = BigDecimal.ZERO;
            if (ItemComprobanteDTO.AFECTACION_GRAVADA.equals(detalle.getAfectacionIgv())) {
                BigDecimal valorUnitario = precio.divide(factor, ESCALA_VALOR_UNITARIO, RoundingMode.HALF_UP);
                detalle.setValorUnitario(valorUnitario);
                base = valorUnitario.multiply(cantidad).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
                // Con el precio unitario con IGV (Catalogo 16, codigo 01) el IGV
                // de la linea es la diferencia entre el importe cobrado y la base
                // imponible: asi base + IGV = importe de la linea.
                igvItem = importe.subtract(base).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
            } else {
                // Inafecta, exonerada y gratuita: el precio de venta es la base.
                detalle.setValorUnitario(precio.setScale(ESCALA_VALOR_UNITARIO, RoundingMode.HALF_UP));
                base = importe;
            }

            detalle.setBase(base);
            detalle.setIgv(igvItem);
            detalle.setTotalItem(base.add(igvItem).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP));

            igv = igv.add(igvItem);
            descuentos = descuentos.add(descuento);

            switch (detalle.getAfectacionIgv() == null ? "" : detalle.getAfectacionIgv()) {
                case ItemComprobanteDTO.AFECTACION_INAFECTA:
                    inafecta = inafecta.add(base);
                    break;
                case ItemComprobanteDTO.AFECTACION_EXONERADA:
                    exonerada = exonerada.add(base);
                    break;
                case ItemComprobanteDTO.AFECTACION_GRATUITA:
                    gratuita = gratuita.add(base);
                    break;
                default:
                    gravada = gravada.add(base);
                    break;
            }
        }

        TotalesDTO totales = new TotalesDTO();
        totales.setOpGravada(copia(gravada));
        totales.setOpInafecta(copia(inafecta));
        totales.setOpExonerada(copia(exonerada));
        totales.setOpGratuita(copia(gratuita));
        totales.setIgv(copia(igv));
        totales.setTotalDescuentos(copia(descuentos));

        BigDecimal totalVenta = gravada.add(inafecta).add(exonerada).add(gratuita).add(igv)
                .setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
        totales.setTotalVenta(totalVenta);
        // El descuento ya descuenta de la base de cada linea, por eso el monto
        // a pagar coincide con el total de la venta.
        totales.setMontoTotalPagar(totalVenta);
        return totales;
    }

    private BigDecimal copia(BigDecimal valor) {
        return valor.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
    }
}
