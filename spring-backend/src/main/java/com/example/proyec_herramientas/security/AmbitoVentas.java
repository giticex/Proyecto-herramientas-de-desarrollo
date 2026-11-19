package com.example.proyec_herramientas.security;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.example.proyec_herramientas.service.TiendaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AmbitoVentas {
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final TiendaService tiendas;

    public AmbitoVentas(ProductoRepository productos, VentaRepository ventas, TiendaService tiendas) {
        this.productos = productos;
        this.ventas = ventas;
        this.tiendas = tiendas;
    }

    public int tiendaAdministrada(SesionPrincipal sesion) {
        if (sesion != null && Rol.ADMIN.equals(sesion.rol()) && sesion.idTienda() != null) {
            return sesion.idTienda();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta operación");
    }

    public int tiendaDeCompra(SesionPrincipal sesion, String slug, Integer primerProducto) {
        if (sesion != null && Rol.ADMIN.equals(sesion.rol())) {
            return tiendaAdministrada(sesion);
        }
        if (sesion == null || !Rol.CLIENTE.equals(sesion.rol()) || sesion.idCliente() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta operación");
        }
        if (slug != null && !slug.isBlank()) {
            return tiendas.obtenerIdPorSlug(slug);
        }
        // Las llamadas anteriores al contrato de slug se limitan a la tienda del primer producto.
        if (primerProducto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica los productos de la compra");
        }
        return productos.findById(primerProducto)
                .map(producto -> producto.getIdTienda())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado en la tienda"));
    }

    public Integer tiendaDelHistorial(SesionPrincipal sesion, String idCliente, String slug) {
        if (sesion != null && Rol.CLIENTE.equals(sesion.rol())) {
            if (idCliente == null || !idCliente.equals(sesion.idCliente())) {
                throw new RecursoNoEncontradoException("Historial no encontrado");
            }
            return slug == null || slug.isBlank() ? null : tiendas.obtenerIdPorSlug(slug);
        }
        return tiendaAdministrada(sesion);
    }

    public void verificarVenta(SesionPrincipal sesion, String idVenta) {
        boolean accesible = sesion != null && Rol.CLIENTE.equals(sesion.rol())
                ? sesion.idCliente() != null && ventas.existsByIdVentaAndIdCliente(idVenta, sesion.idCliente())
                : ventas.existsByIdVentaAndIdTienda(idVenta, tiendaAdministrada(sesion));
        if (!accesible) {
            throw new RecursoNoEncontradoException("Venta no encontrada");
        }
    }
}
