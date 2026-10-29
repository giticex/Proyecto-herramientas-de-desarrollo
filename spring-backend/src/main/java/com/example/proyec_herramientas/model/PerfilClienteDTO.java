package com.example.proyec_herramientas.model;

import java.util.List;

// Datos que ve un CLIENTE de su propia ficha. No expone la contrasena ni
// informacion de otras cuentas.
public class PerfilClienteDTO {

    private String idCliente;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private List<DireccionDTO> direcciones;

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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public List<DireccionDTO> getDirecciones() {
        return direcciones;
    }

    public void setDirecciones(List<DireccionDTO> direcciones) {
        this.direcciones = direcciones;
    }

    public static class DireccionDTO {

        private String tipo;
        private String direccion;

        public DireccionDTO() {
        }

        public DireccionDTO(String tipo, String direccion) {
            this.tipo = tipo;
            this.direccion = direccion;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getDireccion() {
            return direccion;
        }

        public void setDireccion(String direccion) {
            this.direccion = direccion;
        }
    }
}
