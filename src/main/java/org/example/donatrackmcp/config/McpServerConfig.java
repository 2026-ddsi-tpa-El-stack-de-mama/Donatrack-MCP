package org.example.donatrackmcp.config;

import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    /**
     * Ejecuta las tools de a una, en el hilo que lee stdin. El transporte stdio no admite dos respuestas
     * simultáneas ("Failed to enqueue message"): si Claude pide tools en paralelo, sin esto se pierden respuestas.
     */
    @Bean
    McpSyncServerCustomizer ejecucionSerial() {
        return spec -> spec.immediateExecution(true);
    }
}
