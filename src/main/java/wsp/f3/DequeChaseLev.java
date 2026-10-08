package wsp.f3;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * F3, ejercicios 2, 3b y 4: deque de Chase y Lev (2005) con arreglo que crece.
 *
 * <p>Arreglo circular de capacidad potencia de 2 con dos índices {@code long} que solo crecen
 * (el casillero es {@code índice & máscara}). El dueño empuja y saca por {@code bottom}; los
 * ladrones roban por {@code top} con {@code compareAndSet}. Los elementos están en el rango
 * {@code [top, bottom)}.
 *
 * <p><b>Crecimiento:</b> si {@code push} encuentra el arreglo lleno, copia {@code [top, bottom)} a
 * uno del doble y lo publica ({@code setRelease}) antes de publicar {@code bottom}. Nunca se
 * encoge, y el arreglo viejo no se vacía: un ladrón que aún lo tenga puede leerlo sin riesgo y
 * lo libera el GC (DESIGN §5). Quién se lleva cada tarea lo decide el CAS sobre {@code top}, no
 * el contenido del arreglo.
 *
 * <p><b>La última tarea (DESIGN §4.2):</b> cuando {@code pop} y un ladrón van por la única tarea
 * que queda, los dos compiten con {@code compareAndSet} sobre {@code top}; el que pierde se va con
 * las manos vacías. {@code pop} restaura {@code bottom} gane o pierda.
 *
 * <p>Los modos de acceso son las hipótesis de {@code F3.md}, sin confirmar (ejercicio 6).
 * {@code push} y {@code pop} los llama solo el hilo dueño; {@code steal}, cualquiera.
 */
public final class DequeChaseLev<T> {

    private static final VarHandle BOTTOM;
    private static final VarHandle TOP;
    private static final VarHandle CASILLAS;
    private static final int CAPACIDAD_MAXIMA = 1 << 30;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            BOTTOM = l.findVarHandle(DequeChaseLev.class, "bottom", long.class);
            TOP = l.findVarHandle(DequeChaseLev.class, "top", long.class);
            CASILLAS = l.findVarHandle(DequeChaseLev.class, "casillas", Object[].class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused") // solo se accede vía BOTTOM (VarHandle)
    private long bottom;
    @SuppressWarnings("unused") // solo se accede vía TOP (VarHandle)
    private long top;

    // Solo el dueño la reemplaza (en push). Los ladrones la leen con getAcquire.
    @SuppressWarnings("unused") // solo se accede vía CASILLAS (VarHandle)
    private Object[] casillas;

    /** @param capacidad potencia de 2, al menos 2 */
    public DequeChaseLev(int capacidad) {
        if (capacidad < 2 || Integer.bitCount(capacidad) != 1) {
            throw new IllegalArgumentException("la capacidad debe ser potencia de 2 y al menos 2");
        }
        CASILLAS.set(this, new Object[capacidad]);
    }

    /** Solo el dueño. {@code null} no se admite. */
    public void push(T tarea) {
        if (tarea == null) {
            throw new NullPointerException();
        }
        long b = (long) BOTTOM.get(this); // plano: solo el dueño lo escribe
        long t = (long) TOP.getAcquire(this);
        Object[] arreglo = (Object[]) CASILLAS.get(this); // plano: solo el dueño lo reemplaza
        if (b - t >= arreglo.length) {
            arreglo = crecer(arreglo, t, b);
        }
        arreglo[(int) (b & (arreglo.length - 1))] = tarea; // plano: lo publica el release de bottom
        BOTTOM.setRelease(this, b + 1);
    }

    /** Copia {@code [t, b)} a un arreglo del doble y lo publica antes de que {@code bottom} suba. */
    private Object[] crecer(Object[] viejo, long t, long b) {
        if (viejo.length >= CAPACIDAD_MAXIMA) {
            throw new IllegalStateException("deque lleno (capacidad máxima)");
        }
        Object[] nuevo = new Object[viejo.length * 2];
        int mascaraViejo = viejo.length - 1;
        int mascaraNuevo = nuevo.length - 1;
        for (long i = t; i < b; i++) {
            nuevo[(int) (i & mascaraNuevo)] = viejo[(int) (i & mascaraViejo)];
        }
        CASILLAS.setRelease(this, nuevo); // la copia queda completa para quien lo lea con acquire
        return nuevo;
    }

    /**
     * Solo el dueño: saca la tarea más reciente, o {@code null} si está vacío (o si un ladrón se
     * llevó la última tarea antes).
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
        Object[] arreglo = (Object[]) CASILLAS.get(this);
        T tarea = (T) arreglo[(int) (b & (arreglo.length - 1))];
        if (t == b) {
            // última tarea: el ladrón puede estar yendo por la misma. Gana quien avance top.
            if (!TOP.compareAndSet(this, t, t + 1)) {
                tarea = null; // el ladrón ganó
            }
            BOTTOM.set(this, b + 1); // en los dos casos el deque queda vacío: bottom == top
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
        // Se lee después de bottom: ese acquire garantiza ver un arreglo que contiene el índice t
        Object[] arreglo = (Object[]) CASILLAS.getAcquire(this);
        T tarea = (T) arreglo[(int) (t & (arreglo.length - 1))]; // lectura con carrera; la valida el CAS
        if (!TOP.compareAndSet(this, t, t + 1)) {
            return null;
        }
        return tarea;
    }
}
