package wsp.f0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SumaArregloTest {

    private static int[] aleatorio(int n) {
        Random r = new Random(42);
        int[] datos = new int[n];
        for (int i = 0; i < n; i++) {
            datos[i] = r.nextInt();
        }
        return datos;
    }

    @Test
    void poolIgualASecuencial() throws Exception {
        int[] datos = aleatorio(1_000_003);
        assertEquals(SumaArreglo.secuencial(datos), SumaArreglo.conPool(datos, 4));
    }

    @Test
    void casosBorde() throws Exception {
        assertEquals(0, SumaArreglo.conPool(new int[0], 4));
        assertEquals(7, SumaArreglo.conPool(new int[] {7}, 4));
        int[] chico = {1, 2, 3};
        assertEquals(6, SumaArreglo.conPool(chico, 8));
    }
}
