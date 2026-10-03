package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Módulo Logística. No se exponen los endpoints internos del flujo de donación
 * (/depositos/{id}/donacion, /asignaciones POST, /asignacionesDirecta, /stock POST): los invoca Donaciones.
 */
@Component
public class LogisticaTools {

    private static final Servicio S = Servicio.LOGISTICA;
    private final DownstreamClient d;

    public LogisticaTools(DownstreamClient d) {
        this.d = d;
    }

    @McpTool(name = "consultar_depositos",
            description = "Sin id: lista los depósitos. Con id: devuelve ese depósito.")
    public String consultarDepositos(
            @McpToolParam(description = "Id del depósito (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/depositos") : d.get(S, "/depositos/{id}", id);
    }

    @McpTool(name = "crear_deposito",
            description = "Da de alta un depósito con su algoritmo de matchmaking. El stock inicial queda vacío.")
    public String crearDeposito(
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Dirección", required = true) String direccion,
            @McpToolParam(description = "Capacidad máxima (entero)", required = true) Integer capacidadMaxima,
            @McpToolParam(description = "SUB_ATENDIDOS | PRIORIDAD_POR_SCORE", required = true) String algoritmo) {
        return d.post(S, "/depositos", Args.body("nombre", nombre, "direccion", direccion,
                "capacidadMaxima", capacidadMaxima, "algoritmo", algoritmo));
    }

    @McpTool(name = "consultar_asignaciones",
            description = "Sin id: lista las asignaciones de paquetes a necesidades. Con id: devuelve esa asignación.")
    public String consultarAsignaciones(
            @McpToolParam(description = "Id de la asignación (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/asignaciones") : d.get(S, "/asignaciones/{id}", id);
    }

    @McpTool(name = "consultar_historial_asignaciones", description = "Historial de asignaciones.")
    public String consultarHistorialAsignaciones() {
        return d.get(S, "/asignacionesHistorial");
    }

    @McpTool(name = "consultar_paquete", description = "Devuelve un paquete por id.")
    public String consultarPaquete(
            @McpToolParam(description = "Id del paquete", required = true) String id) {
        return d.get(S, "/paquetes/{id}", id);
    }

    @McpTool(name = "consultar_stock", description = "Stock disponible de un producto.")
    public String consultarStock(
            @McpToolParam(description = "Id del producto", required = true) String productoId) {
        return d.get(S, "/stock/{id}", productoId);
    }
}
