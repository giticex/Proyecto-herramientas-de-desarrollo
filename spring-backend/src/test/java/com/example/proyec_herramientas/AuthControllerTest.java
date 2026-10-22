package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.security.HashLegado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @BeforeEach
    void limpiar() {
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();
        tiendaRepository.deleteAll();
        personaRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void registroTiendaCorrecto() throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registroTienda("Tienda Uno", "moda", "tienda-uno", "uno@correo.com", "clave1234"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.usuario.rol", is("ADMIN")))
                .andExpect(jsonPath("$.usuario.correo", is("uno@correo.com")))
                .andExpect(jsonPath("$.tienda.nombre", is("Tienda Uno")))
                .andExpect(jsonPath("$.tienda.slug", is("tienda-uno")));
    }

    @Test
    void registroTiendaConSlugDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        registroTienda("Tienda Uno", "moda", "tienda-uno", "uno@correo.com", "clave1234"))));

        mockMvc.perform(post("/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registroTienda("Tienda Dos", "moda", "tienda-uno", "dos@correo.com", "clave1234"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registroTiendaConCorreoDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        registroTienda("Tienda Uno", "moda", "tienda-uno", "uno@correo.com", "clave1234"))));

        mockMvc.perform(post("/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registroTienda("Tienda Dos", "moda", "tienda-dos", "uno@correo.com", "clave1234"))))
                .andExpect(status().isConflict());
    }

    @Test
    void loginAdminCorrecto() throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        registroTienda("Tienda Uno", "moda", "tienda-uno", "uno@correo.com", "clave1234"))));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login("uno@correo.com", "clave1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.usuario.rol", is("ADMIN")))
                .andExpect(jsonPath("$.tienda.slug", is("tienda-uno")));
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        registroTienda("Tienda Uno", "moda", "tienda-uno", "uno@correo.com", "clave1234"))));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login("uno@correo.com", "clave-equivocada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConUsuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login("nadie@correo.com", "clave1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConHashLegadoLoActualizaABCrypt() throws Exception {
        Tienda tienda = new Tienda();
        tienda.setNombre("Legado");
        tienda.setRubro("moda");
        tienda.setSlug("legado");
        tienda = tiendaRepository.save(tienda);

        Persona persona = new Persona();
        persona.setIdPersona("PER-LEGADO");
        persona.setNombre("Legado");
        persona.setApellido("");
        persona.setCorreo("legado@correo.com");
        personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setUsuario("legado@correo.com");
        usuario.setContrasena(HashLegado.sha256Hex("clave1234"));
        usuario.setIdPersona("PER-LEGADO");
        usuario.setIdTienda(tienda.getIdTienda());
        usuario = usuarioRepository.save(usuario);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login("legado@correo.com", "clave1234"))))
                .andExpect(status().isOk());

        Usuario actualizado = usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow();
        assertTrue(HashLegado.esHashBCrypt(actualizado.getContrasena()), "la contraseña debió subir a BCrypt");
    }

    @Test
    void registroClienteCorrecto() throws Exception {
        mockMvc.perform(post("/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registroCliente("Juan", "Pérez", "juan@correo.com", "clave1234", "987654321"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.usuario.rol", is("CLIENTE")))
                .andExpect(jsonPath("$.usuario.correo", is("juan@correo.com")))
                .andExpect(jsonPath("$.tienda").doesNotExist());
    }

    @Test
    void registroClienteConCorreoDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/auth/registro-cliente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        registroCliente("Juan", "Pérez", "juan@correo.com", "clave1234", "987654321"))));

        mockMvc.perform(post("/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registroCliente("Juan Otra", "Vez", "juan@correo.com", "clave1234", "987654322"))))
                .andExpect(status().isConflict());
    }

    private Map<String, Object> registroTienda(String nombre, String rubro, String slug, String correo, String contrasena) {
        return Map.of("nombre", nombre, "rubro", rubro, "slug", slug, "correo", correo, "contrasena", contrasena);
    }

    private Map<String, Object> login(String usuario, String contrasena) {
        return Map.of("usuario", usuario, "contrasena", contrasena);
    }

    private Map<String, Object> registroCliente(String nombre, String apellido, String correo, String contrasena, String telefono) {
        return Map.of("nombre", nombre, "apellido", apellido, "correo", correo, "contrasena", contrasena, "telefono", telefono);
    }
}
