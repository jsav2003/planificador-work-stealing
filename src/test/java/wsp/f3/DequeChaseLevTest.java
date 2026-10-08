package wsp.f3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Pruebas secuenciales del ejercicio 2: un solo hilo, sin ladrones a la vez. */
class DequeChaseLevTest {

    @Test
    void vacioDevuelveNull() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(4);
        assertNull(d.pop());
        assertNull(d.steal());
    }

    @Test
    void popEsLifo() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(8);
        for (int i = 0; i < 5; i++) {
            d.push(i);
        }
        for (int i = 4; i >= 0; i--) {
            assertEquals(i, d.pop());
        }
        assertNull(d.pop());
    }

    @Test
    void stealEsFifo() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(8);
        for (int i = 0; i < 5; i++) {
            d.push(i);
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(i, d.steal());
        }
        assertNull(d.steal());
    }

    @Test
    void popYStealPorExtremosOpuestos() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(8);
        for (int i = 0; i < 4; i++) {
            d.push(i);
        }
        assertEquals(0, d.steal());
        assertEquals(3, d.pop());
        assertEquals(1, d.steal());
        assertEquals(2, d.pop());
        assertNull(d.pop());
        assertNull(d.steal());
    }

    @Test
    void llenoLanzaExcepcion() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(2);
        d.push(1);
        d.push(2);
        assertThrows(IllegalStateException.class, () -> d.push(3));
        assertEquals(2, d.pop()); // sigue usable
        d.push(3);
        assertEquals(3, d.pop());
    }

    @Test
    void liberarCasillaPorStealPermiteVolverAEmpujar() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(2);
        d.push(1);
        d.push(2);
        assertEquals(1, d.steal());
        d.push(3);
        assertEquals(2, d.steal());
        assertEquals(3, d.steal());
    }

    @Test
    void elArregloDaVariasVueltas() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(4);
        int siguiente = 0;
        int esperado = 0;
        for (int ronda = 0; ronda < 100; ronda++) {
            d.push(siguiente++);
            d.push(siguiente++);
            d.push(siguiente++);
            assertEquals(esperado++, d.steal());
            assertEquals(esperado++, d.steal());
            assertEquals(esperado++, d.steal());
        }
        assertNull(d.steal());
    }

    @Test
    void popVacioSeguidoDePushYStealNoDejaBottomDebajoDeTop() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(4);
        assertNull(d.pop());
        assertNull(d.pop());
        d.push(7);
        assertEquals(7, d.steal());
        assertNull(d.pop());
        d.push(8);
        assertEquals(8, d.pop());
        assertNull(d.steal());
    }

    @Test
    void popDeLaUltimaTareaYSiguienteOperacion() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(4);
        d.push(1);
        assertEquals(1, d.pop());
        assertNull(d.pop());
        assertNull(d.steal());
        d.push(2);
        assertEquals(2, d.steal());
    }

    @Test
    void capacidadInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new DequeChaseLev<Integer>(3));
        assertThrows(IllegalArgumentException.class, () -> new DequeChaseLev<Integer>(1));
    }

    @Test
    void nullNoSeAdmite() {
        assertThrows(NullPointerException.class, () -> new DequeChaseLev<Integer>(2).push(null));
    }
}
