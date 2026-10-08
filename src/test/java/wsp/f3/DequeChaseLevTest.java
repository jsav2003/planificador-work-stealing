package wsp.f3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
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
    void llenoCreceYConservaElOrden() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(2);
        for (int i = 0; i < 1000; i++) {
            d.push(i);
        }
        for (int i = 0; i < 500; i++) {
            assertEquals(i, d.steal());
        }
        for (int i = 999; i >= 500; i--) {
            assertEquals(i, d.pop());
        }
        assertNull(d.pop());
        assertNull(d.steal());
    }

    @Test
    void creceConElRangoDesplazadoYDandoVueltas() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(4);
        int siguiente = 0;
        int esperado = 0;
        // top y bottom avanzan muchas vueltas antes de que llegue el crecimiento
        for (int i = 0; i < 50; i++) {
            d.push(siguiente++);
            d.push(siguiente++);
            assertEquals(esperado++, d.steal());
            assertEquals(esperado++, d.steal());
        }
        d.push(siguiente++);
        for (int i = 0; i < 20; i++) { // obliga a crecer varias veces con top > 0
            d.push(siguiente++);
        }
        while (esperado < siguiente) {
            assertEquals(esperado++, d.steal());
        }
        assertNull(d.steal());
    }

    @Test
    void despuesDeCrecerSigueFuncionandoPopYPush() {
        DequeChaseLev<Integer> d = new DequeChaseLev<>(2);
        d.push(1);
        d.push(2);
        d.push(3); // crece
        assertEquals(3, d.pop());
        d.push(4);
        assertEquals(1, d.steal());
        assertEquals(4, d.pop());
        assertEquals(2, d.pop());
        assertNull(d.pop());
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

    /**
     * Ejercicio 3b: la última tarea, repetida. Cada ronda tiene una sola tarea; el dueño hace
     * {@code pop} y un ladrón hace {@code steal} a la vez. Debe salir exactamente una vez.
     * En x86 un resultado correcto no prueba los modos de acceso (README); sí atrapa la lógica.
     */
    @Test
    void laUltimaTareaSaleExactamenteUnaVez() throws Exception {
        final int rondas = 20_000;
        DequeChaseLev<Integer> d = new DequeChaseLev<>(2);
        AtomicInteger robadas = new AtomicInteger();
        AtomicInteger propias = new AtomicInteger();
        AtomicInteger ronda = new AtomicInteger(-1); // la ronda que el ladrón puede atacar
        AtomicInteger atendida = new AtomicInteger(-1); // última ronda que el ladrón terminó
        Thread ladron = new Thread(() -> {
            for (int r = 0; r < rondas; r++) {
                while (ronda.get() < r) {
                    Thread.onSpinWait();
                }
                if (d.steal() != null) {
                    robadas.incrementAndGet();
                }
                atendida.set(r);
            }
        });
        ladron.start();
        for (int r = 0; r < rondas; r++) {
            d.push(r);
            ronda.set(r);
            if (d.pop() != null) {
                propias.incrementAndGet();
            }
            while (atendida.get() < r) {
                Thread.onSpinWait();
            }
            assertNull(d.pop(), "el deque debe quedar vacío tras cada ronda");
            assertNull(d.steal());
        }
        ladron.join();
        assertEquals(rondas, robadas.get() + propias.get());
    }
}
