package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.model.ProductoDTO;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repository;

    public ProductoServiceImpl(ProductoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> listar() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(int idProducto) {
        Producto producto = repository.findById(idProducto).orElse(null);
        return producto != null ? toDTO(producto) : null;
    }

    @Override
    @Transactional
    public ProductoDTO crear(ProductoDTO producto) {
        Integer id = producto.getIdProducto();
        if (id == null || id <= 0) {
            id = siguienteId();
        }
        Producto entity = new Producto(
                id,
                producto.getNombre(),
                producto.getStock(),
                BigDecimal.valueOf(producto.getPrecio()),
                producto.getCategoria());
        entity.setAtributos(producto.getAtributos());
        entity.setCodigo(producto.getCodigo());
        entity.setUnidadMedida(producto.getUnidadMedida());
        entity.setAfectacionIgv(producto.getAfectacionIgv());
        return toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public ProductoDTO actualizar(int idProducto, ProductoDTO producto) {
        Producto entity = repository.findById(idProducto).orElse(null);
        if (entity == null) {
            return null;
        }
        entity.setNombre(producto.getNombre());
        entity.setStock(producto.getStock());
        entity.setPrecio(BigDecimal.valueOf(producto.getPrecio()));
        entity.setCategoria(producto.getCategoria());
        entity.setAtributos(producto.getAtributos());
        entity.setCodigo(producto.getCodigo());
        entity.setUnidadMedida(producto.getUnidadMedida());
        entity.setAfectacionIgv(producto.getAfectacionIgv());
        return toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void eliminar(int idProducto) {
        repository.deleteById(idProducto);
    }

    @Override
    @Transactional
    public boolean actualizarStock(int idProducto, int cantidad) {
        Producto entity = repository.findById(idProducto).orElse(null);
        if (entity == null) {
            return false;
        }
        entity.setStock(entity.getStock() - cantidad);
        repository.save(entity);
        return true;
    }

    private int siguienteId() {
        return repository.findAll().stream()
                .mapToInt(Producto::getIdProducto)
                .max()
                .orElse(0) + 1;
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
        return dto;
    }
}
