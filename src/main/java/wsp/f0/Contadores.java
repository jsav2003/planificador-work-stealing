package wsp.f0;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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
     * Ejercicio 2: el mismo contador con {@link VarHandle} sobre un campo {@code int}
     * normal (sin envoltorio atómico). {@code getAndAdd} es una sola operación atómica
     * con semántica volatile, igual que {@code incrementAndGet}. {@code AtomicInteger}
     * es en el fondo esto mismo; el VarHandle es la pieza que usará el deque (F3).
     */
    public static final class ConVarHandle {
        private static final VarHandle VALOR;

        static {
            try {
                VALOR = MethodHandles.lookup().findVarHandle(ConVarHandle.class, "valor", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unused") // se accede solo a través de VALOR
        private int valor;

        public void incrementar() {
            VALOR.getAndAdd(this, 1);
        }

        public int valor() {
            return (int) VALOR.getVolatile(this);
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
        ConVarHandle conVarHandle = new ConVarHandle();
        correr(hilos, veces, inseguro::incrementar);
        correr(hilos, veces, atomico::incrementar);
        correr(hilos, veces, conVarHandle::incrementar);
        System.out.println("esperado : " + hilos * veces);
        System.out.println("inseguro : " + inseguro.valor());
        System.out.println("atómico  : " + atomico.valor());
        System.out.println("varhandle: " + conVarHandle.valor());
    }
}
