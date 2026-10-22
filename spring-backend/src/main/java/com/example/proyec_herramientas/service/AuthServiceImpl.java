package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.config.JwtService;
import com.example.proyec_herramientas.exception.CredencialesInvalidasException;
import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.model.TiendaDTO;
import com.example.proyec_herramientas.model.auth.AuthResponse;
import com.example.proyec_herramientas.model.auth.LoginRequest;
import com.example.proyec_herramientas.model.auth.RegistroClienteRequest;
import com.example.proyec_herramientas.model.auth.RegistroTiendaRequest;
import com.example.proyec_herramientas.model.auth.UsuarioAuthDTO;
import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.security.HashLegado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_CLIENTE = "CLIENTE";

    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(TiendaRepository tiendaRepository, UsuarioRepository usuarioRepository,
            PersonaRepository personaRepository, ClienteRepository clienteRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse registroTienda(RegistroTiendaRequest request) {
        String slug = request.getSlug().trim().toLowerCase();
        String correo = request.getCorreo().trim().toLowerCase();

        if (tiendaRepository.findBySlug(slug).isPresent()) {
            throw new RecursoDuplicadoException("Ese slug ya está en uso.");
        }
        if (usuarioRepository.buscarPorUsuarioOCorreo(correo).isPresent()) {
            throw new RecursoDuplicadoException("Ese correo ya está registrado.");
        }

        Tienda tienda = new Tienda();
        tienda.setNombre(request.getNombre().trim());
        tienda.setRubro(request.getRubro().trim());
        tienda.setSlug(slug);
        tienda = tiendaRepository.save(tienda);

        // La solicitud no pide apellido para el ADMIN: persona.apellido es
        // NOT NULL en el esquema historico, se deja vacio.
        Persona persona = new Persona();
        persona.setIdPersona(generarId("PER"));
        persona.setNombre(request.getNombre().trim());
        persona.setApellido("");
        persona.setCorreo(correo);
        persona.setUsuario(correo);
        persona = personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setUsuario(correo);
        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setIdPersona(persona.getIdPersona());
        usuario.setIdTienda(tienda.getIdTienda());
        usuario = usuarioRepository.save(usuario);

        String token = jwtService.generar(String.valueOf(usuario.getIdUsuario()), ROL_ADMIN, tienda.getIdTienda());
        UsuarioAuthDTO usuarioDTO = new UsuarioAuthDTO(String.valueOf(usuario.getIdUsuario()), correo, ROL_ADMIN);
        return new AuthResponse(token, usuarioDTO, aTiendaDTO(tienda));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String valor = request.getUsuario().trim();

        Optional<Usuario> usuarioOpt = usuarioRepository.buscarPorUsuarioOCorreo(valor);
        if (usuarioOpt.isPresent()) {
            return loginAdmin(usuarioOpt.get(), request.getContrasena());
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByCorreo(valor);
        if (clienteOpt.isPresent()) {
            return loginCliente(clienteOpt.get(), request.getContrasena());
        }

        // No revela si el problema fue el usuario o la contraseña.
        throw new CredencialesInvalidasException("Usuario o contraseña incorrectos.");
    }

    @Override
    @Transactional
    public AuthResponse registroCliente(RegistroClienteRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        if (clienteRepository.findByCorreo(correo).isPresent()) {
            throw new RecursoDuplicadoException("Ese correo ya está registrado.");
        }

        Cliente cliente = new Cliente(generarId("CLI"), request.getNombre().trim(), request.getApellido().trim(),
                request.getTelefono(), correo);
        cliente.setContrasena(passwordEncoder.encode(request.getContrasena()));
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
            personaRepository.save(persona);
        }

        String token = jwtService.generar(cliente.getIdCliente(), ROL_CLIENTE, null);
        UsuarioAuthDTO usuarioDTO = new UsuarioAuthDTO(cliente.getIdCliente(), correo, ROL_CLIENTE);
        return new AuthResponse(token, usuarioDTO, null);
    }

    private AuthResponse loginAdmin(Usuario usuario, String contrasenaPlano) {
        if (!verificarYActualizar(usuario, contrasenaPlano)) {
            throw new CredencialesInvalidasException("Usuario o contraseña incorrectos.");
        }
        Tienda tienda = tiendaRepository.findById(usuario.getIdTienda())
                .orElseThrow(() -> new IllegalStateException("La tienda del usuario " + usuario.getUsuario() + " no existe"));
        String correo = personaRepository.findById(usuario.getIdPersona())
                .map(Persona::getCorreo)
                .orElse(usuario.getUsuario());

        String token = jwtService.generar(String.valueOf(usuario.getIdUsuario()), ROL_ADMIN, usuario.getIdTienda());
        UsuarioAuthDTO usuarioDTO = new UsuarioAuthDTO(String.valueOf(usuario.getIdUsuario()), correo, ROL_ADMIN);
        return new AuthResponse(token, usuarioDTO, aTiendaDTO(tienda));
    }

    private AuthResponse loginCliente(Cliente cliente, String contrasenaPlano) {
        if (!verificarYActualizar(cliente, contrasenaPlano)) {
            throw new CredencialesInvalidasException("Usuario o contraseña incorrectos.");
        }
        String token = jwtService.generar(cliente.getIdCliente(), ROL_CLIENTE, null);
        UsuarioAuthDTO usuarioDTO = new UsuarioAuthDTO(cliente.getIdCliente(), cliente.getCorreo(), ROL_CLIENTE);
        return new AuthResponse(token, usuarioDTO, null);
    }

    // Acepta BCrypt o el SHA-256 de la carga inicial; si valida con el hash
    // viejo, lo reemplaza por BCrypt ahi mismo.
    private boolean verificarYActualizar(Usuario usuario, String contrasenaPlano) {
        String hash = usuario.getContrasena();
        if (HashLegado.esHashBCrypt(hash)) {
            return passwordEncoder.matches(contrasenaPlano, hash);
        }
        if (!hash.equals(HashLegado.sha256Hex(contrasenaPlano))) {
            return false;
        }
        usuario.setContrasena(passwordEncoder.encode(contrasenaPlano));
        usuarioRepository.save(usuario);
        return true;
    }

    private boolean verificarYActualizar(Cliente cliente, String contrasenaPlano) {
        String hash = cliente.getContrasena();
        if (HashLegado.esHashBCrypt(hash)) {
            return passwordEncoder.matches(contrasenaPlano, hash);
        }
        if (!hash.equals(HashLegado.sha256Hex(contrasenaPlano))) {
            return false;
        }
        cliente.setContrasena(passwordEncoder.encode(contrasenaPlano));
        clienteRepository.save(cliente);
        return true;
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
