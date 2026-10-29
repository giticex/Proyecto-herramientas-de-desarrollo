package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.model.ActualizarUsuarioStaffRequest;
import com.example.proyec_herramientas.model.CrearUsuarioStaffRequest;
import com.example.proyec_herramientas.model.EstadoUsuarioStaffRequest;
import com.example.proyec_herramientas.model.PersonalTiendaDTO;
import com.example.proyec_herramientas.model.UsuarioStaffDTO;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Panel de tienda: el ADMIN solo ve y administra cuentas VENDEDOR de su
 * empresa. La validacion de identidad, el cupo del plan y la auditoria se
 * reutilizan del servicio de la consola global para que ambas superficies
 * apliquen exactamente las mismas reglas.
 */
@Service
public class AdminPersonalServiceImpl implements AdminPersonalService {

    // El cupo del plan mide puestos de trabajo, igual que en la consola global:
    // el ADMIN ocupa uno y los compradores no consumen ninguno.
    private static final Set<String> ROLES_STAFF = Set.of(Rol.ADMIN, Rol.VENDEDOR);

    private final SuperAdminService superAdminService;
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminPersonalServiceImpl(SuperAdminService superAdminService,
            TiendaRepository tiendaRepository, UsuarioRepository usuarioRepository) {
        this.superAdminService = superAdminService;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalTiendaDTO listar(int idTienda) {
        Tienda tienda = tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La tienda " + idTienda + " no existe"));
        List<UsuarioStaffDTO> personal = superAdminService.usuarios(idTienda).stream()
                .filter(cuenta -> Rol.VENDEDOR.equals(cuenta.rol()))
                .toList();
        long enUso = usuarioRepository.countByIdTiendaAndIdRolIn(idTienda, ROLES_STAFF);
        int limite = tienda.getLimiteUsuarios() == null ? 0 : tienda.getLimiteUsuarios();
        return new PersonalTiendaDTO(tienda.getIdTienda(), tienda.getNombre(), limite, enUso,
                personal);
    }

    @Override
    @Transactional
    public UsuarioStaffDTO crear(int idTienda, CrearUsuarioStaffRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La solicitud de alta es obligatoria");
        }
        // Aunque el cuerpo pida otro rol, este panel solo da de alta vendedores.
        CrearUsuarioStaffRequest alta = new CrearUsuarioStaffRequest(
                request.usuario(), request.nombre(), request.apellido(), request.correo(),
                Rol.VENDEDOR, request.password());
        return superAdminService.crearUsuario(idTienda, alta);
    }

    @Override
    @Transactional
    public UsuarioStaffDTO actualizar(int idTienda, int idUsuario,
            ActualizarUsuarioStaffRequest request) {
        Usuario cuenta = buscarVendedor(idTienda, idUsuario);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La solicitud de edición es obligatoria");
        }
        // El rol no se puede cambiar desde este panel: se mantiene VENDEDOR.
        ActualizarUsuarioStaffRequest edicion = new ActualizarUsuarioStaffRequest(
                request.usuario(), request.nombre(), request.apellido(), request.correo(),
                Rol.VENDEDOR, request.password());
        return superAdminService.actualizarUsuario(idTienda, cuenta.getIdUsuario(), edicion);
    }

    @Override
    @Transactional
    public UsuarioStaffDTO cambiarEstado(int idTienda, int idUsuario,
            EstadoUsuarioStaffRequest request) {
        Usuario cuenta = buscarVendedor(idTienda, idUsuario);
        return superAdminService.cambiarEstadoUsuario(idTienda, cuenta.getIdUsuario(), request);
    }

    // Una cuenta de otra empresa responde 404, igual que una inexistente: no se
    // revela si el id existe fuera de la tienda del administrador.
    private Usuario buscarVendedor(int idTienda, int idUsuario) {
        Usuario cuenta = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La cuenta " + idUsuario + " no existe"));
        if (cuenta.getIdTienda() == null || cuenta.getIdTienda() != idTienda
                || !Rol.VENDEDOR.equals(cuenta.getIdRol())) {
            throw new RecursoNoEncontradoException(
                    "La cuenta no pertenece al personal de esta empresa");
        }
        return cuenta;
    }
}
