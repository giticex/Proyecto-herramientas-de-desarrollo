package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, String> {

    // Turno abierto del vendedor en su tienda. Como maximo debe existir uno:
    // la apertura lo comprueba antes de crear otro.
    Optional<MovimientoCaja> findFirstByIdTiendaAndUsuarioAndEstadoOrderByFechaAperturaDesc(
            Integer idTienda, String usuario, String estado);

    // Ultimo cierre del vendedor, para que el POS muestre el arqueo final del
    // turno aunque ya no se pueda volver a cerrar.
    Optional<MovimientoCaja> findFirstByIdTiendaAndUsuarioAndEstadoOrderByFechaCierreDesc(
            Integer idTienda, String usuario, String estado);

    Optional<MovimientoCaja> findFirstByIdTiendaAndUsuarioAndEstadoAndFechaCierreGreaterThanEqualOrderByFechaCierreDesc(
            Integer idTienda, String usuario, String estado, Date desde);
}
