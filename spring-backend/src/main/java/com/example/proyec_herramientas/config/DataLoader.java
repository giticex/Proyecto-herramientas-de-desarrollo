package com.example.proyec_herramientas.config;

import com.example.proyec_herramientas.persistence.DetalleVenta;
import com.example.proyec_herramientas.persistence.Producto;
import com.example.proyec_herramientas.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

// Catalogo de ejemplo para demos locales: solo se carga si APP_DEMO_DATA=true,
// para no sembrar productos en bases reales.
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DataLoader {

    @Bean
    public CommandLineRunner cargarProductos(ProductoRepository productoRepository) {
        return args -> {
            if (productoRepository.count() > 0) {
                System.out.println("Productos ya existentes en BD, no se insertan datos de prueba.");
                return;
            }

            int idTienda = 1;

            Producto p1 = new Producto(101, "Pulsera de cuero trenzada", 50, new BigDecimal("15.00"), "Bisutería");
            p1.setCodigo("PUL-001");
            p1.setUnidadMedida(Producto.UNIDAD_NIU);
            p1.setAfectacionIgv(DetalleVenta.AFECTACION_GRAVADA);
            p1.setAtributos(new HashMap<>());
            p1.setIdTienda(idTienda);

            Producto p2 = new Producto(102, "Lanyard USB personalizado", 80, new BigDecimal("9.90"), "Tecnología");
            p2.setCodigo("LYN-002");
            p2.setUnidadMedida(Producto.UNIDAD_NIU);
            p2.setAfectacionIgv(DetalleVenta.AFECTACION_GRAVADA);
            p2.setAtributos(new HashMap<>());
            p2.setIdTienda(idTienda);

            Producto p3 = new Producto(103, "Auriculares inalámbricos TWS", 35, new BigDecimal("149.90"), "Tecnología");
            p3.setCodigo("AUD-003");
            p3.setUnidadMedida(Producto.UNIDAD_NIU);
            p3.setAfectacionIgv(DetalleVenta.AFECTACION_GRAVADA);
            p3.setAtributos(new HashMap<>());
            p3.setIdTienda(idTienda);

            Producto p4 = new Producto(104, "Pulsera de acero inoxidable", 60, new BigDecimal("25.50"), "Bisutería");
            p4.setCodigo("PUL-004");
            p4.setUnidadMedida(Producto.UNIDAD_NIU);
            p4.setAfectacionIgv(DetalleVenta.AFECTACION_GRAVADA);
            p4.setAtributos(new HashMap<>());
            p4.setIdTienda(idTienda);

            Producto p5 = new Producto(105, "Cargador USB-C 20W", 90, new BigDecimal("39.90"), "Tecnología");
            p5.setCodigo("CHG-005");
            p5.setUnidadMedida(Producto.UNIDAD_NIU);
            p5.setAfectacionIgv(DetalleVenta.AFECTACION_GRAVADA);
            p5.setAtributos(new HashMap<>());
            p5.setIdTienda(idTienda);

            productoRepository.saveAll(List.of(p1, p2, p3, p4, p5));
            System.out.println("Productos de prueba insertados correctamente (idTienda=" + idTienda + ")");
        };
    }
}
