package com.example.proyec_herramientas.model;

/**
 * Resultado de la busqueda de clientes del punto de venta. Cuando el documento
 * no aparece en la tienda, {@code encontrado} viaja en false para que el POS
 * ofrezca registrar la ficha rapida sin tratar la respuesta como un error.
 *
 * <p>{@code origen} indica de donde salieron los datos: TIENDA (ficha propia
 * de la empresa), HISTORIAL (un comprobante anterior de la tienda) o NINGUNO.</p>
 */
public class ClienteBusquedaDTO {

    public static final String ORIGEN_TIENDA = "TIENDA";
    public static final String ORIGEN_HISTORIAL = "HISTORIAL";
    public static final String ORIGEN_REGISTRADO = "REGISTRADO";
    public static final String ORIGEN_NINGUNO = "NINGUNO";

    private boolean encontrado;
    private String idCliente;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correo;
    private String origen = ORIGEN_NINGUNO;

    public static ClienteBusquedaDTO noEncontrado(String tipoDocumento, String numeroDocumento) {
        ClienteBusquedaDTO dto = new ClienteBusquedaDTO();
        dto.setTipoDocumento(tipoDocumento);
        dto.setNumeroDocumento(numeroDocumento);
        return dto;
    }

    public boolean isEncontrado() {
        return encontrado;
    }

    public void setEncontrado(boolean encontrado) {
        this.encontrado = encontrado;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }
}
