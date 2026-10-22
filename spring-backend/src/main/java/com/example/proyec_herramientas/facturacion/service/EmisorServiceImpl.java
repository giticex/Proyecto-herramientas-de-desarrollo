package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;
import com.example.proyec_herramientas.persistence.Parametro;
import com.example.proyec_herramientas.repository.ParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class EmisorServiceImpl implements EmisorService {

    private static final String RUC_EMISOR = "RUC_EMISOR";
    private static final String RAZON_SOCIAL_EMISOR = "RAZON_SOCIAL_EMISOR";
    private static final String NOMBRE_COMERCIAL = "NOMBRE_COMERCIAL";
    private static final String DOMICILIO_FISCAL_EMISOR = "DOMICILIO_FISCAL_EMISOR";
    private static final String UBIGEO_EMISOR = "UBIGEO_EMISOR";
    private static final String DISTRITO_EMISOR = "DISTRITO_EMISOR";
    private static final String PROVINCIA_EMISOR = "PROVINCIA_EMISOR";
    private static final String DEPARTAMENTO_EMISOR = "DEPARTAMENTO_EMISOR";
    private static final String PAIS_EMISOR = "PAIS_EMISOR";
    private static final String EMAIL_EMISOR = "EMAIL_EMISOR";
    private static final String TELEFONO_EMISOR = "TELEFONO_EMISOR";
    private static final String IGV_PORCENTAJE = "IGV_PORCENTAJE";
    private static final String MONEDA = "MONEDA";
    private static final String SERIE_BOLETA = "SERIE_BOLETA";
    private static final String SERIE_FACTURA = "SERIE_FACTURA";
    private static final String LIMITE_BOLETA_SIN_RUC = "LIMITE_BOLETA_SIN_RUC";
    private static final String URL_REPRESENTACION_IMPRESA = "URL_REPRESENTACION_IMPRESA";
    private static final String CLIENTE_MOSTRADOR = "CLIENTE_MOSTRADOR";
    private static final String PERSONA_MOSTRADOR = "PERSONA_MOSTRADOR";

    private final ParametroRepository parametroRepository;

    public EmisorServiceImpl(ParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public EmisorDTO obtenerEmisor() {
        EmisorDTO emisor = new EmisorDTO();
        emisor.setRuc(valor(RUC_EMISOR, "20604567891"));
        emisor.setRazonSocial(valor(RAZON_SOCIAL_EMISOR, "INNOVATION STORE S.A.C."));
        emisor.setNombreComercial(valor(NOMBRE_COMERCIAL, "TechZone"));
        emisor.setDomicilioFiscal(valor(DOMICILIO_FISCAL_EMISOR, "Av. Javier Prado Este 4200, Of. 802"));
        emisor.setUbigeo(valor(UBIGEO_EMISOR, "150131"));
        emisor.setDistrito(valor(DISTRITO_EMISOR, "Santiago de Surco"));
        emisor.setProvincia(valor(PROVINCIA_EMISOR, "Lima"));
        emisor.setDepartamento(valor(DEPARTAMENTO_EMISOR, "Lima"));
        emisor.setPais(valor(PAIS_EMISOR, "PE"));
        emisor.setEmail(valor(EMAIL_EMISOR, "facturacion@techzone.pe"));
        emisor.setTelefono(valor(TELEFONO_EMISOR, "5115551234"));
        return emisor;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerIgvPorcentaje() {
        try {
            return new BigDecimal(valor(IGV_PORCENTAJE, "18.00"));
        } catch (NumberFormatException e) {
            return new BigDecimal("18.00");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerMoneda() {
        return valor(MONEDA, "PEN");
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerSerie(String tipoComprobante) {
        if (ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoComprobante)) {
            return valor(SERIE_FACTURA, "F001");
        }
        return valor(SERIE_BOLETA, "B001");
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerIdTipoDocumento(String tipoComprobante) {
        return ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoComprobante) ? "TD002" : "TD001";
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerLimiteBoletaSinRuc() {
        try {
            return new BigDecimal(valor(LIMITE_BOLETA_SIN_RUC, "700.00"));
        } catch (NumberFormatException e) {
            return new BigDecimal("700.00");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerUrlRepresentacionImpresa() {
        return valor(URL_REPRESENTACION_IMPRESA, "https://www.techzone.pe/comprobantes");
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerClienteMostrador() {
        return valor(CLIENTE_MOSTRADOR, "CLI001");
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerPersonaMostrador() {
        return valor(PERSONA_MOSTRADOR, "PER001");
    }

    @Override
    public String obtenerDescripcionTipoComprobante(String tipoComprobante) {
        return ComprobanteRequestDTO.TIPO_FACTURA.equals(tipoComprobante) ? "FACTURA" : "BOLETA";
    }

    private String valor(String id, String porDefecto) {
        return parametroRepository.findByIdParametro(id)
                .map(Parametro::getValor)
                .filter(valor -> !valor.isBlank())
                .orElse(porDefecto);
    }
}
