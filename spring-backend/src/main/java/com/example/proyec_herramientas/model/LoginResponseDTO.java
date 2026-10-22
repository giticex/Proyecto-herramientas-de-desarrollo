package com.example.proyec_herramientas.model;

// Respuesta de POST /auth/login. El rol viaja en el cuerpo para que el
// frontend sepa a que panel puede entrar, y tambien va dentro del token para
// que el backend no tenga que volver a consultarlo en cada peticion.
public class LoginResponseDTO {

    private String token;
    private String usuario;
    private String rol;
    private String nombreRol;
    private String idCliente;
    private String nombre;
    private String apellido;
    private String correo;

    // Tienda que administra la cuenta (solo ADMIN). El comprador la tiene en
    // null; su tienda se resuelve por el slug del catalogo.
    private com.example.proyec_herramientas.model.TiendaDTO tienda;
    private Integer idTienda;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, String usuario, String rol, String nombreRol, String idCliente,
            String nombre, String apellido, String correo) {
        this.token = token;
        this.usuario = usuario;
        this.rol = rol;
        this.nombreRol = nombreRol;
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public com.example.proyec_herramientas.model.TiendaDTO getTienda() {
        return tienda;
    }

    public void setTienda(com.example.proyec_herramientas.model.TiendaDTO tienda) {
        this.tienda = tienda;
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }
}
