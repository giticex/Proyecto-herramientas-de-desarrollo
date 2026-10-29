package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoDuplicadoException;
import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.persistence.DetalleVenta;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.ProductoRepository;
import com.example.proyec_herramientas.repository.TiendaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

// Cada operacion de administracion esta acotada a la tienda del ADMIN que
// llama: el idTienda sale del token (ver ProductoController), nunca del body.
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repository;
    private final TiendaRepository tiendaRepository;

    public ProductoServiceImpl(ProductoRepository repository, TiendaRepository tiendaRepository) {
        this.repository = repository;
        this.tiendaRepository = tiendaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> listar(int idTienda) {
        return repository.findByIdTienda(idTienda).stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> filtrarPorNombreOCodigo(int idTienda, String filtro) {
        String texto = filtro == null ? "" : filtro.trim().toLowerCase();
        return repository.findByIdTienda(idTienda).stream()
                .filter(p -> texto.isEmpty()
                        || (p.getNombre() != null && p.getNombre().toLowerCase().contains(texto))
                        || (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(texto)))
                .sorted((a, b) -> {
                    String na = a.getNombre() == null ? "" : a.getNombre();
                    String nb = b.getNombre() == null ? "" : b.getNombre();
                    return na.compareToIgnoreCase(nb);
                })
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(int idProducto, int idTienda) {
        Producto producto = repository.findById(idProducto).orElse(null);
        return esDeLaTienda(producto, idTienda) ? toDTO(producto) : null;
    }

    @Override
    @Transactional
    public ProductoDTO crear(ProductoDTO producto, int idTienda) {
        int id = producto.getIdProducto();
        if (id > 0 && repository.existsById(id)) {
            throw new RecursoDuplicadoException("Ya existe un producto con id " + id);
        }
        int tiendaDestino = idTienda == 0 ? 1 : idTienda;
        validarLimitePlan(tiendaDestino);
        if (id <= 0) {
            id = siguienteId();
        }
        double precio = producto.getPrecio();
        Producto entity = new Producto(
                id,
                producto.getNombre(),
                producto.getStock(),
                BigDecimal.valueOf(precio),
                producto.getCategoria());
        entity.setAtributos(producto.getAtributos());
        String codigo = producto.getCodigo();
        if (codigo == null || codigo.trim().isEmpty()) {
            codigo = generarCodigoUnico();
        } else {
            codigo = codigo.trim();
            if (repository.findByCodigo(codigo).isPresent()) {
                codigo = generarCodigoUnico();
            }
        }
        entity.setCodigo(codigo);
        entity.setUnidadMedida(producto.getUnidadMedida() == null ? Producto.UNIDAD_NIU : producto.getUnidadMedida());
        entity.setAfectacionIgv(producto.getAfectacionIgv() == null ? DetalleVenta.AFECTACION_GRAVADA : producto.getAfectacionIgv());
        entity.setIdTienda(tiendaDestino);
        return toDTO(repository.save(entity));
    }

    // Los limites del plan contratado se administran desde la consola global.
    // Solo bloquean altas: actualizar o eliminar productos existentes sigue
    // disponible para que la tienda pueda operar aunque este en el tope.
    private void validarLimitePlan(int idTienda) {
        Tienda tienda = tiendaRepository.findById(idTienda).orElse(null);
        if (tienda == null || tienda.getLimiteProductos() == null) {
            return;
        }
        long actuales = repository.countByIdTienda(idTienda);
        if (actuales >= tienda.getLimiteProductos()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La tienda alcanzó el límite de " + tienda.getLimiteProductos()
                            + " productos de su plan. Solicita ampliar el límite desde la consola de plataforma.");
        }
    }

    @Override
    @Transactional
    public ProductoDTO actualizar(int idProducto, ProductoDTO producto, int idTienda) {
        Producto entity = repository.findById(idProducto).orElse(null);
        if (!esDeLaTienda(entity, idTienda)) {
            return null;
        }
        entity.setNombre(producto.getNombre());
        entity.setStock(producto.getStock());
        double precio = producto.getPrecio();
        entity.setPrecio(BigDecimal.valueOf(precio));
        entity.setCategoria(producto.getCategoria());
        entity.setAtributos(producto.getAtributos());
        String codigo = producto.getCodigo();
        if (codigo != null && !codigo.trim().isEmpty()) {
            codigo = codigo.trim();
            var existing = repository.findByCodigo(codigo);
            if (existing.isPresent() && !existing.get().getIdProducto().equals(entity.getIdProducto())) {
                codigo = generarCodigoUnico();
            }
            entity.setCodigo(codigo);
        }
        // Si el formulario no envia los campos fiscales se conservan los que ya
        // tenia el producto: no se puede perder la afectacion del IGV al editar
        // solo el precio o el stock.
        if (producto.getUnidadMedida() != null) {
            entity.setUnidadMedida(producto.getUnidadMedida());
        }
        if (producto.getAfectacionIgv() != null) {
            entity.setAfectacionIgv(producto.getAfectacionIgv());
        }
        if (entity.getIdTienda() == null) {
            entity.setIdTienda(idTienda);
        }
        return toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void eliminar(int idProducto, int idTienda) {
        Producto entity = repository.findById(idProducto).orElse(null);
        if (esDeLaTienda(entity, idTienda)) {
            repository.delete(entity);
        }
    }

    private boolean esDeLaTienda(Producto producto, int idTienda) {
        return producto != null && producto.getIdTienda() != null
                && producto.getIdTienda() == idTienda;
    }

    private int siguienteId() {
        return repository.findAll().stream()
                .mapToInt(Producto::getIdProducto)
                .max()
                .orElse(0) + 1;
    }

    private String generarCodigoUnico() {
        String codigo;
        int intentos = 0;
        do {
            intentos++;
            long timestamp = System.currentTimeMillis() % 1000000;
            codigo = "PROD-" + timestamp + "-" + intentos;
        } while (repository.findByCodigo(codigo).isPresent() && intentos < 100);
        return codigo;
    }

    private ProductoDTO toDTO(Producto producto) {
        ProductoDTO dto = new ProductoDTO();
        dto.setIdProducto(producto.getIdProducto());
        dto.setNombre(producto.getNombre());
        dto.setStock(producto.getStock());
        dto.setPrecio(producto.getPrecio().doubleValue());
        dto.setCategoria(producto.getCategoria());
        dto.setCodigo(producto.getCodigo());
        dto.setUnidadMedida(producto.getUnidadMedida());
        dto.setAfectacionIgv(producto.getAfectacionIgv());
        dto.setAtributos(producto.getAtributos());
        dto.setIdTienda(producto.getIdTienda());
        return dto;
    }
}