package wsp.f4.jcstress;

import wsp.f3.DequeChaseLev;

/** Convenciones de los escenarios del deque: un {@code pop}/{@code steal} que da {@code null} se anota como 0. */
final class Resultados {

    private Resultados() {}

    static int comoInt(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * Roba hasta vaciar y devuelve la suma de lo robado. Como las tareas valen 1, 2, 4 y 8, la suma
     * identifica el conjunto que quedó. Solo para el árbitro: en ese momento no hay otros hilos,
     * así que un {@code steal} que da {@code null} es que el deque está vacío.
     */
    static int drenar(DequeChaseLev<Integer> deque) {
        int suma = 0;
        Integer v;
        while ((v = deque.steal()) != null) {
            suma += v;
        }
        return suma;
    }
}
