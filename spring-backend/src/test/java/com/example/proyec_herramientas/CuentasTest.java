package com.example.proyec_herramientas;

import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Usuario;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cuentas y tokens que usan las pruebas. Las cuentas se crean aqui y no en
// datos-pruebas.sql para que cada test sea independiente del orden en que se
// ejecuten: si una prueba borra personas o clientes, la cuenta se vuelve a
// crear en su propio @BeforeEach.
final class CuentasTest {

    // SHA-256 de "123456", el mismo hash que usa la carga inicial.
    static final String HASH_123456 =
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    static final String CLAVE = "123456";

    static final String USUARIO_ADMIN = "admin";
    static final String CORREO_ADMIN = "admin@prueba.com";
    static final String USUARIO_CLIENTE = "comprador";
    static final String CORREO_CLIENTE = "comprador@prueba.com";
    static final String ID_CLIENTE = "CLI-PRUEBA";
    static final String ID_PERSONA_ADMIN = "PER-PRUEBA-ADM";
    static final String ID_PERSONA_CLIENTE = "PER-PRUEBA-CLI";

    private CuentasTest() {
    }

    static Usuario crearAdmin(UsuarioRepository usuarios, PersonaRepository personas) {
        Persona persona = persona(personas, ID_PERSONA_ADMIN, "Administrador", "Prueba", CORREO_ADMIN,
                USUARIO_ADMIN);
        Usuario usuario = usuarios.findByUsuario(USUARIO_ADMIN).orElseGet(() -> new Usuario());
        usuario.setUsuario(USUARIO_ADMIN);
        usuario.setContrasena(HASH_123456);
        usuario.setIdPersona(persona.getIdPersona());
        usuario.setIdRol(Rol.ADMIN);
        return usuarios.save(usuario);
    }

    static Usuario crearCliente(UsuarioRepository usuarios, PersonaRepository personas) {
        Persona persona = persona(personas, ID_PERSONA_CLIENTE, "Comprador", "Prueba", CORREO_CLIENTE,
                USUARIO_CLIENTE);
        Usuario usuario = usuarios.findByUsuario(USUARIO_CLIENTE).orElseGet(() -> new Usuario());
        usuario.setUsuario(USUARIO_CLIENTE);
        usuario.setContrasena(HASH_123456);
        usuario.setIdPersona(persona.getIdPersona());
        usuario.setIdRol(Rol.CLIENTE);
        return usuarios.save(usuario);
    }

    static Cliente crearFichaCliente(ClienteRepository clientes) {
        Cliente cliente = clientes.findById(ID_CLIENTE).orElseGet(() -> new Cliente());
        cliente.setIdCliente(ID_CLIENTE);
        cliente.setNombre("Comprador");
        cliente.setApellido("Prueba");
        cliente.setTelefono("987654321");
        cliente.setCorreo(CORREO_CLIENTE);
        cliente.setContrasena(HASH_123456);
        cliente.setIdRol(Rol.CLIENTE);
        return clientes.save(cliente);
    }

    private static Persona persona(PersonaRepository personas, String id, String nombre, String apellido,
            String correo, String usuario) {
        Persona persona = personas.findById(id).orElseGet(() -> new Persona());
        persona.setIdPersona(id);
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setCorreo(correo);
        persona.setTelefono("987654321");
        persona.setUsuario(usuario);
        return personas.save(persona);
    }

    // Hace login y devuelve el token listo para mandarlo como "Bearer ...".
    static String token(MockMvc mockMvc, ObjectMapper objectMapper, String usuario, String clave)
            throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of("usuario", usuario, "clave", clave));
        String respuesta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(respuesta);
        return json.get("token").asText();
    }
}
