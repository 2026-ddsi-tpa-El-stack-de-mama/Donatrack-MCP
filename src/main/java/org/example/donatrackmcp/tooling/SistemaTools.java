package org.example.donatrackmcp.tooling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.donatrackmcp.config.McpServerProperties;
import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

@Component
public class SistemaTools {

    /** Listados que resume resumen_sistema; campoEstado es opcional y agrega el desglose por ese campo. */
    private record Recurso(String nombre, Servicio servicio, String path, String campoEstado) {
    }

    private static final List<Recurso> RECURSOS = List.of(
            new Recurso("donadores", Servicio.DONADORES_ENTIDADES, "/donadores", "estado"),
            new Recurso("entidades", Servicio.DONADORES_ENTIDADES, "/entidades", null),
            new Recurso("necesidades", Servicio.DONADORES_ENTIDADES, "/necesidades", null),
            new Recurso("donaciones", Servicio.DONACIONES, "/donaciones", "estado"),
            new Recurso("productos", Servicio.DONACIONES, "/productos", null),
            new Recurso("depositos", Servicio.LOGISTICA, "/depositos", null),
            new Recurso("paquetes", Servicio.LOGISTICA, "/paquetes", null),
            new Recurso("asignaciones", Servicio.LOGISTICA, "/asignaciones", "estado"),
            new Recurso("insignias", Servicio.INCENTIVOS, "/insignias", null),
            new Recurso("misiones", Servicio.INCENTIVOS, "/misiones", null));

    private final ObjectMapper mapper = new ObjectMapper();
    private final DownstreamClient downstream;
    private final McpServerProperties props;

    public SistemaTools(DownstreamClient downstream, McpServerProperties props) {
        this.downstream = downstream;
        this.props = props;
    }

    @McpTool(name = "estado_servicios",
            description = "Verifica si responden el API Gateway, los cuatro módulos de DonaTrack (Logística, "
                    + "Donaciones, Incentivos, Donadores y Entidades) y el Servicio de Consultas, todos a través "
                    + "del gateway. Si falla el gateway, fallan todos. Sirve también para despertarlos si están "
                    + "dormidos (cold start). Usar ante cualquier error de conexión de otra tool.")
    public Map<String, Object> estadoServicios() {
        List<CompletableFuture<Map.Entry<String, Object>>> futuros = Arrays.stream(Servicio.values())
                .map(s -> CompletableFuture.supplyAsync(() -> ping(s)))
                .toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        futuros.forEach(f -> {
            var e = f.join();
            resultado.put(e.getKey(), e.getValue());
        });
        return resultado;
    }

    @McpTool(name = "resumen_sistema",
            description = "Foto general del sistema, en vivo desde los módulos: cuántos donadores, entidades, "
                    + "necesidades, donaciones, productos, depósitos, paquetes, asignaciones, insignias y misiones "
                    + "hay, con desglose por estado de donadores, donaciones y asignaciones. Si un recurso falla, "
                    + "se informa el error solo de ese recurso.")
    public Map<String, Object> resumenSistema() {
        List<CompletableFuture<Object>> futuros = RECURSOS.stream()
                .map(r -> CompletableFuture.supplyAsync(() -> contar(r)))
                .toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < RECURSOS.size(); i++) {
            resultado.put(RECURSOS.get(i).nombre(), futuros.get(i).join());
        }
        return resultado;
    }

    private Object contar(Recurso r) {
        try {
            JsonNode lista = mapper.readTree(downstream.getCompleto(r.servicio(), r.path()));
            if (!lista.isArray()) {
                return Map.of("error", r.servicio().nombre() + " no devolvió un listado");
            }
            if (r.campoEstado() == null) {
                return Map.of("total", lista.size());
            }
            Map<String, Integer> porEstado = new TreeMap<>();
            lista.forEach(n -> porEstado.merge(n.path(r.campoEstado()).asText("SIN_ESTADO"), 1, Integer::sum));
            return Map.of("total", lista.size(), "porEstado", porEstado);
        } catch (JsonProcessingException | RuntimeException e) {
            return Map.of("error", String.valueOf(e.getMessage()));
        }
    }

    private Map.Entry<String, Object> ping(Servicio servicio) {
        long inicio = System.currentTimeMillis();
        try {
            downstream.get(servicio, props.gateway().healthPath());
            return Map.entry(servicio.nombre(), Map.of("estado", "OK", "ms", System.currentTimeMillis() - inicio));
        } catch (RuntimeException e) {
            return Map.entry(servicio.nombre(), Map.of("estado", "ERROR", "detalle", String.valueOf(e.getMessage())));
        }
    }
}
