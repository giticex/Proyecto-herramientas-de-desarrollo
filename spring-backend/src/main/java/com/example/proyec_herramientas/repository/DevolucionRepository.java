package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, String> {

    List<Devolucion> findByIdVentaOrderByFechaDesc(String idVenta);
}
