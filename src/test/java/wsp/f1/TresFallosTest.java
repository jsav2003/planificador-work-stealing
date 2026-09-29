package wsp.f1;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TresFallosTest {

    /** Con volatile la bandera siempre se ve: garantizado por el JMM. */
    @Test
    void volatileVeLaBandera() throws InterruptedException {
        assertTrue(TresFallos.visibilidad(true, 50, 5000));
    }

    /**
     * Del total no se afirma que se pierdan incrementos (no determinista), solo que nunca
     * sobran. La variante normal de visibilidad no se prueba: dejaría un hilo girando.
     */
    @Test
    void atomicidadNuncaSuperaElEsperado() throws InterruptedException {
        int veces = 200_000;
        assertTrue(TresFallos.atomicidad(true, veces) <= 4 * veces);
        assertTrue(TresFallos.atomicidad(false, veces) <= 4 * veces);
    }
}
