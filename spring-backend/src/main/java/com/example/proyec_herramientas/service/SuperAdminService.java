package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.EmpresaResumenDTO;
import com.example.proyec_herramientas.model.PaginaVentasDTO;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.model.SuperAdminEmpresaDTO;
import com.example.proyec_herramientas.model.SuperAdminResumenDTO;

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
}
