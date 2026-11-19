package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByIdTienda(int idTienda);

    Optional<Producto> findByIdProductoAndIdTienda(int idProducto, int idTienda);
}
