package org.example.donatrackmcp.downstream;

/** Módulos de DonaTrack a los que el MCP delega. El MCP no tiene lógica de dominio propia. */
public enum Servicio {
    LOGISTICA("Logística"),
    DONACIONES("Donaciones"),
    INCENTIVOS("Incentivos"),
    DONADORES_ENTIDADES("Donadores y Entidades");

    private final String nombre;

    Servicio(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
