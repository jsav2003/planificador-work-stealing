package wsp.f3;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * F3, ejercicio 2: deque de Chase y Lev (2005) de tamaño fijo, versión incompleta a propósito.
 *
 * <p>Arreglo circular de capacidad potencia de 2 con dos índices {@code long} que solo crecen
 * (el casillero es {@code índice & máscara}). El dueño empuja y saca por {@code bottom}; los
 * ladrones roban por {@code top} con {@code compareAndSet}. Los elementos están en el rango
 * {@code [top, bottom)}.
 *
 * <p><b>Incompleto:</b> {@code pop} solo tiene el camino rápido. Con una única tarea en disputa
 * contra un ladrón puede devolverla dos veces (DESIGN §4.2); el ejercicio 3b lo corrige. Además
 * {@code push} lanza {@link IllegalStateException} si está lleno; el crecimiento es el ejercicio 4.
 *
 * <p>Los modos de acceso son las hipótesis de {@code F3.md}, sin confirmar (ejercicio 6).
 * {@code push} y {@code pop} los llama solo el hilo dueño; {@code steal}, cualquiera.
 */
public final class DequeChaseLev<T> {

    private static final VarHandle BOTTOM;
    private static final VarHandle TOP;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            BOTTOM = l.findVarHandle(DequeChaseLev.class, "bottom", long.class);
            TOP = l.findVarHandle(DequeChaseLev.class, "top", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused") // solo se accede vía BOTTOM (VarHandle)
    private long bottom;
    @SuppressWarnings("unused") // solo se accede vía TOP (VarHandle)
    private long top;

    private final Object[] casillas;
    private final int mascara;

    /** @param capacidad potencia de 2, al menos 2 */
    public DequeChaseLev(int capacidad) {
        if (capacidad < 2 || Integer.bitCount(capacidad) != 1) {
            throw new IllegalArgumentException("la capacidad debe ser potencia de 2 y al menos 2");
        }
        casillas = new Object[capacidad];
        mascara = capacidad - 1;
    }

    /** Solo el dueño. {@code null} no se admite. */
    public void push(T tarea) {
        if (tarea == null) {
            throw new NullPointerException();
        }
        long b = (long) BOTTOM.get(this); // plano: solo el dueño lo escribe
        long t = (long) TOP.getAcquire(this);
        if (b - t >= casillas.length) {
            throw new IllegalStateException("deque lleno");
        }
        casillas[(int) (b & mascara)] = tarea; // plano: lo publica el release de bottom
        BOTTOM.setRelease(this, b + 1);
    }

    /**
     * Solo el dueño: saca la tarea más reciente, o {@code null} si está vacío.
     * Solo camino rápido: no es seguro si un ladrón compite por la última tarea.
     */
    @SuppressWarnings("unchecked")
    public T pop() {
        long b = (long) BOTTOM.get(this) - 1;
        BOTTOM.setVolatile(this, b);
        long t = (long) TOP.getVolatile(this);
        if (t > b) {
            BOTTOM.set(this, b + 1); // estaba vacío: restaurar bottom
            return null;
        }
        T tarea = (T) casillas[(int) (b & mascara)];
        if (t == b) {
            // última tarea: consumirla (top = t + 1) y dejar bottom == top. Aquí falta el CAS
            // contra el ladrón: con una escritura normal dos hilos podrían llevarse la misma
            // tarea (ejercicio 3b)
            TOP.set(this, t + 1);
            BOTTOM.set(this, b + 1);
        }
        return tarea;
    }

    /**
     * Cualquier hilo: roba la tarea más antigua. Devuelve {@code null} si está vacío
     * o si otro hilo ganó la carrera por esa tarea (no reintenta).
     */
    @SuppressWarnings("unchecked")
    public T steal() {
        long t = (long) TOP.getAcquire(this);
        VarHandle.fullFence(); // hipótesis de Lê et al.; el porqué se confirma con el 3a
        long b = (long) BOTTOM.getAcquire(this);
        if (t >= b) {
            return null;
        }
        T tarea = (T) casillas[(int) (t & mascara)]; // lectura con carrera; la valida el CAS
        if (!TOP.compareAndSet(this, t, t + 1)) {
            return null;
        }
        return tarea;
    }
}
