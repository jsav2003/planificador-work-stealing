package wsp.f0;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * F0, ejercicio 1: la pérdida de actualizaciones.
 *
 * {@code valor++} son tres pasos (leer, sumar, escribir). Dos hilos pueden leer el mismo
 * valor y ambos escribir valor+1: una suma desaparece. No hay {@code happens-before}
 * entre los hilos, así que además es una carrera de datos según el JMM (DESIGN §3.2).
 */
public final class Contadores {

    private Contadores() {}

    /** Contador sin sincronización. Incorrecto a propósito. */
    public static final class Inseguro {
        private int valor;

        public void incrementar() {
            valor++;
        }

        public int valor() {
            return valor;
        }
    }

    /** Mismo contador con CAS: {@code incrementAndGet} es una sola operación atómica. */
    public static final class Atomico {
        private final AtomicInteger valor = new AtomicInteger();

        public void incrementar() {
            valor.incrementAndGet();
        }

        public int valor() {
            return valor.get();
        }
    }

    /**
     * Lanza {@code hilos} hilos que llaman {@code accion} {@code vecesPorHilo} veces y
     * espera a todos. El {@code join} es lo que da happens-before para leer el resultado
     * después (DESIGN §3.2, fila Thread.join).
     */
    public static void correr(int hilos, int vecesPorHilo, Runnable accion) throws InterruptedException {
        Thread[] ts = new Thread[hilos];
        for (int i = 0; i < hilos; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < vecesPorHilo; j++) {
                    accion.run();
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        for (Thread t : ts) {
            t.join();
        }
    }

    /** Demo: {@code java -cp target/classes wsp.f0.Contadores} tras {@code mvn compile}. */
    public static void main(String[] args) throws InterruptedException {
        int hilos = 4;
        int veces = 1_000_000;
        Inseguro inseguro = new Inseguro();
        Atomico atomico = new Atomico();
        correr(hilos, veces, inseguro::incrementar);
        correr(hilos, veces, atomico::incrementar);
        System.out.println("esperado : " + hilos * veces);
        System.out.println("inseguro : " + inseguro.valor());
        System.out.println("atómico  : " + atomico.valor());
    }
}
