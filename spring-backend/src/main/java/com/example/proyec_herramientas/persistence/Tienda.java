package com.example.proyec_herramientas.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tienda")
public class Tienda {

    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String ESTADO_SUSPENDIDA = "SUSPENDIDA";

    public static final String PLAN_BASICO = "BASICO";
    public static final String PLAN_PRO = "PRO";
    public static final String PLAN_ENTERPRISE = "ENTERPRISE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tienda")
    private Integer idTienda;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "rubro", length = 80, nullable = false)
    private String rubro;

    @Column(name = "slug", length = 80, nullable = false, unique = true)
    private String slug;

    @Column(name = "color_primario", length = 7, nullable = false)
    private String colorPrimario = "#0071e3";

    @Column(name = "color_secundario", length = 7, nullable = false)
    private String colorSecundario = "#1d1d1f";

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    // Estado administrativo que controla el SUPER_ADMIN. Los DEFAULT aseguran
    // que ddl-auto=update pueda agregar las columnas a una tienda ya poblada
    // sin dejar filas en NULL.
    @Column(name = "estado_acceso", length = 15, nullable = false,
            columnDefinition = "VARCHAR(15) NOT NULL DEFAULT 'ACTIVA'")
    private String estadoAcceso = ESTADO_ACTIVA;

    @Column(name = "plan", length = 15, nullable = false,
            columnDefinition = "VARCHAR(15) NOT NULL DEFAULT 'BASICO'")
    private String plan = PLAN_BASICO;

    @Column(name = "limite_productos", nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 100")
    private Integer limiteProductos = 100;

    @Column(name = "limite_usuarios", nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 5")
    private Integer limiteUsuarios = 5;

    public Tienda() {
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(Integer idTienda) {
        this.idTienda = idTienda;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getEstadoAcceso() {
        return estadoAcceso;
    }

    public void setEstadoAcceso(String estadoAcceso) {
        this.estadoAcceso = estadoAcceso;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Integer getLimiteProductos() {
        return limiteProductos;
    }

    public void setLimiteProductos(Integer limiteProductos) {
        this.limiteProductos = limiteProductos;
    }

    public Integer getLimiteUsuarios() {
        return limiteUsuarios;
    }

    public void setLimiteUsuarios(Integer limiteUsuarios) {
        this.limiteUsuarios = limiteUsuarios;
    }

    public boolean estaSuspendida() {
        return ESTADO_SUSPENDIDA.equalsIgnoreCase(estadoAcceso);
    }
}
