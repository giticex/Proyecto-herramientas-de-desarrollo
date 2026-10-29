package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El panel de personal de la tienda es del ADMIN y de nadie mas. La empresa
// nunca viaja en la peticion: sale del token, asi que estas pruebas comprueban
// que un administrador solo alcanza las cuentas VENDEDOR de su tienda.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPersonalApiTest {

    private static final String PERSONAL_URL = "/api/admin/personal";
    private static final String SLUG_EMPRESA_B = "empresa-dos";
    private static final String USUARIO_ADMIN_B = "adminb";
    private static final String CORREO_ADMIN_B = "adminb@prueba.com";
    private static final String ID_PERSONA_ADMIN_B = "PER-PRUEBA-ADMB";
    private static final String CLAVE_STAFF = "ClaveStaff#2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    private Tienda tiendaA;
    private Tienda tiendaB;
    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenVendedor;
    private String tokenCliente;
    private String tokenSuper;

    @BeforeEach
    void preparar() throws Exception {
        jdbcTemplate.update("DELETE FROM auditoria_evento");
        jdbcTemplate.update("DELETE FROM credito");
        jdbcTemplate.update("DELETE FROM devolucion_detalle");
        jdbcTemplate.update("DELETE FROM devolucion");
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        clienteRepository.deleteAll();
        personaRepository.deleteAll();
        tiendaRepository.deleteAll();

        tiendaA = CuentasTest.crearTienda(tiendaRepository);
        tiendaB = tiendaRepository.findBySlug(SLUG_EMPRESA_B).orElseGet(() -> {
            Tienda tienda = new Tienda();
            tienda.setNombre("Empresa Dos");
            tienda.setRubro("Moda");
            tienda.setSlug(SLUG_EMPRESA_B);
            return tiendaRepository.save(tienda);
        });

        CuentasTest.crearAdmin(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearVendedor(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearCliente(usuarioRepository, personaRepository, tiendaRepository);
        CuentasTest.crearFichaCliente(clienteRepository);
        CuentasTest.crearSuperAdmin(usuarioRepository, personaRepository, tiendaRepository);
        crearAdminDeEmpresaB();

        tokenAdminA = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_ADMIN,
                CuentasTest.CLAVE);
        tokenAdminB = CuentasTest.token(mockMvc, objectMapper, USUARIO_ADMIN_B, CuentasTest.CLAVE);
        tokenVendedor = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_VENDEDOR,
                CuentasTest.CLAVE);
        tokenCliente = CuentasTest.token(mockMvc, objectMapper, CuentasTest.CORREO_CLIENTE,
                CuentasTest.CLAVE);
        tokenSuper = CuentasTest.token(mockMvc, objectMapper, CuentasTest.USUARIO_SUPER,
                CuentasTest.CLAVE);
    }

    @Test
    void elListadoTraeSoloVendedoresDeLaTiendaDelAdmin() throws Exception {
        mockMvc.perform(get(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTienda", is(tiendaA.getIdTienda())))
                .andExpect(jsonPath("$.tiendaNombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.limitePersonal", is(5)))
                // El cupo mide puestos de trabajo: el ADMIN ocupa uno y el
                // VENDEDOR el otro. El comprador de la tienda no suma.
                .andExpect(jsonPath("$.personalEnUso", is(2)))
                .andExpect(jsonPath("$.personal", hasSize(1)))
                .andExpect(jsonPath("$.personal[0].usuario", is(CuentasTest.USUARIO_VENDEDOR)))
                .andExpect(jsonPath("$.personal[0].nombreCompleto", is("Vendedor Prueba")))
                .andExpect(jsonPath("$.personal[0].correo", is(CuentasTest.CORREO_VENDEDOR)))
                .andExpect(jsonPath("$.personal[0].rol", is("VENDEDOR")))
                .andExpect(jsonPath("$.personal[0].idTienda", is(tiendaA.getIdTienda())))
                // El propio administrador no aparece en su lista de personal.
                .andExpect(jsonPath("$.personal[?(@.usuario == 'admin')]", hasSize(0)))
                .andExpect(jsonPath("$.personal[?(@.rol != 'VENDEDOR')]", hasSize(0)));

        mockMvc.perform(get(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTienda", is(tiendaB.getIdTienda())))
                .andExpect(jsonPath("$.tiendaNombre", is("Empresa Dos")))
                .andExpect(jsonPath("$.personal", hasSize(0)))
                .andExpect(jsonPath("$.personalEnUso", is(1)));
    }

    @Test
    void elAltaSiempreCreaUnVendedorDentroDeLaTiendaDelAdmin() throws Exception {
        // El cuerpo pide ADMIN: el panel lo ignora y crea un VENDEDOR.
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero1",
                                "nombre", "Caja",
                                "apellido", "Uno",
                                "correo", "cajero1@prueba.com",
                                "rol", "ADMIN",
                                "password", CLAVE_STAFF))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario", is("cajero1")))
                .andExpect(jsonPath("$.nombreCompleto", is("Caja Uno")))
                .andExpect(jsonPath("$.correo", is("cajero1@prueba.com")))
                .andExpect(jsonPath("$.rol", is("VENDEDOR")))
                .andExpect(jsonPath("$.estado", is("ACTIVO")))
                .andExpect(jsonPath("$.formatoContrasena", is("BCrypt")))
                .andExpect(jsonPath("$.idTienda", is(tiendaA.getIdTienda())));

        Usuario creado = usuarioRepository.findByUsuario("cajero1").orElseThrow();
        assertEquals(Rol.VENDEDOR, creado.getIdRol());
        assertEquals(tiendaA.getIdTienda(), creado.getIdTienda());
        assertTrue(creado.getContrasena().startsWith("$2"),
                "La contraseña de la cuenta nueva debe quedar como hash BCrypt");

        // Sin rol en el cuerpo también entra como vendedor.
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero2",
                                "nombre", "Caja",
                                "correo", "cajero2@prueba.com",
                                "password", CLAVE_STAFF))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol", is("VENDEDOR")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("cajero1", CLAVE_STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("VENDEDOR")))
                .andExpect(jsonPath("$.idTienda", is(tiendaA.getIdTienda())));

        // La cuenta nueva no hereda el panel: /api/admin es solo del ADMIN.
        String tokenCajero = CuentasTest.token(mockMvc, objectMapper, "cajero1", CLAVE_STAFF);
        mockMvc.perform(get(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCajero))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAltaRespetaLosPuestosDePersonalDelPlan() throws Exception {
        // En la tienda hay dos puestos ocupados (ADMIN + VENDEDOR), asi que un
        // plan de 2 no deja crear otra cuenta.
        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASICO\",\"limiteProductos\":100,\"limiteUsuarios\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitePersonal", is(2)))
                .andExpect(jsonPath("$.personalEnUso", is(2)));

        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero6",
                                "nombre", "Caja",
                                "correo", "cajero6@prueba.com",
                                "password", CLAVE_STAFF))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("usuarios de su plan")));

        // Con un puesto mas el alta pasa y el contador lo refleja.
        mockMvc.perform(put(planUrl(tiendaA))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASICO\",\"limiteProductos\":100,\"limiteUsuarios\":3}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero6",
                                "nombre", "Caja",
                                "correo", "cajero6@prueba.com",
                                "password", CLAVE_STAFF))))
                .andExpect(status().isCreated());

        mockMvc.perform(get(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitePersonal", is(3)))
                .andExpect(jsonPath("$.personalEnUso", is(3)))
                .andExpect(jsonPath("$.personal", hasSize(2)));
    }

    @Test
    void elAdminEditaYSuspendeASuPersonal() throws Exception {
        Usuario vendedor = usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR)
                .orElseThrow();
        String cuentaUrl = PERSONAL_URL + "/" + vendedor.getIdUsuario();

        mockMvc.perform(put(cuentaUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "vendedor2",
                                "nombre", "Vendedor",
                                "apellido", "Actualizado",
                                "correo", "vendedor2@prueba.com",
                                "password", "NuevaClave#2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario", is("vendedor2")))
                .andExpect(jsonPath("$.nombreCompleto", is("Vendedor Actualizado")))
                .andExpect(jsonPath("$.correo", is("vendedor2@prueba.com")))
                .andExpect(jsonPath("$.rol", is("VENDEDOR")));

        // La clave nueva sirve y la anterior deja de funcionar.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("vendedor2", "NuevaClave#2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol", is("VENDEDOR")));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("vendedor2", CuentasTest.CLAVE)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(cuentaUrl + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("SUSPENDIDO")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("vendedor2", "NuevaClave#2026")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(cuentaUrl + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ACTIVO")));

        String tokenReactivado = CuentasTest.token(mockMvc, objectMapper, "vendedor2",
                "NuevaClave#2026");
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenReactivado))
                .andExpect(status().isOk());

        // El vendedor conserva su rol: el panel del ADMIN no lo asciende.
        assertEquals(Rol.VENDEDOR, usuarioRepository.findByUsuario("vendedor2")
                .orElseThrow().getIdRol());
    }

    @Test
    void elAdminNoPuedeTocarCuentasDeOtraTiendaNiAdministradores() throws Exception {
        Usuario adminB = usuarioRepository.findByUsuario(USUARIO_ADMIN_B).orElseThrow();
        mockMvc.perform(put(PERSONAL_URL + "/" + adminB.getIdUsuario())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", USUARIO_ADMIN_B,
                                "nombre", "Admin",
                                "correo", CORREO_ADMIN_B))))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(PERSONAL_URL + "/" + adminB.getIdUsuario() + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());

        // El propio ADMIN tampoco se gestiona desde esta vista: solo vendedores.
        Usuario adminA = usuarioRepository.findByUsuario(CuentasTest.USUARIO_ADMIN).orElseThrow();
        mockMvc.perform(put(PERSONAL_URL + "/" + adminA.getIdUsuario() + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());

        Usuario superCuenta = usuarioRepository.findByUsuario(CuentasTest.USUARIO_SUPER)
                .orElseThrow();
        mockMvc.perform(put(PERSONAL_URL + "/" + superCuenta.getIdUsuario() + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(PERSONAL_URL + "/999999/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());

        // Ninguna de esas llamadas pudo suspender al personal de la tienda B.
        assertEquals(Rol.ADMIN, usuarioRepository.findById(adminB.getIdUsuario())
                .orElseThrow().getIdRol());
        assertTrue(usuarioRepository.findFirstByIdTiendaAndIdRolOrderByIdUsuarioAsc(
                tiendaB.getIdTienda(), Rol.ADMIN).isPresent());
    }

    @Test
    void elAltaRechazaIdentidadesDeCompradorYCuentasDuplicadas() throws Exception {
        // Duplicados del propio personal.
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("vendedor", "Otro", "repetido@prueba.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("nombre de usuario")));
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("cajero3", "Otro", CuentasTest.CORREO_VENDEDOR)))
                .andExpect(status().isConflict());

        // El comprador registrado en la tienda no puede compartir correo: su
        // persona ya ocupa ese correo en la plataforma.
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("cajero4", "Caja", CuentasTest.CORREO_CLIENTE)))
                .andExpect(status().isConflict());

        // Comprador que solo existe en la tabla cliente (sin persona).
        Cliente soloComprador = new Cliente("CLI-SOLO-CORREO", "Solo", "Comprador",
                "987654399", "solo.comprador@prueba.com");
        soloComprador.setContrasena(CuentasTest.HASH_123456);
        soloComprador.setIdRol(Rol.CLIENTE);
        clienteRepository.save(soloComprador);

        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("cajero5", "Caja", "solo.comprador@prueba.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("comprador")));

        // El correo del comprador tampoco sirve como nombre de cuenta.
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("solo.comprador@prueba.com", "Caja", "otro@prueba.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("comprador")));

        // La contraseña mantiene la longitud mínima del backend.
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usuario", "cajero6",
                                "nombre", "Caja",
                                "correo", "cajero6@prueba.com",
                                "password", "corta"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elCrudDePersonalEsExclusivoDelAdminDeLaTienda() throws Exception {
        mockMvc.perform(get(PERSONAL_URL))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PERSONAL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("intruso", "Intruso", "intruso@prueba.com")))
                .andExpect(status().isUnauthorized());

        Usuario vendedor = usuarioRepository.findByUsuario(CuentasTest.USUARIO_VENDEDOR)
                .orElseThrow();
        for (String token : List.of(tokenVendedor, tokenCliente, tokenSuper)) {
            mockMvc.perform(get(PERSONAL_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(PERSONAL_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(alta("intruso", "Intruso", "intruso@prueba.com")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(PERSONAL_URL + "/" + vendedor.getIdUsuario() + "/estado")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"SUSPENDIDO\"}"))
                    .andExpect(status().isForbidden());
        }

        // El SUPER_ADMIN tampoco hereda el panel de tienda.
        mockMvc.perform(get("/api/dashboard/resumen")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSuper))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk());
    }

    @Test
    void laGestionQuedaAuditadaConElAdminComoActor() throws Exception {
        mockMvc.perform(post(PERSONAL_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta("cajero7", "Caja", "cajero7@prueba.com")))
                .andExpect(status().isCreated());
        Usuario creado = usuarioRepository.findByUsuario("cajero7").orElseThrow();

        mockMvc.perform(put(PERSONAL_URL + "/" + creado.getIdUsuario() + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isOk());

        Integer altas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_evento WHERE tipo = 'USUARIO_CREADO'"
                        + " AND actor = ? AND id_tienda = ?",
                Integer.class, CuentasTest.USUARIO_ADMIN, tiendaA.getIdTienda());
        Integer suspensiones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_evento WHERE tipo = 'USUARIO_SUSPENDIDO'"
                        + " AND actor = ? AND id_tienda = ?",
                Integer.class, CuentasTest.USUARIO_ADMIN, tiendaA.getIdTienda());
        assertEquals(1, altas.intValue());
        assertEquals(1, suspensiones.intValue());
    }

    private void crearAdminDeEmpresaB() {
        Persona persona = new Persona();
        persona.setIdPersona(ID_PERSONA_ADMIN_B);
        persona.setNombre("Admin");
        persona.setApellido("Empresa Dos");
        persona.setCorreo(CORREO_ADMIN_B);
        persona.setTelefono("987654320");
        persona.setUsuario(USUARIO_ADMIN_B);
        personaRepository.save(persona);

        Usuario usuario = new Usuario(USUARIO_ADMIN_B, CuentasTest.HASH_123456,
                ID_PERSONA_ADMIN_B, Rol.ADMIN);
        usuario.setIdTienda(tiendaB.getIdTienda());
        usuarioRepository.save(usuario);
    }

    private String alta(String usuario, String nombre, String correo) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "usuario", usuario,
                "nombre", nombre,
                "correo", correo,
                "password", CLAVE_STAFF));
    }

    private String credenciales(String usuario, String clave) throws Exception {
        return objectMapper.writeValueAsString(Map.of("usuario", usuario, "clave", clave));
    }

    private String planUrl(Tienda tienda) {
        return "/api/super-admin/empresas/" + tienda.getIdTienda() + "/plan";
    }
}
