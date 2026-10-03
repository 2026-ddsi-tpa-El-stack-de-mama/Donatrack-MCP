package org.example.donatrackmcp.downstream;

/**
 * Error de un módulo downstream, con mensaje apto para que el modelo se lo explique al usuario.
 * Se propaga tal cual desde la tool: el MCP no reinterpreta ni duplica reglas de negocio.
 */
public class DownstreamException extends RuntimeException {

    private final Servicio servicio;
    private final Integer status;

    public DownstreamException(Servicio servicio, Integer status, String message, Throwable cause) {
        super(message, cause);
        this.servicio = servicio;
        this.status = status;
    }

    public Servicio servicio() {
        return servicio;
    }

    public Integer status() {
        return status;
    }
}
