package wsp.f0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContadoresTest {

    private static final int HILOS = 4;
    private static final int VECES = 1_000_000;

    @Test
    void atomicoNoPierdeActualizaciones() throws InterruptedException {
        Contadores.Atomico c = new Contadores.Atomico();
        Contadores.correr(HILOS, VECES, c::incrementar);
        assertEquals(HILOS * VECES, c.valor());
    }

    @Test
    void varHandleNoPierdeActualizaciones() throws InterruptedException {
        Contadores.ConVarHandle c = new Contadores.ConVarHandle();
        Contadores.correr(HILOS, VECES, c::incrementar);
        assertEquals(HILOS * VECES, c.valor());
    }

    /** No se puede afirmar que pierda: la carrera es no determinista. Solo que nunca sobra. */
    @Test
    void inseguroNuncaSuperaLoEsperado() throws InterruptedException {
        Contadores.Inseguro c = new Contadores.Inseguro();
        Contadores.correr(HILOS, VECES, c::incrementar);
        assertTrue(c.valor() <= HILOS * VECES);
    }
}
