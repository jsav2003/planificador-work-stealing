package wsp.f1;

/**
 * F1, ejercicio 4: atomicidad, visibilidad y orden son tres problemas distintos.
 * Cada método reproduce uno; {@code volatile} arregla unos y otros no.
 *
 *  - Atomicidad ({@link #atomicidad}): {@code contador++} son tres pasos (leer, sumar,
 *    escribir). Dos hilos se pisan aunque el campo sea {@code volatile}: volatile da
 *    visibilidad y orden, no atomicidad. Arreglo: {@code AtomicInteger} / {@code getAndAdd}.
 *  - Visibilidad ({@link #visibilidad}): un hilo escribe una bandera y otro nunca la ve.
 *    El JIT puede sacar la lectura del bucle. Arreglo: {@code volatile}.
 *  - Orden: no se repite aquí, ya está en {@link CerosMutuos}. Cada hilo hace una escritura
 *    y luego lee *otra* variable, y las dos operaciones se ven al revés. Arreglo:
 *    {@code volatile} en ambas variables. (El orden escritura-escritura, que sería el
 *    caso clásico de publicación, en x86 no se deja ver: ver F0/Publicacion.)
 */
public final class TresFallos {

    private TresFallos() {}

    private static volatile int contadorVolatile;
    private static int contadorNormal;

    /** Cuatro hilos hacen {@code veces} incrementos cada uno. Devuelve el total obtenido. */
    public static int atomicidad(boolean usarVolatile, int veces) throws InterruptedException {
        contadorVolatile = 0;
        contadorNormal = 0;
        Thread[] hilos = new Thread[4];
        for (int t = 0; t < hilos.length; t++) {
            hilos[t] = new Thread(() -> {
                for (int i = 0; i < veces; i++) {
                    if (usarVolatile) {
                        contadorVolatile++; // leer, sumar, escribir: no es atómico
                    } else {
                        contadorNormal++;
                    }
                }
            });
            hilos[t].start();
        }
        for (Thread h : hilos) {
            h.join();
        }
        return usarVolatile ? contadorVolatile : contadorNormal;
    }

    private static boolean paraNormal;
    private static volatile boolean paraVolatile;

    /**
     * Un hilo gira hasta ver la bandera; el principal la levanta a los {@code esperaMs} y
     * espera hasta {@code limiteMs} a que el hilo termine. Devuelve {@code true} si el hilo
     * terminó (vio la bandera); {@code false} si sigue girando (nunca la vio: el bug).
     * El hilo es demonio, así que si se queda atascado no impide cerrar la JVM.
     */
    public static boolean visibilidad(boolean usarVolatile, long esperaMs, long limiteMs)
            throws InterruptedException {
        paraNormal = false;
        paraVolatile = false;
        Thread lector = new Thread(() -> {
            if (usarVolatile) {
                while (!paraVolatile) {
                    // espera activa
                }
            } else {
                while (!paraNormal) {
                    // espera activa
                }
            }
        });
        lector.setDaemon(true);
        lector.start();
        Thread.sleep(esperaMs);
        if (usarVolatile) {
            paraVolatile = true;
        } else {
            paraNormal = true;
        }
        lector.join(limiteMs);
        return !lector.isAlive();
    }

    /** Demo: {@code java -cp target/classes wsp.f1.TresFallos}. */
    public static void main(String[] args) throws InterruptedException {
        int veces = 1_000_000;
        System.out.println("Atomicidad (esperado " + 4 * veces + "):");
        System.out.println("  campo normal    " + atomicidad(false, veces));
        System.out.println("  campo volatile  " + atomicidad(true, veces));
        System.out.println("Visibilidad (el lector ve la bandera?):");
        System.out.println("  campo normal    " + visibilidad(false, 200, 2000));
        System.out.println("  campo volatile  " + visibilidad(true, 200, 2000));
    }
}
