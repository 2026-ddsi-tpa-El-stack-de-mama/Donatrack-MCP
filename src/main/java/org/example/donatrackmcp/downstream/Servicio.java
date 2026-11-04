package org.example.donatrackmcp.downstream;

/**
 * Componentes de DonaTrack a los que el MCP delega, todos detrás del API Gateway.
 * GATEWAY representa al propio gateway (sin prefijo): solo se usa para chequear que responda.
 * El MCP no tiene lógica de dominio propia.
 */
public enum Servicio {
    GATEWAY("API Gateway"),
    LOGISTICA("Logística"),
    DONACIONES("Donaciones"),
    INCENTIVOS("Incentivos"),
    DONADORES_ENTIDADES("Donadores y Entidades"),
    CONSULTAS("Servicio de Consultas");

    private final String nombre;

    Servicio(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
