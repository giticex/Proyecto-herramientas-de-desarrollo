package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.DevolucionRequestDTO;
import com.example.proyec_herramientas.persistence.Devolucion;
import com.example.proyec_herramientas.persistence.Venta;
import org.springframework.security.core.Authentication;

public interface DevolucionService {

    // Registra la devolucion, restaura el stock de los productos y actualiza
    // el estado de la venta (DEVOLUCION_PARCIAL o DEVUELTA).
    Devolucion registrarDevolucion(DevolucionRequestDTO request, Authentication authentication);

    // Venta con sus detalles y la cantidad ya devuelta por linea, para que el
    // vendedor elija que devolver. Valida la pertenencia de la venta.
    Venta obtenerVentaParaDevolucion(String idVenta, Authentication authentication);
}
