package org.example.donatrackmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record McpServerProperties(Services services, Http http) {

    public record Services(
            String logisticaBaseUrl,
            String donacionesBaseUrl,
            String incentivosBaseUrl,
            String donadoresEntidadesBaseUrl,
            String healthPath) {
    }

    public record Http(Duration connectTimeout, Duration readTimeout, int maxResponseChars) {
    }
}
