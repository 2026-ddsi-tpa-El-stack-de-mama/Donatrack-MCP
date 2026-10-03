package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Módulo Donadores y Entidades. No se expone el cambio manual de categoría de un donador: la categoría
 * es consecuencia de procesar_donador (Incentivos); permitir un override es una decisión de negocio.
 */
@Component
public class DonadoresEntidadesTools {

    private static final Servicio S = Servicio.DONADORES_ENTIDADES;
    private static final Servicio INC = Servicio.INCENTIVOS;
    private final DownstreamClient d;

    public DonadoresEntidadesTools(DownstreamClient d) {
        this.d = d;
    }

    // ---- donadores ----

    @McpTool(name = "consultar_donadores",
            description = "Sin id: lista los donadores. Con id: devuelve ese donador.")
    public String consultarDonadores(
            @McpToolParam(description = "Id del donador (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/donadores") : d.get(S, "/donadores/{id}", id);
    }

    @McpTool(name = "crear_donador", description = "Da de alta un donador.")
    public String crearDonador(
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Apellido", required = true) String apellido,
            @McpToolParam(description = "Edad (entero)", required = true) Integer edad,
            @McpToolParam(description = "Email", required = true) String email,
            @McpToolParam(description = "Número de documento", required = true) String nroDocumento,
            @McpToolParam(description = "Domicilio", required = false) String domicilio,
            @McpToolParam(description = "VERIFICADO | SOSPECHOSO | BANEADO; omitir para que lo defina el módulo",
                    required = false) String estado,
            @McpToolParam(description = "OCASIONAL | COLABORADOR | TRANSFORMADOR | SALVADOR | REVOLUCIONARIO; "
                    + "omitir para que lo defina el módulo", required = false) String categoria) {
        return d.post(S, "/donadores", Args.body("nombre", nombre, "apellido", apellido, "edad", edad,
                "email", email, "nroDocumento", nroDocumento, "domicilio", domicilio, "estado", estado,
                "categoria", categoria));
    }

    @McpTool(name = "cambiar_estado_donador", description = "Cambia el estado de un donador (queda en su historial).")
    public String cambiarEstadoDonador(
            @McpToolParam(description = "Id del donador", required = true) String id,
            @McpToolParam(description = "VERIFICADO | SOSPECHOSO | BANEADO", required = true) String estado) {
        return d.patch(S, "/donadores/{id}/estado?estado={estado}", null, id, estado);
    }

    @McpTool(name = "consultar_si_puede_donar",
            description = "Indica si el módulo considera que el donador está habilitado para donar.")
    public String consultarSiPuedeDonar(
            @McpToolParam(description = "Id del donador", required = true) String id) {
        return d.get(S, "/donadores/{id}/puede-donar", id);
    }

    @McpTool(name = "consultar_historial_donador",
            description = "Consulta un aspecto histórico o de incentivos de un donador. Valores de 'aspecto': "
                    + "estados (historial de estados), quejas, categorias (historial de categorías), "
                    + "misiones (historial de misiones), insignias (las que tiene), "
                    + "mision_en_curso (misión actual).")
    public String consultarHistorialDonador(
            @McpToolParam(description = "Id del donador", required = true) String id,
            @McpToolParam(description = "estados | quejas | categorias | misiones | insignias | mision_en_curso",
                    required = true) String aspecto) {
        return switch (aspecto == null ? "" : aspecto.trim().toLowerCase()) {
            case "estados" -> d.get(S, "/donadores/{id}/historial-estados", id);
            case "quejas" -> d.get(S, "/donadores/{id}/quejas", id);
            case "categorias" -> d.get(INC, "/donadores/{id}/categorias/historial", id);
            case "misiones" -> d.get(INC, "/misiones/donadores/{id}/historial", id);
            case "insignias" -> d.get(INC, "/insignias/donadores/{id}", id);
            case "mision_en_curso" -> d.get(INC, "/misiones/donadores/{id}/mision", id);
            default -> throw new IllegalArgumentException("aspecto inválido '" + aspecto
                    + "'. Valores: estados, quejas, categorias, misiones, insignias, mision_en_curso");
        };
    }

    // ---- entidades ----

