package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.service.ContrasenaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El login devuelve el rol que la base tiene asignado para esa cuenta.
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
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private ContrasenaService contrasena;

    // Staff y comprador con el mismo correo: el caso real luis@gmail.com,
    // donde la Persona de la cuenta de panel y la ficha del cliente comparten
    // direccion y el login unificado debe resolver como comprador.
    private static final String USUARIO_STAFF_COLISION = "luisg-prueba";
    private static final String ID_PERSONA_STAFF_COLISION = "PER-COLISION";
    private static final String ID_CLIENTE_COLISION = "CLI-COLISION";
    private static final String CORREO_COLISION = "luis.colision@prueba.com";
    private static final String CLAVE_STAFF = "clave-staff-999";
    private static final String ID_CLIENTE_SOLO_REG = "CLI-SOLO-REG";
    private static final String CORREO_CLIENTE_SOLO_REG = "solo.registro@prueba.com";

    @BeforeEach
    void prepararCuentas() {
        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
    }

    @AfterEach
    void quitarColision() {
        usuarioRepository.findByUsuario(USUARIO_STAFF_COLISION).ifPresent(usuarioRepository::delete);
        clienteRepository.findById(ID_CLIENTE_COLISION).ifPresent(clienteRepository::delete);
        clienteRepository.findById(ID_CLIENTE_SOLO_REG).ifPresent(clienteRepository::delete);
        personaRepository.findById(ID_PERSONA_STAFF_COLISION).ifPresent(personaRepository::delete);
    }

    @Test
    void loginDeAdministradorDevuelveRolAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andExpect(jsonPath("$.nombreRol", is("Administrador")))
                .andExpect(jsonPath("$.usuario", is(CuentasTest.USUARIO_ADMIN)))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void loginDeAdministradorPorCorreoTambienDevuelveRolAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.CORREO_ADMIN, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")));
    }

    @Test
    void loginDeCompradorDevuelveRolCliente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("CLIENTE")))
                .andExpect(jsonPath("$.nombreRol", is("Cliente")))
                .andExpect(jsonPath("$.idCliente", is(CuentasTest.ID_CLIENTE)));
    }

    @Test
    void loginDeCuentaDeUsuarioDevuelveRolCliente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_CLIENTE, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("CLIENTE")));
    }

    @Test
    void elCorreoDeUnCompradorGanaALaCuentaDePanelConEseMismoCorreo() throws Exception {
        crearColision();

        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CORREO_COLISION, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("CLIENTE")))
                .andExpect(jsonPath("$.nombreRol", is("Cliente")))
                .andExpect(jsonPath("$.idCliente", is(ID_CLIENTE_COLISION)))
                .andExpect(jsonPath("$.usuario", is(CORREO_COLISION)))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(cuerpo).get("token").asText();

        // El token emitido es de comprador y solo abre los endpoints del portal.
        mockMvc.perform(get("/api/auth/rol").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("CLIENTE")));
        mockMvc.perform(get("/api/creditos/mios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/clientes/mi-perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCliente", is(ID_CLIENTE_COLISION)));
        mockMvc.perform(get("/api/creditos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void elStaffConCorreoCompartidoSigueEntrandoConSuUsuario() throws Exception {
        crearColision();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(USUARIO_STAFF_COLISION, CLAVE_STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andExpect(jsonPath("$.usuario", is(USUARIO_STAFF_COLISION)));
    }

    @Test
    void laClaveDelPanelNoAbreLaCuentaDeCompradorPorSuCorreo() throws Exception {
        crearColision();

        // La cuenta de panel y la ficha del comprador comparten correo, pero
        // el correo siempre se valida contra el comprador: no hay caida al panel.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CORREO_COLISION, CLAVE_STAFF)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN, "clave-mala")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Usuario o contrasena incorrectos")));
    }

    @Test
    void loginDeCuentaInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("nadie", CuentasTest.CLAVE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"\",\"clave\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authRolDevuelveElRolDelToken() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);

        mockMvc.perform(get("/api/auth/rol").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andExpect(jsonPath("$.usuario", is(CuentasTest.USUARIO_ADMIN)));
    }

    @Test
    void authRolSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/rol"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeAdministradorDevuelveSuTienda() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTienda", notNullValue()))
                .andExpect(jsonPath("$.tienda.slug", is(CuentasTest.SLUG_TIENDA)));
    }

    @Test
    void registroDeTiendaCreaLaCuentaAdminConTienda() throws Exception {
        mockMvc.perform(post("/api/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Bodega Nueva",
                                "rubro", "Abarrotes",
                                "slug", "bodega-nueva",
                                "correo", "dueno@bodega.com",
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andExpect(jsonPath("$.tienda.slug", is("bodega-nueva")))
                .andExpect(jsonPath("$.idTienda", notNullValue()))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void registroDeTiendaConSlugRepetidoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Otra Tienda",
                                "rubro", "Otros",
                                "slug", CuentasTest.SLUG_TIENDA,
                                "correo", "otra@bodega.com",
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registroDeClienteDevuelveRolClienteConToken() throws Exception {
        mockMvc.perform(post("/api/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Nuevo",
                                "apellido", "Comprador",
                                "telefono", "987654321",
                                "correo", "nuevo@comprador.com",
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol", is("CLIENTE")))
                .andExpect(jsonPath("$.idCliente", notNullValue()))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void registroDeClienteConCorreoRepetidoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Repetido",
                                "apellido", "Comprador",
                                "telefono", "987654321",
                                "correo", CuentasTest.CORREO_CLIENTE,
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registroDeTiendaConCorreoDeCompradorDevuelve409() throws Exception {
        // Comprador con ficha solo en la tabla cliente: asi se comprueba que
        // la validacion mira esa tabla y no solo los correos de persona.
        Cliente soloComprador = new Cliente(ID_CLIENTE_SOLO_REG, "Solo", "Comprador",
                "987654398", CORREO_CLIENTE_SOLO_REG);
        soloComprador.setContrasena(contrasena.hash("clave-segura-123"));
        soloComprador.setIdRol(Rol.CLIENTE);
        clienteRepository.save(soloComprador);

        // El ADMIN de una tienda nueva no puede nacer con el correo de un
        // comprador: serian dos identidades para el mismo login.
        mockMvc.perform(post("/api/auth/registro-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Tienda Correo Comprador",
                                "rubro", "Otros",
                                "slug", "tienda-correo-comprador",
                                "correo", CORREO_CLIENTE_SOLO_REG,
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registroDeClienteConCorreoDePersonalDevuelve409() throws Exception {
        // Y a la inversa: un comprador no puede registrarse con el correo de
        // una cuenta de staff ya existente.
        mockMvc.perform(post("/api/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Intruso",
                                "apellido", "Comprador",
                                "telefono", "987654321",
                                "correo", CuentasTest.CORREO_ADMIN,
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isConflict());
    }

    private void crearColision() {
        Tienda tienda = CuentasTest.crearTienda(tiendaRepository);

        Persona persona = new Persona();
        persona.setIdPersona(ID_PERSONA_STAFF_COLISION);
        persona.setNombre("Luis");
        persona.setApellido("Panel");
        persona.setCorreo(CORREO_COLISION);
        persona.setUsuario(USUARIO_STAFF_COLISION);
        personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setUsuario(USUARIO_STAFF_COLISION);
        usuario.setContrasena(contrasena.hash(CLAVE_STAFF));
        usuario.setIdPersona(ID_PERSONA_STAFF_COLISION);
        usuario.setIdRol(Rol.ADMIN);
        usuario.setIdTienda(tienda.getIdTienda());
        usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(ID_CLIENTE_COLISION, "Luis", "Comprador", "987654321", CORREO_COLISION);
        cliente.setContrasena(contrasena.hash(CuentasTest.CLAVE));
        cliente.setIdRol(Rol.CLIENTE);
        clienteRepository.save(cliente);
    }

    private String credenciales(String usuario, String clave) throws Exception {
        return objectMapper.writeValueAsString(Map.of("usuario", usuario, "clave", clave));
    }
}
