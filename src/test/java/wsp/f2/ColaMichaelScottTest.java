package wsp.f2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.junit.jupiter.api.Test;

class ColaMichaelScottTest {

    @Test
    void vaciaDevuelveNull() {
        ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();
        assertTrue(cola.estaVacia());
        assertNull(cola.desencolar());
    }

    @Test
    void respetaOrdenFifo() {
        ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();
        for (int i = 0; i < 1000; i++) {
            cola.encolar(i);
        }
        assertFalse(cola.estaVacia());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, cola.desencolar());
        }
        assertNull(cola.desencolar());
        assertTrue(cola.estaVacia());
    }

    @Test
    void intercalarEncolarYDesencolar() {
        ColaMichaelScott<String> cola = new ColaMichaelScott<>();
        cola.encolar("a");
        assertEquals("a", cola.desencolar());
        assertNull(cola.desencolar());
        cola.encolar("b");
        cola.encolar("c");
        assertEquals("b", cola.desencolar());
        cola.encolar("d");
        assertEquals("c", cola.desencolar());
        assertEquals("d", cola.desencolar());
        assertNull(cola.desencolar());
    }

    @Test
    void rechazaNull() {
        assertThrows(NullPointerException.class, () -> new ColaMichaelScott<String>().encolar(null));
    }

    /**
     * Varios productores y varios consumidores a la vez. Cada elemento se codifica como
     * (productor, secuencia) y debe salir exactamente una vez: ni perdido ni duplicado.
     * Además, lo de un mismo productor debe salir en el orden en que lo encoló (FIFO por
     * productor). Un test normal no demuestra ausencia de carreras (F2 usa jcstress para eso),
     * pero sí caza roturas gruesas.
     */
    @Test
    void cadaElementoSaleUnaVezYEnOrdenPorProductor() throws InterruptedException {
        final int productores = 4;
        final int consumidores = 4;
        final int porProductor = 200_000;
        final int total = productores * porProductor;

        ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();
        AtomicIntegerArray vistos = new AtomicIntegerArray(total);
        // por consumidor y productor, el último número de secuencia visto
        int[][] ultimo = new int[consumidores][productores];
        for (int[] fila : ultimo) {
            java.util.Arrays.fill(fila, -1);
        }
        boolean[] fueraDeOrden = new boolean[1];
        CountDownLatch salida = new CountDownLatch(total);
        CountDownLatch arranque = new CountDownLatch(1);

        List<Thread> hilos = new ArrayList<>();
        for (int p = 0; p < productores; p++) {
            final int id = p;
            hilos.add(new Thread(() -> {
                esperar(arranque);
                for (int s = 0; s < porProductor; s++) {
                    cola.encolar(id * porProductor + s);
                }
            }));
        }
        for (int c = 0; c < consumidores; c++) {
            final int id = c;
            Thread t = new Thread(() -> {
                esperar(arranque);
                while (salida.getCount() > 0) {
                    Integer v = cola.desencolar();
                    if (v == null) {
                        Thread.onSpinWait();
                        continue;
                    }
                    vistos.incrementAndGet(v);
                    int prod = v / porProductor;
                    int seq = v % porProductor;
                    if (seq <= ultimo[id][prod]) {
                        fueraDeOrden[0] = true;
                    }
                    ultimo[id][prod] = seq;
                    salida.countDown();
                }
            });
            hilos.add(t);
        }
        hilos.forEach(Thread::start);
        arranque.countDown();
        for (Thread t : hilos) {
            t.join();
        }

        for (int i = 0; i < total; i++) {
            assertEquals(1, vistos.get(i), "elemento " + i + " visto " + vistos.get(i) + " veces");
        }
        assertFalse(fueraDeOrden[0], "un consumidor vio a un productor fuera de orden");
        assertNull(cola.desencolar());
    }

    /** Referencia: mismo resultado de conteo que la cola de la JDK sobre una secuencia simple. */
    @Test
    void coincideConConcurrentLinkedQueueEnSecuencial() {
        ColaMichaelScott<Integer> mia = new ColaMichaelScott<>();
        ConcurrentLinkedQueue<Integer> ref = new ConcurrentLinkedQueue<>();
        java.util.Random azar = new java.util.Random(42);
        for (int i = 0; i < 100_000; i++) {
            if (azar.nextBoolean()) {
                mia.encolar(i);
                ref.add(i);
            } else {
                assertEquals(ref.poll(), mia.desencolar());
            }
        }
    }

    private static void esperar(CountDownLatch l) {
        try {
            l.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
