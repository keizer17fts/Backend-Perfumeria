package cl.luxury.usuarios.util;

/** Misma lógica que src/utils/validaciones.js del front (módulo 11 chileno). */
public final class RunUtil {

    private RunUtil() {
    }

    /** "19.011.022-2" -> "190110222" (sin puntos ni guion, mayúsculas). */
    public static String limpiar(String run) {
        if (run == null) {
            return "";
        }
        return run.trim().toUpperCase().replace(".", "").replace("-", "");
    }

    public static boolean esValido(String run) {
        String limpio = limpiar(run);
        if (!limpio.matches("[0-9]{7,8}[0-9K]")) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        String digito = limpio.substring(limpio.length() - 1);
        return calcularDigito(cuerpo).equals(digito);
    }

    static String calcularDigito(String cuerpo) {
        int suma = 0;
        int multiplo = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplo;
            multiplo = multiplo == 7 ? 2 : multiplo + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return "0";
        }
        if (resto == 10) {
            return "K";
        }
        return String.valueOf(resto);
    }
}
