package wsp.f3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Un solo hilo, operaciones al azar, comparadas con un {@link ArrayDeque} de referencia:
 * {@code push} = {@code addLast}, {@code pop} = {@code pollLast}, {@code steal} = {@code pollFirst}.
 * Atrapa combinaciones de crecer, vaciar y dar vueltas que los casos escritos a mano no prevén.
 */
class DequeChaseLevModeloTest {

    private static void comparar(int capacidad, long semilla, int pasos, int pesoPush) {
        Random azar = new Random(semilla);
        DequeChaseLev<Integer> d = new DequeChaseLev<>(capacidad);
        ArrayDeque<Integer> modelo = new ArrayDeque<>();
        int siguiente = 0;
        for (int i = 0; i < pasos; i++) {
            int op = azar.nextInt(10);
            if (op < pesoPush) {
                d.push(siguiente);
                modelo.addLast(siguiente);
                siguiente++;
            } else if (op < pesoPush + (10 - pesoPush) / 2) {
                assertEquals(modelo.pollLast(), d.pop(), "pop, paso " + i + ", semilla " + semilla);
            } else {
                assertEquals(modelo.pollFirst(), d.steal(), "steal, paso " + i + ", semilla " + semilla);
            }
        }
        // Lo que quede debe salir igual por los dos extremos.
        while (!modelo.isEmpty()) {
            assertEquals(modelo.pollFirst(), d.steal());
            if (!modelo.isEmpty()) {
                assertEquals(modelo.pollLast(), d.pop());
            }
        }
        assertEquals(null, d.pop());
        assertEquals(null, d.steal());
    }

    @Test
    void azarEquilibradoCapacidadMinima() {
        for (long semilla = 0; semilla < 50; semilla++) {
            comparar(2, semilla, 5_000, 4);
        }
    }

    @Test
    void azarConMuchosPushHaceCrecerVariasVeces() {
        for (long semilla = 0; semilla < 20; semilla++) {
            comparar(2, semilla, 5_000, 7);
        }
    }

    @Test
    void azarConMuchosRetirosMantieneElDequeCasiVacio() {
        for (long semilla = 0; semilla < 50; semilla++) {
            comparar(4, semilla, 5_000, 3);
        }
    }

    @Test
    void azarConCapacidadGrande() {
        for (long semilla = 0; semilla < 20; semilla++) {
            comparar(1024, semilla, 5_000, 5);
        }
    }
}
