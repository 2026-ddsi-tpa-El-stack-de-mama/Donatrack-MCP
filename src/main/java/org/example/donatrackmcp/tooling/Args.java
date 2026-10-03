package org.example.donatrackmcp.tooling;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
