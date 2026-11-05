package org.example.donatrackmcp.downstream;

import org.example.donatrackmcp.config.McpServerProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Único punto de salida HTTP hacia los módulos, siempre a través del API Gateway: cada path se envía como
 * {@code <gateway>/<prefijo del servicio><path>}. Devuelve el body crudo (JSON o texto) sin mapear a DTOs:
 * el contrato lo sigue definiendo cada módulo y no hay DTOs espejo que mantener.
 *
 * Usa el HttpClient del JDK (no HttpURLConnection) porque este último NO soporta el verbo PATCH.
 */
@Component
public class DownstreamClient {

    private static final int MAX_ERROR_BODY = 500;
    private static final String OK_VACIO = "{\"ok\":true}";
    private static final String GET_VACIO =
            "{\"resultado\":null,\"nota\":\"El módulo respondió 200 sin cuerpo: probablemente el recurso no existe.\"}";

    private final RestClient gateway;
    private final Map<Servicio, String> prefijos = new EnumMap<>(Servicio.class);
    private final int maxResponseChars;

    public DownstreamClient(McpServerProperties props) {
        var g = props.gateway();
        var p = g.prefijos();
        this.gateway = build(sinBarraFinal(g.baseUrl()), props);
        prefijos.put(Servicio.GATEWAY, "");
        prefijos.put(Servicio.LOGISTICA, normalizar(p.logistica()));
        prefijos.put(Servicio.DONACIONES, normalizar(p.donaciones()));
        prefijos.put(Servicio.INCENTIVOS, normalizar(p.incentivos()));
        prefijos.put(Servicio.DONADORES_ENTIDADES, normalizar(p.donadoresEntidades()));
        prefijos.put(Servicio.CONSULTAS, normalizar(p.consultas()));
        this.maxResponseChars = props.http().maxResponseChars();
    }

    public String get(Servicio servicio, String path, Object... uriVars) {
        return call(servicio, HttpMethod.GET, path, null, uriVars);
    }

    /** GET sin truncar, para procesar la respuesta dentro del MCP. No devolverla entera al modelo. */
    public String getCompleto(Servicio servicio, String path, Object... uriVars) {
        return sinTruncar(servicio, HttpMethod.GET, path, null, uriVars);
    }

    /** POST sin cuerpo (acciones: procesar, asignar...). */
    public String accion(Servicio servicio, String path, Object... uriVars) {
        return call(servicio, HttpMethod.POST, path, null, uriVars);
    }

    public String post(Servicio servicio, String path, Object body, Object... uriVars) {
        return call(servicio, HttpMethod.POST, path, body, uriVars);
    }

    public String put(Servicio servicio, String path, Object body, Object... uriVars) {
        return call(servicio, HttpMethod.PUT, path, body, uriVars);
    }

    public String patch(Servicio servicio, String path, Object body, Object... uriVars) {
        return call(servicio, HttpMethod.PATCH, path, body, uriVars);
    }

    public String delete(Servicio servicio, String path, Object... uriVars) {
        return call(servicio, HttpMethod.DELETE, path, null, uriVars);
    }

    private String call(Servicio servicio, HttpMethod metodo, String path, Object body, Object[] uriVars) {
        return limitar(sinTruncar(servicio, metodo, path, body, uriVars));
    }

    private String sinTruncar(Servicio servicio, HttpMethod metodo, String path, Object body, Object[] uriVars) {
        try {
            RestClient.RequestBodySpec spec = gateway.method(metodo).uri(prefijos.get(servicio) + path, uriVars);
            if (body != null) {
                spec = spec.body(body);
            }
            String respuesta = spec.retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw error(servicio, res.getStatusCode().value(), res.getBody().readAllBytes());
                    })
                    .body(String.class);
            if (respuesta == null || respuesta.isBlank()) {
                return metodo == HttpMethod.GET ? GET_VACIO : OK_VACIO;
            }
            return respuesta;
        } catch (ResourceAccessException e) {
            throw new DownstreamException(servicio, null,
                    servicio.nombre() + " no responde (timeout o conexión rechazada). "
                            + "Si corre en Render puede estar despertando: reintentar en unos segundos.", e);
        }
    }

    private String limitar(String respuesta) {
        if (respuesta.length() <= maxResponseChars) {
            return respuesta;
        }
        return respuesta.substring(0, maxResponseChars)
                + "\n[RESPUESTA TRUNCADA: " + maxResponseChars + " de " + respuesta.length()
                + " caracteres. Pedir un recurso puntual por id o filtrar.]";
    }

    private DownstreamException error(Servicio servicio, int status, byte[] rawBody) {
        String body = new String(rawBody, StandardCharsets.UTF_8).trim();
        if (body.length() > MAX_ERROR_BODY) {
            body = body.substring(0, MAX_ERROR_BODY) + "...";
        }
        return new DownstreamException(servicio, status,
                servicio.nombre() + " respondió HTTP " + status + (body.isEmpty() ? "" : ": " + body), null);
    }

    /** "" o "/x" (con barra inicial y sin barra final), para concatenar con paths que empiezan con "/". */
    private static String normalizar(String prefijo) {
        String p = prefijo == null ? "" : sinBarraFinal(prefijo.trim());
        return p.isEmpty() || p.startsWith("/") ? p : "/" + p;
    }

    private static String sinBarraFinal(String s) {
        return s.replaceAll("/+$", "");
    }

    private static RestClient build(String baseUrl, McpServerProperties props) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)   // evita el upgrade h2c sobre http plano
                .connectTimeout(props.http().connectTimeout())
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(props.http().readTimeout());
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
