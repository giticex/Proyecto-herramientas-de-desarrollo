package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ActualizarUsuarioStaffRequest;
import com.example.proyec_herramientas.model.CrearUsuarioStaffRequest;
import com.example.proyec_herramientas.model.EstadoUsuarioStaffRequest;
import com.example.proyec_herramientas.model.PersonalTiendaDTO;
import com.example.proyec_herramientas.model.UsuarioStaffDTO;

/**
 * Gestion del personal VENDEDOR de la propia tienda. A diferencia de la
 * consola global, aqui la empresa nunca llega como parametro: la resuelve el
 * controller desde el token del administrador.
 */
public interface AdminPersonalService {

    PersonalTiendaDTO listar(int idTienda);

    UsuarioStaffDTO crear(int idTienda, CrearUsuarioStaffRequest request);

    UsuarioStaffDTO actualizar(int idTienda, int idUsuario,
            ActualizarUsuarioStaffRequest request);

    UsuarioStaffDTO cambiarEstado(int idTienda, int idUsuario,
            EstadoUsuarioStaffRequest request);
}
