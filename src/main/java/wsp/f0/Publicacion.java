package wsp.f0;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * F0, ejercicio 6: publicar un objeto entre hilos.
 *
 * Un hilo llena un {@link Dato} (dos campos normales) y luego lo publica en una
 * referencia compartida; otro hilo espera a verla y lee los campos. Para que el lector
 * vea los campos ya escritos hace falta happens-before entre "llenar" y "leer", y eso
 * depende de cómo se publique la referencia (DESIGN §3.2):
 *
 *  - {@link Variante#VOLATILE}: escritura y lectura volatile. Es la garantía completa.
 *  - {@link Variante#RELEASE_ACQUIRE}: {@code setRelease} / {@code getAcquire}. Lo mismo
 *    para este patrón, con menos coste: es el modo que usará el deque (F3).
 *  - {@link Variante#NORMAL}: sin nada. Carrera de datos: el lector puede no ver nunca
 *    la referencia, o verla con los campos aún en cero. Incorrecto a propósito.
 */
public final class Publicacion {

    private Publicacion() {}

    public enum Variante { VOLATILE, RELEASE_ACQUIRE, NORMAL }

    public enum Resultado { NO_VISTO, INCONSISTENTE, CORRECTO }

    /** Campos normales a propósito: es lo que la publicación tiene que proteger. */
    static final class Dato {
        int a;
        int b;
    }

    /** Referencia compartida. Solo se toca a través de los métodos de abajo. */
    static final class Buzon {
        private static final VarHandle DATO;

        static {
            try {
                DATO = MethodHandles.lookup().findVarHandle(Buzon.class, "dato", Dato.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Dato dato;

        void publicar(Variante v, Dato d) {
            switch (v) {
                case VOLATILE -> DATO.setVolatile(this, d);
                case RELEASE_ACQUIRE -> DATO.setRelease(this, d);
                case NORMAL -> dato = d;
            }
        }

        Dato leer(Variante v) {
            return switch (v) {
                case VOLATILE -> (Dato) DATO.getVolatile(this);
                case RELEASE_ACQUIRE -> (Dato) DATO.getAcquire(this);
                case NORMAL -> dato;
            };
        }
    }

    /** Vueltas máximas del lector esperando la referencia. Acota también la variante NORMAL. */
    static final int ESPERA_MAXIMA = 50_000_000;

    /**
     * Una prueba: un hilo publica {@code Dato(1, 2)} y otro lo lee. El resultado es
     * CORRECTO solo si el lector ve la referencia y ambos campos ya escritos.
     */
    public static Resultado probar(Variante v) throws InterruptedException {
        Buzon buzon = new Buzon();
        Resultado[] salida = new Resultado[1]; // se lee tras join(): happens-before
        Thread lector = new Thread(() -> {
            Dato d = null;
            for (int i = 0; i < ESPERA_MAXIMA && (d = buzon.leer(v)) == null; i++) {
                Thread.onSpinWait();
            }
            if (d == null) {
                salida[0] = Resultado.NO_VISTO;
            } else {
                salida[0] = (d.a == 1 && d.b == 2) ? Resultado.CORRECTO : Resultado.INCONSISTENTE;
            }
        });
        Thread escritor = new Thread(() -> {
            Dato d = new Dato();
            d.a = 1;
            d.b = 2;
            buzon.publicar(v, d);
        });
        lector.start();
        escritor.start();
        escritor.join();
        lector.join();
        return salida[0];
    }

    /** Demo: {@code java -cp target/classes wsp.f0.Publicacion [pruebas]} tras {@code mvn compile}. */
    public static void main(String[] args) throws InterruptedException {
        int pruebas = args.length > 0 ? Integer.parseInt(args[0]) : 20_000;
        for (Variante v : Variante.values()) {
            int[] cuenta = new int[Resultado.values().length];
            for (int i = 0; i < pruebas; i++) {
                cuenta[probar(v).ordinal()]++;
            }
            System.out.printf("%-16s", v);
            for (Resultado r : Resultado.values()) {
                System.out.printf("  %s=%d", r, cuenta[r.ordinal()]);
            }
            System.out.println();
        }
    }
}
