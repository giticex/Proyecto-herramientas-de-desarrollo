package com.example.proyec_herramientas.facturacion.service;

import com.example.proyec_herramientas.facturacion.model.ComprobanteRequestDTO;
import com.example.proyec_herramientas.facturacion.model.ComprobanteRespuestaDTO;
import com.example.proyec_herramientas.facturacion.model.DetalleComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.DomicilioDTO;
import com.example.proyec_herramientas.facturacion.model.EmisorDTO;
import com.example.proyec_herramientas.facturacion.model.ItemComprobanteDTO;
import com.example.proyec_herramientas.facturacion.model.LeyendaDTO;
import com.example.proyec_herramientas.facturacion.model.MedioPagoDTO;
import com.example.proyec_herramientas.facturacion.model.ReceptorDTO;
import com.example.proyec_herramientas.facturacion.model.TotalesDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el XML del comprobante de pago electronico con el estandar UBL 2.1
 * de SUNAT (Anexo N.° 9-A del Anexo 8 de la RSO 097-2012/SUNAT).
 *
 * <p>Mapeo de los campos principales:</p>
 * <ul>
 *   <li>{@code cbc:UBLVersionID} = 2.1</li>
 *   <li>{@code cbc:CustomizationID} = 2.0</li>
 *   <li>{@code cbc:ProfileID} = tipo de operacion (Catalogo 51; 0101 venta interna)</li>
 *   <li>{@code cbc:ID} = serie-correlativo</li>
 *   <li>{@code cbc:IssueDate} / {@code cbc:IssueTime} = fecha y hora de emision</li>
 *   <li>{@code cbc:InvoiceTypeCode} = tipo de documento (Catalogo 01: 01 factura, 03 boleta)</li>
 *   <li>{@code cbc:Note} = leyendas (Catalogo 52)</li>
 *   <li>{@code cbc:DocumentCurrencyCode} = moneda (Catalogo 02)</li>
 *   <li>{@code cac:PaymentTerms} / {@code cac:PaymentMeans} = forma de pago y medios de pago</li>
 *   <li>{@code cac:TaxTotal} = total del IGV</li>
 *   <li>{@code cac:LegalMonetaryTotal} = totales del comprobante</li>
 *   <li>{@code cac:InvoiceLine} = items</li>
 * </ul>
 *
 * <p>En cada linea: {@code cbc:LineExtensionAmount} es la base sin IGV,
 * {@code cbc:PricingReference} el precio unitario con IGV (Catalogo 16, codigo 01)
 * y {@code cac:Price/cbc:PriceAmount} el valor unitario sin IGV.</p>
 *
 * <p>El codigo QR NO se embebe en el XML: la RSM 300-2019/SUNAT lo exige solo
 * en la representacion impresa y el Valor Resumen se calcula sobre el XML
 * firmado. La firma digital (ds:Signature) queda como placeholder: se
 * completa con el certificado del emisor.</p>
 */
