package wsp.f3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.junit.jupiter.api.Test;

/**
 * Ejercicio 5: un dueño y N ladrones. Cada tarea es un entero distinto y debe salir
 * exactamente una vez, por {@code pop} o por {@code steal}. Los tests corren en x86
 * (README): atrapan errores de lógica, no prueban los modos de acceso. La variante
 * {@code mvn test -Pxint} los corre con el intérprete puro (DESIGN §7.4).
 *
 * <p>La última tarea repetida con un ladrón está en {@code DequeChaseLevTest}.
 */
class DequeChaseLevConcurrenteTest {

    private static final int TAREAS = 100_000;

    /** Una prueba: capacidad inicial, ladrones y cuántas tareas empuja el dueño seguidas. */
    private static void correr(int capacidad, int ladrones, int rafaga, int popsPorRafaga)
            throws Exception {
        DequeChaseLev<Integer> deque = new DequeChaseLev<>(capacidad);
        AtomicIntegerArray salidas = new AtomicIntegerArray(TAREAS);
        AtomicBoolean terminado = new AtomicBoolean();
        CountDownLatch salida = new CountDownLatch(1);
        List<Thread> hilos = new ArrayList<>();
        List<Throwable> errores = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < ladrones; i++) {
            Thread t = new Thread(() -> {
                try {
                    salida.await();
                    // seguir robando hasta que el dueño termine y no quede nada
                    while (true) {
                        Integer tarea = deque.steal();
                        if (tarea != null) {
                            salidas.incrementAndGet(tarea);
                        } else if (terminado.get()) {
                            return;
                        } else {
                            Thread.onSpinWait();
                        }
                    }
                } catch (Throwable e) {
                    errores.add(e);
                }
            });
            t.start();
            hilos.add(t);
        }

        salida.countDown();
        int siguiente = 0;
        while (siguiente < TAREAS) {
            int hasta = Math.min(TAREAS, siguiente + rafaga);
            while (siguiente < hasta) {
                deque.push(siguiente++);
            }
            for (int i = 0; i < popsPorRafaga; i++) {
                Integer propia = deque.pop();
                if (propia == null) {
                    break;
                }
                salidas.incrementAndGet(propia);
            }
        }
        Integer propia;
        while ((propia = deque.pop()) != null) {
            salidas.incrementAndGet(propia);
        }
        terminado.set(true);
        for (Thread t : hilos) {
            t.join();
        }

        assertEquals(List.of(), errores);
        int perdidas = 0;
        int repetidas = 0;
        for (int i = 0; i < TAREAS; i++) {
            int veces = salidas.get(i);
            if (veces == 0) {
                perdidas++;
            } else if (veces > 1) {
                repetidas++;
            }
        }
        assertEquals(0, perdidas, "tareas que no salieron nunca");
        assertEquals(0, repetidas, "tareas que salieron más de una vez");
        assertNull(deque.pop());
        assertNull(deque.steal());
    }

    @Test
    void unLadronCadaTareaSaleUnaVez() throws Exception {
        correr(1024, 1, 100, 50);
    }

    @Test
    void variosLadronesCadaTareaSaleUnaVez() throws Exception {
        correr(1024, 4, 100, 50);
    }

    @Test
    void ladronesContraDuenoQueSoloEmpuja() throws Exception {
        // el dueño no hace pop durante la carga: los ladrones se llevan casi todo
        correr(1024, 4, 1000, 0);
    }

    @Test
    void crecimientoConLadronesActivos() throws Exception {
        // capacidad 2 y ráfagas de 500: el arreglo crece varias veces con ladrones robando
        correr(2, 4, 500, 100);
    }

    @Test
    void crecimientoConLadronesYRafagasCortas() throws Exception {
        // ráfagas de 3 con capacidad 2: crece apenas el deque se llena, otra vez y otra
        correr(2, 3, 3, 1);
    }

    @Test
    void variasRepeticionesPequenasDeLaMismaCarrera() throws Exception {
        for (int i = 0; i < 20; i++) {
            correr(2, 2, 7, 3);
        }
    }
}
