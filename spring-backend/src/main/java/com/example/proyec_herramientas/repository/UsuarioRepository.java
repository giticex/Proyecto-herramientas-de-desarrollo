package com.example.proyec_herramientas.repository;

import com.example.proyec_herramientas.persistence.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsuario(String usuario);

    Optional<Usuario> findByIdPersona(String idPersona);

    List<Usuario> findByIdTiendaOrderByIdUsuarioAsc(Integer idTienda);

    // El personal operativo de una tienda. Se filtra por rol en la consulta
    // para que una cuenta de comprador con fila en "usuario" nunca aparezca
    // como staff ni ocupe un cupo del plan.
    List<Usuario> findByIdTiendaAndIdRolInOrderByIdUsuarioAsc(Integer idTienda, Collection<String> roles);

    long countByIdTiendaAndIdRolIn(Integer idTienda, Collection<String> roles);

    // Administrador operativo de la tienda. El orden por id hace determinista
    // la respuesta si una tienda tuviera mas de una cuenta ADMIN.
    Optional<Usuario> findFirstByIdTiendaAndIdRolOrderByIdUsuarioAsc(Integer idTienda, String idRol);

    // Cuentas vigentes de un rol: las filas creadas antes de la columna estado
    // no la tienen y cuentan como activas.
    @Query("select count(u) from Usuario u where u.idTienda = :idTienda and u.idRol = :rol "
            + "and (u.estado is null or u.estado <> 'SUSPENDIDO')")
    long contarActivasPorTiendaYRol(@Param("idTienda") Integer idTienda, @Param("rol") String rol);
}
