package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Los 6 flujos principales de DonaTrack (los que integran varios módulos), uno a uno con la consigna.
 * Cada tool es un adaptador fino: la orquestación y las reglas viven en los módulos.
 */
@Component
public class FlujosTools {

    private final DownstreamClient d;

    public FlujosTools(DownstreamClient d) {
        this.d = d;
    }

    // 1
    @McpTool(name = "realizar_donacion",
            description = "FLUJO 1 - Registra una donación nueva en el módulo de Donaciones, que aplica todas las "
                    + "reglas de negocio (el estado inicial lo define el módulo). Ids: donador con "
                    + "consultar_donadores, producto con consultar_productos, depósito con consultar_depositos.")
    public String realizarDonacion(
            @McpToolParam(description = "Id del donador", required = true) String donadorId,
            @McpToolParam(description = "Id del producto donado", required = true) String productoId,
            @McpToolParam(description = "Cantidad donada (entero positivo)", required = true) Integer cantidad,
            @McpToolParam(description = "Descripción de la donación", required = false) String descripcion,
            @McpToolParam(description = "Id del depósito destino, solo si el usuario lo indica", required = false)
            String depositoId) {
        return d.post(Servicio.DONACIONES, "/donaciones", Args.body("donadorID", donadorId, "productoID", productoId,
                "cantidad", cantidad, "descripcion", descripcion, "depositoID", depositoId));
    }

    // 2
    @McpTool(name = "reportar_entrega_paquete",
            description = "FLUJO 2 - Reporta en Logística la entrega de un paquete. Completar los datos que el "
                    + "usuario conozca; Logística valida. El paquete se encuentra con consultar_paquete o "
                    + "consultar_asignaciones.")
    public String reportarEntregaPaquete(
            @McpToolParam(description = "Id del paquete entregado", required = true) String paqueteId,
            @McpToolParam(description = "Id de la donación a la que pertenece", required = false) String donacionId,
            @McpToolParam(description = "Producto del paquete", required = false) String producto,
            @McpToolParam(description = "Cantidad entregada (entero)", required = false) Integer cantidad) {
        return d.post(Servicio.LOGISTICA, "/entregas", Args.body("id", paqueteId, "donacionID", donacionId,
                "producto", producto, "cantidad", cantidad));
    }

    // 3
    @McpTool(name = "registrar_queja_donacion",
            description = "FLUJO 3 - Registra una queja sobre una donación ya entregada. Solo el módulo de "
                    + "Donaciones decide si la queja es válida (p. ej. si la donación fue entregada).")
    public String registrarQuejaDonacion(
            @McpToolParam(description = "Id de la donación (ver consultar_donaciones)", required = true)
            String donacionId,
            @McpToolParam(description = "Texto de la queja", required = true)
            String descripcion) {
        return d.post(Servicio.DONACIONES, "/donaciones/{id}/queja", descripcion, donacionId);
    }

    // 4
    @McpTool(name = "procesar_donador",
            description = "FLUJO 4 - Ejecuta el procesamiento de incentivos de un donador (evaluación de misión, "
                    + "insignias y categoría) en el módulo de Incentivos. Devuelve el resultado del procesamiento.")
    public String procesarDonador(
            @McpToolParam(description = "Id del donador", required = true) String donadorId) {
        return d.accion(Servicio.INCENTIVOS, "/misiones/donadores/{id}/procesar", donadorId);
    }

    // 5
    @McpTool(name = "registrar_necesidad",
            description = "FLUJO 5 - Registra una nueva necesidad material de una entidad benéfica. La entidad sale "
                    + "de consultar_entidades y el producto de consultar_productos.")
    public String registrarNecesidad(
            @McpToolParam(description = "Id de la entidad benéfica", required = true) String entidadId,
            @McpToolParam(description = "Id del producto solicitado", required = true) String productoSolicitadoId,
            @McpToolParam(description = "Cantidad objetivo (entero)", required = true) Integer cantidadObjetivo,
            @McpToolParam(description = "Nivel de urgencia (entero)", required = true) Integer nivelDeUrgencia,
            @McpToolParam(description = "EXTRAORDINARIA | RECURRENTE", required = true) String tipo,
            @McpToolParam(description = "Descripción", required = false) String descripcion,
            @McpToolParam(description = "Cantidad ya cubierta; omitir para que la defina el módulo", required = false)
            Integer cantidadActual) {
        return d.post(Servicio.DONADORES_ENTIDADES, "/necesidades", Args.body("entidadID", entidadId,
                "productoSolicitadoID", productoSolicitadoId, "cantidadObjetivo", cantidadObjetivo,
                "nivelDeUrgencia", nivelDeUrgencia, "tipo", tipo, "descripcion", descripcion,
                "cantidadActual", cantidadActual));
    }

    // 6
    @McpTool(name = "obtener_estadisticas_donador",
            description = "FLUJO 6 - Devuelve las estadísticas de un donador (datos, estado, categoría, misión actual "
                    + "e insignias). Si el usuario no dio el id, obtenerlo con consultar_donadores.")
    public String obtenerEstadisticasDonador(
            @McpToolParam(description = "Id del donador", required = true) String donadorId) {
        return d.get(Servicio.DONADORES_ENTIDADES, "/donadores/{id}/estadisticas", donadorId);
    }
}
