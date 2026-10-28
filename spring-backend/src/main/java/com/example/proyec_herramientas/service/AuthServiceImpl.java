package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.model.LoginRequestDTO;
import com.example.proyec_herramientas.model.LoginResponseDTO;
import com.example.proyec_herramientas.model.TiendaDTO;
import com.example.proyec_herramientas.model.auth.RegistroClienteRequest;
import com.example.proyec_herramientas.model.auth.RegistroTiendaRequest;
import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.RolRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

// Login con rol. El rol sale de la FK id_rol de la base, nunca se deduce de
// "en que tabla esta la cuenta". La cuenta ADMIN ademas trae su tienda, que
// viaja firmada en el token para acotar todas sus operaciones.
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final TiendaRepository tiendaRepository;
    private final ContrasenaService contrasena;
    private final TokenService tokenService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            PersonaRepository personaRepository,
            RolRepository rolRepository,
            TiendaRepository tiendaRepository,
            ContrasenaService contrasena,
            TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.tiendaRepository = tiendaRepository;
        this.contrasena = contrasena;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        String identificador = request.getUsuario() == null ? "" : request.getUsuario().trim();
        String clave = request.getClave() == null ? "" : request.getClave();
        String tipo = request.getTipo() == null ? "" : request.getTipo().trim().toUpperCase();
        boolean soloComprador = "CLIENTE".equals(tipo);
        boolean soloPersonal = "PERSONAL".equals(tipo);

        // 1) Cuenta del panel: por nombre de cuenta o por correo de la persona.
        //    Con tipo=CLIENTE se omite: un comprador no puede entrar al panel
        //    aunque su correo exista en la tabla de personas.
        Optional<Usuario> cuenta = soloComprador ? Optional.empty() : buscarCuenta(identificador);
        if (cuenta.isPresent() && verificarYActualizar(cuenta.get(), clave)) {
            Usuario usuario = cuenta.get();
            Persona persona = personaRepository.findById(usuario.getIdPersona()).orElse(null);
            String correo = persona != null ? persona.getCorreo() : null;
            Tienda tienda = usuario.getIdTienda() != null
                    ? tiendaRepository.findById(usuario.getIdTienda()).orElse(null)
                    : null;
            return respuesta(usuario.getUsuario(), usuario.getIdRol(), idClienteDe(correo),
                    persona != null ? persona.getNombre() : null,
                    persona != null ? persona.getApellido() : null,
                    correo, tienda);
        }

        // 2) Comprador: entra con el correo con el que se registro.
        Optional<Cliente> cliente = soloPersonal ? Optional.empty()
                : clienteRepository.findByCorreo(identificador);
        if (cliente.isPresent() && verificarYActualizar(cliente.get(), clave)) {
            Cliente comprador = cliente.get();
            return respuesta(comprador.getCorreo(), comprador.getIdRol(), comprador.getIdCliente(),
                    comprador.getNombre(), comprador.getApellido(), comprador.getCorreo(), null);
        }

        return null;
    }

    @Override
    @Transactional
    public LoginResponseDTO registroTienda(RegistroTiendaRequest request) {
        String slug = request.getSlug().trim().toLowerCase();
        String correo = request.getCorreo().trim().toLowerCase();

        if (tiendaRepository.findBySlug(slug).isPresent()) {
            throw new RecursoDuplicadoException("Ese slug ya esta en uso");
        }
        if (correoYaRegistrado(correo)) {
            throw new RecursoDuplicadoException("Ese correo ya esta registrado");
        }

        Tienda tienda = new Tienda();
        tienda.setNombre(request.getNombre().trim());
        tienda.setRubro(request.getRubro().trim());
        tienda.setSlug(slug);
        tienda = tiendaRepository.save(tienda);

        // El registro no pide apellido: persona.apellido es NOT NULL en el
        // esquema historico, se deja vacio.
        Persona persona = new Persona();
        persona.setIdPersona(generarId("PER"));
        persona.setNombre(request.getNombre().trim());
        persona.setApellido("");
        persona.setCorreo(correo);
        persona.setUsuario(correo);
        persona = personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setUsuario(correo);
        usuario.setContrasena(contrasena.hashBcrypt(request.getContrasena()));
        usuario.setIdPersona(persona.getIdPersona());
        usuario.setIdRol(Rol.ADMIN);
        usuario.setIdTienda(tienda.getIdTienda());
        usuarioRepository.save(usuario);

        return respuesta(correo, Rol.ADMIN, "", persona.getNombre(), persona.getApellido(), correo, tienda);
    }

    @Override
    @Transactional
    public LoginResponseDTO registroCliente(RegistroClienteRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();

        if (correoYaRegistrado(correo)) {
            throw new RecursoDuplicadoException("Ese correo ya esta registrado");
        }

        Cliente cliente = new Cliente(generarId("CLI"), request.getNombre().trim(),
                request.getApellido().trim(), request.getTelefono(), correo);
        cliente.setContrasena(contrasena.hashBcrypt(request.getContrasena()));
        cliente.setIdRol(Rol.CLIENTE);
        cliente = clienteRepository.save(cliente);

        // VentaServiceImpl resuelve el id_persona de la venta buscando una
        // Persona con el mismo correo del cliente (PersonaRepository); sin
        // esta fila, registrar una venta fallaria con 404.
        if (personaRepository.findByCorreo(correo).isEmpty()) {
            Persona persona = new Persona();
            persona.setIdPersona(generarId("PER"));
            persona.setNombre(request.getNombre().trim());
            persona.setApellido(request.getApellido().trim());
            persona.setCorreo(correo);
            persona.setTelefono(request.getTelefono());
            personaRepository.save(persona);
        }

        return respuesta(correo, Rol.CLIENTE, cliente.getIdCliente(),
                cliente.getNombre(), cliente.getApellido(), correo, null);
    }

    private boolean correoYaRegistrado(String correo) {
        return usuarioRepository.findByUsuario(correo).isPresent()
                || personaRepository.findByCorreo(correo).isPresent()
                || clienteRepository.findByCorreo(correo).isPresent();
    }

    // Acepta BCrypt o el SHA-256 de la carga inicial; si valida con el hash
    // viejo, lo reemplaza por BCrypt en la misma transaccion.
    private boolean verificarYActualizar(Usuario usuario, String clave) {
        String hash = usuario.getContrasena();
        if (!contrasena.coincide(clave, hash)) {
            return false;
        }
        if (contrasena.esHashLegado(hash)) {
            usuario.setContrasena(contrasena.hashBcrypt(clave));
            usuarioRepository.save(usuario);
        }
        return true;
    }

    private boolean verificarYActualizar(Cliente cliente, String clave) {
        String hash = cliente.getContrasena();
        if (!contrasena.coincide(clave, hash)) {
            return false;
        }
        if (contrasena.esHashLegado(hash)) {
            cliente.setContrasena(contrasena.hashBcrypt(clave));
            clienteRepository.save(cliente);
        }
        return true;
    }

    // Una cuenta que tambien esta en la tabla cliente (el caso de luisg)
    // compra con el mismo id de cliente que el de su ficha.
    private String idClienteDe(String correo) {
        if (correo == null) {
            return "";
        }
        return clienteRepository.findByCorreo(correo)
                .map(Cliente::getIdCliente)
                .orElse("");
    }

    private Optional<Usuario> buscarCuenta(String identificador) {
        Optional<Usuario> porNombre = usuarioRepository.findByUsuario(identificador);
        if (porNombre.isPresent()) {
            return porNombre;
        }
        return personaRepository.findByCorreo(identificador)
                .flatMap(persona -> usuarioRepository.findByIdPersona(persona.getIdPersona()));
    }

    private LoginResponseDTO respuesta(String usuario, String idRol, String idCliente, String nombre,
            String apellido, String correo, Tienda tienda) {
        String id = idRol == null ? Rol.CLIENTE : idRol;
        String nombreRol = rolRepository.findById(id)
                .map(Rol::getNombre)
                .orElse(id);
        Integer idTienda = tienda != null ? tienda.getIdTienda() : null;
        String token = tokenService.generar(usuario, id, idCliente, idTienda);

        LoginResponseDTO respuesta = new LoginResponseDTO(token, usuario, id, nombreRol, idCliente,
                nombre, apellido, correo);
        respuesta.setIdTienda(idTienda);
        respuesta.setTienda(tienda != null ? aTiendaDTO(tienda) : null);
        return respuesta;
    }

    private String generarId(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TiendaDTO aTiendaDTO(Tienda tienda) {
        TiendaDTO dto = new TiendaDTO();
        dto.setNombre(tienda.getNombre());
        dto.setRubro(tienda.getRubro());
        dto.setSlug(tienda.getSlug());
        dto.setColorPrimario(tienda.getColorPrimario());
        dto.setColorSecundario(tienda.getColorSecundario());
        dto.setLogoUrl(tienda.getLogoUrl());
        return dto;
    }
}
