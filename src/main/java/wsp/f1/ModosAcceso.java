package wsp.f1;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * F1, ejercicio 6: qué ordena cada modo de acceso de {@link VarHandle}.
 *
 * Se aplica cada modo al ejemplo de {@link CerosMutuos} (escribir una variable y leer
 * <em>otra</em>) y se cuenta cuántas veces sale {@code (0,0)}:
 *
 *  - {@link Modo#PLANO}: acceso normal. Sin garantías.
 *  - {@link Modo#OPACO}: {@code setOpaque}/{@code getOpaque}. Solo garantiza que el acceso
 *    ocurre de verdad (el JIT no lo elimina ni lo saca de un bucle) y coherencia sobre
 *    <em>una</em> variable. No ordena nada respecto a otras variables.
 *  - {@link Modo#RELEASE_ACQUIRE}: {@code setRelease}/{@code getAcquire}. Lo escrito antes de
 *    un release es visible para quien haga el acquire que lo lee. Pero no prohíbe que una
 *    lectura posterior se adelante a una escritura anterior de otra variable
 *    (store→load), que es justo lo que hace falta para el {@code (0,0)}.
 *  - {@link Modo#VOLATILE}: orden total; prohíbe el {@code (0,0)}.
 *  - {@link Modo#RELEASE_ACQUIRE_Y_VALLA}: release/acquire más
 *    {@link VarHandle#fullFence()} entre la escritura y la lectura. Recupera el orden total
 *    a mano: es lo que {@code volatile} hace por dentro.
 */
public final class ModosAcceso {

    private ModosAcceso() {}

    public enum Modo { PLANO, OPACO, RELEASE_ACQUIRE, VOLATILE, RELEASE_ACQUIRE_Y_VALLA }

    private static final VarHandle ELEM = MethodHandles.arrayElementVarHandle(int[].class);

    private static void escribir(Modo m, int[] a, int i) {
        switch (m) {
            case PLANO -> a[i] = 1;
            case OPACO -> ELEM.setOpaque(a, i, 1);
            case RELEASE_ACQUIRE, RELEASE_ACQUIRE_Y_VALLA -> ELEM.setRelease(a, i, 1);
            case VOLATILE -> ELEM.setVolatile(a, i, 1);
        }
    }

    private static int leer(Modo m, int[] a, int i) {
        return switch (m) {
            case PLANO -> a[i];
            case OPACO -> (int) ELEM.getOpaque(a, i);
            case RELEASE_ACQUIRE, RELEASE_ACQUIRE_Y_VALLA -> (int) ELEM.getAcquire(a, i);
            case VOLATILE -> (int) ELEM.getVolatile(a, i);
        };
    }

    /** Cuántas de {@code iteraciones} terminaron con r1 == 0 y r2 == 0. */
    public static int cerosMutuos(Modo m, int iteraciones) throws InterruptedException {
        int[] x = new int[iteraciones];
        int[] y = new int[iteraciones];
        int[] r1 = new int[iteraciones];
        int[] r2 = new int[iteraciones];
        AtomicInteger llegadas = new AtomicInteger();
        boolean valla = m == Modo.RELEASE_ACQUIRE_Y_VALLA;

        Thread a = new Thread(() -> {
            for (int i = 0; i < iteraciones; i++) {
                barrera(llegadas, i);
                escribir(m, x, i);
                if (valla) VarHandle.fullFence();
                r1[i] = leer(m, y, i);
            }
        });
        Thread b = new Thread(() -> {
            for (int i = 0; i < iteraciones; i++) {
                barrera(llegadas, i);
                escribir(m, y, i);
                if (valla) VarHandle.fullFence();
                r2[i] = leer(m, x, i);
            }
        });
        a.start();
        b.start();
        a.join();
        b.join();

        int ceros = 0;
        for (int i = 0; i < iteraciones; i++) {
            if (r1[i] == 0 && r2[i] == 0) ceros++;
        }
        return ceros;
    }

    private static void barrera(AtomicInteger llegadas, int i) {
        int objetivo = 2 * (i + 1);
        llegadas.incrementAndGet();
        while (llegadas.get() < objetivo) {
            Thread.onSpinWait();
        }
    }

    /** Demo: {@code java -cp target/classes wsp.f1.ModosAcceso [iteraciones] [repeticiones]}. */
    public static void main(String[] args) throws InterruptedException {
        int iteraciones = args.length > 0 ? Integer.parseInt(args[0]) : 2_000_000;
        int repeticiones = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        for (Modo m : Modo.values()) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < repeticiones; k++) {
                sb.append(' ').append(cerosMutuos(m, iteraciones));
            }
            System.out.printf("%-24s (0,0) en %d:%s%n", m, iteraciones, sb);
        }
    }
}
