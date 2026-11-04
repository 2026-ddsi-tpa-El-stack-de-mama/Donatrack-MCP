package org.example.donatrackmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record McpServerProperties(Gateway gateway, Http http) {

    /**
     * Todo el tráfico sale por el API Gateway. Cada servicio se distingue por el prefijo de su ruta
     * (el gateway lo quita antes de reenviar). Los prefijos deben coincidir con las rutas del gateway.
     */
    public record Gateway(String baseUrl, Prefijos prefijos, String healthPath) {
    }

    public record Prefijos(
            String logistica,
            String donaciones,
            String incentivos,
            String donadoresEntidades,
            String consultas) {
    }

    public record Http(Duration connectTimeout, Duration readTimeout, int maxResponseChars) {
    }
}
