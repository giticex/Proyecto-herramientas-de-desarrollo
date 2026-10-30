package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.ValidacionSolicitudException;
import com.example.proyec_herramientas.model.ClienteBusquedaDTO;
import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.model.PerfilClienteDTO;
import com.example.proyec_herramientas.model.RegistroClienteRapidoRequest;
import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Persona;
import com.example.proyec_herramientas.persistence.Rol;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.PersonaRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClienteServiceImpl implements ClienteService {

    private static final String TIPO_DOCUMENTO_DNI = "1";
    private static final String TIPO_DOCUMENTO_RUC = "6";
    private static final String NOMBRE_DNI = "DNI";
    private static final String NOMBRE_RUC = "RUC";
    private static final String REGEX_DNI = "\\d{8}";
    private static final String REGEX_RUC = "\\d{11}";
    private static final int MAX_NOMBRE = 50;
    private static final int MAX_DIRECCION = 200;
    private static final int MAX_TELEFONO = 15;

    private final ClienteRepository repository;
    private final VentaRepository ventaRepository;
    private final PersonaRepository personaRepository;
    private final ContrasenaService contrasena;

    public ClienteServiceImpl(ClienteRepository repository, VentaRepository ventaRepository,
            PersonaRepository personaRepository, ContrasenaService contrasena) {
        this.repository = repository;
        this.ventaRepository = ventaRepository;
        this.personaRepository = personaRepository;
        this.contrasena = contrasena;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDTO> listar() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDTO obtenerPorId(String idCliente) {
        Cliente cliente = repository.findById(idCliente).orElse(null);
        return cliente != null ? toDTO(cliente) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteBusquedaDTO buscarPorDocumento(int idTienda, String documento) {
        String numero = normalizarDocumento(documento);
        String tipo = tipoDocumentoDe(numero);

        Cliente ficha = repository.findFirstByNumeroDocumentoAndIdTienda(numero, idTienda).orElse(null);
        if (ficha != null) {
            ClienteBusquedaDTO dto = deFicha(ficha);
            dto.setOrigen(ClienteBusquedaDTO.ORIGEN_TIENDA);
            return dto;
        }

        // La venta congela el receptor: sirve para autocompletar a quien ya
        // compro en la tienda aunque no tenga ficha en la tabla cliente.
        List<Venta> comprobantes = ventaRepository.ventasPorDocumentoReceptor(
                numero, idTienda, PageRequest.of(0, 1));
        if (!comprobantes.isEmpty()) {
            Venta venta = comprobantes.get(0);
            ClienteBusquedaDTO dto = new ClienteBusquedaDTO();
            dto.setEncontrado(true);
            dto.setTipoDocumento(tipo);
            dto.setNumeroDocumento(numero);
            dto.setNombre(venta.getReceptorRazonSocial());
            dto.setDireccion(venta.getReceptorDireccion());
            dto.setCorreo(venta.getEmailContacto());
            dto.setOrigen(ClienteBusquedaDTO.ORIGEN_HISTORIAL);
            return dto;
        }

        return ClienteBusquedaDTO.noEncontrado(tipo, numero);
    }

    @Override
    @Transactional
    public ClienteBusquedaDTO registrarRapido(int idTienda, RegistroClienteRapidoRequest request) {
        String numero = normalizarDocumento(request.getDocumento());
        String tipo = tipoDocumentoDe(numero);
        String nombre = texto(request.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionSolicitudException("Indica el nombre o la razon social del cliente.");
        }
        if (nombre.length() > MAX_NOMBRE) {
            throw new ValidacionSolicitudException(
                    "El nombre o razon social no puede superar " + MAX_NOMBRE + " caracteres.");
        }
        String direccion = limitar(request.getDireccion(), MAX_DIRECCION,
                "La direccion no puede superar " + MAX_DIRECCION + " caracteres.");
        String telefono = limitar(request.getTelefono(), MAX_TELEFONO,
                "El telefono no puede superar " + MAX_TELEFONO + " caracteres.");

        if (repository.findFirstByNumeroDocumentoAndIdTienda(numero, idTienda).isPresent()) {
            throw new RecursoDuplicadoException(
                    "Ya existe un cliente con el documento " + numero + " en esta tienda.");
        }

        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String correo = "cli-" + sufijo.toLowerCase() + "@mostrador.local";
        Cliente cliente = new Cliente("CLI-" + sufijo, nombre, "", telefono, correo);
        // La ficha no es una cuenta de acceso: la clave es aleatoria y no se
        // puede iniciar sesion con ella desde el portal.
        cliente.setContrasena(contrasena.hashBcrypt(UUID.randomUUID().toString()));
        cliente.setIdRol(Rol.CLIENTE);
        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(numero);
        cliente.setDireccion(direccion);
        cliente.setIdTienda(idTienda);
        cliente = repository.save(cliente);

        // Registrar la venta busca la persona por el correo del cliente; sin
        // esta fila el POS fallaria al cobrar una ficha creada aqui.
        Persona persona = new Persona();
        persona.setIdPersona("PER-" + sufijo);
        persona.setNombre(nombre);
        persona.setApellido("");
        persona.setCorreo(correo);
        persona.setTelefono(telefono.isEmpty() ? null : telefono);
        personaRepository.save(persona);

        ClienteBusquedaDTO dto = deFicha(cliente);
        dto.setOrigen(ClienteBusquedaDTO.ORIGEN_REGISTRADO);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilClienteDTO obtenerPerfil(String idCliente) {
        Cliente cliente = repository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cliente " + idCliente + " no encontrado"));
        PerfilClienteDTO dto = new PerfilClienteDTO();
        dto.setIdCliente(cliente.getIdCliente());
        dto.setNombre(cliente.getNombre());
        dto.setApellido(cliente.getApellido());
        dto.setCorreo(cliente.getCorreo());
        dto.setTelefono(cliente.getTelefono());
        dto.setDirecciones(direccionesDe(idCliente));
        return dto;
    }

    // El cliente no tiene una tabla de direcciones: se reutilizan las que
    // quedaron registradas en sus comprobantes, sin duplicados.
    private List<PerfilClienteDTO.DireccionDTO> direccionesDe(String idCliente) {
        Map<String, PerfilClienteDTO.DireccionDTO> unicas = new LinkedHashMap<>();
        for (Venta venta : ventaRepository.findByIdClienteOrderByFechaEmisionDesc(idCliente)) {
            registrarDireccion(unicas, "Dirección de facturación", venta.getReceptorDireccion());
            registrarDireccion(unicas, "Dirección de entrega", venta.getEntregaDireccion());
        }
        return new ArrayList<>(unicas.values());
    }

    private void registrarDireccion(Map<String, PerfilClienteDTO.DireccionDTO> unicas,
            String tipo, String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return;
        }
        String limpia = direccion.trim();
        unicas.putIfAbsent(tipo + "|" + limpia, new PerfilClienteDTO.DireccionDTO(tipo, limpia));
    }

    private ClienteDTO toDTO(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setIdCliente(cliente.getIdCliente());
        dto.setNombre(cliente.getNombre());
        dto.setApellido(cliente.getApellido());
        dto.setTelefono(cliente.getTelefono());
        dto.setCorreo(cliente.getCorreo());
        return dto;
    }

    private ClienteBusquedaDTO deFicha(Cliente cliente) {
        ClienteBusquedaDTO dto = new ClienteBusquedaDTO();
        dto.setEncontrado(true);
        dto.setIdCliente(cliente.getIdCliente());
        dto.setTipoDocumento(TIPO_DOCUMENTO_RUC.equals(cliente.getTipoDocumento())
                ? NOMBRE_RUC : NOMBRE_DNI);
        dto.setNumeroDocumento(cliente.getNumeroDocumento());
        dto.setNombre((cliente.getNombre() + " " + cliente.getApellido()).trim());
        dto.setDireccion(cliente.getDireccion());
        dto.setTelefono(cliente.getTelefono());
        dto.setCorreo(cliente.getCorreo());
        return dto;
    }

    private String normalizarDocumento(String documento) {
        String numero = documento == null ? "" : documento.replaceAll("\\D", "");
        if (numero.matches(REGEX_DNI) || numero.matches(REGEX_RUC)) {
            return numero;
        }
        throw new ValidacionSolicitudException(
                "El documento debe tener 8 digitos (DNI) u 11 digitos (RUC).");
    }

    private String tipoDocumentoDe(String numero) {
        return numero.matches(REGEX_RUC) ? TIPO_DOCUMENTO_RUC : TIPO_DOCUMENTO_DNI;
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String limitar(String valor, int maximo, String mensaje) {
        String limpio = texto(valor);
        if (limpio.length() > maximo) {
            throw new ValidacionSolicitudException(mensaje);
        }
        return limpio;
    }
}
