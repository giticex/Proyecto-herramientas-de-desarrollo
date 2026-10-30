package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    Optional<Cliente> findByCorreo(String correo);

    // Ficha registrada por el punto de venta de esa tienda. La busqueda del POS
    // nunca cruza tiendas: un ADMIN no ve los clientes de otra empresa.
    Optional<Cliente> findFirstByNumeroDocumentoAndIdTienda(String numeroDocumento, Integer idTienda);
}