    @McpTool(name = "consultar_entidades",
            description = "Sin id: lista las entidades benéficas. Con id: devuelve esa entidad.")
    public String consultarEntidades(
            @McpToolParam(description = "Id de la entidad (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/entidades") : d.get(S, "/entidades/{id}", id);
    }

    @McpTool(name = "crear_entidad", description = "Da de alta una entidad benéfica.")
    public String crearEntidad(
            @McpToolParam(description = "Razón social", required = true) String razonSocial,
            @McpToolParam(description = "Domicilio", required = true) String domicilio,
            @McpToolParam(description = "Teléfono", required = false) String telefono,
            @McpToolParam(description = "Correo", required = false) String correo) {
        return d.post(S, "/entidades", Args.body("razonSocial", razonSocial, "domicilio", domicilio,
                "telefono", telefono, "correo", correo));
    }

    @McpTool(name = "modificar_entidad",
            description = "Reemplaza los datos de una entidad benéfica (PUT): enviar todos los campos, no solo los que cambian.")
    public String modificarEntidad(
            @McpToolParam(description = "Id de la entidad", required = true) String id,
            @McpToolParam(description = "Razón social", required = true) String razonSocial,
            @McpToolParam(description = "Domicilio", required = true) String domicilio,
            @McpToolParam(description = "Teléfono", required = false) String telefono,
            @McpToolParam(description = "Correo", required = false) String correo) {
        return d.put(S, "/entidades/{id}", Args.body("id", id, "razonSocial", razonSocial, "domicilio", domicilio,
                "telefono", telefono, "correo", correo), id);
    }

    // ---- necesidades (registrar_necesidad está en FlujosTools) ----

    @McpTool(name = "consultar_necesidades",
            description = "Sin id: lista las necesidades materiales. Con id: devuelve esa necesidad.")
    public String consultarNecesidades(
            @McpToolParam(description = "Id de la necesidad (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/necesidades") : d.get(S, "/necesidades/{id}", id);
    }

    @McpTool(name = "consultar_necesidades_insatisfechas",
            description = "Lista las necesidades aún no satisfechas de un producto.")
    public String consultarNecesidadesInsatisfechas(
            @McpToolParam(description = "Id del producto", required = true) String productoId) {
        return d.get(S, "/necesidades/insatisfechas?productoId={p}", productoId);
    }

    @McpTool(name = "satisfacer_necesidad",
            description = "Registra que se cubrió una cantidad de una necesidad.")
    public String satisfacerNecesidad(
            @McpToolParam(description = "Id de la necesidad", required = true) String id,
            @McpToolParam(description = "Cantidad satisfecha (entero)", required = true) Integer cantidad) {
        return d.post(S, "/necesidades/{id}/satisfaccion?cantidad={c}", null, id, cantidad);
    }

    @McpTool(name = "modificar_necesidad",
            description = "Reemplaza los datos de una necesidad (PUT): enviar todos los campos, no solo los que cambian.")
    public String modificarNecesidad(
            @McpToolParam(description = "Id de la necesidad", required = true) String id,
            @McpToolParam(description = "Id de la entidad benéfica", required = true) String entidadId,
            @McpToolParam(description = "Id del producto solicitado", required = true) String productoSolicitadoId,
            @McpToolParam(description = "Cantidad objetivo (entero)", required = true) Integer cantidadObjetivo,
            @McpToolParam(description = "Cantidad ya cubierta (entero)", required = true) Integer cantidadActual,
            @McpToolParam(description = "Nivel de urgencia (entero)", required = true) Integer nivelDeUrgencia,
            @McpToolParam(description = "EXTRAORDINARIA | RECURRENTE", required = true) String tipo,
            @McpToolParam(description = "Descripción", required = false) String descripcion) {
        return d.put(S, "/necesidades/{id}", Args.body("id", id, "entidadID", entidadId,
                "productoSolicitadoID", productoSolicitadoId, "cantidadObjetivo", cantidadObjetivo,
                "cantidadActual", cantidadActual, "nivelDeUrgencia", nivelDeUrgencia, "tipo", tipo,
                "descripcion", descripcion), id);
    }

    @McpTool(name = "eliminar_necesidad", description = "DESTRUCTIVA: elimina una necesidad. Confirmar con el usuario.")
    public String eliminarNecesidad(
            @McpToolParam(description = "Id de la necesidad", required = true) String id) {
        return d.delete(S, "/necesidades/{id}", id);
    }
}
