package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.config.McpServerProperties;
import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class SistemaTools {

    private final DownstreamClient downstream;
    private final McpServerProperties props;

    public SistemaTools(DownstreamClient downstream, McpServerProperties props) {
        this.downstream = downstream;
        this.props = props;
    }

    @McpTool(name = "estado_servicios",
            description = "Verifica si los cuatro módulos de DonaTrack (Logística, Donaciones, Incentivos, "
                    + "Donadores y Entidades) responden. Sirve también para despertarlos si están dormidos "
                    + "(cold start). Usar ante cualquier error de conexión de otra tool.")
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

    private Map.Entry<String, Object> ping(Servicio servicio) {
        long inicio = System.currentTimeMillis();
        try {
            downstream.get(servicio, props.services().healthPath());
            return Map.entry(servicio.nombre(), Map.of("estado", "OK", "ms", System.currentTimeMillis() - inicio));
        } catch (RuntimeException e) {
            return Map.entry(servicio.nombre(), Map.of("estado", "ERROR", "detalle", String.valueOf(e.getMessage())));
        }
    }
}
