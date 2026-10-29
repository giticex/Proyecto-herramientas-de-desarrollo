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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Corte de caja del ADMIN: desglose por metodo de pago, por vendedor y por
// turno, aislamiento entre empresas, roles permitidos y exportaciones CSV/PDF.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminArqueoApiTest {

    private static final String ARQUEO_URL = "/api/admin/arqueo";
    private static final String EXPORT_URL = "/api/admin/arqueo/export";
    private static final String SLUG_EMPRESA_B = "empresa-dos";
    private static final String USUARIO_ADMIN_B = "adminb";
    private static final String CORREO_ADMIN_B = "adminb@prueba.com";
    private static final String ID_PERSONA_ADMIN_B = "PER-PRUEBA-ADMB";
    private static final String USUARIO_VENDEDOR_2 = "vendedor2";

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
    private int correlativoJdbc;

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
        correlativoJdbc = 0;

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
    void elArqueoDelDiaDesglosaLosIngresosPorMetodoDePago() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");
        crearProducto(2, tiendaA, "Teclado", 100, "30.00");

        vender(tokenAdminA, "Efectivo", 1, 2);
        vender(tokenAdminA, "Tarjeta", 2, 1);
        vender(tokenAdminA, "Yape", 1, 1);
        vender(tokenAdminA, "Plin", 2, 1);

        JsonNode arqueo = arqueo(tokenAdminA, "diario", null);

        assertEquals("DIARIO", arqueo.get("periodo").asText());
        assertEquals("Tienda Prueba", arqueo.get("tiendaNombre").asText());
        importe("210.00", arqueo.get("totalVentas"));
        importe("52.50", arqueo.get("ticketPromedio"));
        assertEquals(4, arqueo.get("comprobantes").asLong());
        assertEquals(4, arqueo.get("detalle").size());

        JsonNode grupos = arqueo.get("metodosPago");
        assertEquals(4, grupos.size(), "Siempre se listan los cuatro grupos del corte");

        assertEquals("Efectivo", grupos.get(0).get("grupo").asText());
        importe("100.00", grupos.get(0).get("total"));
        assertEquals(1, grupos.get(0).get("comprobantes").asLong());
        importe("47.62", grupos.get(0).get("porcentaje"));

        assertEquals("Tarjeta", grupos.get(1).get("grupo").asText());
        importe("30.00", grupos.get(1).get("total"));
        importe("14.29", grupos.get(1).get("porcentaje"));

        assertEquals("Yape/Plin", grupos.get(2).get("grupo").asText());
        importe("80.00", grupos.get(2).get("total"));
        assertEquals(2, grupos.get(2).get("comprobantes").asLong());
        importe("38.10", grupos.get(2).get("porcentaje"));
        assertEquals(2, grupos.get(2).get("metodos").size());
        String metodosYapePlin = grupos.get(2).get("metodos").toString();
        assertTrue(metodosYapePlin.contains("Yape") && metodosYapePlin.contains("Plin"),
                "El grupo Yape/Plin debe nombrar los medios usados: " + metodosYapePlin);

        assertEquals("Créditos", grupos.get(3).get("grupo").asText());
        importe("0.00", grupos.get(3).get("total"));
        assertEquals(0, grupos.get(3).get("comprobantes").asLong());
        importe("0.00", grupos.get(3).get("porcentaje"));
    }

    @Test
    void elArqueoAtribuyeLasVentasAlVendedorQueLasAtendio() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");
        crearProducto(2, tiendaA, "Teclado", 100, "60.00");
        crearProducto(3, tiendaA, "Pad Mouse", 100, "30.00");
        crearVendedorSinVentas();

        JsonNode ventaAdmin = vender(tokenAdminA, "Efectivo", 1, 1);
        JsonNode ventaVendedor = vender(tokenVendedor, "Efectivo", 2, 1);
        JsonNode ventaCliente = vender(tokenCliente, "Efectivo", 3, 1);

        assertEquals("admin", ventaRepository.findById(ventaAdmin.get("id").asText()).orElseThrow()
                .getVendedorUsuario());
        assertEquals("Vendedor Prueba",
                ventaRepository.findById(ventaVendedor.get("id").asText()).orElseThrow()
                        .getVendedorNombre());
        // La autocompra del CLIENTE no tiene vendedor de mostrador.
        assertEquals(null, ventaRepository.findById(ventaCliente.get("id").asText()).orElseThrow()
                .getVendedorUsuario());

        JsonNode arqueo = arqueo(tokenAdminA, "diario", null);
        JsonNode vendedores = arqueo.get("vendedores");
        assertEquals(4, vendedores.size(), "Staff activo (aunque no venda) + sin vendedor");

        assertEquals("vendedor", vendedores.get(0).get("usuario").asText());
        assertEquals("Vendedor Prueba", vendedores.get(0).get("nombre").asText());
        importe("60.00", vendedores.get(0).get("total"));
        assertEquals(1, vendedores.get(0).get("comprobantes").asLong());

        assertEquals("admin", vendedores.get(1).get("usuario").asText());
        assertEquals("Administrador Prueba", vendedores.get(1).get("nombre").asText());
        importe("50.00", vendedores.get(1).get("total"));

        assertEquals("", vendedores.get(2).get("usuario").asText());
        assertEquals("Sin vendedor asignado", vendedores.get(2).get("nombre").asText());
        importe("30.00", vendedores.get(2).get("total"));

        assertEquals(USUARIO_VENDEDOR_2, vendedores.get(3).get("usuario").asText());
        importe("0.00", vendedores.get(3).get("total"));
        assertEquals(0, vendedores.get(3).get("comprobantes").asLong());

        JsonNode detalleCliente = detalleDe(arqueo, ventaCliente.get("id").asText());
        assertEquals("Sin vendedor asignado", detalleCliente.get("vendedor").asText());
        assertEquals("", detalleCliente.get("vendedorUsuario").asText());

        JsonNode detalleAdmin = detalleDe(arqueo, ventaAdmin.get("id").asText());
        assertEquals("Administrador Prueba", detalleAdmin.get("vendedor").asText());
        assertEquals("admin", detalleAdmin.get("vendedorUsuario").asText());
    }

    @Test
    void lasVentasAlCreditoSeMuestranEnSuPropioGrupo() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");

        // El grupo Créditos se decide por la forma de pago, no por el medio:
        // una venta al crédito pagada con "Efectivo" tampoco es caja del día.
        insertarVenta(1, "MP013", "02", "70.00", hoy(10, 0), "admin", "Administrador Prueba",
                "Juan Pérez");
        insertarVenta(1, "MP001", "02", "30.00", hoy(11, 0), "admin", "Administrador Prueba",
                "Juan Pérez");
        insertarVenta(1, "MP001", "01", "50.00", hoy(12, 0), "admin", "Administrador Prueba",
                "Juan Pérez");

        JsonNode arqueo = arqueo(tokenAdminA, "diario", null);
        importe("150.00", arqueo.get("totalVentas"));

        JsonNode grupos = arqueo.get("metodosPago");
        assertEquals("Efectivo", grupos.get(0).get("grupo").asText());
        importe("50.00", grupos.get(0).get("total"));
        assertEquals(1, grupos.get(0).get("comprobantes").asLong());

        assertEquals("Créditos", grupos.get(3).get("grupo").asText());
        importe("100.00", grupos.get(3).get("total"));
        assertEquals(2, grupos.get(3).get("comprobantes").asLong());

        int creditos = 0;
        boolean efectivoMarcadoComoCredito = false;
        for (JsonNode comprobante : arqueo.get("detalle")) {
            if ("Créditos".equals(comprobante.get("grupoPago").asText())) {
                creditos++;
                if ("Efectivo".equals(comprobante.get("medioPago").asText())) {
                    efectivoMarcadoComoCredito = true;
                }
            }
        }
        assertEquals(2, creditos);
        assertTrue(efectivoMarcadoComoCredito,
                "El detalle debe mostrar el medio Efectivo dentro del grupo Créditos");
    }

    @Test
    void elArqueoSumaPorTurnoYSeparaElDiaDelMes() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");

        LocalDate dia = LocalDate.of(2026, 9, 15);
        insertarVenta(1, "MP001", "01", "50.00", dia.atTime(8, 0), "admin", "Administrador Prueba",
                "Juan Pérez");
        insertarVenta(1, "MP002", "01", "30.00", dia.atTime(15, 0), "admin", "Administrador Prueba",
                "Juan Pérez");
        insertarVenta(1, "MP004", "01", "20.00", dia.atTime(23, 30), "admin", "Administrador Prueba",
                "Juan Pérez");
        // Vecinos del dia y del mes: no deben entrar en el corte diario.
        insertarVenta(1, "MP001", "01", "999.00", dia.minusDays(1).atTime(12, 0), "admin",
                "Administrador Prueba", "Juan Pérez");
        insertarVenta(1, "MP001", "01", "888.00", dia.plusDays(1).atTime(12, 0), "admin",
                "Administrador Prueba", "Juan Pérez");
        insertarVenta(1, "MP001", "01", "777.00", LocalDate.of(2026, 8, 31).atTime(12, 0), "admin",
                "Administrador Prueba", "Juan Pérez");
        insertarVenta(1, "MP001", "01", "666.00", LocalDate.of(2026, 10, 1).atTime(12, 0), "admin",
                "Administrador Prueba", "Juan Pérez");

        JsonNode diario = arqueo(tokenAdminA, "diario", dia);
        assertEquals("2026-09-15", diario.get("desde").asText());
        assertEquals("2026-09-15", diario.get("hasta").asText());
        importe("100.00", diario.get("totalVentas"));
        assertEquals(3, diario.get("comprobantes").asLong());

        JsonNode turnos = diario.get("turnos");
        assertEquals(3, turnos.size());
        assertEquals("Mañana", turnos.get(0).get("turno").asText());
        assertEquals("06:00 – 13:59", turnos.get(0).get("rango").asText());
        importe("50.00", turnos.get(0).get("total"));
        assertEquals(1, turnos.get(0).get("comprobantes").asLong());

        assertEquals("Tarde", turnos.get(1).get("turno").asText());
        assertEquals("14:00 – 20:59", turnos.get(1).get("rango").asText());
        importe("30.00", turnos.get(1).get("total"));

        assertEquals("Noche", turnos.get(2).get("turno").asText());
        assertEquals("21:00 – 05:59", turnos.get(2).get("rango").asText());
        importe("20.00", turnos.get(2).get("total"));

        JsonNode mensual = arqueo(tokenAdminA, "mensual", dia);
        assertEquals("2026-09-01", mensual.get("desde").asText());
        assertEquals("2026-09-30", mensual.get("hasta").asText());
        // 50 + 30 + 20 del dia 15 y los vecinos del 14 y del 16; agosto y
        // octubre quedan fuera.
        importe("1987.00", mensual.get("totalVentas"));
        assertEquals(5, mensual.get("comprobantes").asLong());

        mockMvc.perform(get(EXPORT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .param("periodo", "mensual")
                        .param("fecha", "2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("arqueo-mensual-2026-09.csv")));
    }

    @Test
    void cadaAdminSoloVeElArqueoDeSuTienda() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "25.00");
        crearProducto(3, tiendaB, "Polo Oversize", 100, "40.00");

        vender(tokenAdminA, "Efectivo", 1, 1);
        vender(tokenAdminB, "Efectivo", 3, 1);

        JsonNode arqueoA = arqueo(tokenAdminA, "diario", null);
        importe("25.00", arqueoA.get("totalVentas"));
        assertEquals(1, arqueoA.get("detalle").size());
        assertEquals(2, arqueoA.get("vendedores").size(),
                "En la empresa A solo aparecen su ADMIN y su VENDEDOR");
        assertEquals("admin", arqueoA.get("vendedores").get(0).get("usuario").asText());

        JsonNode arqueoB = arqueo(tokenAdminB, "diario", null);
        importe("40.00", arqueoB.get("totalVentas"));
        assertEquals(1, arqueoB.get("detalle").size());
        assertEquals(1, arqueoB.get("vendedores").size(),
                "En la empresa B solo aparece su ADMIN");
        assertEquals(USUARIO_ADMIN_B, arqueoB.get("vendedores").get(0).get("usuario").asText());
    }

    @Test
    void elArqueoEsExclusivoDelAdmin() throws Exception {
        mockMvc.perform(get(ARQUEO_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EXPORT_URL)).andExpect(status().isUnauthorized());

        for (String token : List.of(tokenVendedor, tokenCliente, tokenSuper)) {
            mockMvc.perform(get(ARQUEO_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(EXPORT_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void laExportacionCsvEsUnReporteContableCompleto() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "100.00");

        vender(tokenAdminA, "Efectivo", 1, 1);
        insertarVenta(1, "MP013", "02", "70.00", hoy(10, 0), "", "",
                "Pérez, Juan");

        MvcResult descarga = mockMvc.perform(get(EXPORT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .param("periodo", "diario")
                        .param("formato", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("arqueo-diario-" + LocalDate.now() + ".csv")))
                .andReturn();

        String csv = new String(descarga.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"), "El CSV debe llevar BOM para Excel");
        assertTrue(csv.contains("CORTE DE CAJA Y ARQUEO"), csv);
        assertTrue(csv.contains("Tienda,Tienda Prueba"), csv);
        assertTrue(csv.contains("Periodo,DIARIO"), csv);
        assertTrue(csv.contains("Total vendido,170.00"), csv);
        assertTrue(csv.contains("Comprobantes,2"), csv);
        assertTrue(csv.contains("Ticket promedio,85.00"), csv);
        assertTrue(csv.contains("INGRESOS POR METODO DE PAGO"), csv);
        assertTrue(csv.contains("Efectivo,1,58.82,100.00"), csv);
        assertTrue(csv.contains("Créditos,1,41.18,70.00"), csv);
        assertTrue(csv.contains("TOTAL POR VENDEDOR"), csv);
        assertTrue(csv.contains("Sin vendedor asignado,,1,70.00"), csv);
        assertTrue(csv.contains("TOTAL POR TURNO"), csv);
        assertTrue(csv.contains("DETALLE DE COMPROBANTES"), csv);
        // RFC 4180: la coma dentro del nombre del cliente obliga a entrecomillar.
        assertTrue(csv.contains("\"Pérez, Juan\""), csv);
    }

    @Test
    void laExportacionPdfEsUnDocumentoValido() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");
        vender(tokenAdminA, "Efectivo", 1, 1);

        MvcResult descarga = mockMvc.perform(get(EXPORT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .param("periodo", "diario")
                        .param("formato", "pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("arqueo-diario-" + LocalDate.now() + ".pdf")))
                .andReturn();

        byte[] pdf = descarga.getResponse().getContentAsByteArray();
        assertTrue(pdf.length > 1000, "El PDF debe tener contenido");
        String cabecera = new String(pdf, 0, 5, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-", cabecera);
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("%%EOF"),
                "El PDF debe estar cerrado");
    }

    @Test
    void laEmisionElectronicaRegistraAlVendedor() throws Exception {
        crearProducto(1, tiendaA, "Mouse Gamer", 100, "50.00");

        MvcResult emision = mockMvc.perform(post("/api/comprobantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comprobante()))
                .andExpect(status().isCreated())
                .andReturn();
        String idVenta = objectMapper.readTree(new String(
                emision.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .get("idVenta").asText();

        assertEquals("vendedor", ventaRepository.findById(idVenta).orElseThrow()
                .getVendedorUsuario());
        assertEquals("Vendedor Prueba", ventaRepository.findById(idVenta).orElseThrow()
                .getVendedorNombre());

        JsonNode arqueo = arqueo(tokenAdminA, "diario", null);
        assertEquals("vendedor", arqueo.get("vendedores").get(0).get("usuario").asText());
        importe("50.00", arqueo.get("vendedores").get(0).get("total"));
        assertEquals("Vendedor Prueba",
                detalleDe(arqueo, idVenta).get("vendedor").asText());
    }

    @Test
    void elPeriodoYElFormatoSeValidan() throws Exception {
        mockMvc.perform(get(ARQUEO_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .param("periodo", "semanal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("diario o mensual")));

        mockMvc.perform(get(EXPORT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA)
                        .param("formato", "xlsx"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("csv o pdf")));

        // Sin parametros se asume el corte diario en CSV.
        mockMvc.perform(get(EXPORT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    // ------------------------------------------------------------------

    private JsonNode arqueo(String token, String periodo, LocalDate fecha) throws Exception {
        MockHttpServletRequestBuilder peticion = get(ARQUEO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("periodo", periodo);
        if (fecha != null) {
            peticion.param("fecha", fecha.toString());
        }
        MvcResult resultado = mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(new String(
                resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private JsonNode vender(String token, String medioPago, int idProducto, int cantidad)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipoDocumento", "Boleta");
        cuerpo.put("medioPago", medioPago);
        cuerpo.put("idCliente", CuentasTest.ID_CLIENTE);
        cuerpo.put("documentoCliente", "12345678");
        cuerpo.put("items", List.of(Map.of("idProducto", idProducto, "cantidad", cantidad)));
        MvcResult resultado = mockMvc.perform(post("/api/ventas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(new String(
                resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    // Inserta una venta historica con fecha y vendedor fijos, sin pasar por el
    // POS, para poder probar turnos y filas anteriores a la atribucion.
    private void insertarVenta(int idProducto, String idMedioPago, String formaPago, String total,
            LocalDateTime fecha, String vendedorUsuario, String vendedorNombre,
            String razonSocial) {
        String id = UUID.randomUUID().toString();
        int correlativo = ++correlativoJdbc;
        jdbcTemplate.update("""
                INSERT INTO venta (id_venta, id_cliente, id_persona, id_tipo_documento,
                    numero_documento, id_medio_pago, fecha_emision, serie, correlativo, forma_pago,
                    total_venta, receptor_tipo_documento, receptor_num_documento,
                    receptor_razon_social, vendedor_usuario, vendedor_nombre)
                VALUES (?, ?, ?, 'TD001', ?, ?, ?, 'T999', ?, ?, ?, '1', '12345678', ?, ?, ?)
                """, id, CuentasTest.ID_CLIENTE, CuentasTest.ID_PERSONA_ADMIN,
                "T999-" + String.format("%06d", correlativo), idMedioPago,
                Timestamp.valueOf(fecha), correlativo, formaPago, new BigDecimal(total),
                razonSocial, vendedorUsuario, vendedorNombre);
        jdbcTemplate.update("""
                INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, 1, ?, ?)
                """, id, idProducto, new BigDecimal(total), new BigDecimal(total));
    }

    private JsonNode detalleDe(JsonNode arqueo, String idVenta) {
        for (JsonNode comprobante : arqueo.get("detalle")) {
            if (idVenta.equals(comprobante.get("idVenta").asText())) {
                return comprobante;
            }
        }
        throw new AssertionError("El arqueo no incluye la venta " + idVenta);
    }

    private LocalDateTime hoy(int hora, int minuto) {
        return LocalDate.now().atTime(hora, minuto);
    }

    private void importe(String esperado, JsonNode valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor.decimalValue()),
                "Importe esperado " + esperado + " pero fue " + valor.decimalValue());
    }

    private void crearProducto(int id, Tienda tienda, String nombre, int stock, String precio) {
        Producto producto = new Producto(id, nombre, stock, new BigDecimal(precio), "Accesorios");
        producto.setIdTienda(tienda.getIdTienda());
        productoRepository.save(producto);
    }

    private void crearVendedorSinVentas() {
        Persona persona = new Persona();
        persona.setIdPersona("PER-PRUEBA-VEN2");
        persona.setNombre("Vendedor");
        persona.setApellido("Turno Noche");
        persona.setCorreo("vendedor2@prueba.com");
        persona.setTelefono("987654322");
        persona.setUsuario(USUARIO_VENDEDOR_2);
        personaRepository.save(persona);

        Usuario usuario = new Usuario(USUARIO_VENDEDOR_2, CuentasTest.HASH_123456,
                "PER-PRUEBA-VEN2", Rol.VENDEDOR);
        usuario.setIdTienda(tiendaA.getIdTienda());
        usuarioRepository.save(usuario);
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
}
