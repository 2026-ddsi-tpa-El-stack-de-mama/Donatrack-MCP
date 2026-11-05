package org.example.donatrackmcp.tooling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Helpers de armado de argumentos. Sin lógica de dominio. */
final class Args {

    private Args() {
    }

    static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    /** Arma un body JSON a partir de pares clave/valor, omitiendo los valores nulos. */
    static Map<String, Object> body(Object... clavesYValores) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < clavesYValores.length; i += 2) {
            if (clavesYValores[i + 1] != null) {
                m.put((String) clavesYValores[i], clavesYValores[i + 1]);
            }
        }
        return m;
    }

    /**
     * Agrega al path un placeholder por cada parámetro presente (la URI los codifica).
     * Los valores se pasan como uriVars, en el mismo orden: {@code params.values().toArray()}.
     */
    static String query(String path, Map<String, Object> params) {
        if (params.isEmpty()) {
            return path;
        }
        return path + params.keySet().stream().map(k -> k + "={" + k + "}").collect(Collectors.joining("&", "?", ""));
    }
}
