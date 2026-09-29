package wsp.f1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModosAccesoTest {

    private static final int ITERACIONES = 500_000;

    /** Prohibido por el JMM con volatile. */
    @Test
    void volatileProhibeCerosMutuos() throws InterruptedException {
        assertEquals(0, ModosAcceso.cerosMutuos(ModosAcceso.Modo.VOLATILE, ITERACIONES));
    }

    /** Release/acquire + valla completa equivale a orden total: tampoco puede salir. */
    @Test
    void releaseAcquireConVallaProhibeCerosMutuos() throws InterruptedException {
        assertEquals(0, ModosAcceso.cerosMutuos(ModosAcceso.Modo.RELEASE_ACQUIRE_Y_VALLA, ITERACIONES));
    }
    // De PLANO, OPACO y RELEASE_ACQUIRE no se afirma nada: (0,0) está permitido, no obligado.
}
