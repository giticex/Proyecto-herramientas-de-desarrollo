package com.example.proyec_herramientas;

import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cada endpoint exige el rol que corresponde: el panel es del ADMIN y el
// catalogo se puede leer sin sesion.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadRolesTest {

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
    private ProductoRepository productoRepository;

    @BeforeEach
    void prepararCuentas() {
        productoRepository.deleteAll();
        CuentasTest.crearAdmin(usuarioRepository, personaRepository);
        CuentasTest.crearCliente(usuarioRepository, personaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
    }

    @Test
    void crearProductoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(50, "Mouse Pro")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearProductoConTokenDeClienteDevuelve403() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);

        mockMvc.perform(post("/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(50, "Mouse Pro")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearProductoConTokenDeAdminDevuelve201() throws Exception {
        String token = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);

        mockMvc.perform(post("/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(50, "Mouse Pro")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre", is("Mouse Pro")));
    }

    @Test
    void listarProductosEsPublico() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void listarClientesExigeRolAdmin() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        String tokenAdmin = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void historialExigeRolAdmin() throws Exception {
        mockMvc.perform(get("/historial"))
                .andExpect(status().isUnauthorized());

        String tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/historial").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        String tokenAdmin = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        mockMvc.perform(get("/historial").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void tokenManipuladoDevuelve401() throws Exception {
        mockMvc.perform(get("/historial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.inventado.abc"))
                .andExpect(status().isUnauthorized());
    }

    private String producto(int idProducto, String nombre) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "idProducto", idProducto,
                "nombre", nombre,
                "stock", 10,
                "precio", 25.0,
                "categoria", "Accesorios"));
    }
}
