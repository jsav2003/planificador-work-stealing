package wsp.f1;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * F1, ejercicios 1 y 2: el ejemplo de DESIGN §3.1.
 *
 * <pre>
 *   // hilo A          // hilo B
 *   x = 1;             y = 1;
 *   r1 = y;            r2 = x;
 * </pre>
 *
 * Con {@code x} e {@code y} en cero, ¿puede acabar con {@code r1 == 0 && r2 == 0}?
 *
 *  - {@link Variante#NORMAL}: campos normales. Sí puede (buffer de escrituras del
 *    procesador y reordenamiento del JIT), y en hardware real ocurre.
 *  - {@link Variante#VOLATILE}: {@code x} e {@code y} volatile. No puede: los accesos
 *    volatile se comportan como si hubiera un único orden global.
 *
 * Para repetir el experimento millones de veces sin crear hilos cada vez, cada iteración
 * usa su propia posición de los arreglos ({@code x[i]}, {@code y[i]}) y los dos hilos se
 * alinean con una barrera de espera activa justo antes de competir.
 */
public final class CerosMutuos {

    private CerosMutuos() {}

    public enum Variante { NORMAL, VOLATILE }

    /** Conteo de los cuatro resultados posibles de (r1, r2). */
    public record Conteo(int iteraciones, int cero_cero, int cero_uno, int uno_cero, int uno_uno) {}

    private static final VarHandle ELEM = MethodHandles.arrayElementVarHandle(int[].class);

    public static Conteo probar(Variante v, int iteraciones) throws InterruptedException {
        int[] x = new int[iteraciones];
        int[] y = new int[iteraciones];
        int[] r1 = new int[iteraciones];
        int[] r2 = new int[iteraciones];
        AtomicInteger llegadas = new AtomicInteger();

        Thread a = new Thread(() -> {
            for (int i = 0; i < iteraciones; i++) {
                barrera(llegadas, i);
                if (v == Variante.VOLATILE) {
                    ELEM.setVolatile(x, i, 1);
                    r1[i] = (int) ELEM.getVolatile(y, i);
                } else {
                    x[i] = 1;
                    r1[i] = y[i];
                }
            }
        });
        Thread b = new Thread(() -> {
            for (int i = 0; i < iteraciones; i++) {
                barrera(llegadas, i);
                if (v == Variante.VOLATILE) {
                    ELEM.setVolatile(y, i, 1);
                    r2[i] = (int) ELEM.getVolatile(x, i);
                } else {
                    y[i] = 1;
                    r2[i] = x[i];
                }
            }
        });
        a.start();
        b.start();
        a.join();
        b.join();

        // Tras join(): happens-before, se pueden leer r1 y r2 sin más.
        int[] c = new int[4];
        for (int i = 0; i < iteraciones; i++) {
            c[r1[i] * 2 + r2[i]]++;
        }
        return new Conteo(iteraciones, c[0], c[1], c[2], c[3]);
    }

    /** Espera activa hasta que los dos hilos han llegado a la iteración {@code i}. */
    private static void barrera(AtomicInteger llegadas, int i) {
        int objetivo = 2 * (i + 1);
        llegadas.incrementAndGet();
        while (llegadas.get() < objetivo) {
            Thread.onSpinWait();
        }
    }

    /** Demo: {@code java -cp target/classes wsp.f1.CerosMutuos [iteraciones] [repeticiones]}. */
    public static void main(String[] args) throws InterruptedException {
        int iteraciones = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
        int repeticiones = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        for (Variante v : Variante.values()) {
            for (int k = 0; k < repeticiones; k++) {
                Conteo c = probar(v, iteraciones);
                System.out.printf("%-8s  (0,0)=%d  (0,1)=%d  (1,0)=%d  (1,1)=%d  de %d%n",
                        v, c.cero_cero(), c.cero_uno(), c.uno_cero(), c.uno_uno(), c.iteraciones());
            }
        }
    }
}
