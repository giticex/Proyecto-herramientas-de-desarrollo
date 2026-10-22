package com.example.proyec_herramientas;

import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @BeforeEach
    void prepararCuentas() {
        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
    }

    @Test
    void loginDeAdministradorDevuelveRolAdmin() throws Exception {
        mockMvc.perform(post("/auth/login")
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
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.CORREO_ADMIN, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")));
    }

    @Test
    void loginDeCompradorDevuelveRolCliente() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.CORREO_CLIENTE, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("CLIENTE")))
                .andExpect(jsonPath("$.nombreRol", is("Cliente")))
                .andExpect(jsonPath("$.idCliente", is(CuentasTest.ID_CLIENTE)));
    }

    @Test
    void loginDeCuentaDeUsuarioDevuelveRolCliente() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_CLIENTE, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("CLIENTE")));
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN, "clave-mala")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Usuario o contrasena incorrectos")));
    }

    @Test
    void loginDeCuentaInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("nadie", CuentasTest.CLAVE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"\",\"clave\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authRolDevuelveElRolDelToken() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);

        mockMvc.perform(get("/auth/rol").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("ADMIN")))
                .andExpect(jsonPath("$.usuario", is(CuentasTest.USUARIO_ADMIN)));
    }

    @Test
    void authRolSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/auth/rol"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeAdministradorDevuelveSuTienda() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(CuentasTest.USUARIO_ADMIN, CuentasTest.CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTienda", notNullValue()))
                .andExpect(jsonPath("$.tienda.slug", is(CuentasTest.SLUG_TIENDA)));
    }

    @Test
    void registroDeTiendaCreaLaCuentaAdminConTienda() throws Exception {
        mockMvc.perform(post("/auth/registro-tienda")
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
        mockMvc.perform(post("/auth/registro-tienda")
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
        mockMvc.perform(post("/auth/registro-cliente")
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
        mockMvc.perform(post("/auth/registro-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Repetido",
                                "apellido", "Comprador",
                                "telefono", "987654321",
                                "correo", CuentasTest.CORREO_CLIENTE,
                                "contrasena", "clave-segura-123"))))
                .andExpect(status().isConflict());
    }

    private String credenciales(String usuario, String clave) throws Exception {
        return objectMapper.writeValueAsString(Map.of("usuario", usuario, "clave", clave));
    }
}
