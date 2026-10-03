package org.example.donatrackmcp.tooling;

import org.example.donatrackmcp.downstream.DownstreamClient;
import org.example.donatrackmcp.downstream.Servicio;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** Módulo Incentivos: insignias y misiones. (procesar_donador está en FlujosTools.) */
@Component
public class IncentivosTools {

    private static final Servicio S = Servicio.INCENTIVOS;
    private static final String CATEGORIAS = "OCASIONAL | COLABORADOR | TRANSFORMADOR | SALVADOR | REVOLUCIONARIO";
    private static final String TIPOS_MISION =
            "COMPLETITUD | DONACIONES_EXITOSAS | DONACIONES_ASCENDENTES | REVOLUCION_DONADORA";

    private final DownstreamClient d;

    public IncentivosTools(DownstreamClient d) {
        this.d = d;
    }

    // ---- insignias ----

    @McpTool(name = "consultar_insignias",
            description = "Sin id: lista las insignias. Con id: devuelve esa insignia.")
    public String consultarInsignias(
            @McpToolParam(description = "Id de la insignia (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/insignias") : d.get(S, "/insignias/{id}", id);
    }

    @McpTool(name = "crear_insignia", description = "Crea una insignia.")
    public String crearInsignia(
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Descripción", required = true) String descripcion) {
        return d.post(S, "/insignias", Args.body("nombre", nombre, "descripcion", descripcion));
    }

    @McpTool(name = "modificar_insignia", description = "Modifica nombre y descripción de una insignia existente.")
    public String modificarInsignia(
            @McpToolParam(description = "Id de la insignia", required = true) String id,
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Descripción", required = true) String descripcion) {
        return d.put(S, "/insignias/{id}", Args.body("id", id, "nombre", nombre, "descripcion", descripcion), id);
    }

    @McpTool(name = "asignar_insignia_a_donador", description = "Asigna manualmente una insignia a un donador.")
    public String asignarInsigniaADonador(
            @McpToolParam(description = "Id de la insignia", required = true) String insigniaId,
            @McpToolParam(description = "Id del donador", required = true) String donadorId) {
        return d.accion(S, "/insignias/{insignia}/donadores/{donador}", insigniaId, donadorId);
    }

    // ---- misiones ----

    @McpTool(name = "consultar_misiones",
            description = "Sin id: lista las misiones. Con id: devuelve esa misión.")
    public String consultarMisiones(
            @McpToolParam(description = "Id de la misión (opcional)", required = false) String id) {
        return Args.vacio(id) ? d.get(S, "/misiones") : d.get(S, "/misiones/{id}", id);
    }

    @McpTool(name = "crear_mision",
            description = "Crea una misión que lleva al donador de una categoría a otra y otorga una insignia. "
                    + "La insignia debe existir (consultar_insignias).")
    public String crearMision(
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Id de la insignia que otorga", required = true) String insigniaId,
            @McpToolParam(description = CATEGORIAS, required = true) String categoriaInicio,
            @McpToolParam(description = CATEGORIAS, required = true) String categoriaFin,
            @McpToolParam(description = TIPOS_MISION, required = true) String tipo) {
        return d.post(S, "/misiones", Args.body("nombre", nombre, "insigniaID", insigniaId,
                "categoriaInicio", categoriaInicio, "categoriaFin", categoriaFin, "tipo", tipo));
    }

    @McpTool(name = "modificar_mision",
            description = "Reemplaza los datos de una misión existente (PUT): enviar todos los campos.")
    public String modificarMision(
            @McpToolParam(description = "Id de la misión", required = true) String id,
            @McpToolParam(description = "Nombre", required = true) String nombre,
            @McpToolParam(description = "Id de la insignia que otorga", required = true) String insigniaId,
            @McpToolParam(description = CATEGORIAS, required = true) String categoriaInicio,
            @McpToolParam(description = CATEGORIAS, required = true) String categoriaFin,
            @McpToolParam(description = TIPOS_MISION, required = true) String tipo) {
        return d.put(S, "/misiones/{id}", Args.body("id", id, "nombre", nombre, "insigniaID", insigniaId,
                "categoriaInicio", categoriaInicio, "categoriaFin", categoriaFin, "tipo", tipo), id);
    }

    @McpTool(name = "asignar_mision_a_donador", description = "Asigna una misión a un donador.")
    public String asignarMisionADonador(
            @McpToolParam(description = "Id de la misión", required = true) String misionId,
            @McpToolParam(description = "Id del donador", required = true) String donadorId) {
        return d.accion(S, "/misiones/{mision}/donadores/{donador}", misionId, donadorId);
    }
}
