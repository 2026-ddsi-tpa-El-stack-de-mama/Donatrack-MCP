package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Consultas complejas del modelo de lectura (Servicio de Consultas, resuelve solo contra MongoDB).
 * Los datos tienen consistencia eventual: las descriptions se lo avisan al modelo para que no lo tome como error.
 */
@Component
public class ConsultasTools {

    private static final Servicio S = Servicio.CONSULTAS;
    private static final String CONSISTENCIA = " Modelo de lectura con consistencia eventual: una operación recién "
            + "hecha puede tardar unos segundos en verse; si falta, reintentar antes de concluir que no existe.";
    private final DownstreamClient d;

    public ConsultasTools(DownstreamClient d) {
        this.d = d;
    }

    @McpTool(name = "ficha_integral_donador",
            description = "CONSULTA 1 - Resumen completo de un donador: estado, categoría actual e historial, "
                    + "insignias, misión en curso con progreso, donaciones por estado, quejas recibidas y entidades "
                    + "beneficiadas con unidades entregadas. Preferirla para cualquier resumen de un donador. "
                    + "Si el usuario no dio el id, obtenerlo con consultar_donadores." + CONSISTENCIA)
    public String fichaIntegralDonador(
            @McpToolParam(description = "Id del donador", required = true) String donadorId) {
        return d.get(S, "/fichaIntegralDonador/{id}", donadorId);
    }

    @McpTool(name = "necesidades_urgentes_sin_cubrir",
            description = "CONSULTA 2 - Necesidades insatisfechas ordenadas por urgencia. Para cada una: entidad, "
                    + "cantidad faltante, stock del producto en cada depósito y unidades asignadas aún no entregadas. "
                    + "Los ids de categoría y subcategoría salen de consultar_categorias." + CONSISTENCIA)
    public String necesidadesUrgentesSinCubrir(
            @McpToolParam(description = "Urgencia mínima (entero, opcional)", required = false) Integer urgenciaMinima,
            @McpToolParam(description = "EXTRAORDINARIA | RECURRENTE (opcional)", required = false) String tipo,
            @McpToolParam(description = "Id de la categoría del producto (opcional)", required = false)
            String categoriaId,
            @McpToolParam(description = "Id de la subcategoría del producto (opcional)", required = false)
            String subcategoriaId) {
        var q = Args.body("urgenciaMinima", urgenciaMinima, "tipo", tipo,
                "categoriaId", categoriaId, "subcategoriaId", subcategoriaId);
        return d.get(S, Args.query("/necesidadesUrgentes", q), q.values().toArray());
    }

    @McpTool(name = "estado_depositos",
            description = "CONSULTA 3 - Para todos los depósitos o uno en particular: ocupación (stock sobre "
                    + "capacidad), algoritmo de matchmaking, asignaciones, tiempo promedio entre ingreso y entrega de "
                    + "una donación y entidades atendidas."
                    + CONSISTENCIA)
    public String estadoDepositos(
            @McpToolParam(description = "Id del depósito; omitir para todos", required = false) String depositoId) {
        return Args.vacio(depositoId) ? d.get(S, "/estadoDepositos") : d.get(S, "/estadoDepositos/{id}", depositoId);
    }

    @McpTool(name = "calidad_donaciones",
            description = "CONSULTA 4 - Proporción de donaciones que terminaron con queja por categoría y subcategoría "
                    + "de producto, y donadores con más quejas con su estado y categoría." + CONSISTENCIA)
    public String calidadDonaciones(
            @McpToolParam(description = "Cantidad de donadores a listar (entero, opcional)", required = false)
            Integer topDonadores) {
        var q = Args.body("topDonadores", topDonadores);
        return d.get(S, Args.query("/calidadDonacion", q), q.values().toArray());
    }
}
