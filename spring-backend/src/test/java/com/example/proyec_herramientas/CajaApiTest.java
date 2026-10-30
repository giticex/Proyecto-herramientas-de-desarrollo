package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.MovimientoCajaRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Apertura, resumen y cierre de caja del POS: la tienda y el vendedor salen
// del token, el resumen se separa por metodo de pago y el cierre guarda el
// arqueo fisico con su diferencia.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CajaApiTest {

    private static final String CAJA_URL = "/api/caja";
    private static final String APERTURA_URL = "/api/caja/apertura";
    private static final String CIERRE_URL = "/api/caja/cierre";
    private static final String SLUG_EMPRESA_B = "empresa-dos";
    private static final String USUARIO_ADMIN_B = "adminb";
    private static final String CORREO_ADMIN_B = "adminb@prueba.com";
    private static final String ID_PERSONA_ADMIN_B = "PER-PRUEBA-ADMB";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovimientoCajaRepository movimientoRepository;

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
        jdbcTemplate.update("DELETE FROM movimiento_caja");
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
    void elVendedorAbreSuCajaConElFondoInicial() throws Exception {
        JsonNode inicial = estado(tokenVendedor);
        assertFalse(inicial.get("turnoAbierto").asBoolean());
        assertEquals("SIN_TURNO", inicial.get("estado").asText());
        assertEquals(4, inicial.get("metodosPago").size(),
                "Sin ventas se muestran los cuatro grupos fijos");

        JsonNode abierta = abrir(tokenVendedor, "150.00");
        assertTrue(abierta.get("turnoAbierto").asBoolean());
        assertEquals("ABIERTA", abierta.get("estado").asText());
        assertEquals("vendedor", abierta.get("usuario").asText());
        assertEquals("Vendedor Prueba", abierta.get("nombreVendedor").asText());
        importe("150.00", abierta.get("montoApertura"));
        importe("150.00", abierta.get("efectivoEsperado"));
        importe("0.00", abierta.get("totalVentas"));
        assertTrue(abierta.hasNonNull("fechaApertura"));

        JsonNode consultada = estado(tokenVendedor);
        assertEquals(abierta.get("idCaja").asText(), consultada.get("idCaja").asText());
        assertTrue(consultada.get("turnoAbierto").asBoolean());
    }

    @Test
    void laCajaNoSeAbreDosVecesSinCerrarLaAnterior() throws Exception {
        abrir(tokenVendedor, "100.00");

        mockMvc.perform(post(APERTURA_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoApertura("50.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("turno de caja abierto")));

        assertEquals(1, movimientoRepository.count());
    }

    @Test
    void elResumenSeparaLasVentasPorMetodoYCalculaElEfectivoEsperado() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");
        abrir(tokenVendedor, "100.00");

        vender(tokenVendedor, "Efectivo", 1, 2);   // 100.00
        vender(tokenVendedor, "Tarjeta", 1, 1);    // 50.00
        vender(tokenVendedor, "Yape", 1, 1);       // 50.00

        JsonNode estado = estado(tokenVendedor);
        assertEquals(3, estado.get("comprobantes").asLong());
        importe("200.00", estado.get("totalVentas"));
        importe("100.00", grupoDe(estado, "Efectivo"));
        importe("50.00", grupoDe(estado, "Tarjeta"));
        importe("50.00", grupoDe(estado, "Yape/Plin"));
        importe("0.00", grupoDe(estado, "Créditos"));
        // Fondo inicial 100 + ventas en efectivo 100.
        importe("200.00", estado.get("efectivoEsperado"));
    }

    @Test
    void soloSeResumenLasVentasDelVendedorAutenticado() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "40.00");
        abrir(tokenVendedor, "0.00");

        vender(tokenVendedor, "Efectivo", 1, 1);  // 40.00 del vendedor
        vender(tokenAdminA, "Efectivo", 1, 2);    // 80.00 del admin (mismo dia)

        JsonNode cajaVendedor = estado(tokenVendedor);
        assertEquals(1, cajaVendedor.get("comprobantes").asLong());
        importe("40.00", cajaVendedor.get("totalVentas"));
        importe("40.00", cajaVendedor.get("efectivoEsperado"));

        // El admin no abrio caja: ve el dia, pero solo sus propias ventas.
        JsonNode cajaAdmin = estado(tokenAdminA);
        assertEquals("SIN_TURNO", cajaAdmin.get("estado").asText());
        assertEquals(1, cajaAdmin.get("comprobantes").asLong());
        importe("80.00", cajaAdmin.get("totalVentas"));
    }

    @Test
    void elCierreGuardaElArqueoFisicoYLaDiferencia() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");
        JsonNode abierta = abrir(tokenVendedor, "100.00");
        vender(tokenVendedor, "Efectivo", 1, 2); // +100.00 -> esperado 200.00

        JsonNode cierre = cerrar(tokenVendedor, "190.00", "Falto cambio de un billete");
        assertEquals("CERRADA", cierre.get("estado").asText());
        assertFalse(cierre.get("turnoAbierto").asBoolean());
        importe("200.00", cierre.get("montoSistema"));
        importe("190.00", cierre.get("efectivoContado"));
        importe("-10.00", cierre.get("diferencia"));
        assertEquals("Falto cambio de un billete", cierre.get("observacion").asText());
        assertTrue(cierre.hasNonNull("fechaCierre"));
        assertEquals(abierta.get("idCaja").asText(), cierre.get("idCaja").asText());

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT estado, monto_sistema, efectivo_contado, diferencia "
                        + "FROM movimiento_caja WHERE id_caja = ?", cierre.get("idCaja").asText());
        assertEquals("CERRADA", fila.get("estado"));
        assertEquals(0, new BigDecimal("200.00").compareTo((BigDecimal) fila.get("monto_sistema")));
        assertEquals(0, new BigDecimal("190.00").compareTo((BigDecimal) fila.get("efectivo_contado")));
        assertEquals(0, new BigDecimal("-10.00").compareTo((BigDecimal) fila.get("diferencia")));

        // El cierre se audita con su diferencia.
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_evento WHERE tipo = 'CAJA'", Integer.class));
        String descripcion = jdbcTemplate.queryForObject(
                "SELECT descripcion FROM auditoria_evento WHERE tipo = 'CAJA' ORDER BY id_evento DESC LIMIT 1",
                String.class);
        assertTrue(descripcion.contains("diferencia S/ -10.00"), descripcion);
        assertTrue(descripcion.contains("faltante"), descripcion);

        JsonNode consultada = estado(tokenVendedor);
        assertEquals("CERRADA", consultada.get("estado").asText());
        importe("-10.00", consultada.get("diferencia"));

        mockMvc.perform(post(CIERRE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCierre("190.00", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("No hay un turno de caja abierto")));
    }

    @Test
    void laCajaSeCierraCuadradaYSinObservacion() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "25.00");
        abrir(tokenVendedor, "75.00");
        vender(tokenVendedor, "Efectivo", 1, 1); // 25 -> esperado 100

        JsonNode cierre = cerrar(tokenVendedor, "100.00", "");
        importe("0.00", cierre.get("diferencia"));
        assertEquals("", cierre.get("observacion").asText());
        String severidad = jdbcTemplate.queryForObject(
                "SELECT severidad FROM auditoria_evento WHERE tipo = 'CAJA' ORDER BY id_evento DESC LIMIT 1",
                String.class);
        assertEquals("INFO", severidad);
    }

    @Test
    void cadaTiendaOperaSuPropiaCaja() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "10.00");
        crearProducto(900, tiendaB, "Polo Oversize", 100, "70.00");

        JsonNode cajaA = abrir(tokenVendedor, "20.00");
        JsonNode cajaB = abrir(tokenAdminB, "300.00");
        assertNotEquals(cajaA.get("idCaja").asText(), cajaB.get("idCaja").asText());

        vender(tokenVendedor, "Efectivo", 1, 1);   // 10.00 en A
        vender(tokenAdminB, "Efectivo", 900, 1);   // 70.00 en B

        System.out.println("DIAG caja=" + jdbcTemplate.queryForList(
                "SELECT usuario, fecha_apertura, fecha_cierre FROM movimiento_caja"));
        System.out.println("DIAG ventas=" + jdbcTemplate.queryForList(
                "SELECT vendedor_usuario, fecha_emision, total_venta, id_medio_pago FROM venta"));
        System.out.println("DIAG ahora=" + new java.util.Date());
        System.out.println("DIAG estadoA=" + estado(tokenVendedor));

        importe("30.00", estado(tokenVendedor).get("efectivoEsperado"));
        importe("370.00", estado(tokenAdminB).get("efectivoEsperado"));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movimiento_caja", Integer.class));
    }

    @Test
    void laAperturaIgnoraLaTiendaYElUsuarioDelCuerpo() throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("montoApertura", new BigDecimal("60.00"));
        cuerpo.put("idTienda", tiendaB.getIdTienda());
        cuerpo.put("usuario", USUARIO_ADMIN_B);
        cuerpo.put("nombreVendedor", "Otro");

        MvcResult resultado = mockMvc.perform(post(APERTURA_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode abierta = leer(resultado);

        assertEquals("vendedor", abierta.get("usuario").asText());
        assertEquals("Vendedor Prueba", abierta.get("nombreVendedor").asText());
        assertEquals(tiendaA.getIdTienda(), jdbcTemplate.queryForObject(
                "SELECT id_tienda FROM movimiento_caja WHERE id_caja = ?", Integer.class,
                abierta.get("idCaja").asText()));
    }

    @Test
    void laCajaEsSoloParaElPersonalDeLaTienda() throws Exception {
        mockMvc.perform(get(CAJA_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(APERTURA_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoApertura("10.00")))
                .andExpect(status().isUnauthorized());

        for (String token : List.of(tokenCliente, tokenSuper)) {
            mockMvc.perform(get(CAJA_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(APERTURA_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoApertura("10.00")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(CIERRE_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCierre("10.00", "")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laAperturaYElCierreValidanLosMontos() throws Exception {
        mockMvc.perform(post(APERTURA_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoApertura("-5.00")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(APERTURA_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("montoApertura")));

        mockMvc.perform(post(CIERRE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCierre("10.00", "")))
                .andExpect(status().isConflict());

        abrir(tokenVendedor, "10.00");
        mockMvc.perform(post(CIERRE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("efectivoContado")));

        mockMvc.perform(post(CIERRE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCierre("10.00", "x".repeat(201))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JsonNode estado(String token) throws Exception {
        return leer(mockMvc.perform(get(CAJA_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode abrir(String token, String monto) throws Exception {
        return leer(mockMvc.perform(post(APERTURA_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoApertura(monto)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode cerrar(String token, String contado, String observacion) throws Exception {
        return leer(mockMvc.perform(post(CIERRE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCierre(contado, observacion)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String cuerpoApertura(String monto) throws Exception {
        return objectMapper.writeValueAsString(Map.of("montoApertura", new BigDecimal(monto)));
    }

    private String cuerpoCierre(String contado, String observacion) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("efectivoContado", new BigDecimal(contado));
        cuerpo.put("observacion", observacion);
        return objectMapper.writeValueAsString(cuerpo);
    }

    private JsonNode vender(String token, String medioPago, int idProducto, int cantidad)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipoDocumento", "Boleta");
        cuerpo.put("medioPago", medioPago);
        cuerpo.put("idCliente", CuentasTest.ID_CLIENTE);
        cuerpo.put("documentoCliente", "12345678");
        cuerpo.put("items", List.of(Map.of("idProducto", idProducto, "cantidad", cantidad)));
        return leer(mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private JsonNode leer(MvcResult resultado) throws Exception {
        return objectMapper.readTree(new String(
                resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private BigDecimal grupoDe(JsonNode estado, String nombre) {
        for (JsonNode grupo : estado.get("metodosPago")) {
            if (nombre.equals(grupo.get("grupo").asText())) {
                return grupo.get("total").decimalValue();
            }
        }
        throw new AssertionError("El resumen no incluye el grupo " + nombre);
    }

    private void importe(String esperado, JsonNode valor) {
        importe(esperado, valor.decimalValue());
    }

    private void importe(String esperado, BigDecimal valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor),
                "Importe esperado " + esperado + " pero fue " + valor);
    }

    private void crearProducto(int id, Tienda tienda, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
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
}
