package wsp.f0;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * F0, ejercicio 5: la misma suma con {@link ForkJoinPool} y un {@link RecursiveTask}.
 *
 * En vez de partir en un trozo por hilo, la tarea se parte a la mitad de forma recursiva
 * hasta llegar a un umbral, y ahí suma en secuencial. {@code fork()} deja una mitad en el
 * deque del hilo actual (donde otros hilos pueden robarla) y {@code compute()} sigue con
 * la otra. Este es el work-stealing que el proyecto reimplementa; esta versión será la
 * referencia de comparación en la F5 (DESIGN §7.3).
 */
public final class SumaForkJoin {

    /** Por debajo de este tamaño partir cuesta más que sumar. */
    static final int UMBRAL = 100_000;

    private SumaForkJoin() {}

    static final class Suma extends RecursiveTask<Long> {
        private final int[] datos;
        private final int desde;
        private final int hasta;

        Suma(int[] datos, int desde, int hasta) {
            this.datos = datos;
            this.desde = desde;
            this.hasta = hasta;
        }

        @Override
        protected Long compute() {
            if (hasta - desde <= UMBRAL) {
                long total = 0;
                for (int i = desde; i < hasta; i++) {
                    total += datos[i];
                }
                return total;
            }
            int medio = (desde + hasta) >>> 1;
            Suma izquierda = new Suma(datos, desde, medio);
            Suma derecha = new Suma(datos, medio, hasta);
            izquierda.fork();
            long r = derecha.compute();
            return izquierda.join() + r;
        }
    }

    public static long suma(int[] datos, ForkJoinPool pool) {
        return pool.invoke(new Suma(datos, 0, datos.length));
    }

    /** Demo: {@code java -cp target/classes wsp.f0.SumaForkJoin} tras {@code mvn compile}. */
    public static void main(String[] args) throws Exception {
        int n = 100_000_000;
        int hilos = Runtime.getRuntime().availableProcessors();
        int[] datos = new int[n];
        for (int i = 0; i < n; i++) {
            datos[i] = i % 1000;
        }
        ForkJoinPool pool = new ForkJoinPool(hilos);
        try {
            SumaArreglo.secuencial(datos);
            SumaArreglo.conPool(datos, hilos);
            suma(datos, pool);
            long t0 = System.nanoTime();
            long s = SumaArreglo.secuencial(datos);
            long t1 = System.nanoTime();
            long e = SumaArreglo.conPool(datos, hilos);
            long t2 = System.nanoTime();
            long f = suma(datos, pool);
            long t3 = System.nanoTime();
            System.out.println("hilos       : " + hilos);
            System.out.println("secuencial  : " + s + "  " + (t1 - t0) / 1_000_000 + " ms");
            System.out.println("executor    : " + e + "  " + (t2 - t1) / 1_000_000 + " ms");
            System.out.println("forkjoin    : " + f + "  " + (t3 - t2) / 1_000_000 + " ms");
        } finally {
            pool.shutdown();
        }
    }
}
