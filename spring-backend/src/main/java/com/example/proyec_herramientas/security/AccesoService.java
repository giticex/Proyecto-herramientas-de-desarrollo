package com.example.proyec_herramientas.security;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.ItemComprobanteDTO;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Objects;

/**
 * Reglas de pertenencia que no se pueden expresar solo con roles: un CLIENTE
 * solo ve lo suyo y un ADMIN/VENDEDOR solo lo de su tienda. Los controllers
 * llaman aqui antes de devolver o modificar un recurso.
 */
@Service
public class AccesoService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public AccesoService(VentaRepository ventaRepository, ProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    public SesionPrincipal principal(Authentication autenticacion) {
        if (autenticacion != null && autenticacion.getPrincipal() instanceof SesionPrincipal sesion) {
            return sesion;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no iniciada");
    }

    public boolean esCliente(SesionPrincipal sesion) {
        return Rol.CLIENTE.equals(sesion.rol());
    }

    public int tiendaObligatoria(SesionPrincipal sesion) {
        if (sesion.idTienda() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene una tienda asignada");
        }
        return sesion.idTienda();
    }

    // Un CLIENTE solo accede a sus propias ventas; el personal solo a las ventas
    // que incluyen productos de su tienda.
    @Transactional(readOnly = true)
    public void validarVenta(String idVenta, Authentication autenticacion) {
        SesionPrincipal sesion = principal(autenticacion);
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Comprobante " + idVenta + " no encontrado"));
        if (esCliente(sesion)) {
            if (sesion.idCliente() == null || !Objects.equals(venta.getIdCliente(), sesion.idCliente())) {
                throw new AccessDeniedException("La venta no pertenece al cliente autenticado");
            }
            return;
        }
        int idTienda = tiendaObligatoria(sesion);
        if (ventaRepository.contarDetallesDeTienda(idVenta, idTienda) == 0) {
            throw new AccessDeniedException("La venta no pertenece a la tienda autenticada");
        }
    }

    // Un CLIENTE solo puede consultar su propia ficha de compras.
    public void validarCliente(String idCliente, Authentication autenticacion) {
        SesionPrincipal sesion = principal(autenticacion);
        if (esCliente(sesion) && !Objects.equals(idCliente, sesion.idCliente())) {
            throw new AccessDeniedException("El cliente solicitado no es el autenticado");
        }
    }

    // El personal solo puede vender productos de su tienda. El CLIENTE compra
    // en la tienda publica, por lo que no se le restringe el catalogo.
    @Transactional(readOnly = true)
    public void validarProductos(Collection<Integer> idsProducto, Authentication autenticacion) {
        SesionPrincipal sesion = principal(autenticacion);
        if (esCliente(sesion) || idsProducto == null) {
            return;
        }
        int idTienda = tiendaObligatoria(sesion);
        for (Integer id : idsProducto) {
            if (id == null) {
                continue;
            }
            Producto producto = productoRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto " + id + " no existe"));
            if (!Integer.valueOf(idTienda).equals(producto.getIdTienda())) {
                throw new AccessDeniedException("El producto " + id + " no pertenece a la tienda autenticada");
            }
        }
    }

    // Antes de emitir: el CLIENTE no puede elegir a nombre de quien compra y el
    // personal no puede facturar productos de otra tienda.
    public void prepararEmision(ComprobanteRequestDTO request, Authentication autenticacion) {
        SesionPrincipal sesion = principal(autenticacion);
        if (esCliente(sesion)) {
            if (sesion.idCliente() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La cuenta no tiene un cliente asociado");
            }
            request.setIdCliente(sesion.idCliente());
            return;
        }
        validarProductos(idsProducto(request), autenticacion);
    }

    private Collection<Integer> idsProducto(ComprobanteRequestDTO request) {
        if (request == null || request.getItems() == null) {
            return null;
        }
        return request.getItems().stream()
                .filter(Objects::nonNull)
                .map(ItemComprobanteDTO::getIdProducto)
                .toList();
    }
}
