package wsp.f0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Test;

class SumaForkJoinTest {

    @Test
    void igualASecuencial() {
        Random r = new Random(42);
        int[] datos = new int[1_000_003];
        for (int i = 0; i < datos.length; i++) {
            datos[i] = r.nextInt();
        }
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            assertEquals(SumaArreglo.secuencial(datos), SumaForkJoin.suma(datos, pool));
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void casosBorde() {
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            assertEquals(0, SumaForkJoin.suma(new int[0], pool));
            assertEquals(7, SumaForkJoin.suma(new int[] {7}, pool));
        } finally {
            pool.shutdown();
        }
    }
}
