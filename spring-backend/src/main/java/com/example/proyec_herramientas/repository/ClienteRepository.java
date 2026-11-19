package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    Optional<Cliente> findByCorreo(String correo);

    @Query("""
            SELECT c FROM Cliente c
            WHERE EXISTS (SELECT 1 FROM Venta v WHERE v.idCliente = c.idCliente AND v.idTienda = :idTienda)
            ORDER BY c.apellido, c.nombre, c.idCliente
            """)
    List<Cliente> listarConVentasDeTienda(@Param("idTienda") int idTienda);
}
