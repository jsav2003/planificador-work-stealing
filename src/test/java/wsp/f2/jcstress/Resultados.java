package wsp.f2.jcstress;

/** Convención de los tests de jcstress de la cola: un {@code desencolar} que da {@code null} se anota como 0. */
final class Resultados {

    private Resultados() {}

    static int comoInt(Integer v) {
        return v == null ? 0 : v;
    }
}
