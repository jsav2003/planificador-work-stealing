package wsp.f2;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * F2, ejercicios 1-2: cola FIFO lock-free de Michael y Scott (1996), sin límite de tamaño.
 *
 * <p>Una lista enlazada con un nodo centinela. {@code head} apunta al centinela (el último
 * nodo ya sacado); el primer elemento real es {@code head.next}. {@code tail} apunta al último
 * nodo o, a lo sumo, uno antes: añadir son dos pasos (enlazar {@code next} y mover
 * {@code tail}) y entre ellos la cola queda "atrasada". Cualquier hilo que la encuentre así
 * la adelanta él mismo (ayuda); por eso ningún hilo espera a otro: es lock-free.
 *
 * <p>Los accesos están debilitados con {@code VarHandle}: lecturas {@code getAcquire}, avance de
 * {@code tail} con {@code weakCompareAndSetRelease}, y CAS fuerte solo donde un elemento entra o sale
 * de la cola. La justificación de cada modo está en {@code F2.md}.
 *
 * <p>Sin problema ABA: los nodos no se reutilizan y el GC no recicla uno mientras algún hilo
 * conserve una referencia (DESIGN §5).
 */
@SuppressWarnings("unchecked")
public final class ColaMichaelScott<T> {

    private static final class Nodo<T> {
        final T item;
        @SuppressWarnings("unused") // solo se accede vía SIGUIENTE (VarHandle)
        Nodo<T> siguiente;

        Nodo(T item) {
            this.item = item;
        }
    }

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle SIGUIENTE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(ColaMichaelScott.class, "head", Nodo.class);
            TAIL = l.findVarHandle(ColaMichaelScott.class, "tail", Nodo.class);
            SIGUIENTE = l.findVarHandle(Nodo.class, "siguiente", Nodo.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Solo se tocan a través de HEAD/TAIL: lectura getAcquire, cambio con CAS.
    @SuppressWarnings("unused") // solo se accede vía HEAD (VarHandle)
    private Nodo<T> head;
    @SuppressWarnings("unused") // solo se accede vía TAIL (VarHandle)
    private Nodo<T> tail;

    public ColaMichaelScott() {
        Nodo<T> centinela = new Nodo<>(null);
        HEAD.set(this, centinela);
        TAIL.set(this, centinela);
    }

    /** Añade {@code valor} al final. Nunca bloquea; {@code null} no se admite. */
    public void encolar(T valor) {
        if (valor == null) {
            throw new NullPointerException();
        }
        Nodo<T> nuevo = new Nodo<>(valor);
        while (true) {
            Nodo<T> t = (Nodo<T>) TAIL.getAcquire(this);
            Nodo<T> sig = (Nodo<T>) SIGUIENTE.getAcquire(t);
            if (t != TAIL.getAcquire(this)) {
                continue; // tail cambió mientras leíamos: lo leído ya no sirve
            }
            if (sig == null) {
                // t es de verdad el último: intentar enlazar el nodo nuevo
                if (SIGUIENTE.compareAndSet(t, null, nuevo)) {
                    // éxito: el elemento ya está en la cola. Mover tail es opcional;
                    // si falla es que otro hilo ya lo adelantó
                    TAIL.weakCompareAndSetRelease(this, t, nuevo);
                    return;
                }
            } else {
                // tail atrasada: ayudar a adelantarla y reintentar
                TAIL.weakCompareAndSetRelease(this, t, sig);
            }
        }
    }

    /** Saca el primero, o devuelve {@code null} si la cola está vacía. Nunca bloquea. */
    public T desencolar() {
        while (true) {
            Nodo<T> h = (Nodo<T>) HEAD.getAcquire(this);
            Nodo<T> t = (Nodo<T>) TAIL.getAcquire(this);
            Nodo<T> primero = (Nodo<T>) SIGUIENTE.getAcquire(h);
            if (h != HEAD.getAcquire(this)) {
                continue; // instantánea inconsistente
            }
            if (primero == null) {
                return null; // vacía
            }
            if (h == t) {
                // hay un elemento enlazado pero tail se quedó atrás: adelantarla
                // antes de sacar, para que tail nunca quede por detrás de head
                TAIL.weakCompareAndSetRelease(this, t, primero);
                continue;
            }
            // item es final: se lee antes del CAS sin riesgo de ver nada a medias.
            // Si el CAS gana, primero pasa a ser el nuevo centinela.
            T valor = primero.item;
            if (HEAD.compareAndSet(this, h, primero)) {
                return valor;
            }
        }
    }

    /** Sin elementos en este instante. En concurrencia el resultado puede quedar viejo. */
    public boolean estaVacia() {
        return SIGUIENTE.getAcquire(HEAD.getAcquire(this)) == null;
    }
}
