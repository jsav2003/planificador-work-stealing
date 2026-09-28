package wsp.f0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PublicacionTest {

    private static final int PRUEBAS = 5_000;

    private static void todasCorrectas(Publicacion.Variante v) throws InterruptedException {
        for (int i = 0; i < PRUEBAS; i++) {
            assertEquals(Publicacion.Resultado.CORRECTO, Publicacion.probar(v), v + " en la prueba " + i);
        }
    }

    @Test
    void volatilePublicaBien() throws InterruptedException {
        todasCorrectas(Publicacion.Variante.VOLATILE);
    }

    @Test
    void releaseAcquirePublicaBien() throws InterruptedException {
        todasCorrectas(Publicacion.Variante.RELEASE_ACQUIRE);
    }

    /**
     * De NORMAL no se afirma nada: la carrera es no determinista y puede salir CORRECTO por
     * suerte (sobre todo en x86). Solo se comprueba que termina, gracias a la espera acotada.
     * Demostrar el fallo de verdad es trabajo de jcstress (F2 en adelante).
     */
    @Test
    void normalTermina() throws InterruptedException {
        Publicacion.probar(Publicacion.Variante.NORMAL);
    }
}
