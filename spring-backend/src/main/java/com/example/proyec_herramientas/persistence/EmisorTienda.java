package com.example.proyec_herramientas.persistence;

import com.example.proyec_herramientas.facturacion.model.ConfiguracionEmisor;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "emisor_tienda")
public class EmisorTienda {
    @Id
    @Column(name = "id_tienda")
    private Integer idTienda;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos", columnDefinition = "json", nullable = false)
    private EmisorDTO datos;

    @Column(name = "serie_boleta", length = 4, nullable = false)
    private String serieBoleta;

    @Column(name = "serie_factura", length = 4, nullable = false)
    private String serieFactura;

    protected EmisorTienda() {
    }

    public EmisorTienda(int idTienda, ConfiguracionEmisor configuracion) {
        this.idTienda = idTienda;
        actualizar(configuracion);
    }

    public void actualizar(ConfiguracionEmisor configuracion) {
        datos = configuracion.emisor();
        serieBoleta = configuracion.serieBoleta();
        serieFactura = configuracion.serieFactura();
    }

    public ConfiguracionEmisor configuracion() {
        return new ConfiguracionEmisor(datos, serieBoleta, serieFactura);
    }
}
