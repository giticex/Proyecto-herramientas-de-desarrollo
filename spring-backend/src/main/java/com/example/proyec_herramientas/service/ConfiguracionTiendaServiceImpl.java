package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.exception.RecursoNoEncontradoException;
import com.example.proyec_herramientas.exception.ValidacionSolicitudException;
import com.example.proyec_herramientas.model.ConfiguracionTiendaDTO;
import com.example.proyec_herramientas.persistence.AuditoriaEvento;
import com.example.proyec_herramientas.persistence.Tienda;
import com.example.proyec_herramientas.repository.TiendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Datos comerciales y de facturacion de una tienda. Las series se guardan en
 * la propia tienda y son las que usan tanto el punto de venta como la emision
 * de comprobantes electronicos.
 */
@Service
public class ConfiguracionTiendaServiceImpl implements ConfiguracionTiendaService {

    // RUC peruano: 11 digitos y prefijo de persona juridica (10, 15, 17, 20).
    private static final Pattern REGEX_RUC = Pattern.compile("^(10|15|17|20)\\d{9}$");
    // SUNAT exige que la serie de boleta empiece con B y la de factura con F.
    private static final Pattern REGEX_SERIE_BOLETA = Pattern.compile("^B[A-Z0-9]{3}$");
    private static final Pattern REGEX_SERIE_FACTURA = Pattern.compile("^F[A-Z0-9]{3}$");
    private static final Pattern REGEX_TELEFONO = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");
    private static final Pattern REGEX_LOGO = Pattern.compile("^https?://\\S+$");

    private final TiendaRepository tiendaRepository;
    private final AuditoriaService auditoriaService;

    public ConfiguracionTiendaServiceImpl(TiendaRepository tiendaRepository,
                                          AuditoriaService auditoriaService) {
        this.tiendaRepository = tiendaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionTiendaDTO obtener(int idTienda) {
        return aDTO(buscar(idTienda));
    }

    @Override
    @Transactional
    public ConfiguracionTiendaDTO actualizar(int idTienda, ConfiguracionTiendaDTO configuracion) {
        if (configuracion == null) {
            throw new ValidacionSolicitudException("No se recibió la configuración de la tienda.");
        }
        Tienda tienda = buscar(idTienda);

        String razonSocial = obligatorio(configuracion.getRazonSocial(),
                "La razón social es obligatoria.", 150);
        String ruc = obligatorio(configuracion.getRuc(), "El RUC es obligatorio.", 11);
        if (!REGEX_RUC.matcher(ruc).matches()) {
            throw new ValidacionSolicitudException(
                    "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20.");
        }
        String direccion = obligatorio(configuracion.getDireccionFiscal(),
                "La dirección fiscal es obligatoria.", 200);
        String telefono = opcional(configuracion.getTelefono(), 30);
        if (telefono != null && !REGEX_TELEFONO.matcher(telefono).matches()) {
            throw new ValidacionSolicitudException(
                    "El teléfono solo admite números, espacios, +, guiones y paréntesis.");
        }
        String logo = opcional(configuracion.getLogoUrl(), 255);
        if (logo != null && !REGEX_LOGO.matcher(logo).matches()) {
            throw new ValidacionSolicitudException(
                    "El logotipo debe ser una URL que empiece con http:// o https://.");
        }
        String serieBoleta = serie(configuracion.getSerieBoleta(), REGEX_SERIE_BOLETA,
                "La serie de boletas debe tener 4 caracteres y empezar con B (por ejemplo B001).");
        String serieFactura = serie(configuracion.getSerieFactura(), REGEX_SERIE_FACTURA,
                "La serie de facturas debe tener 4 caracteres y empezar con F (por ejemplo F001).");

        tienda.setRazonSocial(razonSocial);
        tienda.setRuc(ruc);
        tienda.setDireccionFiscal(direccion);
        tienda.setTelefono(telefono);
        tienda.setLogoUrl(logo);
        tienda.setSerieBoleta(serieBoleta);
        tienda.setSerieFactura(serieFactura);
        Tienda guardada = tiendaRepository.save(tienda);

        auditoriaService.registrar(AuditoriaEvento.TIPO_CONFIGURACION_ACTUALIZADA,
                AuditoriaEvento.SEVERIDAD_INFO,
                "Configuración comercial actualizada (RUC " + ruc + ", series "
                        + serieBoleta + "/" + serieFactura + ").",
                guardada.getIdTienda(), guardada.getNombre());

        return aDTO(guardada);
    }

    private Tienda buscar(int idTienda) {
        return tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tienda " + idTienda + " no existe"));
    }

    private String obligatorio(String valor, String mensaje, int maximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new ValidacionSolicitudException(mensaje);
        }
        if (texto.length() > maximo) {
            throw new ValidacionSolicitudException(
                    "El valor no puede superar los " + maximo + " caracteres.");
        }
        return texto;
    }

    private String opcional(String valor, int maximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() > maximo) {
            throw new ValidacionSolicitudException(
                    "El valor no puede superar los " + maximo + " caracteres.");
        }
        return texto;
    }

    private String serie(String valor, Pattern patron, String mensaje) {
        String texto = valor == null ? "" : valor.trim().toUpperCase();
        if (texto.isEmpty()) {
            throw new ValidacionSolicitudException(mensaje);
        }
        if (!patron.matcher(texto).matches()) {
            throw new ValidacionSolicitudException(mensaje);
        }
        return texto;
    }

    private ConfiguracionTiendaDTO aDTO(Tienda tienda) {
        ConfiguracionTiendaDTO dto = new ConfiguracionTiendaDTO();
        dto.setNombre(tienda.getNombre());
        dto.setSlug(tienda.getSlug());
        dto.setRazonSocial(valor(tienda.getRazonSocial()));
        dto.setRuc(valor(tienda.getRuc()));
        dto.setDireccionFiscal(valor(tienda.getDireccionFiscal()));
        dto.setTelefono(valor(tienda.getTelefono()));
        dto.setLogoUrl(valor(tienda.getLogoUrl()));
        dto.setSerieBoleta(valor(tienda.getSerieBoleta()));
        dto.setSerieFactura(valor(tienda.getSerieFactura()));
        return dto;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
