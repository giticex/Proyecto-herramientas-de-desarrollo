package com.example.proyec_herramientas.facturacion.model;

/**
 * Leyenda del comprobante (catalogo 15 de SUNAT).
 *
 * <p>Los codigos que se usan en la representacion impresa:</p>
 * <ul>
 *   <li>1000 Monto en letras (monto total del comprobante)</li>
 *   <li>1002 Transferencia gratuita</li>
 *   <li>2000 Detraccion (retained)</li>
 *   <li>3000 Detraccion (worked)</li>
 * </ul>
 *
 * <p>Los codigos de las leyendas fijas del emisor no estan catalogados: se
 * escriben con el codigo {@code MONEDA} y el texto del parametro
 * {@code LEYENDA_REPRESENTACION_IMPRESA}.</p>
 */
public class LeyendaDTO {

    private String codigo;
    private String texto;

    public LeyendaDTO() {
    }

    public LeyendaDTO(String codigo, String texto) {
        this.codigo = codigo;
        this.texto = texto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
