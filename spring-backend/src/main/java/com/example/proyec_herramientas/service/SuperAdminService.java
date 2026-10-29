package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ActualizarPlanRequest;
import com.example.proyec_herramientas.model.ActualizarUsuarioStaffRequest;
import com.example.proyec_herramientas.model.AdminTiendaDTO;
import com.example.proyec_herramientas.model.CrearUsuarioStaffRequest;
import com.example.proyec_herramientas.model.EmpresaResumenDTO;
import com.example.proyec_herramientas.model.EstadoUsuarioStaffRequest;
import com.example.proyec_herramientas.model.PaginaAuditoriaDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.model.PlanTiendaDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.RestablecerPasswordRequest;
import com.example.proyec_herramientas.model.SuperAdminEmpresaDTO;
import com.example.proyec_herramientas.model.SuperAdminResumenDTO;
import com.example.proyec_herramientas.model.UsuarioStaffDTO;

import java.util.List;

/**
 * Consultas globales de la plataforma. Solo las consume el SUPER_ADMIN: no
 * estan acotadas por la tienda del token, sino por la empresa que se indique
 * como parametro.
 */
public interface SuperAdminService {

    SuperAdminResumenDTO resumen();

    List<EmpresaResumenDTO> empresas();

    SuperAdminEmpresaDTO dashboard(int idTienda);

    List<ProductoDTO> productos(int idTienda);

    PaginaVentasDTO comprobantes(int idTienda, String busqueda, int pagina, int tamanio);

    EmpresaResumenDTO cambiarEstado(int idTienda, String estado);

    PlanTiendaDTO plan(int idTienda);

    PlanTiendaDTO actualizarPlan(int idTienda, ActualizarPlanRequest request);

    AdminTiendaDTO admin(int idTienda);

    AdminTiendaDTO restablecerPasswordAdmin(int idTienda, RestablecerPasswordRequest request);

    List<UsuarioStaffDTO> usuarios(int idTienda);

    UsuarioStaffDTO crearUsuario(int idTienda, CrearUsuarioStaffRequest request);

    UsuarioStaffDTO actualizarUsuario(int idTienda, int idUsuario,
            ActualizarUsuarioStaffRequest request);

    UsuarioStaffDTO cambiarEstadoUsuario(int idTienda, int idUsuario,
            EstadoUsuarioStaffRequest request);

    void eliminarUsuario(int idTienda, int idUsuario);

    PaginaAuditoriaDTO auditoria(String tipo, String busqueda, int pagina, int tamanio);
}
