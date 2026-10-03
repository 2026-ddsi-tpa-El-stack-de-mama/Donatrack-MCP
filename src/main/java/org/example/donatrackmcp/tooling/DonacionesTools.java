package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** Módulo Donaciones: consultas y operaciones que no son uno de los 6 flujos principales. */
@Component
public class DonacionesTools {

    private static final Servicio S = Servicio.DONACIONES;
    private final DownstreamClient d;

    public DonacionesTools(DownstreamClient d) {
        this.d = d;
    }

    @McpTool(name = "consultar_donaciones",
            description = "Sin id: lista todas las donaciones. Con id: devuelve esa donación (estado, etc.).")
    public String consultarDonaciones(
            @McpToolParam(description = "Id de la donación (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/donaciones") : d.get(S, "/donaciones/{id}", id);
    }

    @McpTool(name = "buscar_donaciones_de_donador_desde",
            description = "Lista las donaciones de un donador a partir de una fecha.")
    public String buscarDonacionesDeDonadorDesde(
            @McpToolParam(description = "Id del donador", required = true) String donadorId,
            @McpToolParam(description = "Fecha de inicio en formato ISO yyyy-MM-dd", required = true) String fecha) {
        return d.get(S, "/donaciones/search/{id}/{fecha}", donadorId, fecha);
    }

    @McpTool(name = "cambiar_estado_donacion",
            description = "Cambia el estado de una donación; el módulo valida si la transición es permitida. "
                    + "Para quejas usar registrar_queja_donacion, que es el flujo completo.")
    public String cambiarEstadoDonacion(
            @McpToolParam(description = "Id de la donación", required = true) String donacionId,
            @McpToolParam(description = "INGRESADA | ACEPTADA | CONQUEJA", required = true) String estado) {
        return d.patch(S, "/donaciones/{id}/estado?estado={estado}", null, donacionId, estado);
    }

    @McpTool(name = "consultar_historial_donaciones",
            description = "Historial (trazabilidad) de cambios de las donaciones.")
    public String consultarHistorialDonaciones() {
        return d.get(S, "/donacioneshist");
    }

    @McpTool(name = "consultar_productos",
            description = "Sin id: lista el catálogo de productos. Con id: devuelve ese producto.")
    public String consultarProductos(
            @McpToolParam(description = "Id del producto (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/productos") : d.get(S, "/productos/{id}", id);
    }

    @McpTool(name = "crear_producto",
            description = "Agrega un producto al catálogo. La categoría y el identificador deben existir "
                    + "(consultar_categorias, consultar_identificadores).")
    public String crearProducto(
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Descripción", required = false) String descripcion,
            @McpToolParam(description = "Id de la categoría", required = true) String categoriaId,
            @McpToolParam(description = "Id del identificador (QR / código de barras)", required = false)
            String identificadorId) {
        return d.post(S, "/productos", Args.body("nombre", nombre, "descripcion", descripcion,
                "categoriaID", categoriaId, "identificadorID", identificadorId));
    }

    @McpTool(name = "consultar_categorias", description = "Lista las categorías de productos.")
    public String consultarCategorias() {
        return d.get(S, "/categorias");
    }

    @McpTool(name = "crear_categoria", description = "Agrega una categoría de productos.")
    public String crearCategoria(
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Descripción", required = false) String descripcion,
            @McpToolParam(description = "Id de la subcategoría asociada", required = false) String subcategoriaId) {
        return d.post(S, "/categorias", Args.body("nombre", nombre, "descripcion", descripcion,
                "subcategoriaID", subcategoriaId));
    }

    @McpTool(name = "consultar_identificadores",
            description = "Lista los identificadores de producto (QR / código de barras).")
    public String consultarIdentificadores() {
        return d.get(S, "/identificadores");
    }

    @McpTool(name = "crear_identificador", description = "Crea un identificador de producto.")
    public String crearIdentificador(
            @McpToolParam(description = "QR | CODIGODEBARRAS", required = true) String tipo,
            @McpToolParam(description = "Descripción / valor del identificador", required = false)
            String descripcion) {
        return d.post(S, "/identificadores", Args.body("tipo", tipo, "descripcion", descripcion));
    }
}
