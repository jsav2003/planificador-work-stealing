package wsp.f0;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * F0, ejercicio 4: sumar un arreglo grande, secuencial contra un pool de tamaño fijo.
 *
 * El arreglo se parte en un trozo por tarea. Cada tarea suma su trozo sin compartir
 * estado y devuelve un {@code long}; el hilo principal junta los resultados con
 * {@code Future.get()}, que da happens-before entre la tarea y quien lee el resultado.
 * Se suma en {@code long} porque la suma de muchos {@code int} desborda.
 */
public final class SumaArreglo {

    private SumaArreglo() {}

    public static long secuencial(int[] datos) {
        long total = 0;
        for (int x : datos) {
            total += x;
        }
        return total;
    }

    public static long conPool(int[] datos, int hilos) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Long>> parciales = new ArrayList<>();
            int trozo = (datos.length + hilos - 1) / hilos;
            for (int desde = 0; desde < datos.length; desde += trozo) {
                int d = desde;
                int h = Math.min(desde + trozo, datos.length);
                parciales.add(pool.submit(() -> {
                    long suma = 0;
                    for (int i = d; i < h; i++) {
                        suma += datos[i];
                    }
                    return suma;
                }));
            }
            long total = 0;
            for (Future<Long> f : parciales) {
                total += f.get();
            }
            return total;
        } finally {
            pool.shutdown();
        }
    }

    /** Demo: {@code java -cp target/classes wsp.f0.SumaArreglo} tras {@code mvn compile}. */
    public static void main(String[] args) throws Exception {
        int n = 100_000_000;
        int hilos = Runtime.getRuntime().availableProcessors();
        int[] datos = new int[n];
        for (int i = 0; i < n; i++) {
            datos[i] = i % 1000;
        }
        // Una corrida de calentamiento de cada una para que el JIT compile antes de medir.
        secuencial(datos);
        conPool(datos, hilos);
        long t0 = System.nanoTime();
        long s = secuencial(datos);
        long t1 = System.nanoTime();
        long p = conPool(datos, hilos);
        long t2 = System.nanoTime();
        System.out.println("hilos     : " + hilos);
        System.out.println("secuencial: " + s + "  " + (t1 - t0) / 1_000_000 + " ms");
        System.out.println("pool      : " + p + "  " + (t2 - t1) / 1_000_000 + " ms");
    }
}