@Service
public class UblXmlGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String NS_SAC = "urn:sunat:names:specification:ubl:peru:commonAggregateComponents-2";
    private static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";

    /** Tipo de operacion: venta interna (Catalogo 51). */
    public static final String TIPO_OPERACION_VENTA_INTERNA = "0101";

    /** Codigos de tipo de tributo (Catalogo 05). */
    private static final String TRIBUTO_IGV = "1000";
    private static final String TRIBUTO_EXONERADO = "9997";
    private static final String TRIBUTO_INAFECTO = "9998";
    private static final String TRIBUTO_GRATUITO = "9996";

    /** Zona horaria de Lima (UTC-05:00) para cbc:IssueTime. */
    private static final String ZONA_HORARIA = "-05:00";

    /**
     * Genera el XML del comprobante.
     *
     * @param comprobante datos ya calculados por el backend
     */
    public String generar(ComprobanteRespuestaDTO comprobante) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Invoice xmlns=\"").append(NS_INVOICE).append("\"\n")
                .append("         xmlns:cac=\"").append(NS_CAC).append("\"\n")
                .append("         xmlns:cbc=\"").append(NS_CBC).append("\"\n")
                .append("         xmlns:ext=\"").append(NS_EXT).append("\"\n")
                .append("         xmlns:sac=\"").append(NS_SAC).append("\"")
                .append(">\n");

        escribirExtensiones(xml, comprobante);
        escribirCabecera(xml, comprobante);
        escribirEmisor(xml, comprobante.getEmisor());
        escribirReceptor(xml, comprobante.getReceptor());
        escribirEntrega(xml, comprobante.getEntrega());
        escribirFormaPago(xml, comprobante);
        escribirDescuentos(xml, comprobante);
        escribirTotales(xml, comprobante);
        escribirItems(xml, comprobante);

        xml.append("</Invoice>\n");
        return xml.toString();
    }

    private void escribirExtensiones(StringBuilder xml, ComprobanteRespuestaDTO comprobante) {
        TotalesDTO totales = comprobante.getTotales();
        String moneda = comprobante.getMoneda();

        xml.append("  <ext:UBLExtensions>\n");
        // Firma digital: el placeholder lo reemplaza el firmante (XmlDSig).
        xml.append("    <ext:UBLExtension>\n")
                .append("      <ext:ExtensionContent>\n")
                .append("        <ds:Signature xmlns:ds=\"").append(NS_DS).append("\" Id=\"SignatureSP\"/>\n")
                .append("      </ext:ExtensionContent>\n")
                .append("    </ext:UBLExtension>\n");

        // Otros conceptos tributarios (Catalogo 14).
        xml.append("    <ext:UBLExtension>\n")
                .append("      <ext:ExtensionContent>\n")
                .append("        <sac:AdditionalInformation>\n");
        montoAdicional(xml, "1001", totales.getOpGravada(), moneda);
        montoAdicional(xml, "1002", totales.getOpInafecta(), moneda);
        montoAdicional(xml, "1003", totales.getOpExonerada(), moneda);
        montoAdicional(xml, "1004", totales.getOpGratuita(), moneda);
        montoAdicional(xml, "1005", sumar(totales.getOpGravada(), totales.getOpInafecta(),
                totales.getOpExonerada(), totales.getOpGratuita()), moneda);
        if (totales.getTotalDescuentos() != null
                && totales.getTotalDescuentos().compareTo(BigDecimal.ZERO) > 0) {
            montoAdicional(xml, "2005", totales.getTotalDescuentos(), moneda);
        }
        xml.append("        </sac:AdditionalInformation>\n")
                .append("      </ext:ExtensionContent>\n")
                .append("    </ext:UBLExtension>\n")
                .append("  </ext:UBLExtensions>\n");
    }

    private void montoAdicional(StringBuilder xml, String codigo, BigDecimal monto, String moneda) {
        xml.append("          <sac:AdditionalMonetaryTotal>\n")
                .append("            <cbc:ID>").append(codigo).append("</cbc:ID>\n")
                .append("            <cbc:Amount currencyID=\"").append(moneda).append("\">")
                .append(importe(monto)).append("</cbc:Amount>\n")
                .append("          </sac:AdditionalMonetaryTotal>\n");
    }

    private void escribirCabecera(StringBuilder xml, ComprobanteRespuestaDTO comprobante) {
        xml.append("  <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        xml.append("  <cbc:CustomizationID>2.0</cbc:CustomizationID>\n");
        xml.append("  <cbc:ProfileID schemeName=\"SUNAT:Identificador de Tipo de Operación\"")
                .append(" schemeAgencyName=\"PE:SUNAT\"")
                .append(" schemeURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo17\">")
                .append(TIPO_OPERACION_VENTA_INTERNA).append("</cbc:ProfileID>\n");
        xml.append("  <cbc:ID>").append(escape(comprobante.getNumeroDocumento())).append("</cbc:ID>\n");
        xml.append("  <cbc:IssueDate>").append(comprobante.getFechaEmision()).append("</cbc:IssueDate>\n");
        xml.append("  <cbc:IssueTime>").append(comprobante.getHoraEmision()).append(ZONA_HORARIA)
                .append("</cbc:IssueTime>\n");
        xml.append("  <cbc:InvoiceTypeCode listAgencyName=\"PE:SUNAT\"")
                .append(" listName=\"SUNAT:Identificador de Tipo de Documento\"")
                .append(" listURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01\">")
                .append(comprobante.getTipoComprobante()).append("</cbc:InvoiceTypeCode>\n");

        for (LeyendaDTO leyenda : comprobante.getLeyendas()) {
            if (leyenda.getCodigo() == null || leyenda.getCodigo().isBlank()) {
                // Leyenda no codificada (glosa y "Representacion impresa de ...").
                xml.append("  <cbc:Note><![CDATA[").append(leyenda.getTexto()).append("]]></cbc:Note>\n");
            } else {
                xml.append("  <cbc:Note languageLocaleID=\"").append(escape(leyenda.getCodigo())).append("\">")
                        .append("<![CDATA[").append(leyenda.getTexto()).append("]]>")
                        .append("</cbc:Note>\n");
            }
        }

        xml.append("  <cbc:DocumentCurrencyCode listID=\"ISO 4217 Alpha\" listName=\"Currency\"")
                .append(" listAgencyName=\"United Nations Economic Commission for Europe\"")
                .append(" listURI=\"urn:unece:uncefact:iso4217\">")
                .append(comprobante.getMoneda()).append("</cbc:DocumentCurrencyCode>\n");
        xml.append("  <cbc:LineCountNumeric>").append(comprobante.getDetalles().size())
                .append("</cbc:LineCountNumeric>\n");
    }

    private void escribirEmisor(StringBuilder xml, EmisorDTO emisor) {
        xml.append("  <cac:AccountingSupplierParty>\n")
                .append("    <cac:Party>\n")
                .append("      <cac:PartyIdentification>\n")
                .append("        <cbc:ID schemeID=\"6\">").append(escape(emisor.getRuc())).append("</cbc:ID>\n")
                .append("      </cac:PartyIdentification>\n")
                .append("      <cac:PostalAddress>\n")
                .append("        <cbc:ID schemeID=\"6\">").append(escape(emisor.getUbigeo())).append("</cbc:ID>\n")
                .append("        <cbc:AddressTypeCode>1200</cbc:AddressTypeCode>\n")
                .append("        <cbc:CityName>").append(escape(emisor.getDistrito())).append("</cbc:CityName>\n")
                .append("        <cbc:CountrySubentity>").append(escape(emisor.getProvincia())).append("</cbc:CountrySubentity>\n")
                .append("        <cac:AddressLine>\n")
                .append("          <cbc:Line>").append(escape(emisor.getDomicilioFiscal())).append("</cbc:Line>\n")
                .append("        </cac:AddressLine>\n")
                .append("      </cac:PostalAddress>\n")
                .append("      <cac:PartyLegalEntity>\n")
                .append("        <cbc:RegistrationName>").append(escape(emisor.getRazonSocial())).append("</cbc:RegistrationName>\n")
                .append("      </cac:PartyLegalEntity>\n")
                .append("    </cac:Party>\n")
                .append("  </cac:AccountingSupplierParty>\n");
    }

    private String tipoDocumentoReceptor(ReceptorDTO receptor) {
        String tipo = receptor.getTipoDocumento();
        return tipo == null || tipo.isBlank() ? "0" : escape(tipo);
    }

    private void escribirReceptor(StringBuilder xml, ReceptorDTO receptor) {
        xml.append("  <cac:AccountingCustomerParty>\n")
                .append("    <cac:Party>\n")
                .append("      <cac:PartyIdentification>\n")
                .append("        <cbc:ID schemeID=\"").append(tipoDocumentoReceptor(receptor)).append("\">")
                .append(escape(receptor.getNumeroDocumento())).append("</cbc:ID>\n")
                .append("      </cac:PartyIdentification>\n");
        DomicilioDTO domicilio = receptor.getDomicilioFiscal();
        if (domicilio != null) {
            xml.append("      <cac:PostalAddress>\n")
                    .append("        <cbc:ID schemeID=\"6\">").append(escape(domicilio.getUbigeo())).append("</cbc:ID>\n")
                    .append("        <cbc:AddressTypeCode>1200</cbc:AddressTypeCode>\n")
                    .append("        <cbc:CityName>").append(escape(domicilio.getDistrito())).append("</cbc:CityName>\n")
                    .append("        <cbc:CountrySubentity>").append(escape(domicilio.getProvincia())).append("</cbc:CountrySubentity>\n")
                    .append("        <cac:AddressLine>\n")
                    .append("          <cbc:Line>").append(escape(domicilio.getDireccion())).append("</cbc:Line>\n")
                    .append("        </cac:AddressLine>\n")
                    .append("      </cac:PostalAddress>\n");
        }
        xml.append("      <cac:PartyLegalEntity>\n")
                .append("        <cbc:RegistrationName>").append(escape(receptor.getRazonSocial())).append("</cbc:RegistrationName>\n")
                .append("      </cac:PartyLegalEntity>\n")
                .append("    </cac:Party>\n")
                .append("  </cac:AccountingCustomerParty>\n");
    }

    private void escribirEntrega(StringBuilder xml, DomicilioDTO entrega) {
        if (entrega == null) {
            return;
        }
        xml.append("  <cac:Delivery>\n")
                .append("    <cac:DeliveryAddress>\n")
                .append("      <cac:PostalAddress>\n")
                .append("        <cbc:ID schemeID=\"6\">").append(escape(entrega.getUbigeo())).append("</cbc:ID>\n")
                .append("        <cbc:AddressTypeCode>1200</cbc:AddressTypeCode>\n")
                .append("        <cbc:CityName>").append(escape(entrega.getDistrito())).append("</cbc:CityName>\n")
                .append("        <cbc:CountrySubentity>").append(escape(entrega.getProvincia())).append("</cbc:CountrySubentity>\n")
                .append("        <cac:AddressLine>\n")
                .append("          <cbc:Line>").append(escape(entrega.getDireccion())).append("</cbc:Line>\n")
                .append("        </cac:AddressLine>\n")
                .append("      </cac:PostalAddress>\n")
                .append("    </cac:DeliveryAddress>\n")
                .append("  </cac:Delivery>\n");
    }

    private void escribirFormaPago(StringBuilder xml, ComprobanteRespuestaDTO comprobante) {
        String moneda = comprobante.getMoneda();

        if (ComprobanteRequestDTO.FORMA_PAGO_CREDITO.equals(comprobante.getFormaPago())) {
            xml.append("  <cac:PaymentTerms>\n")
                    .append("    <cbc:ID>FormaPago</cbc:ID>\n")
                    .append("    <cbc:PaymentMeansID>FormaPago</cbc:PaymentMeansID>\n")
                    .append("    <cac:PaymentMandate>\n")
                    .append("      <cbc:ID>CUOTA001</cbc:ID>\n")
                    .append("      <cbc:PaymentMeansID>001</cbc:PaymentMeansID>\n")
                    .append("      <cbc:PaymentDueDate>").append(comprobante.getFechaEmision()).append("</cbc:PaymentDueDate>\n")
                    .append("    </cac:PaymentMandate>\n")
                    .append("  </cac:PaymentTerms>\n");
        } else {
            xml.append("  <cac:PaymentTerms>\n")
                    .append("    <cbc:ID>FormaPago</cbc:ID>\n")
                    .append("    <cbc:PaymentMeansID>FormaPago</cbc:PaymentMeansID>\n")
                    .append("  </cac:PaymentTerms>\n");
        }

        List<MedioPagoDTO> medios = comprobante.getMediosPago();
        if (medios == null || medios.isEmpty()) {
            medios = List.of(medioPorDefecto(comprobante));
        }
        for (MedioPagoDTO medio : medios) {
            xml.append("  <cac:PaymentMeans>\n")
                    .append("    <cbc:ID>").append(escape(medio.getCodigo())).append("</cbc:ID>\n")
                    .append("    <cbc:PaymentMeansCode>").append(escape(medio.getCodigo())).append("</cbc:PaymentMeansCode>\n")
                    .append("    <cbc:Amount currencyID=\"").append(moneda).append("\">")
                    .append(importe(medio.getMonto())).append("</cbc:Amount>\n");
            if (medio.getReferencia() != null && !medio.getReferencia().isBlank()) {
                xml.append("    <cbc:InstructionNote>").append(escape(medio.getReferencia())).append("</cbc:InstructionNote>\n");
            }
            xml.append("  </cac:PaymentMeans>\n");
        }
    }

    private MedioPagoDTO medioPorDefecto(ComprobanteRespuestaDTO comprobante) {
        MedioPagoDTO medio = new MedioPagoDTO();
        medio.setCodigo("001");
        medio.setDescripcion("Efectivo");
        medio.setMonto(comprobante.getTotales().getMontoTotalPagar());
        return medio;
    }

    private void escribirDescuentos(StringBuilder xml, ComprobanteRespuestaDTO comprobante) {
        TotalesDTO totales = comprobante.getTotales();
        if (totales.getTotalDescuentos() == null
                || totales.getTotalDescuentos().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        xml.append("  <cac:AllowanceCharge>\n")
                .append("    <cbc:ChargeIndicator>false</cbc:ChargeIndicator>\n")
                .append("    <cbc:AllowanceChargeReasonCode>00</cbc:AllowanceChargeReasonCode>\n")
                .append("    <cbc:Amount currencyID=\"").append(comprobante.getMoneda()).append("\">")
                .append(importe(totales.getTotalDescuentos())).append("</cbc:Amount>\n")
                .append("  </cac:AllowanceCharge>\n");
    }

    private void escribirTotales(StringBuilder xml, ComprobanteRespuestaDTO comprobante) {
        TotalesDTO totales = comprobante.getTotales();
        String moneda = comprobante.getMoneda();

        BigDecimal base = sumar(totales.getOpGravada(), totales.getOpInafecta(),
                totales.getOpExonerada(), totales.getOpGratuita());

        xml.append("  <cac:TaxTotal>\n")
                .append("    <cbc:TaxAmount currencyID=\"").append(moneda).append("\">")
                .append(importe(totales.getIgv())).append("</cbc:TaxAmount>\n")
                .append("  </cac:TaxTotal>\n");

        xml.append("  <cac:LegalMonetaryTotal>\n")
                .append("    <cbc:LineExtensionAmount currencyID=\"").append(moneda).append("\">")
                .append(importe(base)).append("</cbc:LineExtensionAmount>\n")
                .append("    <cbc:TaxInclusiveAmount currencyID=\"").append(moneda).append("\">")
                .append(importe(totales.getTotalVenta())).append("</cbc:TaxInclusiveAmount>\n")
                .append("    <cbc:AllowanceTotalAmount currencyID=\"").append(moneda).append("\">")
                .append(importe(totales.getTotalDescuentos())).append("</cbc:AllowanceTotalAmount>\n")
                .append("    <cbc:PayableAmount currencyID=\"").append(moneda).append("\">")
                .append(importe(totales.getMontoTotalPagar())).append("</cbc:PayableAmount>\n")
                .append("  </cac:LegalMonetaryTotal>\n");
    }

    private void escribirItems(StringBuilder xml, ComprobanteRespuestaDTO comprobante) {
        String moneda = comprobante.getMoneda();
        int numero = 0;

        for (DetalleComprobanteDTO detalle : comprobante.getDetalles()) {
            numero++;
            String afectacion = detalle.getAfectacionIgv();

            xml.append("  <cac:InvoiceLine>\n");
            xml.append("    <cbc:ID>").append(numero).append("</cbc:ID>\n");
            xml.append("    <cbc:Quantity unitCode=\"").append(escape(detalle.getUnidadMedida())).append("\">")
                    .append(cantidad(detalle.getCantidad())).append("</cbc:Quantity>\n");
            // Monto base del item (sin IGV).
            xml.append("    <cbc:LineExtensionAmount currencyID=\"").append(moneda).append("\">")
                    .append(importe(detalle.getBase())).append("</cbc:LineExtensionAmount>\n");
            // Precio unitario con IGV (Catalogo 16, codigo 01).
            xml.append("    <cbc:PricingReference>\n")
                    .append("      <cac:AlternativeConditionPrice>\n")
                    .append("        <cbc:PriceAmount currencyID=\"").append(moneda).append("\">")
                    .append(importe(detalle.getPrecioUnitario())).append("</cbc:PriceAmount>\n")
                    .append("        <cbc:PriceTypeCode>01</cbc:PriceTypeCode>\n")
                    .append("      </cac:AlternativeConditionPrice>\n")
                    .append("    </cbc:PricingReference>\n");
            // Descuento o recargo asociado al tributo del item.
            xml.append("    <cac:AllowanceCharge>\n")
                    .append("      <cbc:ChargeIndicator>false</cbc:ChargeIndicator>\n")
                    .append("      <cbc:AllowanceChargeReasonCode>00</cbc:AllowanceChargeReasonCode>\n")
                    .append("      <cac:TaxCategory>\n")
                    .append("        <cbc:TierRange>8</cbc:TierRange>\n")
                    .append("        <cbc:TaxScheme>\n")
                    .append("          <cbc:ID>").append(codigoTributo(afectacion)).append("</cbc:ID>\n")
                    .append("          <cbc:Name>").append(nombreTributo(afectacion)).append("</cbc:Name>\n")
                    .append("          <cbc:TaxTypeCode>VAT</cbc:TaxTypeCode>\n")
                    .append("        </cbc:TaxScheme>\n")
                    .append("      </cac:TaxCategory>\n")
                    .append("    </cac:AllowanceCharge>\n");
            // IGV del item.
            xml.append("    <cac:TaxTotal>\n")
                    .append("      <cbc:TaxAmount currencyID=\"").append(moneda).append("\">")
                    .append(importe(detalle.getIgv())).append("</cbc:TaxAmount>\n")
                    .append("    </cac:TaxTotal>\n");
            xml.append("    <cac:Item>\n")
                    .append("      <cbc:Description>").append(escape(detalle.getDescripcion())).append("</cbc:Description>\n")
                    .append("      <cac:CommodityClassification>\n")
                    .append("        <cbc:ItemClassificationCode listID=\"UNSPSC\">")
                    .append(escape(detalle.getCodigo())).append("</cbc:ItemClassificationCode>\n")
                    .append("      </cac:CommodityClassification>\n")
                    .append("    </cac:Item>\n");
            // Valor unitario sin IGV.
            xml.append("    <cac:Price>\n")
                    .append("      <cbc:PriceAmount currencyID=\"").append(moneda).append("\">")
                    .append(valorUnitario(detalle.getValorUnitario())).append("</cbc:PriceAmount>\n")
                    .append("    </cac:Price>\n");
            xml.append("  </cac:InvoiceLine>\n");
        }
    }

    private String codigoTributo(String afectacion) {
        if (ItemComprobanteDTO.AFECTACION_INAFECTA.equals(afectacion)) {
            return TRIBUTO_INAFECTO;
        }
        if (ItemComprobanteDTO.AFECTACION_EXONERADA.equals(afectacion)) {
            return TRIBUTO_EXONERADO;
        }
        if (ItemComprobanteDTO.AFECTACION_GRATUITA.equals(afectacion)) {
            return TRIBUTO_GRATUITO;
        }
        return TRIBUTO_IGV;
    }

    private String nombreTributo(String afectacion) {
        if (ItemComprobanteDTO.AFECTACION_INAFECTA.equals(afectacion)) {
            return "INA";
        }
        if (ItemComprobanteDTO.AFECTACION_EXONERADA.equals(afectacion)) {
            return "EXO";
        }
        if (ItemComprobanteDTO.AFECTACION_GRATUITA.equals(afectacion)) {
            return "GRA";
        }
        return "IGV";
    }

    private BigDecimal sumar(BigDecimal... valores) {
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal valor : valores) {
            if (valor != null) {
                suma = suma.add(valor);
            }
        }
        return suma;
    }

    private String importe(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String valorUnitario(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    private String cantidad(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Nombre del archivo XML segun el Anexo I de la RSM 300-2019/SUNAT:
     * {@code RUC-TIPO-SERIE-CORRELATIVO.XML}, con el correlativo en 6 digitos.
     */
    public String nombreArchivoXml(String ruc, String tipoComprobante, String serie, int correlativo) {
        return ruc + "-" + tipoComprobante + "-" + serie + "-" + String.format("%06d", correlativo) + ".XML";
    }

    public String fechaIso(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA);
    }

    public String horaIso(LocalDateTime fechaHora) {
        return fechaHora.format(HORA);
    }
}
