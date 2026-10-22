package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.LoginRequestDTO;
import com.example.proyec_herramientas.model.LoginResponseDTO;
import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.RolRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Login con rol. Antes el rol se deducia en el codigo ("si esta en cliente
// es CLIENTE, si no es ADMIN"); ahora sale de la FK id_rol de la base.
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final ContrasenaService contrasena;
    private final TokenService tokenService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            PersonaRepository personaRepository,
            RolRepository rolRepository,
            ContrasenaService contrasena,
            TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.contrasena = contrasena;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        String identificador = request.getUsuario() == null ? "" : request.getUsuario().trim();
        String clave = request.getClave() == null ? "" : request.getClave();

        // 1) Cuenta del panel: por nombre de cuenta o por correo de la persona.
        Optional<Usuario> cuenta = buscarCuenta(identificador);
        if (cuenta.isPresent() && contrasena.coincide(clave, cuenta.get().getContrasena())) {
            Usuario usuario = cuenta.get();
            Persona persona = personaRepository.findById(usuario.getIdPersona()).orElse(null);
            String correo = persona != null ? persona.getCorreo() : null;
            return respuesta(usuario.getUsuario(), usuario.getIdRol(), idClienteDe(correo),
                    persona != null ? persona.getNombre() : null,
                    persona != null ? persona.getApellido() : null,
                    correo);
        }

        // 2) Comprador: entra con el correo con el que se registro.
        Optional<Cliente> cliente = clienteRepository.findByCorreo(identificador);
        if (cliente.isPresent() && contrasena.coincide(clave, cliente.get().getContrasena())) {
            Cliente comprador = cliente.get();
            return respuesta(comprador.getCorreo(), comprador.getIdRol(), comprador.getIdCliente(),
                    comprador.getNombre(), comprador.getApellido(), comprador.getCorreo());
        }

        return null;
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
            String apellido, String correo) {
        String id = idRol == null ? Rol.CLIENTE : idRol;
        String nombreRol = rolRepository.findById(id)
                .map(Rol::getNombre)
                .orElse(id);
        String token = tokenService.generar(usuario, id, idCliente);
        return new LoginResponseDTO(token, usuario, id, nombreRol, idCliente, nombre, apellido, correo);
    }
}
