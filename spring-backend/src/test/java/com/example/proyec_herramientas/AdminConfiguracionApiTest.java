package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Producto;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Configuracion comercial y de facturacion de la tienda del ADMIN. La empresa
// sale siempre del token: estas pruebas comprueban la persistencia, las
// validaciones fiscales y que las series y los datos del emisor lleguen a los
// comprobantes (POS y emision electronica).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminConfiguracionApiTest {

    private static final String CONFIGURACION_URL = "/api/admin/configuracion";
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
    void laTiendaSinConfigurarDevuelveLosCamposVacios() throws Exception {
        mockMvc.perform(get(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.slug", is(CuentasTest.SLUG_TIENDA)))
                .andExpect(jsonPath("$.razonSocial", is("")))
                .andExpect(jsonPath("$.ruc", is("")))
                .andExpect(jsonPath("$.direccionFiscal", is("")))
                .andExpect(jsonPath("$.telefono", is("")))
                .andExpect(jsonPath("$.logoUrl", is("")))
                .andExpect(jsonPath("$.serieBoleta", is("")))
                .andExpect(jsonPath("$.serieFactura", is("")));
    }

    @Test
    void elAdminGuardaSuConfiguracionYLaVuelveALeer() throws Exception {
        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Tienda Prueba")))
                .andExpect(jsonPath("$.razonSocial", is("Comercial Andina S.A.C.")))
                .andExpect(jsonPath("$.ruc", is("20604567891")))
                .andExpect(jsonPath("$.direccionFiscal", is("Av. Los Próceres 123, Lima")))
                .andExpect(jsonPath("$.telefono", is("+51 1 555-1234")))
                .andExpect(jsonPath("$.logoUrl", is("https://cdn.prueba.com/logo.png")))
                .andExpect(jsonPath("$.serieBoleta", is("B002")))
                .andExpect(jsonPath("$.serieFactura", is("F002")));

        mockMvc.perform(get(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial", is("Comercial Andina S.A.C.")))
                .andExpect(jsonPath("$.ruc", is("20604567891")))
                .andExpect(jsonPath("$.serieBoleta", is("B002")))
                .andExpect(jsonPath("$.serieFactura", is("F002")));

        Tienda guardada = tiendaRepository.findById(tiendaA.getIdTienda()).orElseThrow();
        assertEquals("20604567891", guardada.getRuc());
        assertEquals("Comercial Andina S.A.C.", guardada.getRazonSocial());
        assertEquals("Av. Los Próceres 123, Lima", guardada.getDireccionFiscal());
        assertEquals("+51 1 555-1234", guardada.getTelefono());
        assertEquals("https://cdn.prueba.com/logo.png", guardada.getLogoUrl());
        assertEquals("B002", guardada.getSerieBoleta());
        assertEquals("F002", guardada.getSerieFactura());
        // El nombre y el slug identifican la tienda en la plataforma: el
        // formulario los muestra pero no los reescribe.
        assertEquals("Tienda Prueba", guardada.getNombre());
        assertEquals(CuentasTest.SLUG_TIENDA, guardada.getSlug());
    }

    @Test
    void lasSeriesSeGuardanEnMayusculasYAceptanElFormatoDeSunat() throws Exception {
        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "b101", "f9x2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serieBoleta", is("B101")))
                .andExpect(jsonPath("$.serieFactura", is("F9X2")));
    }

    @Test
    void laConfiguracionRechazaDatosFiscalesInvalidos() throws Exception {
        for (String cuerpo : List.of(
                configuracion("123", "B001", "F001"),
                configuracion("30604567891", "B001", "F001"),
                configuracion("20604567891", "F001", "F001"),
                configuracion("20604567891", "B001", "B001"),
                configuracion("20604567891", "B01", "F001"),
                configuracion("20604567891", "B001", "F001").replace("+51 1 555-1234", "abc"),
                configuracion("20604567891", "B001", "F001").replace(
                        "https://cdn.prueba.com/logo.png", "logo.png"),
                configuracion("20604567891", "B001", "F001").replace(
                        "Comercial Andina S.A.C.", " "),
                configuracion("20604567891", "B001", "F001").replace(
                        "Av. Los Próceres 123, Lima", ""))) {
            mockMvc.perform(put(CONFIGURACION_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        // Nada de lo rechazado quedo guardado a medias.
        Tienda intacta = tiendaRepository.findById(tiendaA.getIdTienda()).orElseThrow();
        assertNull(intacta.getRuc());
        assertNull(intacta.getSerieBoleta());
        assertNull(intacta.getLogoUrl());
    }

    @Test
    void cadaAdminSoloVeYCambiaLaConfiguracionDeSuTienda() throws Exception {
        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")))
                .andExpect(status().isOk());

        mockMvc.perform(get(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Empresa Dos")))
                .andExpect(jsonPath("$.ruc", is("")))
                .andExpect(jsonPath("$.serieBoleta", is("")));

        // Lo que guarda la empresa B no toca a la empresa A.
        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20555555555", "B900", "F900")))
                .andExpect(status().isOk());

        assertEquals("20604567891",
                tiendaRepository.findById(tiendaA.getIdTienda()).orElseThrow().getRuc());
        assertEquals("B002",
                tiendaRepository.findById(tiendaA.getIdTienda()).orElseThrow().getSerieBoleta());
        assertEquals("20555555555",
                tiendaRepository.findById(tiendaB.getIdTienda()).orElseThrow().getRuc());
    }

    @Test
    void laConfiguracionEsExclusivaDelAdminDeLaTienda() throws Exception {
        mockMvc.perform(get(CONFIGURACION_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(CONFIGURACION_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")))
                .andExpect(status().isUnauthorized());

        for (String token : List.of(tokenVendedor, tokenCliente, tokenSuper)) {
            mockMvc.perform(get(CONFIGURACION_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(CONFIGURACION_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(configuracion("20604567891", "B002", "F002")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elCambioQuedaAuditadoConElAdminComoActor() throws Exception {
        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")))
                .andExpect(status().isOk());

        Integer eventos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_evento WHERE tipo = 'CONFIGURACION_ACTUALIZADA'"
                        + " AND actor = ? AND id_tienda = ? AND tienda_nombre = ?",
                Integer.class, CuentasTest.USUARIO_ADMIN, tiendaA.getIdTienda(), "Tienda Prueba");
        assertEquals(1, eventos.intValue());

        String descripcion = jdbcTemplate.queryForObject(
                "SELECT descripcion FROM auditoria_evento WHERE tipo = 'CONFIGURACION_ACTUALIZADA'",
                String.class);
        org.junit.jupiter.api.Assertions.assertTrue(descripcion.contains("B002/F002"),
                "La auditoria debe dejar constancia de las series guardadas");
    }

    @Test
    void lasVentasDelPosUsanLaSerieConfigurada() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 20, "50.00");

        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "12345678", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serie", is("B002")))
                .andExpect(jsonPath("$.numeroDocumento", matchesPattern("^B002-\\d{6}$")));

        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Factura", "20123456789", "Cliente Corporativo S.A.C.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serie", is("F002")))
                .andExpect(jsonPath("$.numeroDocumento", matchesPattern("^F002-\\d{6}$")));
    }

    @Test
    void elComprobanteElectronicoUsaLaSerieYElEmisorDeLaTienda() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 20, "50.00");

        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")))
                .andExpect(status().isOk());

        MvcResult emision = mockMvc.perform(post("/api/comprobantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comprobante()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serie", is("B002")))
                .andExpect(jsonPath("$.numeroDocumento", matchesPattern("^B002-\\d{6}$")))
                .andExpect(jsonPath("$.nombreComercio", is("Tienda Prueba")))
                .andExpect(jsonPath("$.emisor.ruc", is("20604567891")))
                .andExpect(jsonPath("$.emisor.razonSocial", is("Comercial Andina S.A.C.")))
                .andExpect(jsonPath("$.emisor.nombreComercial", is("Tienda Prueba")))
                .andExpect(jsonPath("$.emisor.domicilioFiscal", is("Av. Los Próceres 123, Lima")))
                .andExpect(jsonPath("$.emisor.telefono", is("+51 1 555-1234")))
                .andReturn();

        String idVenta = objectMapper.readTree(emision.getResponse().getContentAsString())
                .get("idVenta").asText();

        // Al volver a leerlo desde la base tambien sale el emisor de la tienda.
        mockMvc.perform(get("/api/comprobantes/" + idVenta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emisor.ruc", is("20604567891")))
                .andExpect(jsonPath("$.emisor.razonSocial", is("Comercial Andina S.A.C.")))
                .andExpect(jsonPath("$.nombreComercio", is("Tienda Prueba")));

        // El XML UBL que se enviaria al CDR usa el RUC configurado.
        String xml = mockMvc.perform(get("/api/comprobantes/" + idVenta + "/xml")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(xml.contains("20604567891"),
                "El XML debe identificar al emisor con el RUC de la configuracion");
        org.junit.jupiter.api.Assertions.assertTrue(xml.contains("Comercial Andina S.A.C."),
                "El XML debe llevar la razon social de la configuracion");
    }

    @Test
    void unaVentaNoPuedeMezclarProductosDeDosTiendas() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 20, "50.00");
        crearProducto(2, tiendaB, "Polo Oversize", 20, "30.00");

        // El comprador de la tienda publica no esta restringido por catalogo:
        // la venta se rechaza porque una sola venta no puede tener dos emisores.
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tipoDocumento", "Boleta",
                                "medioPago", "Efectivo",
                                "items", List.of(
                                        Map.of("idProducto", 1, "cantidad", 1),
                                        Map.of("idProducto", 2, "cantidad", 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("misma tienda")));

        assertEquals(0, ventaRepository.findAll().size(),
                "La venta mixta no debe quedar registrada");
        assertEquals(20, productoRepository.findById(1).orElseThrow().getStock());
        assertEquals(20, productoRepository.findById(2).orElseThrow().getStock());

        // El ADMIN si esta restringido antes: solo vende productos de su tienda.
        mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tipoDocumento", "Boleta",
                                "medioPago", "Efectivo",
                                "items", List.of(Map.of("idProducto", 2, "cantidad", 1))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elLogoInaccesibleNoImpideImprimirElComprobante() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 20, "50.00");

        // URL valida para el formulario pero no descargable: el PDF se emite
        // sin logo en lugar de fallar.
        mockMvc.perform(put(CONFIGURACION_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuracion("20604567891", "B002", "F002")
                                .replace("https://cdn.prueba.com/logo.png",
                                        "http://127.0.0.1/logo.png")))
                .andExpect(status().isOk());

        String idVenta = objectMapper.readTree(mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrito("Boleta", "12345678", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/comprobantes/" + idVenta + "/ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/api/comprobantes/" + idVenta + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    // ------------------------------------------------------------------

    private void crearProducto(int id, Tienda tienda, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
    }

    private String configuracion(String ruc, String serieBoleta, String serieFactura)
            throws Exception {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombre", "Nombre alterado");
        cuerpo.put("slug", "slug-alterado");
        cuerpo.put("razonSocial", "Comercial Andina S.A.C.");
        cuerpo.put("ruc", ruc);
        cuerpo.put("direccionFiscal", "Av. Los Próceres 123, Lima");
        cuerpo.put("telefono", "+51 1 555-1234");
        cuerpo.put("logoUrl", "https://cdn.prueba.com/logo.png");
        cuerpo.put("serieBoleta", serieBoleta);
        cuerpo.put("serieFactura", serieFactura);
        return objectMapper.writeValueAsString(cuerpo);
    }

    private String carrito(String tipoDocumento, String documento, String razonSocial)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipoDocumento", tipoDocumento);
        cuerpo.put("medioPago", "Efectivo");
        cuerpo.put("idCliente", CuentasTest.ID_CLIENTE);
        cuerpo.put("documentoCliente", documento);
        if (razonSocial != null) {
            cuerpo.put("razonSocial", razonSocial);
        }
        cuerpo.put("items", List.of(Map.of("idProducto", 1, "cantidad", 1)));
        return objectMapper.writeValueAsString(cuerpo);
    }

    private String comprobante() throws Exception {
        Map<String, Object> receptor = new LinkedHashMap<>();
        receptor.put("tipoDocumento", "1");
        receptor.put("numeroDocumento", "12345678");
        receptor.put("razonSocial", "Juan Pérez");

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipoComprobante", "03");
        cuerpo.put("idCliente", CuentasTest.ID_CLIENTE);
        cuerpo.put("receptor", receptor);
        cuerpo.put("items", List.of(Map.of("idProducto", 1, "cantidad", 1)));
        return objectMapper.writeValueAsString(cuerpo);
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
