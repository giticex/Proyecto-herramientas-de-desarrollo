package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.model.ClienteDTO;
import com.example.proyec_herramientas.model.PerfilClienteDTO;
import com.example.proyec_herramientas.persistence.Cliente;
import com.example.proyec_herramientas.persistence.Venta;
import com.example.proyec_herramientas.repository.ClienteRepository;
import com.example.proyec_herramientas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository repository;
    private final VentaRepository ventaRepository;

    public ClienteServiceImpl(ClienteRepository repository, VentaRepository ventaRepository) {
        this.repository = repository;
        this.ventaRepository = ventaRepository;
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
}
