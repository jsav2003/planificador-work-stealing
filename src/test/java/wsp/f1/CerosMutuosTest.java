package wsp.f1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CerosMutuosTest {

    private static final int ITERACIONES = 500_000;

    /** Con volatile el resultado (0,0) está prohibido por el JMM: aquí sí se puede afirmar. */
    @Test
    void volatileNuncaDaCerosMutuos() throws InterruptedException {
        CerosMutuos.Conteo c = CerosMutuos.probar(CerosMutuos.Variante.VOLATILE, ITERACIONES);
        assertEquals(0, c.cero_cero());
        assertEquals(ITERACIONES, c.cero_cero() + c.cero_uno() + c.uno_cero() + c.uno_uno());
    }

    /** De NORMAL no se afirma nada sobre (0,0): puede o no salir. Solo se comprueba que cuenta bien. */
    @Test
    void normalCuentaTodasLasIteraciones() throws InterruptedException {
        CerosMutuos.Conteo c = CerosMutuos.probar(CerosMutuos.Variante.NORMAL, ITERACIONES);
        assertEquals(ITERACIONES, c.cero_cero() + c.cero_uno() + c.uno_cero() + c.uno_uno());
    }
}
