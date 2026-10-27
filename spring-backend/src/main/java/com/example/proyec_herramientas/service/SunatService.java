package com.example.proyec_herramientas.service;

import com.example.proyec_herramientas.dto.DniResponseDTO;
import com.example.proyec_herramientas.dto.RucResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Consultas de identidad contra RENIEC (DNI) y SUNAT (RUC) mediante una API
 * externa. La URL base y el token se configuran con app.sunat.base-url y
 * app.sunat.token (o la variable de entorno SUNAT_API_TOKEN).
 */
@Service
public class SunatService {

    private static final String RUTA_DNI = "/reniec/dni";
    private static final String RUTA_RUC = "/sunat/ruc";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String token;

    public SunatService(
            @Value("${app.sunat.base-url:https://api.apis.net.pe/v2}") String baseUrl,
            @Value("${app.sunat.token:${SUNAT_API_TOKEN:}}") String token) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5000);
        fabrica.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(fabrica);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token == null ? "" : token.trim();
    }

    public DniResponseDTO consultarDni(String numero) {
        String documento = validar(numero, 8, "DNI");
        JsonNode datos = consultar(RUTA_DNI, documento);
        return new DniResponseDTO(
                texto(datos, "numeroDocumento", documento),
                texto(datos, "nombres", ""),
                texto(datos, "apellidos", ""),
                texto(datos, "direccion", ""),
                texto(datos, "estado", ""));
    }

    public RucResponseDTO consultarRuc(String numero) {
        String documento = validar(numero, 11, "RUC");
        JsonNode datos = consultar(RUTA_RUC, documento);
        return new RucResponseDTO(
                texto(datos, "numeroDocumento", documento),
                texto(datos, "razonSocial", texto(datos, "nombre", "")),
                texto(datos, "direccion", ""),
                texto(datos, "estado", ""));
    }

    private JsonNode consultar(String ruta, String numero) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + ruta)
                .queryParam("numero", numero)
                .toUriString();

        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (!token.isBlank()) {
            cabeceras.setBearerAuth(token);
        }

        try {
            ResponseEntity<JsonNode> respuesta = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(cabeceras), JsonNode.class);
            JsonNode cuerpo = respuesta.getBody();
            if (cuerpo == null || cuerpo.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontro informacion para el documento " + numero);
            }
            return cuerpo;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontro informacion para el documento " + numero);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar el servicio externo de SUNAT/RENIEC");
        }
    }

    private String validar(String numero, int longitud, String etiqueta) {
        String limpio = numero == null ? "" : numero.trim();
        if (!limpio.matches("\\d{" + longitud + "}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El " + etiqueta + " debe tener " + longitud + " digitos");
        }
        return limpio;
    }

    private String texto(JsonNode datos, String campo, String porDefecto) {
        JsonNode valor = datos.get(campo);
        if (valor == null || valor.isNull()) {
            return porDefecto;
        }
        String texto = valor.asText("").trim();
        return texto.isEmpty() ? porDefecto : texto;
    }
}
